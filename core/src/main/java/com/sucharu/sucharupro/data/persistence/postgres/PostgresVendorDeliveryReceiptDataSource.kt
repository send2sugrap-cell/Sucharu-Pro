package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.VendorDeliveryReceiptDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.sql.ResultSet
import java.sql.Timestamp

class PostgresVendorDeliveryReceiptDataSource(
    private val transactionManager: TransactionManager,
    private val defaultTenantId: String = "TENANT-001"
) : VendorDeliveryReceiptDataSource {

    private val receiptFlows = mutableMapOf<String, MutableStateFlow<List<VendorDeliveryReceipt>>>()

    private fun mapItemRow(rs: ResultSet): VendorDeliveryReceiptItem {
        return VendorDeliveryReceiptItem(
            receiptItemId = rs.getString("receipt_item_id"),
            deliveryReceiptId = rs.getString("delivery_receipt_id"),
            purchaseOrderId = rs.getString("purchase_order_id"),
            purchaseOrderItemId = rs.getString("purchase_order_item_id"),
            itemDescription = rs.getString("item_description"),
            itemCode = rs.getString("item_code"),
            orderedQuantity = rs.getBigDecimal("ordered_quantity"),
            previouslyReceivedQuantity = rs.getBigDecimal("previously_received_quantity"),
            receivedQuantity = rs.getBigDecimal("received_quantity"),
            acceptedQuantity = rs.getBigDecimal("accepted_quantity"),
            rejectedQuantity = rs.getBigDecimal("rejected_quantity"),
            damagedQuantity = rs.getBigDecimal("damaged_quantity"),
            shortQuantity = rs.getBigDecimal("short_quantity"),
            excessQuantity = rs.getBigDecimal("excess_quantity"),
            unitOfMeasure = UnitOfMeasure.valueOf(rs.getString("unit_of_measure")),
            unitRate = Money(rs.getBigDecimal("unit_rate")),
            taxAmount = Money(rs.getBigDecimal("tax_amount")),
            lineTotal = Money(rs.getBigDecimal("line_total")),
            remarks = rs.getString("remarks"),
            version = rs.getLong("version")
        )
    }

    private fun mapReceiptRow(rs: ResultSet, items: List<VendorDeliveryReceiptItem> = emptyList()): VendorDeliveryReceipt {
        return VendorDeliveryReceipt(
            deliveryReceiptId = rs.getString("delivery_receipt_id"),
            projectId = rs.getString("project_id"),
            tenantId = rs.getString("tenant_id") ?: defaultTenantId,
            receiptNumber = rs.getString("receipt_number"),
            purchaseOrderId = rs.getString("purchase_order_id"),
            vendorId = rs.getString("vendor_id"),
            vendorDeliveryReference = rs.getString("vendor_delivery_reference"),
            receiptDate = rs.getTimestamp("receipt_date")?.time ?: System.currentTimeMillis(),
            receivedAt = rs.getTimestamp("received_at")?.time,
            receivedBy = rs.getString("received_by"),
            status = VendorDeliveryReceiptStatus.valueOf(rs.getString("status")),
            warehouseId = rs.getString("warehouse_id"),
            remarks = rs.getString("remarks"),
            items = items,
            createdAt = rs.getTimestamp("created_at")?.time ?: System.currentTimeMillis(),
            createdBy = rs.getString("created_by"),
            updatedAt = rs.getTimestamp("updated_at")?.time ?: System.currentTimeMillis(),
            updatedBy = rs.getString("updated_by"),
            version = rs.getLong("version")
        )
    }

    private fun mapAuditRow(rs: ResultSet): VendorDeliveryReceiptAuditEvent {
        return VendorDeliveryReceiptAuditEvent(
            auditId = rs.getString("audit_id"),
            projectId = rs.getString("project_id"),
            deliveryReceiptId = rs.getString("delivery_receipt_id"),
            purchaseOrderId = rs.getString("purchase_order_id"),
            eventType = rs.getString("event_type"),
            actorId = rs.getString("actor_id"),
            correlationId = rs.getString("correlation_id"),
            occurredAt = rs.getTimestamp("occurred_at")?.time ?: System.currentTimeMillis(),
            details = rs.getString("details")
        )
    }

    override fun observeDeliveryReceipts(projectId: String, vendorId: String?, purchaseOrderId: String?): Flow<List<VendorDeliveryReceipt>> {
        val key = "$projectId:$vendorId:$purchaseOrderId"
        return synchronized(receiptFlows) {
            receiptFlows.getOrPut(key) { MutableStateFlow(emptyList()) }.asStateFlow()
        }
    }

    override suspend fun findById(projectId: String, deliveryReceiptId: String): DomainResult<VendorDeliveryReceipt> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val receipt = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = "SELECT * FROM vendor_delivery_receipts WHERE project_id = ? AND delivery_receipt_id = ?"
                val rawReceipt = ctx.sqlExecutor.querySingleOrNull(sql, listOf(tenant.projectId, deliveryReceiptId)) { rs -> mapReceiptRow(rs) }
                if (rawReceipt != null) {
                    val itemSql = "SELECT * FROM vendor_delivery_receipt_items WHERE project_id = ? AND delivery_receipt_id = ? ORDER BY receipt_item_id ASC"
                    val items = ctx.sqlExecutor.queryList(itemSql, listOf(tenant.projectId, deliveryReceiptId)) { rs -> mapItemRow(rs) }
                    rawReceipt.copy(items = items)
                } else null
            }
            if (receipt != null) {
                DomainResult.Success(receipt)
            } else {
                DomainResult.Error(NoSuchElementException("Vendor delivery receipt '$deliveryReceiptId' not found in project '${tenant.projectId}'."))
            }
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "find delivery receipt by ID")
        }
    }

    override suspend fun findByReceiptNumber(projectId: String, receiptNumber: String): DomainResult<VendorDeliveryReceipt> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val receipt = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = "SELECT * FROM vendor_delivery_receipts WHERE project_id = ? AND receipt_number = ?"
                val rawReceipt = ctx.sqlExecutor.querySingleOrNull(sql, listOf(tenant.projectId, receiptNumber)) { rs -> mapReceiptRow(rs) }
                if (rawReceipt != null) {
                    val itemSql = "SELECT * FROM vendor_delivery_receipt_items WHERE project_id = ? AND delivery_receipt_id = ? ORDER BY receipt_item_id ASC"
                    val items = ctx.sqlExecutor.queryList(itemSql, listOf(tenant.projectId, rawReceipt.deliveryReceiptId)) { rs -> mapItemRow(rs) }
                    rawReceipt.copy(items = items)
                } else null
            }
            if (receipt != null) {
                DomainResult.Success(receipt)
            } else {
                DomainResult.Error(NoSuchElementException("Vendor delivery receipt '$receiptNumber' not found in project '${tenant.projectId}'."))
            }
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "find delivery receipt by number")
        }
    }

    override suspend fun list(
        projectId: String,
        vendorId: String?,
        purchaseOrderId: String?,
        status: VendorDeliveryReceiptStatus?
    ): DomainResult<List<VendorDeliveryReceipt>> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val receipts = transactionManager.inReadOnly(tenant) { ctx ->
                val sb = StringBuilder("SELECT * FROM vendor_delivery_receipts WHERE project_id = ?")
                val params = mutableListOf<Any?>(tenant.projectId)

                if (vendorId != null) {
                    sb.append(" AND vendor_id = ?")
                    params.add(vendorId)
                }
                if (purchaseOrderId != null) {
                    sb.append(" AND purchase_order_id = ?")
                    params.add(purchaseOrderId)
                }
                if (status != null) {
                    sb.append(" AND status = ?")
                    params.add(status.name)
                }
                sb.append(" ORDER BY created_at DESC")

                val rawReceipts = ctx.sqlExecutor.queryList(sb.toString(), params) { rs -> mapReceiptRow(rs) }
                rawReceipts.map { r ->
                    val itemSql = "SELECT * FROM vendor_delivery_receipt_items WHERE project_id = ? AND delivery_receipt_id = ? ORDER BY receipt_item_id ASC"
                    val items = ctx.sqlExecutor.queryList(itemSql, listOf(tenant.projectId, r.deliveryReceiptId)) { rs -> mapItemRow(rs) }
                    r.copy(items = items)
                }
            }
            DomainResult.Success(receipts)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "list vendor delivery receipts")
        }
    }

    override suspend fun createReceipt(receipt: VendorDeliveryReceipt): DomainResult<VendorDeliveryReceipt> {
        val tenant = TenantContext(receipt.projectId.ifBlank { defaultTenantId })
        return try {
            val created = transactionManager.inTransaction(tenant) { ctx ->
                val now = Timestamp(System.currentTimeMillis())
                val insertSql = """
                    INSERT INTO vendor_delivery_receipts (
                        project_id, delivery_receipt_id, tenant_id, receipt_number, purchase_order_id,
                        vendor_id, vendor_delivery_reference, receipt_date, received_at, received_by,
                        status, warehouse_id, remarks, created_at, created_by, updated_at, updated_by, version
                    ) VALUES (
                        ?, ?, ?, ?, ?,
                        ?, ?, ?, ?, ?,
                        ?, ?, ?, ?, ?, ?, ?, 1
                    )
                """.trimIndent()

                val params = listOf(
                    tenant.projectId,
                    receipt.deliveryReceiptId,
                    receipt.tenantId,
                    receipt.receiptNumber,
                    receipt.purchaseOrderId,
                    receipt.vendorId,
                    receipt.vendorDeliveryReference,
                    Timestamp(receipt.receiptDate),
                    receipt.receivedAt?.let { Timestamp(it) },
                    receipt.receivedBy,
                    receipt.status.name,
                    receipt.warehouseId,
                    receipt.remarks,
                    now,
                    receipt.createdBy,
                    now,
                    receipt.updatedBy
                )

                ctx.sqlExecutor.executeUpdate(insertSql, params)

                val insertItemSql = """
                    INSERT INTO vendor_delivery_receipt_items (
                        project_id, receipt_item_id, delivery_receipt_id, purchase_order_id, purchase_order_item_id,
                        item_description, item_code, ordered_quantity, previously_received_quantity, received_quantity,
                        accepted_quantity, rejected_quantity, damaged_quantity, short_quantity, excess_quantity,
                        unit_of_measure, unit_rate, tax_amount, line_total, remarks, version
                    ) VALUES (
                        ?, ?, ?, ?, ?,
                        ?, ?, ?, ?, ?,
                        ?, ?, ?, ?, ?,
                        ?, ?, ?, ?, ?, 1
                    )
                """.trimIndent()

                for (item in receipt.items) {
                    val itemParams = listOf(
                        tenant.projectId,
                        item.receiptItemId,
                        receipt.deliveryReceiptId,
                        item.purchaseOrderId,
                        item.purchaseOrderItemId,
                        item.itemDescription,
                        item.itemCode,
                        item.orderedQuantity,
                        item.previouslyReceivedQuantity,
                        item.receivedQuantity,
                        item.acceptedQuantity,
                        item.rejectedQuantity,
                        item.damagedQuantity,
                        item.shortQuantity,
                        item.excessQuantity,
                        item.unitOfMeasure.name,
                        item.unitRate.amount,
                        item.taxAmount.amount,
                        item.lineTotal.amount,
                        item.remarks
                    )
                    ctx.sqlExecutor.executeUpdate(insertItemSql, itemParams)
                }

                receipt.copy(createdAt = now.time, updatedAt = now.time, version = 1L)
            }
            DomainResult.Success(created)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "create vendor delivery receipt")
        }
    }

    override suspend fun updateReceipt(receipt: VendorDeliveryReceipt): DomainResult<VendorDeliveryReceipt> {
        val tenant = TenantContext(receipt.projectId.ifBlank { defaultTenantId })
        return try {
            val updated = transactionManager.inTransaction(tenant) { ctx ->
                val now = Timestamp(System.currentTimeMillis())
                val updateSql = """
                    UPDATE vendor_delivery_receipts SET
                        status = ?,
                        vendor_delivery_reference = ?,
                        warehouse_id = ?,
                        remarks = ?,
                        updated_at = ?,
                        updated_by = ?,
                        version = version + 1
                    WHERE project_id = ? AND delivery_receipt_id = ? AND version = ?
                """.trimIndent()

                val params = listOf(
                    receipt.status.name,
                    receipt.vendorDeliveryReference,
                    receipt.warehouseId,
                    receipt.remarks,
                    now,
                    receipt.updatedBy,
                    tenant.projectId,
                    receipt.deliveryReceiptId,
                    receipt.version
                )

                val rows = ctx.sqlExecutor.executeUpdate(updateSql, params)
                if (rows == 0) {
                    throw IllegalStateException("Optimistic lock failure or receipt not found: '${receipt.deliveryReceiptId}'.")
                }

                // Delete and re-insert items
                ctx.sqlExecutor.executeUpdate(
                    "DELETE FROM vendor_delivery_receipt_items WHERE project_id = ? AND delivery_receipt_id = ?",
                    listOf(tenant.projectId, receipt.deliveryReceiptId)
                )

                val insertItemSql = """
                    INSERT INTO vendor_delivery_receipt_items (
                        project_id, receipt_item_id, delivery_receipt_id, purchase_order_id, purchase_order_item_id,
                        item_description, item_code, ordered_quantity, previously_received_quantity, received_quantity,
                        accepted_quantity, rejected_quantity, damaged_quantity, short_quantity, excess_quantity,
                        unit_of_measure, unit_rate, tax_amount, line_total, remarks, version
                    ) VALUES (
                        ?, ?, ?, ?, ?,
                        ?, ?, ?, ?, ?,
                        ?, ?, ?, ?, ?,
                        ?, ?, ?, ?, ?, 1
                    )
                """.trimIndent()

                for (item in receipt.items) {
                    val itemParams = listOf(
                        tenant.projectId,
                        item.receiptItemId,
                        receipt.deliveryReceiptId,
                        item.purchaseOrderId,
                        item.purchaseOrderItemId,
                        item.itemDescription,
                        item.itemCode,
                        item.orderedQuantity,
                        item.previouslyReceivedQuantity,
                        item.receivedQuantity,
                        item.acceptedQuantity,
                        item.rejectedQuantity,
                        item.damagedQuantity,
                        item.shortQuantity,
                        item.excessQuantity,
                        item.unitOfMeasure.name,
                        item.unitRate.amount,
                        item.taxAmount.amount,
                        item.lineTotal.amount,
                        item.remarks
                    )
                    ctx.sqlExecutor.executeUpdate(insertItemSql, itemParams)
                }

                receipt.copy(version = receipt.version + 1L, updatedAt = now.time)
            }
            DomainResult.Success(updated)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "update vendor delivery receipt")
        }
    }

    override suspend fun updateStatus(
        projectId: String,
        deliveryReceiptId: String,
        status: VendorDeliveryReceiptStatus,
        updatedBy: String
    ): DomainResult<VendorDeliveryReceipt> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val updated = transactionManager.inTransaction(tenant) { ctx ->
                val now = Timestamp(System.currentTimeMillis())
                val updateSql = """
                    UPDATE vendor_delivery_receipts SET
                        status = ?,
                        updated_at = ?,
                        updated_by = ?,
                        version = version + 1
                    WHERE project_id = ? AND delivery_receipt_id = ?
                """.trimIndent()

                val params = listOf(status.name, now, updatedBy, tenant.projectId, deliveryReceiptId)
                val rows = ctx.sqlExecutor.executeUpdate(updateSql, params)
                if (rows == 0) {
                    throw NoSuchElementException("Vendor delivery receipt '$deliveryReceiptId' not found.")
                }

                val findSql = "SELECT * FROM vendor_delivery_receipts WHERE project_id = ? AND delivery_receipt_id = ?"
                val rawReceipt = ctx.sqlExecutor.querySingleOrNull(findSql, listOf(tenant.projectId, deliveryReceiptId)) { rs -> mapReceiptRow(rs) }
                    ?: throw NoSuchElementException("Failed to retrieve updated delivery receipt '$deliveryReceiptId'.")

                val itemSql = "SELECT * FROM vendor_delivery_receipt_items WHERE project_id = ? AND delivery_receipt_id = ? ORDER BY receipt_item_id ASC"
                val items = ctx.sqlExecutor.queryList(itemSql, listOf(tenant.projectId, deliveryReceiptId)) { rs -> mapItemRow(rs) }
                rawReceipt.copy(items = items)
            }
            DomainResult.Success(updated)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "update vendor delivery receipt status")
        }
    }

    override suspend fun appendAudit(auditEvent: VendorDeliveryReceiptAuditEvent): DomainResult<VendorDeliveryReceiptAuditEvent> {
        val tenant = TenantContext(auditEvent.projectId.ifBlank { defaultTenantId })
        return try {
            val created = transactionManager.inTransaction(tenant) { ctx ->
                val now = Timestamp(auditEvent.occurredAt)
                val sql = """
                    INSERT INTO vendor_delivery_receipt_audits (
                        project_id, audit_id, delivery_receipt_id, purchase_order_id, event_type,
                        actor_id, correlation_id, occurred_at, details
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()

                val params = listOf(
                    tenant.projectId,
                    auditEvent.auditId,
                    auditEvent.deliveryReceiptId,
                    auditEvent.purchaseOrderId,
                    auditEvent.eventType,
                    auditEvent.actorId,
                    auditEvent.correlationId,
                    now,
                    auditEvent.details
                )
                ctx.sqlExecutor.executeUpdate(sql, params)
                auditEvent
            }
            DomainResult.Success(created)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "append delivery receipt audit event")
        }
    }

    override suspend fun listAudits(projectId: String, deliveryReceiptId: String): DomainResult<List<VendorDeliveryReceiptAuditEvent>> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val list = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = """
                    SELECT * FROM vendor_delivery_receipt_audits
                    WHERE project_id = ? AND delivery_receipt_id = ?
                    ORDER BY occurred_at ASC
                """.trimIndent()
                ctx.sqlExecutor.queryList(sql, listOf(tenant.projectId, deliveryReceiptId)) { rs -> mapAuditRow(rs) }
            }
            DomainResult.Success(list)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "list delivery receipt audits")
        }
    }
}

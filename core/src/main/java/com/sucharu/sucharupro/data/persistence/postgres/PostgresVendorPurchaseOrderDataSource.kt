package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.VendorPurchaseOrderDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.sql.ResultSet
import java.sql.Timestamp

class PostgresVendorPurchaseOrderDataSource(
    private val transactionManager: TransactionManager,
    private val defaultTenantId: String = "TENANT-001"
) : VendorPurchaseOrderDataSource {

    private val orderFlows = mutableMapOf<String, MutableStateFlow<List<VendorPurchaseOrder>>>()

    private fun mapItemRow(rs: ResultSet): VendorPurchaseOrderItem {
        return VendorPurchaseOrderItem(
            itemId = rs.getString("item_id"),
            purchaseOrderId = rs.getString("purchase_order_id"),
            vendorServiceRateId = rs.getString("vendor_service_rate_id"),
            capabilityType = rs.getString("capability_type")?.let { runCatching { CapabilityType.valueOf(it) }.getOrNull() },
            itemDescription = rs.getString("item_description"),
            itemCode = rs.getString("item_code"),
            quantity = rs.getBigDecimal("quantity"),
            unitOfMeasure = UnitOfMeasure.valueOf(rs.getString("unit_of_measure")),
            unitRate = Money(rs.getBigDecimal("unit_rate")),
            pricingMethod = PricingMethod.valueOf(rs.getString("pricing_method")),
            currency = rs.getString("currency"),
            discount = Money(rs.getBigDecimal("discount")),
            taxAmount = Money(rs.getBigDecimal("tax_amount")),
            lineTotal = Money(rs.getBigDecimal("line_total")),
            expectedDeliveryDate = rs.getTimestamp("expected_delivery_date")?.time,
            notes = rs.getString("notes"),
            sourceWorkOrderId = rs.getString("source_work_order_id"),
            version = rs.getLong("version")
        )
    }

    private fun mapOrderRow(rs: ResultSet, items: List<VendorPurchaseOrderItem> = emptyList()): VendorPurchaseOrder {
        return VendorPurchaseOrder(
            purchaseOrderId = rs.getString("purchase_order_id"),
            projectId = rs.getString("project_id"),
            orderNumber = rs.getString("order_number"),
            vendorId = rs.getString("vendor_id"),
            status = VendorPurchaseOrderStatus.valueOf(rs.getString("status")),
            orderDate = rs.getTimestamp("order_date")?.time ?: System.currentTimeMillis(),
            requestedBy = rs.getString("requested_by"),
            approvedBy = rs.getString("approved_by"),
            approvedAt = rs.getTimestamp("approved_at")?.time,
            issuedBy = rs.getString("issued_by"),
            issuedAt = rs.getTimestamp("issued_at")?.time,
            expectedDeliveryDate = rs.getTimestamp("expected_delivery_date")?.time,
            deliveryLocation = rs.getString("delivery_location"),
            currency = rs.getString("currency"),
            subtotal = Money(rs.getBigDecimal("subtotal")),
            taxAmount = Money(rs.getBigDecimal("tax_amount")),
            discountAmount = Money(rs.getBigDecimal("discount_amount")),
            totalAmount = Money(rs.getBigDecimal("total_amount")),
            notes = rs.getString("notes"),
            sourceReferenceType = rs.getString("source_reference_type"),
            sourceReferenceId = rs.getString("source_reference_id"),
            items = items,
            createdAt = rs.getTimestamp("created_at")?.time ?: System.currentTimeMillis(),
            createdBy = rs.getString("created_by"),
            updatedAt = rs.getTimestamp("updated_at")?.time ?: System.currentTimeMillis(),
            updatedBy = rs.getString("updated_by"),
            version = rs.getLong("version")
        )
    }

    private fun mapRevisionRow(rs: ResultSet): VendorPurchaseOrderRevision {
        return VendorPurchaseOrderRevision(
            revisionId = rs.getString("revision_id"),
            projectId = rs.getString("project_id"),
            purchaseOrderId = rs.getString("purchase_order_id"),
            revisionNumber = rs.getInt("revision_number"),
            previousTotalAmount = Money(rs.getBigDecimal("previous_total_amount")),
            newTotalAmount = Money(rs.getBigDecimal("new_total_amount")),
            changeSummary = rs.getString("change_summary"),
            revisedBy = rs.getString("revised_by"),
            revisedAt = rs.getTimestamp("revised_at")?.time ?: System.currentTimeMillis()
        )
    }

    private fun mapAuditRow(rs: ResultSet): VendorPurchaseOrderAuditEvent {
        return VendorPurchaseOrderAuditEvent(
            auditId = rs.getString("audit_id"),
            projectId = rs.getString("project_id"),
            purchaseOrderId = rs.getString("purchase_order_id"),
            eventType = rs.getString("event_type"),
            actorId = rs.getString("actor_id"),
            correlationId = rs.getString("correlation_id"),
            occurredAt = rs.getTimestamp("occurred_at")?.time ?: System.currentTimeMillis(),
            details = rs.getString("details")
        )
    }

    override fun observePurchaseOrders(projectId: String, vendorId: String?): Flow<List<VendorPurchaseOrder>> {
        val key = if (vendorId != null) "$projectId:$vendorId" else projectId
        return synchronized(orderFlows) {
            orderFlows.getOrPut(key) { MutableStateFlow(emptyList()) }.asStateFlow()
        }
    }

    override suspend fun findById(projectId: String, purchaseOrderId: String): DomainResult<VendorPurchaseOrder> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val order = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = "SELECT * FROM vendor_purchase_orders WHERE project_id = ? AND purchase_order_id = ?"
                val rawOrder = ctx.sqlExecutor.querySingleOrNull(sql, listOf(tenant.projectId, purchaseOrderId)) { rs -> mapOrderRow(rs) }
                if (rawOrder != null) {
                    val itemSql = "SELECT * FROM vendor_purchase_order_items WHERE project_id = ? AND purchase_order_id = ? ORDER BY item_id ASC"
                    val items = ctx.sqlExecutor.queryList(itemSql, listOf(tenant.projectId, purchaseOrderId)) { rs -> mapItemRow(rs) }
                    rawOrder.copy(items = items)
                } else null
            }
            if (order != null) {
                DomainResult.Success(order)
            } else {
                DomainResult.Error(NoSuchElementException("Vendor purchase order '$purchaseOrderId' not found in project '${tenant.projectId}'."))
            }
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "find purchase order by ID")
        }
    }

    override suspend fun findByOrderNumber(projectId: String, orderNumber: String): DomainResult<VendorPurchaseOrder> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val order = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = "SELECT * FROM vendor_purchase_orders WHERE project_id = ? AND order_number = ?"
                val rawOrder = ctx.sqlExecutor.querySingleOrNull(sql, listOf(tenant.projectId, orderNumber)) { rs -> mapOrderRow(rs) }
                if (rawOrder != null) {
                    val itemSql = "SELECT * FROM vendor_purchase_order_items WHERE project_id = ? AND purchase_order_id = ? ORDER BY item_id ASC"
                    val items = ctx.sqlExecutor.queryList(itemSql, listOf(tenant.projectId, rawOrder.purchaseOrderId)) { rs -> mapItemRow(rs) }
                    rawOrder.copy(items = items)
                } else null
            }
            if (order != null) {
                DomainResult.Success(order)
            } else {
                DomainResult.Error(NoSuchElementException("Vendor purchase order '$orderNumber' not found in project '${tenant.projectId}'."))
            }
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "find purchase order by number")
        }
    }

    override suspend fun list(
        projectId: String,
        vendorId: String?,
        status: VendorPurchaseOrderStatus?,
        sourceReferenceType: String?,
        sourceReferenceId: String?
    ): DomainResult<List<VendorPurchaseOrder>> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val orders = transactionManager.inReadOnly(tenant) { ctx ->
                val sb = StringBuilder("SELECT * FROM vendor_purchase_orders WHERE project_id = ?")
                val params = mutableListOf<Any?>(tenant.projectId)

                if (vendorId != null) {
                    sb.append(" AND vendor_id = ?")
                    params.add(vendorId)
                }
                if (status != null) {
                    sb.append(" AND status = ?")
                    params.add(status.name)
                }
                if (sourceReferenceType != null) {
                    sb.append(" AND source_reference_type = ?")
                    params.add(sourceReferenceType)
                }
                if (sourceReferenceId != null) {
                    sb.append(" AND source_reference_id = ?")
                    params.add(sourceReferenceId)
                }
                sb.append(" ORDER BY created_at DESC")

                val rawOrders = ctx.sqlExecutor.queryList(sb.toString(), params) { rs -> mapOrderRow(rs) }
                rawOrders.map { o ->
                    val itemSql = "SELECT * FROM vendor_purchase_order_items WHERE project_id = ? AND purchase_order_id = ? ORDER BY item_id ASC"
                    val items = ctx.sqlExecutor.queryList(itemSql, listOf(tenant.projectId, o.purchaseOrderId)) { rs -> mapItemRow(rs) }
                    o.copy(items = items)
                }
            }
            DomainResult.Success(orders)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "list vendor purchase orders")
        }
    }

    override suspend fun createOrder(order: VendorPurchaseOrder): DomainResult<VendorPurchaseOrder> {
        val tenant = TenantContext(order.projectId.ifBlank { defaultTenantId })
        return try {
            val created = transactionManager.inTransaction(tenant) { ctx ->
                val now = Timestamp(System.currentTimeMillis())
                val insertPoSql = """
                    INSERT INTO vendor_purchase_orders (
                        project_id, purchase_order_id, order_number, vendor_id, status,
                        order_date, requested_by, approved_by, approved_at, issued_by, issued_at,
                        expected_delivery_date, delivery_location, currency, subtotal, tax_amount,
                        discount_amount, total_amount, notes, source_reference_type, source_reference_id,
                        created_at, created_by, updated_at, updated_by, version
                    ) VALUES (
                        ?, ?, ?, ?, ?,
                        ?, ?, ?, ?, ?, ?,
                        ?, ?, ?, ?, ?,
                        ?, ?, ?, ?, ?,
                        ?, ?, ?, ?, 1
                    )
                """.trimIndent()

                val poParams = listOf(
                    tenant.projectId,
                    order.purchaseOrderId,
                    order.orderNumber,
                    order.vendorId,
                    order.status.name,
                    Timestamp(order.orderDate),
                    order.requestedBy,
                    order.approvedBy,
                    order.approvedAt?.let { Timestamp(it) },
                    order.issuedBy,
                    order.issuedAt?.let { Timestamp(it) },
                    order.expectedDeliveryDate?.let { Timestamp(it) },
                    order.deliveryLocation,
                    order.currency,
                    order.subtotal.amount,
                    order.taxAmount.amount,
                    order.discountAmount.amount,
                    order.totalAmount.amount,
                    order.notes,
                    order.sourceReferenceType,
                    order.sourceReferenceId,
                    now,
                    order.createdBy,
                    now,
                    order.updatedBy
                )

                ctx.sqlExecutor.executeUpdate(insertPoSql, poParams)

                val insertItemSql = """
                    INSERT INTO vendor_purchase_order_items (
                        project_id, item_id, purchase_order_id, vendor_service_rate_id, capability_type,
                        item_description, item_code, quantity, unit_of_measure, unit_rate, pricing_method,
                        currency, discount, tax_amount, line_total, expected_delivery_date, notes,
                        source_work_order_id, version
                    ) VALUES (
                        ?, ?, ?, ?, ?,
                        ?, ?, ?, ?, ?, ?,
                        ?, ?, ?, ?, ?, ?,
                        ?, 1
                    )
                """.trimIndent()

                for (item in order.items) {
                    val itemParams = listOf(
                        tenant.projectId,
                        item.itemId,
                        order.purchaseOrderId,
                        item.vendorServiceRateId,
                        item.capabilityType?.name,
                        item.itemDescription,
                        item.itemCode,
                        item.quantity,
                        item.unitOfMeasure.name,
                        item.unitRate.amount,
                        item.pricingMethod.name,
                        item.currency,
                        item.discount.amount,
                        item.taxAmount.amount,
                        item.lineTotal.amount,
                        item.expectedDeliveryDate?.let { Timestamp(it) },
                        item.notes,
                        item.sourceWorkOrderId
                    )
                    ctx.sqlExecutor.executeUpdate(insertItemSql, itemParams)
                }

                order.copy(createdAt = now.time, updatedAt = now.time, version = 1L)
            }
            DomainResult.Success(created)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "create vendor purchase order")
        }
    }

    override suspend fun updateOrder(order: VendorPurchaseOrder): DomainResult<VendorPurchaseOrder> {
        val tenant = TenantContext(order.projectId.ifBlank { defaultTenantId })
        return try {
            val updated = transactionManager.inTransaction(tenant) { ctx ->
                val now = Timestamp(System.currentTimeMillis())
                val updateSql = """
                    UPDATE vendor_purchase_orders SET
                        expected_delivery_date = ?,
                        delivery_location = ?,
                        subtotal = ?,
                        tax_amount = ?,
                        discount_amount = ?,
                        total_amount = ?,
                        notes = ?,
                        updated_at = ?,
                        updated_by = ?,
                        version = version + 1
                    WHERE project_id = ? AND purchase_order_id = ? AND version = ?
                """.trimIndent()

                val params = listOf(
                    order.expectedDeliveryDate?.let { Timestamp(it) },
                    order.deliveryLocation,
                    order.subtotal.amount,
                    order.taxAmount.amount,
                    order.discountAmount.amount,
                    order.totalAmount.amount,
                    order.notes,
                    now,
                    order.updatedBy,
                    tenant.projectId,
                    order.purchaseOrderId,
                    order.version
                )

                val rows = ctx.sqlExecutor.executeUpdate(updateSql, params)
                if (rows == 0) {
                    throw IllegalStateException("Optimistic lock failure or purchase order not found: '${order.purchaseOrderId}'.")
                }

                // Delete and re-insert items
                ctx.sqlExecutor.executeUpdate(
                    "DELETE FROM vendor_purchase_order_items WHERE project_id = ? AND purchase_order_id = ?",
                    listOf(tenant.projectId, order.purchaseOrderId)
                )

                val insertItemSql = """
                    INSERT INTO vendor_purchase_order_items (
                        project_id, item_id, purchase_order_id, vendor_service_rate_id, capability_type,
                        item_description, item_code, quantity, unit_of_measure, unit_rate, pricing_method,
                        currency, discount, tax_amount, line_total, expected_delivery_date, notes,
                        source_work_order_id, version
                    ) VALUES (
                        ?, ?, ?, ?, ?,
                        ?, ?, ?, ?, ?, ?,
                        ?, ?, ?, ?, ?, ?,
                        ?, 1
                    )
                """.trimIndent()

                for (item in order.items) {
                    val itemParams = listOf(
                        tenant.projectId,
                        item.itemId,
                        order.purchaseOrderId,
                        item.vendorServiceRateId,
                        item.capabilityType?.name,
                        item.itemDescription,
                        item.itemCode,
                        item.quantity,
                        item.unitOfMeasure.name,
                        item.unitRate.amount,
                        item.pricingMethod.name,
                        item.currency,
                        item.discount.amount,
                        item.taxAmount.amount,
                        item.lineTotal.amount,
                        item.expectedDeliveryDate?.let { Timestamp(it) },
                        item.notes,
                        item.sourceWorkOrderId
                    )
                    ctx.sqlExecutor.executeUpdate(insertItemSql, itemParams)
                }

                order.copy(version = order.version + 1L, updatedAt = now.time)
            }
            DomainResult.Success(updated)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "update vendor purchase order")
        }
    }

    override suspend fun updateStatus(
        projectId: String,
        purchaseOrderId: String,
        status: VendorPurchaseOrderStatus,
        updatedBy: String,
        approvedBy: String?,
        approvedAt: Long?,
        issuedBy: String?,
        issuedAt: Long?
    ): DomainResult<VendorPurchaseOrder> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val updated = transactionManager.inTransaction(tenant) { ctx ->
                val now = Timestamp(System.currentTimeMillis())
                val updateSql = """
                    UPDATE vendor_purchase_orders SET
                        status = ?,
                        approved_by = COALESCE(?, approved_by),
                        approved_at = COALESCE(?, approved_at),
                        issued_by = COALESCE(?, issued_by),
                        issued_at = COALESCE(?, issued_at),
                        updated_at = ?,
                        updated_by = ?,
                        version = version + 1
                    WHERE project_id = ? AND purchase_order_id = ?
                """.trimIndent()

                val params = listOf(
                    status.name,
                    approvedBy,
                    approvedAt?.let { Timestamp(it) },
                    issuedBy,
                    issuedAt?.let { Timestamp(it) },
                    now,
                    updatedBy,
                    tenant.projectId,
                    purchaseOrderId
                )

                val rows = ctx.sqlExecutor.executeUpdate(updateSql, params)
                if (rows == 0) {
                    throw NoSuchElementException("Vendor purchase order '$purchaseOrderId' not found.")
                }

                val findSql = "SELECT * FROM vendor_purchase_orders WHERE project_id = ? AND purchase_order_id = ?"
                val rawOrder = ctx.sqlExecutor.querySingleOrNull(findSql, listOf(tenant.projectId, purchaseOrderId)) { rs -> mapOrderRow(rs) }
                    ?: throw NoSuchElementException("Failed to retrieve updated order '$purchaseOrderId'.")

                val itemSql = "SELECT * FROM vendor_purchase_order_items WHERE project_id = ? AND purchase_order_id = ? ORDER BY item_id ASC"
                val items = ctx.sqlExecutor.queryList(itemSql, listOf(tenant.projectId, purchaseOrderId)) { rs -> mapItemRow(rs) }
                rawOrder.copy(items = items)
            }
            DomainResult.Success(updated)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "update vendor purchase order status")
        }
    }

    override suspend fun recordRevision(revision: VendorPurchaseOrderRevision): DomainResult<VendorPurchaseOrderRevision> {
        val tenant = TenantContext(revision.projectId.ifBlank { defaultTenantId })
        return try {
            val created = transactionManager.inTransaction(tenant) { ctx ->
                val now = Timestamp(revision.revisedAt)
                val sql = """
                    INSERT INTO vendor_purchase_order_revisions (
                        project_id, revision_id, purchase_order_id, revision_number,
                        previous_total_amount, new_total_amount, change_summary, revised_by, revised_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()

                val params = listOf(
                    tenant.projectId,
                    revision.revisionId,
                    revision.purchaseOrderId,
                    revision.revisionNumber,
                    revision.previousTotalAmount.amount,
                    revision.newTotalAmount.amount,
                    revision.changeSummary,
                    revision.revisedBy,
                    now
                )
                ctx.sqlExecutor.executeUpdate(sql, params)
                revision
            }
            DomainResult.Success(created)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "record purchase order revision")
        }
    }

    override suspend fun listRevisions(projectId: String, purchaseOrderId: String): DomainResult<List<VendorPurchaseOrderRevision>> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val list = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = """
                    SELECT * FROM vendor_purchase_order_revisions
                    WHERE project_id = ? AND purchase_order_id = ?
                    ORDER BY revision_number ASC
                """.trimIndent()
                ctx.sqlExecutor.queryList(sql, listOf(tenant.projectId, purchaseOrderId)) { rs -> mapRevisionRow(rs) }
            }
            DomainResult.Success(list)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "list purchase order revisions")
        }
    }

    override suspend fun appendAudit(auditEvent: VendorPurchaseOrderAuditEvent): DomainResult<VendorPurchaseOrderAuditEvent> {
        val tenant = TenantContext(auditEvent.projectId.ifBlank { defaultTenantId })
        return try {
            val created = transactionManager.inTransaction(tenant) { ctx ->
                val now = Timestamp(auditEvent.occurredAt)
                val sql = """
                    INSERT INTO vendor_purchase_order_audits (
                        project_id, audit_id, purchase_order_id, event_type, actor_id,
                        correlation_id, occurred_at, details
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()

                val params = listOf(
                    tenant.projectId,
                    auditEvent.auditId,
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
            PostgresErrorTranslator.translate(e, "append purchase order audit event")
        }
    }

    override suspend fun listAudits(projectId: String, purchaseOrderId: String): DomainResult<List<VendorPurchaseOrderAuditEvent>> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val list = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = """
                    SELECT * FROM vendor_purchase_order_audits
                    WHERE project_id = ? AND purchase_order_id = ?
                    ORDER BY occurred_at ASC
                """.trimIndent()
                ctx.sqlExecutor.queryList(sql, listOf(tenant.projectId, purchaseOrderId)) { rs -> mapAuditRow(rs) }
            }
            DomainResult.Success(list)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "list purchase order audits")
        }
    }
}

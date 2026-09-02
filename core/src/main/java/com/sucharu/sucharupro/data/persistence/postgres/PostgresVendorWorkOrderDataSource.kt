package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.VendorWorkOrderDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.sql.ResultSet
import java.sql.Timestamp

class PostgresVendorWorkOrderDataSource(
    private val transactionManager: TransactionManager,
    private val defaultTenantId: String = "TENANT-001"
) : VendorWorkOrderDataSource {

    private val workOrderFlows = mutableMapOf<String, MutableStateFlow<List<VendorWorkOrder>>>()

    private fun mapOrderRow(rs: ResultSet): VendorWorkOrder {
        val snapshot = VendorWorkOrderRateSnapshot(
            sourceRateId = rs.getString("rate_snapshot_source_rate_id"),
            pricingMethod = PricingMethod.valueOf(rs.getString("pricing_method")),
            unitOfMeasure = UnitOfMeasure.valueOf(rs.getString("unit_of_measure")),
            currency = rs.getString("rate_snapshot_currency"),
            baseRate = Money(rs.getBigDecimal("rate_snapshot_base_rate")),
            resolvedUnitRate = Money(rs.getBigDecimal("rate_snapshot_resolved_unit_rate")),
            tierMetadata = rs.getString("rate_snapshot_tier_metadata"),
            quantityBasis = rs.getBigDecimal("rate_snapshot_quantity_basis"),
            resolvedAt = rs.getTimestamp("rate_snapshot_resolved_at")?.time ?: System.currentTimeMillis()
        )

        return VendorWorkOrder(
            workOrderId = rs.getString("work_order_id"),
            projectId = rs.getString("project_id"),
            workOrderNumber = rs.getString("work_order_number"),
            vendorId = rs.getString("vendor_id"),
            capabilityType = CapabilityType.valueOf(rs.getString("capability_type")),
            serviceRateId = rs.getString("service_rate_id"),
            sourceReferenceId = rs.getString("source_reference_id"),
            sourceReferenceType = rs.getString("source_reference_type"),
            title = rs.getString("title"),
            description = rs.getString("description"),
            quantity = rs.getBigDecimal("quantity"),
            unitOfMeasure = UnitOfMeasure.valueOf(rs.getString("unit_of_measure")),
            pricingMethod = PricingMethod.valueOf(rs.getString("pricing_method")),
            rateSnapshot = snapshot,
            currency = rs.getString("currency"),
            estimatedAmount = Money(rs.getBigDecimal("estimated_amount")),
            scheduledStartAt = rs.getTimestamp("scheduled_start_at")?.time,
            scheduledDueAt = rs.getTimestamp("scheduled_due_at")?.time,
            priority = rs.getString("priority"),
            status = VendorWorkOrderStatus.valueOf(rs.getString("status")),
            notes = rs.getString("notes"),
            createdAt = rs.getTimestamp("created_at")?.time ?: System.currentTimeMillis(),
            createdBy = rs.getString("created_by"),
            updatedAt = rs.getTimestamp("updated_at")?.time ?: System.currentTimeMillis(),
            updatedBy = rs.getString("updated_by"),
            version = rs.getLong("version")
        )
    }

    private fun mapAuditRow(rs: ResultSet): VendorWorkOrderAuditEvent {
        return VendorWorkOrderAuditEvent(
            auditId = rs.getString("audit_id"),
            projectId = rs.getString("project_id"),
            workOrderId = rs.getString("work_order_id"),
            eventType = rs.getString("event_type"),
            actorId = rs.getString("actor_id"),
            correlationId = rs.getString("correlation_id"),
            occurredAt = rs.getTimestamp("occurred_at")?.time ?: System.currentTimeMillis(),
            details = rs.getString("details")
        )
    }

    override fun observeWorkOrders(projectId: String, vendorId: String?): Flow<List<VendorWorkOrder>> {
        val key = if (vendorId != null) "$projectId:$vendorId" else projectId
        return synchronized(workOrderFlows) {
            workOrderFlows.getOrPut(key) { MutableStateFlow(emptyList()) }.asStateFlow()
        }
    }

    override suspend fun findById(projectId: String, workOrderId: String): DomainResult<VendorWorkOrder> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val order = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = "SELECT * FROM vendor_work_orders WHERE project_id = ? AND work_order_id = ?"
                ctx.sqlExecutor.querySingleOrNull(sql, listOf(tenant.projectId, workOrderId)) { rs -> mapOrderRow(rs) }
            }
            if (order != null) {
                DomainResult.Success(order)
            } else {
                DomainResult.Error(NoSuchElementException("Vendor work order '$workOrderId' not found in project '${tenant.projectId}'."))
            }
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "find vendor work order by ID")
        }
    }

    override suspend fun findByNumber(projectId: String, workOrderNumber: String): DomainResult<VendorWorkOrder> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val order = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = "SELECT * FROM vendor_work_orders WHERE project_id = ? AND work_order_number = ?"
                ctx.sqlExecutor.querySingleOrNull(sql, listOf(tenant.projectId, workOrderNumber)) { rs -> mapOrderRow(rs) }
            }
            if (order != null) {
                DomainResult.Success(order)
            } else {
                DomainResult.Error(NoSuchElementException("Vendor work order '$workOrderNumber' not found in project '${tenant.projectId}'."))
            }
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "find vendor work order by number")
        }
    }

    override suspend fun list(
        projectId: String,
        vendorId: String?,
        status: VendorWorkOrderStatus?,
        capabilityType: CapabilityType?,
        sourceReferenceType: String?,
        sourceReferenceId: String?
    ): DomainResult<List<VendorWorkOrder>> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val orders = transactionManager.inReadOnly(tenant) { ctx ->
                val sb = StringBuilder("SELECT * FROM vendor_work_orders WHERE project_id = ?")
                val params = mutableListOf<Any?>(tenant.projectId)

                if (vendorId != null) {
                    sb.append(" AND vendor_id = ?")
                    params.add(vendorId)
                }
                if (status != null) {
                    sb.append(" AND status = ?")
                    params.add(status.name)
                }
                if (capabilityType != null) {
                    sb.append(" AND capability_type = ?")
                    params.add(capabilityType.name)
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

                ctx.sqlExecutor.queryList(sb.toString(), params) { rs -> mapOrderRow(rs) }
            }
            DomainResult.Success(orders)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "list vendor work orders")
        }
    }

    override suspend fun createWorkOrder(workOrder: VendorWorkOrder): DomainResult<VendorWorkOrder> {
        val tenant = TenantContext(workOrder.projectId.ifBlank { defaultTenantId })
        return try {
            val created = transactionManager.inTransaction(tenant) { ctx ->
                val now = Timestamp(System.currentTimeMillis())
                val insertSql = """
                    INSERT INTO vendor_work_orders (
                        project_id, work_order_id, work_order_number, vendor_id, capability_type,
                        service_rate_id, source_reference_id, source_reference_type, title, description,
                        quantity, unit_of_measure, pricing_method, rate_snapshot_base_rate,
                        rate_snapshot_resolved_unit_rate, rate_snapshot_currency, rate_snapshot_source_rate_id,
                        rate_snapshot_tier_metadata, rate_snapshot_quantity_basis, rate_snapshot_resolved_at,
                        currency, estimated_amount, scheduled_start_at, scheduled_due_at,
                        priority, status, notes, created_at, created_by, updated_at, updated_by, version
                    ) VALUES (
                        ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                        ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                        ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1
                    )
                """.trimIndent()

                val params = listOf(
                    tenant.projectId,
                    workOrder.workOrderId,
                    workOrder.workOrderNumber,
                    workOrder.vendorId,
                    workOrder.capabilityType.name,
                    workOrder.serviceRateId,
                    workOrder.sourceReferenceId,
                    workOrder.sourceReferenceType,
                    workOrder.title,
                    workOrder.description,
                    workOrder.quantity,
                    workOrder.unitOfMeasure.name,
                    workOrder.pricingMethod.name,
                    workOrder.rateSnapshot.baseRate.amount,
                    workOrder.rateSnapshot.resolvedUnitRate.amount,
                    workOrder.rateSnapshot.currency,
                    workOrder.rateSnapshot.sourceRateId,
                    workOrder.rateSnapshot.tierMetadata,
                    workOrder.rateSnapshot.quantityBasis,
                    Timestamp(workOrder.rateSnapshot.resolvedAt),
                    workOrder.currency,
                    workOrder.estimatedAmount.amount,
                    workOrder.scheduledStartAt?.let { Timestamp(it) },
                    workOrder.scheduledDueAt?.let { Timestamp(it) },
                    workOrder.priority,
                    workOrder.status.name,
                    workOrder.notes,
                    now,
                    workOrder.createdBy,
                    now,
                    workOrder.updatedBy
                )

                ctx.sqlExecutor.executeUpdate(insertSql, params)
                workOrder.copy(createdAt = now.time, updatedAt = now.time, version = 1L)
            }
            DomainResult.Success(created)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "create vendor work order")
        }
    }

    override suspend fun updateWorkOrder(workOrder: VendorWorkOrder): DomainResult<VendorWorkOrder> {
        val tenant = TenantContext(workOrder.projectId.ifBlank { defaultTenantId })
        return try {
            val updated = transactionManager.inTransaction(tenant) { ctx ->
                val now = Timestamp(System.currentTimeMillis())
                val updateSql = """
                    UPDATE vendor_work_orders SET
                        vendor_id = ?,
                        capability_type = ?,
                        title = ?,
                        description = ?,
                        quantity = ?,
                        unit_of_measure = ?,
                        pricing_method = ?,
                        rate_snapshot_base_rate = ?,
                        rate_snapshot_resolved_unit_rate = ?,
                        rate_snapshot_currency = ?,
                        rate_snapshot_source_rate_id = ?,
                        rate_snapshot_tier_metadata = ?,
                        rate_snapshot_quantity_basis = ?,
                        rate_snapshot_resolved_at = ?,
                        estimated_amount = ?,
                        scheduled_start_at = ?,
                        scheduled_due_at = ?,
                        priority = ?,
                        status = ?,
                        notes = ?,
                        updated_at = ?,
                        updated_by = ?,
                        version = version + 1
                    WHERE project_id = ? AND work_order_id = ? AND version = ?
                """.trimIndent()

                val params = listOf(
                    workOrder.vendorId,
                    workOrder.capabilityType.name,
                    workOrder.title,
                    workOrder.description,
                    workOrder.quantity,
                    workOrder.unitOfMeasure.name,
                    workOrder.pricingMethod.name,
                    workOrder.rateSnapshot.baseRate.amount,
                    workOrder.rateSnapshot.resolvedUnitRate.amount,
                    workOrder.rateSnapshot.currency,
                    workOrder.rateSnapshot.sourceRateId,
                    workOrder.rateSnapshot.tierMetadata,
                    workOrder.rateSnapshot.quantityBasis,
                    Timestamp(workOrder.rateSnapshot.resolvedAt),
                    workOrder.estimatedAmount.amount,
                    workOrder.scheduledStartAt?.let { Timestamp(it) },
                    workOrder.scheduledDueAt?.let { Timestamp(it) },
                    workOrder.priority,
                    workOrder.status.name,
                    workOrder.notes,
                    now,
                    workOrder.updatedBy,
                    tenant.projectId,
                    workOrder.workOrderId,
                    workOrder.version
                )

                val rows = ctx.sqlExecutor.executeUpdate(updateSql, params)
                if (rows == 0) {
                    throw IllegalStateException("Optimistic lock failure or work order not found: '${workOrder.workOrderId}'.")
                }
                workOrder.copy(version = workOrder.version + 1L, updatedAt = now.time)
            }
            DomainResult.Success(updated)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "update vendor work order")
        }
    }

    override suspend fun updateStatus(
        projectId: String,
        workOrderId: String,
        status: VendorWorkOrderStatus,
        updatedBy: String
    ): DomainResult<VendorWorkOrder> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val updated = transactionManager.inTransaction(tenant) { ctx ->
                val now = Timestamp(System.currentTimeMillis())
                val updateSql = """
                    UPDATE vendor_work_orders SET
                        status = ?,
                        updated_at = ?,
                        updated_by = ?,
                        version = version + 1
                    WHERE project_id = ? AND work_order_id = ?
                """.trimIndent()

                val rows = ctx.sqlExecutor.executeUpdate(updateSql, listOf(status.name, now, updatedBy, tenant.projectId, workOrderId))
                if (rows == 0) {
                    throw NoSuchElementException("Vendor work order '$workOrderId' not found.")
                }

                val findSql = "SELECT * FROM vendor_work_orders WHERE project_id = ? AND work_order_id = ?"
                ctx.sqlExecutor.querySingleOrNull(findSql, listOf(tenant.projectId, workOrderId)) { rs ->
                    mapOrderRow(rs)
                } ?: throw NoSuchElementException("Failed to retrieve updated work order '$workOrderId'.")
            }
            DomainResult.Success(updated)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "update vendor work order status")
        }
    }

    override suspend fun appendAudit(auditEvent: VendorWorkOrderAuditEvent): DomainResult<VendorWorkOrderAuditEvent> {
        val tenant = TenantContext(auditEvent.projectId.ifBlank { defaultTenantId })
        return try {
            val created = transactionManager.inTransaction(tenant) { ctx ->
                val now = Timestamp(auditEvent.occurredAt)
                val sql = """
                    INSERT INTO vendor_work_order_audits (
                        project_id, audit_id, work_order_id, event_type, actor_id,
                        correlation_id, occurred_at, details
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()

                val params = listOf(
                    tenant.projectId,
                    auditEvent.auditId,
                    auditEvent.workOrderId,
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
            PostgresErrorTranslator.translate(e, "append work order audit event")
        }
    }

    override suspend fun listAudits(projectId: String, workOrderId: String): DomainResult<List<VendorWorkOrderAuditEvent>> {
        val tenant = TenantContext(projectId.ifBlank { defaultTenantId })
        return try {
            val list = transactionManager.inReadOnly(tenant) { ctx ->
                val sql = """
                    SELECT * FROM vendor_work_order_audits
                    WHERE project_id = ? AND work_order_id = ?
                    ORDER BY occurred_at ASC
                """.trimIndent()
                ctx.sqlExecutor.queryList(sql, listOf(tenant.projectId, workOrderId)) { rs -> mapAuditRow(rs) }
            }
            DomainResult.Success(list)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "list work order audits")
        }
    }
}

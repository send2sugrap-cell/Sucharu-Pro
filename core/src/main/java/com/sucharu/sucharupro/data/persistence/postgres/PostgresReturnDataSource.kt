package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.ReturnDataSource
import com.sucharu.sucharupro.data.persistence.postgres.RowMappers.getEnumByName
import com.sucharu.sucharupro.data.persistence.postgres.RowMappers.getTimestampMillis
import com.sucharu.sucharupro.domain.model.returns.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.sql.ResultSet
import java.sql.Timestamp

/**
 * Production-grade PostgreSQL DataSource for Customer Return Requests (Module 11).
 */
class PostgresReturnDataSource(
    private val transactionManager: TransactionManager,
    private val defaultTenantId: String = "TENANT-001"
) : ReturnDataSource {

    private fun mapReturnRequest(rs: ResultSet): ReturnRequest {
        return ReturnRequest(
            returnId = rs.getString("return_id"),
            projectId = rs.getString("project_id"),
            returnNo = rs.getString("return_no"),
            customerId = rs.getString("customer_id"),
            originalChallanId = rs.getString("original_challan_id"),
            status = rs.getEnumByName("status", ReturnStatus.REQUESTED),
            reason = rs.getEnumByName("reason", ReturnReason.PRINTING_DEFECT),
            description = rs.getString("description"),
            requestedAt = rs.getTimestampMillis("requested_at"),
            requestedBy = rs.getString("requested_by"),
            createdAt = rs.getTimestampMillis("created_at"),
            updatedAt = rs.getTimestampMillis("updated_at"),
            version = rs.getLong("version")
        )
    }

    override fun observeReturns(projectId: String): Flow<List<ReturnRequest>> = flow {
        val list = getReturnsByProject(projectId)
        emit(list)
    }

    override fun observeReturn(returnId: String): Flow<ReturnRequest?> = flow {
        val item = getReturn(returnId)
        emit(item)
    }

    override suspend fun getReturn(returnId: String): ReturnRequest? {
        val tenant = TenantContext(defaultTenantId)
        return transactionManager.inReadOnly(tenant) { ctx ->
            val sql = """
                SELECT return_id, project_id, return_no, customer_id, original_challan_id,
                       status, reason, description, requested_at, requested_by,
                       created_at, updated_at, version
                FROM return_requests
                WHERE project_id = ? AND return_id = ?
            """.trimIndent()

            ctx.sqlExecutor.querySingleOrNull(sql, listOf(tenant.projectId, returnId)) { rs ->
                mapReturnRequest(rs)
            }
        }
    }

    override suspend fun getReturnsByProject(projectId: String, customerId: String?): List<ReturnRequest> {
        val tenant = TenantContext(projectId)
        return transactionManager.inReadOnly(tenant) { ctx ->
            val sql = if (customerId != null) {
                """
                    SELECT return_id, project_id, return_no, customer_id, original_challan_id,
                           status, reason, description, requested_at, requested_by,
                           created_at, updated_at, version
                    FROM return_requests
                    WHERE project_id = ? AND customer_id = ?
                    ORDER BY created_at DESC
                """.trimIndent()
            } else {
                """
                    SELECT return_id, project_id, return_no, customer_id, original_challan_id,
                           status, reason, description, requested_at, requested_by,
                           created_at, updated_at, version
                    FROM return_requests
                    WHERE project_id = ?
                    ORDER BY created_at DESC
                """.trimIndent()
            }

            val params = if (customerId != null) listOf(projectId, customerId) else listOf(projectId)
            ctx.sqlExecutor.queryList(sql, params) { rs ->
                mapReturnRequest(rs)
            }
        }
    }

    private fun mapReturnItem(rs: ResultSet): ReturnItem {
        return ReturnItem(
            returnItemId = rs.getString("return_item_id"),
            returnId = rs.getString("return_id"),
            productId = rs.getString("product_id"),
            originalChallanItemId = rs.getString("original_challan_item_id"),
            requestedQuantity = rs.getInt("requested_quantity"),
            acceptedQuantity = rs.getInt("accepted_quantity"),
            rejectedQuantity = rs.getInt("rejected_quantity"),
            unit = rs.getString("unit") ?: "PCS",
            condition = null,
            notes = null
        )
    }

    override suspend fun getReturnItems(returnId: String): List<ReturnItem> {
        val tenant = TenantContext(defaultTenantId)
        return transactionManager.inReadOnly(tenant) { ctx ->
            val sql = """
                SELECT return_item_id, project_id, return_id, product_id, original_challan_item_id,
                       requested_quantity, accepted_quantity, rejected_quantity, unit, created_at
                FROM return_items
                WHERE project_id = ? AND return_id = ?
                ORDER BY created_at ASC
            """.trimIndent()
            ctx.sqlExecutor.queryList(sql, listOf(tenant.projectId, returnId)) { rs ->
                mapReturnItem(rs)
            }
        }
    }

    override suspend fun insertReturn(request: ReturnRequest, items: List<ReturnItem>) {
        val tenant = TenantContext(request.projectId)
        transactionManager.inTransaction(tenant) { ctx ->
            val sql = """
                INSERT INTO return_requests (
                    project_id, return_id, return_no, customer_id, original_challan_id,
                    status, reason, description, requested_at, requested_by,
                    created_at, updated_at, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), 1)
            """.trimIndent()

            ctx.sqlExecutor.executeUpdate(
                sql,
                listOf(
                    request.projectId,
                    request.returnId,
                    request.returnNo,
                    request.customerId,
                    request.originalChallanId,
                    request.status.name,
                    request.reason.name,
                    request.description,
                    Timestamp(request.requestedAt),
                    request.requestedBy
                )
            )

            items.forEach { item ->
                val itemSql = """
                    INSERT INTO return_items (
                        project_id, return_item_id, return_id, product_id, original_challan_item_id,
                        requested_quantity, accepted_quantity, rejected_quantity, unit, created_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
                """.trimIndent()
                ctx.sqlExecutor.executeUpdate(
                    itemSql,
                    listOf(
                        request.projectId,
                        item.returnItemId,
                        request.returnId,
                        item.productId,
                        item.originalChallanItemId,
                        item.requestedQuantity,
                        item.acceptedQuantity,
                        item.rejectedQuantity,
                        item.unit ?: "PCS"
                    )
                )
            }
        }
    }

    override suspend fun updateReturn(request: ReturnRequest) {
        val tenant = TenantContext(request.projectId)
        transactionManager.inTransaction(tenant) { ctx ->
            val sql = """
                UPDATE return_requests
                SET status = ?, reason = ?, description = ?, updated_at = NOW(), version = version + 1
                WHERE project_id = ? AND return_id = ? AND version = ?
            """.trimIndent()

            val affected = ctx.sqlExecutor.executeUpdate(
                sql,
                listOf(
                    request.status.name,
                    request.reason.name,
                    request.description,
                    request.projectId,
                    request.returnId,
                    request.version
                )
            )
            if (affected == 0) {
                throw OptimisticLockException("ReturnRequest", request.returnId, request.version)
            }
        }
    }

    override suspend fun updateReturnItem(item: ReturnItem) {
        val tenant = TenantContext(defaultTenantId)
        transactionManager.inTransaction(tenant) { ctx ->
            val sql = """
                UPDATE return_items
                SET accepted_quantity = ?, rejected_quantity = ?
                WHERE project_id = ? AND return_item_id = ?
            """.trimIndent()
            ctx.sqlExecutor.executeUpdate(
                sql,
                listOf(item.acceptedQuantity, item.rejectedQuantity, tenant.projectId, item.returnItemId)
            )
        }
    }

    override fun observeActivityEvents(returnId: String): Flow<List<ReturnActivityEvent>> = flow { emit(emptyList()) }
    override suspend fun getActivityEvents(returnId: String): List<ReturnActivityEvent> = emptyList()
    override suspend fun insertActivityEvent(event: ReturnActivityEvent) {}

    // Return Inspection
    override suspend fun getInspection(returnId: String): ReturnInspection? {
        val tenant = TenantContext(defaultTenantId)
        return transactionManager.inReadOnly(tenant) { ctx ->
            val sql = """
                SELECT inspection_id, project_id, return_id, status, inspected_by, inspection_notes,
                       inspected_at, created_at, updated_at, version
                FROM return_inspections
                WHERE project_id = ? AND return_id = ?
            """.trimIndent()
            ctx.sqlExecutor.querySingleOrNull(sql, listOf(tenant.projectId, returnId)) { rs ->
                ReturnInspection(
                    inspectionId = rs.getString("inspection_id"),
                    returnId = rs.getString("return_id"),
                    projectId = rs.getString("project_id"),
                    inspectorId = rs.getString("inspected_by") ?: "SYSTEM",
                    status = rs.getEnumByName("status", ReturnInspectionStatus.PENDING),
                    findings = rs.getString("inspection_notes"),
                    decision = if (rs.getString("status") == "COMPLETED") ReturnDecision.APPROVE else null,
                    inspectedAt = rs.getTimestampMillis("inspected_at"),
                    createdAt = rs.getTimestampMillis("created_at"),
                    updatedAt = rs.getTimestampMillis("updated_at"),
                    version = rs.getLong("version")
                )
            }
        }
    }

    override suspend fun insertOrUpdateInspection(inspection: ReturnInspection) {
        val tenant = TenantContext(inspection.projectId)
        transactionManager.inTransaction(tenant) { ctx ->
            val existing = getInspection(inspection.returnId)
            if (existing == null) {
                val sql = """
                    INSERT INTO return_inspections (
                        project_id, inspection_id, return_id, status, inspected_by,
                        inspection_notes, inspected_at, created_at, updated_at, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), 1)
                """.trimIndent()
                ctx.sqlExecutor.executeUpdate(
                    sql,
                    listOf(
                        inspection.projectId,
                        inspection.inspectionId,
                        inspection.returnId,
                        inspection.status.name,
                        inspection.inspectorId,
                        inspection.findings,
                        Timestamp(inspection.inspectedAt)
                    )
                )
            } else {
                val sql = """
                    UPDATE return_inspections
                    SET status = ?, inspected_by = ?, inspection_notes = ?, inspected_at = ?,
                        updated_at = NOW(), version = version + 1
                    WHERE project_id = ? AND inspection_id = ?
                """.trimIndent()
                ctx.sqlExecutor.executeUpdate(
                    sql,
                    listOf(
                        inspection.status.name,
                        inspection.inspectorId,
                        inspection.findings,
                        Timestamp(inspection.inspectedAt),
                        inspection.projectId,
                        inspection.inspectionId
                    )
                )
            }
        }
    }

    override fun observeInspection(returnId: String): Flow<ReturnInspection?> = flow { emit(getInspection(returnId)) }

    // Return Receiving
    override suspend fun getReceiving(returnId: String): ReturnReceivingInfo? = null
    override suspend fun getReceivingByIdempotencyKey(idempotencyKey: String): ReturnReceivingInfo? = null
    override suspend fun insertOrUpdateReceiving(receivingInfo: ReturnReceivingInfo) {}
    override fun observeReceiving(returnId: String): Flow<ReturnReceivingInfo?> = flow { emit(null) }

    // Return Reconciliation
    override suspend fun getReconciliationResult(returnId: String): ReturnReconciliationResult? = null
    override suspend fun insertOrUpdateReconciliationResult(result: ReturnReconciliationResult) {}
    override fun observeReconciliationResult(returnId: String): Flow<ReturnReconciliationResult?> = flow { emit(null) }

    // Return Settlement
    override suspend fun getSettlement(returnId: String): ReturnSettlement? {
        val tenant = TenantContext(defaultTenantId)
        return transactionManager.inReadOnly(tenant) { ctx ->
            val sql = """
                SELECT settlement_id, project_id, return_id, customer_id, resolution_type,
                       amount, currency, status, settled_by, settled_at, version, idempotency_key
                FROM return_settlements
                WHERE project_id = ? AND return_id = ?
            """.trimIndent()
            ctx.sqlExecutor.querySingleOrNull(sql, listOf(tenant.projectId, returnId)) { rs ->
                ReturnSettlement(
                    settlementId = rs.getString("settlement_id"),
                    returnId = rs.getString("return_id"),
                    projectId = rs.getString("project_id"),
                    customerId = rs.getString("customer_id"),
                    resolutionType = rs.getEnumByName("resolution_type", ReturnResolutionType.CREDIT_NOTE),
                    amount = com.sucharu.sucharupro.domain.model.common.Money(rs.getDouble("amount")),
                    status = rs.getEnumByName("status", ReturnSettlementStatus.COMPLETED),
                    settledBy = rs.getString("settled_by"),
                    settledAt = rs.getTimestampMillis("settled_at"),
                    version = rs.getLong("version"),
                    idempotencyKey = rs.getString("idempotency_key") ?: "LEGACY"
                )
            }
        }
    }

    override suspend fun getSettlementByIdempotencyKey(idempotencyKey: String): ReturnSettlement? {
        val tenant = TenantContext(defaultTenantId)
        return transactionManager.inReadOnly(tenant) { ctx ->
            val sql = """
                SELECT settlement_id, project_id, return_id, customer_id, resolution_type,
                       amount, currency, status, settled_by, settled_at, version, idempotency_key
                FROM return_settlements
                WHERE project_id = ? AND idempotency_key = ?
            """.trimIndent()
            ctx.sqlExecutor.querySingleOrNull(sql, listOf(tenant.projectId, idempotencyKey)) { rs ->
                ReturnSettlement(
                    settlementId = rs.getString("settlement_id"),
                    returnId = rs.getString("return_id"),
                    projectId = rs.getString("project_id"),
                    customerId = rs.getString("customer_id"),
                    resolutionType = rs.getEnumByName("resolution_type", ReturnResolutionType.CREDIT_NOTE),
                    amount = com.sucharu.sucharupro.domain.model.common.Money(rs.getDouble("amount")),
                    status = rs.getEnumByName("status", ReturnSettlementStatus.COMPLETED),
                    settledBy = rs.getString("settled_by"),
                    settledAt = rs.getTimestampMillis("settled_at"),
                    version = rs.getLong("version"),
                    idempotencyKey = rs.getString("idempotency_key") ?: "LEGACY"
                )
            }
        }
    }

    override suspend fun insertOrUpdateSettlement(settlement: ReturnSettlement) {
        val tenant = TenantContext(settlement.projectId)
        transactionManager.inTransaction(tenant) { ctx ->
            val sql = """
                INSERT INTO return_settlements (
                    project_id, settlement_id, return_id, customer_id, resolution_type,
                    amount, currency, status, settled_by, settled_at, idempotency_key, created_at, version
                ) VALUES (?, ?, ?, ?, ?, ?, 'BDT', ?, ?, ?, ?, NOW(), 1)
            """.trimIndent()
            ctx.sqlExecutor.executeUpdate(
                sql,
                listOf(
                    settlement.projectId,
                    settlement.settlementId,
                    settlement.returnId,
                    settlement.customerId,
                    settlement.resolutionType.name,
                    settlement.amount.amount,
                    settlement.status.name,
                    settlement.settledBy,
                    Timestamp(settlement.settledAt),
                    settlement.idempotencyKey
                )
            )
        }
    }

    override fun observeSettlement(returnId: String): Flow<ReturnSettlement?> = flow { emit(getSettlement(returnId)) }
}

package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.VendorSettlementDataSource
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import java.sql.ResultSet

/**
 * PostgreSQL JDBC implementation of VendorSettlementDataSource with RLS (Module 12 Step 10).
 */
class PostgresVendorSettlementDataSource(
    private val transactionManager: TransactionManager,
    private val defaultTenantId: String = "TENANT-001"
) : VendorSettlementDataSource {

    private fun mapAllocationRow(rs: ResultSet): VendorSettlementAllocation {
        return VendorSettlementAllocation(
            allocationId = rs.getString("allocation_id"),
            settlementId = rs.getString("settlement_id"),
            payableId = rs.getString("payable_id"),
            invoiceId = rs.getString("invoice_id"),
            allocatedAmount = Money(rs.getBigDecimal("allocated_amount")),
            currency = rs.getString("currency") ?: "BDT",
            status = rs.getString("status") ?: "ALLOCATED",
            createdAt = rs.getLong("created_at"),
            createdBy = rs.getString("created_by") ?: "system"
        )
    }

    private fun mapSettlementRow(rs: ResultSet, allocations: List<VendorSettlementAllocation> = emptyList()): VendorSettlement {
        val appAt = rs.getLong("approved_at")
        val setAt = rs.getLong("settled_at")
        return VendorSettlement(
            settlementId = rs.getString("settlement_id"),
            projectId = rs.getString("project_id"),
            tenantId = rs.getString("tenant_id") ?: defaultTenantId,
            vendorId = rs.getString("vendor_id"),
            settlementNumber = rs.getString("settlement_number"),
            settlementDate = rs.getLong("settlement_date"),
            currency = rs.getString("currency") ?: "BDT",
            totalAmount = Money(rs.getBigDecimal("total_amount")),
            status = VendorSettlementStatus.valueOf(rs.getString("status")),
            settlementMethod = SettlementMethod.valueOf(rs.getString("settlement_method")),
            referenceNumber = rs.getString("reference_number"),
            paymentId = rs.getString("payment_id"),
            notes = rs.getString("notes"),
            approvedBy = rs.getString("approved_by"),
            approvedAt = if (rs.wasNull() || appAt == 0L) null else appAt,
            settledAt = if (rs.wasNull() || setAt == 0L) null else setAt,
            allocations = allocations,
            createdAt = rs.getLong("created_at"),
            createdBy = rs.getString("created_by") ?: "system",
            updatedAt = rs.getLong("updated_at"),
            updatedBy = rs.getString("updated_by") ?: "system",
            version = rs.getLong("version")
        )
    }

    override suspend fun insertSettlement(settlement: VendorSettlement): VendorSettlement {
        val tenant = TenantContext(settlement.projectId.ifBlank { defaultTenantId })
        return transactionManager.inTransaction(tenant) { ctx ->
            val sql = """
                INSERT INTO vendor_settlements (
                    settlement_id, project_id, tenant_id, vendor_id, settlement_number,
                    settlement_date, currency, total_amount, status, settlement_method,
                    reference_number, payment_id, notes, approved_by, approved_at,
                    settled_at, created_at, created_by, updated_at, updated_by, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, settlement.settlementId)
                stmt.setString(2, settlement.projectId)
                stmt.setString(3, settlement.tenantId)
                stmt.setString(4, settlement.vendorId)
                stmt.setString(5, settlement.settlementNumber)
                stmt.setLong(6, settlement.settlementDate)
                stmt.setString(7, settlement.currency)
                stmt.setBigDecimal(8, settlement.totalAmount.amount)
                stmt.setString(9, settlement.status.name)
                stmt.setString(10, settlement.settlementMethod.name)
                stmt.setString(11, settlement.referenceNumber)
                stmt.setString(12, settlement.paymentId)
                stmt.setString(13, settlement.notes)
                stmt.setString(14, settlement.approvedBy)
                if (settlement.approvedAt != null) stmt.setLong(15, settlement.approvedAt) else stmt.setNull(15, java.sql.Types.BIGINT)
                if (settlement.settledAt != null) stmt.setLong(16, settlement.settledAt) else stmt.setNull(16, java.sql.Types.BIGINT)
                stmt.setLong(17, settlement.createdAt)
                stmt.setString(18, settlement.createdBy)
                stmt.setLong(19, settlement.updatedAt)
                stmt.setString(20, settlement.updatedBy)
                stmt.setLong(21, settlement.version)
                stmt.executeUpdate()
            }

            val allocSql = """
                INSERT INTO vendor_settlement_allocations (
                    allocation_id, settlement_id, payable_id, invoice_id, allocated_amount,
                    currency, status, created_at, created_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            ctx.connection.prepareStatement(allocSql).use { stmt ->
                for (alloc in settlement.allocations) {
                    stmt.setString(1, alloc.allocationId)
                    stmt.setString(2, settlement.settlementId)
                    stmt.setString(3, alloc.payableId)
                    stmt.setString(4, alloc.invoiceId)
                    stmt.setBigDecimal(5, alloc.allocatedAmount.amount)
                    stmt.setString(6, alloc.currency)
                    stmt.setString(7, alloc.status)
                    stmt.setLong(8, alloc.createdAt)
                    stmt.setString(9, alloc.createdBy)
                    stmt.addBatch()
                }
                stmt.executeBatch()
            }
            settlement
        }
    }

    override suspend fun findSettlementById(settlementId: String, tenantId: String): VendorSettlement? {
        val tenant = TenantContext(tenantId.ifBlank { defaultTenantId })
        return transactionManager.inTransaction(tenant) { ctx ->
            val allocs = mutableListOf<VendorSettlementAllocation>()
            val allocSql = "SELECT * FROM vendor_settlement_allocations WHERE settlement_id = ?"
            ctx.connection.prepareStatement(allocSql).use { stmt ->
                stmt.setString(1, settlementId)
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        allocs.add(mapAllocationRow(rs))
                    }
                }
            }

            val sql = "SELECT * FROM vendor_settlements WHERE settlement_id = ? AND tenant_id = ?"
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, settlementId)
                stmt.setString(2, tenantId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapSettlementRow(rs, allocs) else null
                }
            }
        }
    }

    override suspend fun findSettlementByNumber(settlementNumber: String, tenantId: String): VendorSettlement? {
        val tenant = TenantContext(tenantId.ifBlank { defaultTenantId })
        return transactionManager.inTransaction(tenant) { ctx ->
            val sql = "SELECT * FROM vendor_settlements WHERE settlement_number = ? AND tenant_id = ?"
            var settlement: VendorSettlement? = null
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, settlementNumber)
                stmt.setString(2, tenantId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        settlement = mapSettlementRow(rs)
                    }
                }
            }
            if (settlement != null) {
                val allocs = mutableListOf<VendorSettlementAllocation>()
                val allocSql = "SELECT * FROM vendor_settlement_allocations WHERE settlement_id = ?"
                ctx.connection.prepareStatement(allocSql).use { stmt ->
                    stmt.setString(1, settlement!!.settlementId)
                    stmt.executeQuery().use { rs ->
                        while (rs.next()) {
                            allocs.add(mapAllocationRow(rs))
                        }
                    }
                }
                settlement = settlement!!.copy(allocations = allocs)
            }
            settlement
        }
    }

    override suspend fun updateSettlement(settlement: VendorSettlement): VendorSettlement {
        val tenant = TenantContext(settlement.projectId.ifBlank { defaultTenantId })
        return transactionManager.inTransaction(tenant) { ctx ->
            val sql = """
                UPDATE vendor_settlements SET
                    status = ?, reference_number = ?, payment_id = ?, notes = ?,
                    approved_by = ?, approved_at = ?, settled_at = ?,
                    updated_at = ?, updated_by = ?, version = ?
                WHERE settlement_id = ? AND tenant_id = ? AND version = ?
            """.trimIndent()
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, settlement.status.name)
                stmt.setString(2, settlement.referenceNumber)
                stmt.setString(3, settlement.paymentId)
                stmt.setString(4, settlement.notes)
                stmt.setString(5, settlement.approvedBy)
                if (settlement.approvedAt != null) stmt.setLong(6, settlement.approvedAt) else stmt.setNull(6, java.sql.Types.BIGINT)
                if (settlement.settledAt != null) stmt.setLong(7, settlement.settledAt) else stmt.setNull(7, java.sql.Types.BIGINT)
                stmt.setLong(8, settlement.updatedAt)
                stmt.setString(9, settlement.updatedBy)
                stmt.setLong(10, settlement.version)
                stmt.setString(11, settlement.settlementId)
                stmt.setString(12, settlement.tenantId)
                stmt.setLong(13, settlement.version - 1)
                val updated = stmt.executeUpdate()
                if (updated == 0) {
                    throw IllegalStateException("Optimistic locking conflict on settlement '${settlement.settlementId}'")
                }
            }
            settlement
        }
    }

    override suspend fun listSettlements(
        vendorId: String?,
        status: VendorSettlementStatus?,
        projectId: String?,
        tenantId: String
    ): List<VendorSettlement> {
        val tenant = TenantContext(tenantId.ifBlank { defaultTenantId })
        return transactionManager.inTransaction(tenant) { ctx ->
            val query = StringBuilder("SELECT * FROM vendor_settlements WHERE tenant_id = ?")
            val params = mutableListOf<Any>(tenantId)
            if (vendorId != null) {
                query.append(" AND vendor_id = ?")
                params.add(vendorId)
            }
            if (status != null) {
                query.append(" AND status = ?")
                params.add(status.name)
            }
            if (projectId != null) {
                query.append(" AND project_id = ?")
                params.add(projectId)
            }
            query.append(" ORDER BY created_at DESC")

            val list = mutableListOf<VendorSettlement>()
            ctx.connection.prepareStatement(query.toString()).use { stmt ->
                params.forEachIndexed { i, p -> stmt.setObject(i + 1, p) }
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        list.add(mapSettlementRow(rs))
                    }
                }
            }
            list
        }
    }

    override suspend fun insertReconciliationResult(result: VendorReconciliationResult): VendorReconciliationResult {
        val tenant = TenantContext(result.projectId.ifBlank { defaultTenantId })
        return transactionManager.inTransaction(tenant) { ctx ->
            val sql = """
                INSERT INTO vendor_reconciliation_results (
                    reconciliation_id, vendor_id, project_id, tenant_id, settlement_id,
                    payable_id, payment_id, status, expected_amount, settled_amount,
                    paid_amount, ledger_amount, variance, reasons, reconciled_at, reconciled_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, result.reconciliationId)
                stmt.setString(2, result.vendorId)
                stmt.setString(3, result.projectId)
                stmt.setString(4, result.tenantId)
                stmt.setString(5, result.settlementId)
                stmt.setString(6, result.payableId)
                stmt.setString(7, result.paymentId)
                stmt.setString(8, result.status.name)
                stmt.setBigDecimal(9, result.expectedAmount.amount)
                stmt.setBigDecimal(10, result.settledAmount.amount)
                stmt.setBigDecimal(11, result.paidAmount.amount)
                stmt.setBigDecimal(12, result.ledgerAmount.amount)
                stmt.setBigDecimal(13, result.variance.amount)
                stmt.setString(14, result.reasons.joinToString(";"))
                stmt.setLong(15, result.reconciledAt)
                stmt.setString(16, result.reconciledBy)
                stmt.executeUpdate()
            }
            result
        }
    }

    override suspend fun listReconciliationResults(
        vendorId: String?,
        status: ReconciliationStatus?,
        tenantId: String
    ): List<VendorReconciliationResult> {
        val tenant = TenantContext(tenantId.ifBlank { defaultTenantId })
        return transactionManager.inTransaction(tenant) { ctx ->
            val query = StringBuilder("SELECT * FROM vendor_reconciliation_results WHERE tenant_id = ?")
            val params = mutableListOf<Any>(tenantId)
            if (vendorId != null) {
                query.append(" AND vendor_id = ?")
                params.add(vendorId)
            }
            if (status != null) {
                query.append(" AND status = ?")
                params.add(status.name)
            }
            query.append(" ORDER BY reconciled_at DESC")

            val list = mutableListOf<VendorReconciliationResult>()
            ctx.connection.prepareStatement(query.toString()).use { stmt ->
                params.forEachIndexed { i, p -> stmt.setObject(i + 1, p) }
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        val reasonsStr = rs.getString("reasons") ?: ""
                        list.add(
                            VendorReconciliationResult(
                                reconciliationId = rs.getString("reconciliation_id"),
                                vendorId = rs.getString("vendor_id"),
                                projectId = rs.getString("project_id"),
                                tenantId = rs.getString("tenant_id") ?: defaultTenantId,
                                settlementId = rs.getString("settlement_id"),
                                payableId = rs.getString("payable_id"),
                                paymentId = rs.getString("payment_id"),
                                status = ReconciliationStatus.valueOf(rs.getString("status")),
                                expectedAmount = Money(rs.getBigDecimal("expected_amount")),
                                settledAmount = Money(rs.getBigDecimal("settled_amount")),
                                paidAmount = Money(rs.getBigDecimal("paid_amount")),
                                ledgerAmount = Money(rs.getBigDecimal("ledger_amount")),
                                variance = Money(rs.getBigDecimal("variance")),
                                reasons = if (reasonsStr.isBlank()) emptyList() else reasonsStr.split(";"),
                                reconciledAt = rs.getLong("reconciled_at"),
                                reconciledBy = rs.getString("reconciled_by") ?: "system"
                            )
                        )
                    }
                }
            }
            list
        }
    }

    override suspend fun insertAnalyticsSnapshot(snapshot: VendorAnalyticsSnapshot): VendorAnalyticsSnapshot {
        val tenant = TenantContext(snapshot.projectId.ifBlank { defaultTenantId })
        return transactionManager.inTransaction(tenant) { ctx ->
            val sql = """
                INSERT INTO vendor_analytics_snapshots (
                    snapshot_id, vendor_id, project_id, tenant_id, period,
                    start_date, end_date, generated_at, generated_by,
                    calculation_version, metrics_json
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, snapshot.snapshotId)
                stmt.setString(2, snapshot.vendorId)
                stmt.setString(3, snapshot.projectId)
                stmt.setString(4, snapshot.tenantId)
                stmt.setString(5, snapshot.period.name)
                stmt.setLong(6, snapshot.startDate)
                stmt.setLong(7, snapshot.endDate)
                stmt.setLong(8, snapshot.generatedAt)
                stmt.setString(9, snapshot.generatedBy)
                stmt.setString(10, snapshot.calculationVersion)
                stmt.setString(11, snapshot.metricsJson)
                stmt.executeUpdate()
            }
            snapshot
        }
    }

    override suspend fun listAnalyticsSnapshots(
        vendorId: String,
        period: AnalyticsPeriod?,
        tenantId: String
    ): List<VendorAnalyticsSnapshot> {
        val tenant = TenantContext(tenantId.ifBlank { defaultTenantId })
        return transactionManager.inTransaction(tenant) { ctx ->
            val query = StringBuilder("SELECT * FROM vendor_analytics_snapshots WHERE tenant_id = ? AND vendor_id = ?")
            val params = mutableListOf<Any>(tenantId, vendorId)
            if (period != null) {
                query.append(" AND period = ?")
                params.add(period.name)
            }
            query.append(" ORDER BY generated_at DESC")

            val list = mutableListOf<VendorAnalyticsSnapshot>()
            ctx.connection.prepareStatement(query.toString()).use { stmt ->
                params.forEachIndexed { i, p -> stmt.setObject(i + 1, p) }
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        list.add(
                            VendorAnalyticsSnapshot(
                                snapshotId = rs.getString("snapshot_id"),
                                vendorId = rs.getString("vendor_id"),
                                projectId = rs.getString("project_id"),
                                tenantId = rs.getString("tenant_id") ?: defaultTenantId,
                                period = AnalyticsPeriod.valueOf(rs.getString("period")),
                                startDate = rs.getLong("start_date"),
                                endDate = rs.getLong("end_date"),
                                generatedAt = rs.getLong("generated_at"),
                                generatedBy = rs.getString("generated_by") ?: "system",
                                calculationVersion = rs.getString("calculation_version") ?: "1.0.0",
                                metricsJson = rs.getString("metrics_json") ?: "{}"
                            )
                        )
                    }
                }
            }
            list
        }
    }

    override suspend fun appendAuditEvent(event: VendorSettlementAuditEvent): VendorSettlementAuditEvent {
        val tenant = TenantContext(event.projectId.ifBlank { defaultTenantId })
        return transactionManager.inTransaction(tenant) { ctx ->
            val sql = """
                INSERT INTO vendor_settlement_audit_events (
                    event_id, settlement_id, vendor_id, project_id, tenant_id,
                    event_type, details, actor, timestamp
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, event.eventId)
                stmt.setString(2, event.settlementId)
                stmt.setString(3, event.vendorId)
                stmt.setString(4, event.projectId)
                stmt.setString(5, event.tenantId)
                stmt.setString(6, event.eventType.name)
                stmt.setString(7, event.details)
                stmt.setString(8, event.actor)
                stmt.setLong(9, event.timestamp)
                stmt.executeUpdate()
            }
            event
        }
    }

    override suspend fun listAuditEvents(
        settlementId: String?,
        vendorId: String?,
        tenantId: String
    ): List<VendorSettlementAuditEvent> {
        val tenant = TenantContext(tenantId.ifBlank { defaultTenantId })
        return transactionManager.inTransaction(tenant) { ctx ->
            val query = StringBuilder("SELECT * FROM vendor_settlement_audit_events WHERE tenant_id = ?")
            val params = mutableListOf<Any>(tenantId)
            if (settlementId != null) {
                query.append(" AND settlement_id = ?")
                params.add(settlementId)
            }
            if (vendorId != null) {
                query.append(" AND vendor_id = ?")
                params.add(vendorId)
            }
            query.append(" ORDER BY timestamp DESC")

            val list = mutableListOf<VendorSettlementAuditEvent>()
            ctx.connection.prepareStatement(query.toString()).use { stmt ->
                params.forEachIndexed { i, p -> stmt.setObject(i + 1, p) }
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        list.add(
                            VendorSettlementAuditEvent(
                                eventId = rs.getString("event_id"),
                                settlementId = rs.getString("settlement_id"),
                                vendorId = rs.getString("vendor_id"),
                                projectId = rs.getString("project_id"),
                                tenantId = rs.getString("tenant_id") ?: defaultTenantId,
                                eventType = VendorSettlementAuditEventType.valueOf(rs.getString("event_type")),
                                details = rs.getString("details") ?: "",
                                actor = rs.getString("actor") ?: "system",
                                timestamp = rs.getLong("timestamp")
                            )
                        )
                    }
                }
            }
            list
        }
    }
}

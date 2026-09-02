package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.businessreconciliation.*
import com.sucharu.sucharupro.domain.model.businessreconciliation.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.ResultSet

/**
 * Production PostgreSQL JDBC Data Source for Business Financial Reconciliation Runs, Discrepancies, Snapshots & Audits.
 */
class PostgresBusinessFinancialReconciliationDataSource(
    private val transactionManager: TransactionManager
) : BusinessFinancialReconciliationDataSource {

    // --- Reconciliation Runs ---

    override suspend fun createRun(run: BusinessFinancialReconciliationRun): BusinessFinancialReconciliationRun {
        return transactionManager.inTransaction(TenantContext(run.projectId)) { tx ->
            val sql = """
                INSERT INTO business_financial_reconciliation_runs (
                    id, tenant_id, project_id, period_id, run_number, run_type, status,
                    started_at, completed_at, created_by, reviewed_by, approved_by,
                    total_records_checked, matched_records, discrepancy_count,
                    critical_discrepancy_count, warning_count, checksum, notes,
                    idempotency_key, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, run.id)
                ps.setString(2, run.tenantId)
                ps.setString(3, run.projectId)
                ps.setString(4, run.periodId)
                ps.setString(5, run.runNumber)
                ps.setString(6, run.runType.name)
                ps.setString(7, run.status.name)
                ps.setLong(8, run.startedAt)
                if (run.completedAt != null) ps.setLong(9, run.completedAt) else ps.setNull(9, java.sql.Types.BIGINT)
                ps.setString(10, run.createdBy)
                ps.setString(11, run.reviewedBy)
                ps.setString(12, run.approvedBy)
                ps.setInt(13, run.totalRecordsChecked)
                ps.setInt(14, run.matchedRecords)
                ps.setInt(15, run.discrepancyCount)
                ps.setInt(16, run.criticalDiscrepancyCount)
                ps.setInt(17, run.warningCount)
                ps.setString(18, run.checksum)
                ps.setString(19, run.notes)
                ps.setString(20, run.idempotencyKey)
                ps.setLong(21, run.createdAt)
                ps.setLong(22, run.updatedAt)
                ps.executeUpdate()
            }
            run
        }
    }

    override suspend fun updateRun(run: BusinessFinancialReconciliationRun): BusinessFinancialReconciliationRun {
        return transactionManager.inTransaction(TenantContext(run.projectId)) { tx ->
            val sql = """
                UPDATE business_financial_reconciliation_runs SET
                    status = ?, completed_at = ?, reviewed_by = ?, approved_by = ?,
                    total_records_checked = ?, matched_records = ?, discrepancy_count = ?,
                    critical_discrepancy_count = ?, warning_count = ?, checksum = ?,
                    notes = ?, updated_at = ?
                WHERE id = ? AND tenant_id = ? AND project_id = ?
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, run.status.name)
                if (run.completedAt != null) ps.setLong(2, run.completedAt) else ps.setNull(2, java.sql.Types.BIGINT)
                ps.setString(3, run.reviewedBy)
                ps.setString(4, run.approvedBy)
                ps.setInt(5, run.totalRecordsChecked)
                ps.setInt(6, run.matchedRecords)
                ps.setInt(7, run.discrepancyCount)
                ps.setInt(8, run.criticalDiscrepancyCount)
                ps.setInt(9, run.warningCount)
                ps.setString(10, run.checksum)
                ps.setString(11, run.notes)
                ps.setLong(12, run.updatedAt)
                ps.setString(13, run.id)
                ps.setString(14, run.tenantId)
                ps.setString(15, run.projectId)
                ps.executeUpdate()
            }
            run
        }
    }

    override suspend fun findRunById(id: String, tenantId: String, projectId: String): BusinessFinancialReconciliationRun? {
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            val sql = "SELECT * FROM business_financial_reconciliation_runs WHERE id = ? AND tenant_id = ? AND project_id = ?"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, id)
                ps.setString(2, tenantId)
                ps.setString(3, projectId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapRun(rs) else null
                }
            }
        }
    }

    override suspend fun findRunByNumber(runNumber: String, tenantId: String, projectId: String): BusinessFinancialReconciliationRun? {
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            val sql = "SELECT * FROM business_financial_reconciliation_runs WHERE run_number = ? AND tenant_id = ? AND project_id = ?"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, runNumber)
                ps.setString(2, tenantId)
                ps.setString(3, projectId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapRun(rs) else null
                }
            }
        }
    }

    override suspend fun listRuns(tenantId: String, projectId: String, filter: ReconciliationRunFilter): List<BusinessFinancialReconciliationRun> {
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            val conditions = mutableListOf("tenant_id = ?", "project_id = ?")
            val params = mutableListOf<Any>(tenantId, projectId)

            if (filter.periodId != null) {
                conditions.add("period_id = ?")
                params.add(filter.periodId)
            }
            if (filter.runType != null) {
                conditions.add("run_type = ?")
                params.add(filter.runType.name)
            }
            if (filter.status != null) {
                conditions.add("status = ?")
                params.add(filter.status.name)
            }

            val sql = """
                SELECT * FROM business_financial_reconciliation_runs
                WHERE ${conditions.joinToString(" AND ")}
                ORDER BY created_at DESC
                LIMIT ? OFFSET ?
            """.trimIndent()

            params.add(filter.limit)
            params.add(filter.offset)

            tx.connection.prepareStatement(sql).use { ps ->
                params.forEachIndexed { index, param ->
                    when (param) {
                        is String -> ps.setString(index + 1, param)
                        is Int -> ps.setInt(index + 1, param)
                        is Long -> ps.setLong(index + 1, param)
                    }
                }
                ps.executeQuery().use { rs ->
                    val result = mutableListOf<BusinessFinancialReconciliationRun>()
                    while (rs.next()) {
                        result.add(mapRun(rs))
                    }
                    result
                }
            }
        }
    }

    override suspend fun countRuns(tenantId: String, projectId: String, filter: ReconciliationRunFilter): Long {
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            val conditions = mutableListOf("tenant_id = ?", "project_id = ?")
            val params = mutableListOf<Any>(tenantId, projectId)

            if (filter.periodId != null) {
                conditions.add("period_id = ?")
                params.add(filter.periodId)
            }
            if (filter.runType != null) {
                conditions.add("run_type = ?")
                params.add(filter.runType.name)
            }
            if (filter.status != null) {
                conditions.add("status = ?")
                params.add(filter.status.name)
            }

            val sql = "SELECT COUNT(*) FROM business_financial_reconciliation_runs WHERE ${conditions.joinToString(" AND ")}"
            tx.connection.prepareStatement(sql).use { ps ->
                params.forEachIndexed { index, param ->
                    when (param) {
                        is String -> ps.setString(index + 1, param)
                        is Int -> ps.setInt(index + 1, param)
                        is Long -> ps.setLong(index + 1, param)
                    }
                }
                ps.executeQuery().use { rs ->
                    if (rs.next()) rs.getLong(1) else 0L
                }
            }
        }
    }

    // --- Discrepancies ---

    override suspend fun createDiscrepancy(discrepancy: BusinessFinancialReconciliationDiscrepancy): BusinessFinancialReconciliationDiscrepancy {
        return transactionManager.inTransaction(TenantContext(discrepancy.projectId)) { tx ->
            val sql = """
                INSERT INTO business_financial_reconciliation_discrepancies (
                    id, tenant_id, project_id, reconciliation_run_id, period_id,
                    discrepancy_type, severity, source_type, source_id, expected_amount,
                    actual_amount, difference_amount, currency, description, status,
                    detected_at, assigned_to, resolution_note, resolved_by, resolved_at,
                    approved_by, approved_at, linked_correction_type, linked_correction_id,
                    created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, discrepancy.id)
                ps.setString(2, discrepancy.tenantId)
                ps.setString(3, discrepancy.projectId)
                ps.setString(4, discrepancy.reconciliationRunId)
                ps.setString(5, discrepancy.periodId)
                ps.setString(6, discrepancy.discrepancyType.name)
                ps.setString(7, discrepancy.severity.name)
                ps.setString(8, discrepancy.sourceType)
                ps.setString(9, discrepancy.sourceId)
                ps.setBigDecimal(10, discrepancy.expectedAmount.setScale(4, RoundingMode.HALF_UP))
                ps.setBigDecimal(11, discrepancy.actualAmount.setScale(4, RoundingMode.HALF_UP))
                ps.setBigDecimal(12, discrepancy.differenceAmount.setScale(4, RoundingMode.HALF_UP))
                ps.setString(13, discrepancy.currency)
                ps.setString(14, discrepancy.description)
                ps.setString(15, discrepancy.status.name)
                ps.setLong(16, discrepancy.detectedAt)
                ps.setString(17, discrepancy.assignedTo)
                ps.setString(18, discrepancy.resolutionNote)
                ps.setString(19, discrepancy.resolvedBy)
                if (discrepancy.resolvedAt != null) ps.setLong(20, discrepancy.resolvedAt) else ps.setNull(20, java.sql.Types.BIGINT)
                ps.setString(21, discrepancy.approvedBy)
                if (discrepancy.approvedAt != null) ps.setLong(22, discrepancy.approvedAt) else ps.setNull(22, java.sql.Types.BIGINT)
                ps.setString(23, discrepancy.linkedCorrectionType)
                ps.setString(24, discrepancy.linkedCorrectionId)
                ps.setLong(25, discrepancy.createdAt)
                ps.setLong(26, discrepancy.updatedAt)
                ps.executeUpdate()
            }
            discrepancy
        }
    }

    override suspend fun createDiscrepanciesBatch(discrepancies: List<BusinessFinancialReconciliationDiscrepancy>): List<BusinessFinancialReconciliationDiscrepancy> {
        if (discrepancies.isEmpty()) return emptyList()
        val first = discrepancies.first()
        return transactionManager.inTransaction(TenantContext(first.projectId)) { tx ->
            val sql = """
                INSERT INTO business_financial_reconciliation_discrepancies (
                    id, tenant_id, project_id, reconciliation_run_id, period_id,
                    discrepancy_type, severity, source_type, source_id, expected_amount,
                    actual_amount, difference_amount, currency, description, status,
                    detected_at, assigned_to, resolution_note, resolved_by, resolved_at,
                    approved_by, approved_at, linked_correction_type, linked_correction_id,
                    created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                for (d in discrepancies) {
                    ps.setString(1, d.id)
                    ps.setString(2, d.tenantId)
                    ps.setString(3, d.projectId)
                    ps.setString(4, d.reconciliationRunId)
                    ps.setString(5, d.periodId)
                    ps.setString(6, d.discrepancyType.name)
                    ps.setString(7, d.severity.name)
                    ps.setString(8, d.sourceType)
                    ps.setString(9, d.sourceId)
                    ps.setBigDecimal(10, d.expectedAmount.setScale(4, RoundingMode.HALF_UP))
                    ps.setBigDecimal(11, d.actualAmount.setScale(4, RoundingMode.HALF_UP))
                    ps.setBigDecimal(12, d.differenceAmount.setScale(4, RoundingMode.HALF_UP))
                    ps.setString(13, d.currency)
                    ps.setString(14, d.description)
                    ps.setString(15, d.status.name)
                    ps.setLong(16, d.detectedAt)
                    ps.setString(17, d.assignedTo)
                    ps.setString(18, d.resolutionNote)
                    ps.setString(19, d.resolvedBy)
                    if (d.resolvedAt != null) ps.setLong(20, d.resolvedAt) else ps.setNull(20, java.sql.Types.BIGINT)
                    ps.setString(21, d.approvedBy)
                    if (d.approvedAt != null) ps.setLong(22, d.approvedAt) else ps.setNull(22, java.sql.Types.BIGINT)
                    ps.setString(23, d.linkedCorrectionType)
                    ps.setString(24, d.linkedCorrectionId)
                    ps.setLong(25, d.createdAt)
                    ps.setLong(26, d.updatedAt)
                    ps.addBatch()
                }
                ps.executeBatch()
            }
            discrepancies
        }
    }

    override suspend fun updateDiscrepancy(discrepancy: BusinessFinancialReconciliationDiscrepancy): BusinessFinancialReconciliationDiscrepancy {
        return transactionManager.inTransaction(TenantContext(discrepancy.projectId)) { tx ->
            val sql = """
                UPDATE business_financial_reconciliation_discrepancies SET
                    status = ?, assigned_to = ?, resolution_note = ?, resolved_by = ?,
                    resolved_at = ?, approved_by = ?, approved_at = ?,
                    linked_correction_type = ?, linked_correction_id = ?, updated_at = ?
                WHERE id = ? AND tenant_id = ? AND project_id = ?
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, discrepancy.status.name)
                ps.setString(2, discrepancy.assignedTo)
                ps.setString(3, discrepancy.resolutionNote)
                ps.setString(4, discrepancy.resolvedBy)
                if (discrepancy.resolvedAt != null) ps.setLong(5, discrepancy.resolvedAt) else ps.setNull(5, java.sql.Types.BIGINT)
                ps.setString(6, discrepancy.approvedBy)
                if (discrepancy.approvedAt != null) ps.setLong(7, discrepancy.approvedAt) else ps.setNull(7, java.sql.Types.BIGINT)
                ps.setString(8, discrepancy.linkedCorrectionType)
                ps.setString(9, discrepancy.linkedCorrectionId)
                ps.setLong(10, discrepancy.updatedAt)
                ps.setString(11, discrepancy.id)
                ps.setString(12, discrepancy.tenantId)
                ps.setString(13, discrepancy.projectId)
                ps.executeUpdate()
            }
            discrepancy
        }
    }

    override suspend fun findDiscrepancyById(id: String, tenantId: String, projectId: String): BusinessFinancialReconciliationDiscrepancy? {
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            val sql = "SELECT * FROM business_financial_reconciliation_discrepancies WHERE id = ? AND tenant_id = ? AND project_id = ?"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, id)
                ps.setString(2, tenantId)
                ps.setString(3, projectId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapDiscrepancy(rs) else null
                }
            }
        }
    }

    override suspend fun listDiscrepancies(tenantId: String, projectId: String, filter: DiscrepancyFilter): List<BusinessFinancialReconciliationDiscrepancy> {
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            val conditions = mutableListOf("tenant_id = ?", "project_id = ?")
            val params = mutableListOf<Any>(tenantId, projectId)

            if (filter.reconciliationRunId != null) {
                conditions.add("reconciliation_run_id = ?")
                params.add(filter.reconciliationRunId)
            }
            if (filter.periodId != null) {
                conditions.add("period_id = ?")
                params.add(filter.periodId)
            }
            if (filter.severity != null) {
                conditions.add("severity = ?")
                params.add(filter.severity.name)
            }
            if (filter.status != null) {
                conditions.add("status = ?")
                params.add(filter.status.name)
            }
            if (filter.discrepancyType != null) {
                conditions.add("discrepancy_type = ?")
                params.add(filter.discrepancyType.name)
            }
            if (filter.assignedTo != null) {
                conditions.add("assigned_to = ?")
                params.add(filter.assignedTo)
            }

            val sql = """
                SELECT * FROM business_financial_reconciliation_discrepancies
                WHERE ${conditions.joinToString(" AND ")}
                ORDER BY created_at DESC
                LIMIT ? OFFSET ?
            """.trimIndent()

            params.add(filter.limit)
            params.add(filter.offset)

            tx.connection.prepareStatement(sql).use { ps ->
                params.forEachIndexed { index, param ->
                    when (param) {
                        is String -> ps.setString(index + 1, param)
                        is Int -> ps.setInt(index + 1, param)
                        is Long -> ps.setLong(index + 1, param)
                    }
                }
                ps.executeQuery().use { rs ->
                    val result = mutableListOf<BusinessFinancialReconciliationDiscrepancy>()
                    while (rs.next()) {
                        result.add(mapDiscrepancy(rs))
                    }
                    result
                }
            }
        }
    }

    override suspend fun countDiscrepancies(tenantId: String, projectId: String, filter: DiscrepancyFilter): Long {
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            val conditions = mutableListOf("tenant_id = ?", "project_id = ?")
            val params = mutableListOf<Any>(tenantId, projectId)

            if (filter.reconciliationRunId != null) {
                conditions.add("reconciliation_run_id = ?")
                params.add(filter.reconciliationRunId)
            }
            if (filter.periodId != null) {
                conditions.add("period_id = ?")
                params.add(filter.periodId)
            }
            if (filter.severity != null) {
                conditions.add("severity = ?")
                params.add(filter.severity.name)
            }
            if (filter.status != null) {
                conditions.add("status = ?")
                params.add(filter.status.name)
            }
            if (filter.discrepancyType != null) {
                conditions.add("discrepancy_type = ?")
                params.add(filter.discrepancyType.name)
            }
            if (filter.assignedTo != null) {
                conditions.add("assigned_to = ?")
                params.add(filter.assignedTo)
            }

            val sql = "SELECT COUNT(*) FROM business_financial_reconciliation_discrepancies WHERE ${conditions.joinToString(" AND ")}"
            tx.connection.prepareStatement(sql).use { ps ->
                params.forEachIndexed { index, param ->
                    when (param) {
                        is String -> ps.setString(index + 1, param)
                        is Int -> ps.setInt(index + 1, param)
                        is Long -> ps.setLong(index + 1, param)
                    }
                }
                ps.executeQuery().use { rs ->
                    if (rs.next()) rs.getLong(1) else 0L
                }
            }
        }
    }

    // --- Snapshots ---

    override suspend fun saveSnapshot(snapshot: BusinessFinancialReconciliationSnapshot): BusinessFinancialReconciliationSnapshot {
        return transactionManager.inTransaction(TenantContext(snapshot.projectId)) { tx ->
            val sql = """
                INSERT INTO business_financial_reconciliation_snapshots (
                    id, tenant_id, project_id, reconciliation_run_id, period_id, snapshot_data, checksum, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, snapshot.id)
                ps.setString(2, snapshot.tenantId)
                ps.setString(3, snapshot.projectId)
                ps.setString(4, snapshot.reconciliationRunId)
                ps.setString(5, snapshot.periodId)
                ps.setString(6, snapshot.snapshotData)
                ps.setString(7, snapshot.checksum)
                ps.setLong(8, snapshot.createdAt)
                ps.executeUpdate()
            }
            snapshot
        }
    }

    override suspend fun findSnapshotByRunId(runId: String, tenantId: String, projectId: String): BusinessFinancialReconciliationSnapshot? {
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            val sql = "SELECT * FROM business_financial_reconciliation_snapshots WHERE reconciliation_run_id = ? AND tenant_id = ? AND project_id = ?"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, runId)
                ps.setString(2, tenantId)
                ps.setString(3, projectId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) mapSnapshot(rs) else null
                }
            }
        }
    }

    // --- Audit Events ---

    override suspend fun recordAuditEvent(event: BusinessFinancialReconciliationAuditEvent): BusinessFinancialReconciliationAuditEvent {
        return transactionManager.inTransaction(TenantContext(event.projectId)) { tx ->
            val sql = """
                INSERT INTO business_financial_reconciliation_audit_events (
                    id, tenant_id, project_id, reconciliation_run_id, discrepancy_id,
                    event_type, actor_id, actor_role, correlation_id, idempotency_key,
                    reason, before_state, after_state, checksum, timestamp
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, event.id)
                ps.setString(2, event.tenantId)
                ps.setString(3, event.projectId)
                ps.setString(4, event.reconciliationRunId)
                ps.setString(5, event.discrepancyId)
                ps.setString(6, event.eventType)
                ps.setString(7, event.actorId)
                ps.setString(8, event.actorRole)
                ps.setString(9, event.correlationId)
                ps.setString(10, event.idempotencyKey)
                ps.setString(11, event.reason)
                ps.setString(12, event.beforeState)
                ps.setString(13, event.afterState)
                ps.setString(14, event.checksum)
                ps.setLong(15, event.timestamp)
                ps.executeUpdate()
            }
            event
        }
    }

    override suspend fun listAuditEvents(tenantId: String, projectId: String, runId: String?, discrepancyId: String?): List<BusinessFinancialReconciliationAuditEvent> {
        return transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
            val conditions = mutableListOf("tenant_id = ?", "project_id = ?")
            val params = mutableListOf<Any>(tenantId, projectId)

            if (runId != null) {
                conditions.add("reconciliation_run_id = ?")
                params.add(runId)
            }
            if (discrepancyId != null) {
                conditions.add("discrepancy_id = ?")
                params.add(discrepancyId)
            }

            val sql = """
                SELECT * FROM business_financial_reconciliation_audit_events
                WHERE ${conditions.joinToString(" AND ")}
                ORDER BY timestamp DESC
            """.trimIndent()

            tx.connection.prepareStatement(sql).use { ps ->
                params.forEachIndexed { index, param ->
                    when (param) {
                        is String -> ps.setString(index + 1, param)
                        is Int -> ps.setInt(index + 1, param)
                        is Long -> ps.setLong(index + 1, param)
                    }
                }
                ps.executeQuery().use { rs ->
                    val result = mutableListOf<BusinessFinancialReconciliationAuditEvent>()
                    while (rs.next()) {
                        result.add(mapAuditEvent(rs))
                    }
                    result
                }
            }
        }
    }

    // --- ResultSet Mappers ---

    private fun mapRun(rs: ResultSet): BusinessFinancialReconciliationRun {
        return BusinessFinancialReconciliationRun(
            id = rs.getString("id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            periodId = rs.getString("period_id"),
            runNumber = rs.getString("run_number"),
            runType = ReconciliationRunType.valueOf(rs.getString("run_type")),
            status = ReconciliationRunStatus.valueOf(rs.getString("status")),
            startedAt = rs.getLong("started_at"),
            completedAt = rs.getLong("completed_at").takeIf { !rs.wasNull() },
            createdBy = rs.getString("created_by"),
            reviewedBy = rs.getString("reviewed_by"),
            approvedBy = rs.getString("approved_by"),
            totalRecordsChecked = rs.getInt("total_records_checked"),
            matchedRecords = rs.getInt("matched_records"),
            discrepancyCount = rs.getInt("discrepancy_count"),
            criticalDiscrepancyCount = rs.getInt("critical_discrepancy_count"),
            warningCount = rs.getInt("warning_count"),
            checksum = rs.getString("checksum"),
            notes = rs.getString("notes"),
            idempotencyKey = rs.getString("idempotency_key"),
            createdAt = rs.getLong("created_at"),
            updatedAt = rs.getLong("updated_at")
        )
    }

    private fun mapDiscrepancy(rs: ResultSet): BusinessFinancialReconciliationDiscrepancy {
        return BusinessFinancialReconciliationDiscrepancy(
            id = rs.getString("id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            reconciliationRunId = rs.getString("reconciliation_run_id"),
            periodId = rs.getString("period_id"),
            discrepancyType = FinancialDiscrepancyType.valueOf(rs.getString("discrepancy_type")),
            severity = DiscrepancySeverity.valueOf(rs.getString("severity")),
            sourceType = rs.getString("source_type"),
            sourceId = rs.getString("source_id"),
            expectedAmount = rs.getBigDecimal("expected_amount").setScale(4, RoundingMode.HALF_UP),
            actualAmount = rs.getBigDecimal("actual_amount").setScale(4, RoundingMode.HALF_UP),
            differenceAmount = rs.getBigDecimal("difference_amount").setScale(4, RoundingMode.HALF_UP),
            currency = rs.getString("currency"),
            description = rs.getString("description"),
            status = DiscrepancyStatus.valueOf(rs.getString("status")),
            detectedAt = rs.getLong("detected_at"),
            assignedTo = rs.getString("assigned_to"),
            resolutionNote = rs.getString("resolution_note"),
            resolvedBy = rs.getString("resolved_by"),
            resolvedAt = rs.getLong("resolved_at").takeIf { !rs.wasNull() },
            approvedBy = rs.getString("approved_by"),
            approvedAt = rs.getLong("approved_at").takeIf { !rs.wasNull() },
            linkedCorrectionType = rs.getString("linked_correction_type"),
            linkedCorrectionId = rs.getString("linked_correction_id"),
            createdAt = rs.getLong("created_at"),
            updatedAt = rs.getLong("updated_at")
        )
    }

    private fun mapSnapshot(rs: ResultSet): BusinessFinancialReconciliationSnapshot {
        return BusinessFinancialReconciliationSnapshot(
            id = rs.getString("id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            reconciliationRunId = rs.getString("reconciliation_run_id"),
            periodId = rs.getString("period_id"),
            snapshotData = rs.getString("snapshot_data"),
            checksum = rs.getString("checksum"),
            createdAt = rs.getLong("created_at")
        )
    }

    private fun mapAuditEvent(rs: ResultSet): BusinessFinancialReconciliationAuditEvent {
        return BusinessFinancialReconciliationAuditEvent(
            id = rs.getString("id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            reconciliationRunId = rs.getString("reconciliation_run_id"),
            discrepancyId = rs.getString("discrepancy_id"),
            eventType = rs.getString("event_type"),
            actorId = rs.getString("actor_id"),
            actorRole = rs.getString("actor_role"),
            correlationId = rs.getString("correlation_id"),
            idempotencyKey = rs.getString("idempotency_key"),
            reason = rs.getString("reason"),
            beforeState = rs.getString("before_state"),
            afterState = rs.getString("after_state"),
            checksum = rs.getString("checksum"),
            timestamp = rs.getLong("timestamp")
        )
    }
}

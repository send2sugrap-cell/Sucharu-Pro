package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.businessintegrity.BusinessFinancialIntegrityDataSource
import com.sucharu.sucharupro.domain.model.businessintegrity.*
import com.sucharu.sucharupro.domain.repository.businessintegrity.FinancialIntegrityRunFilter
import java.sql.ResultSet

class PostgresBusinessFinancialIntegrityDataSource(
    private val transactionManager: TransactionManager
) : BusinessFinancialIntegrityDataSource {

    override suspend fun saveIntegrityRun(run: FinancialIntegrityRun): FinancialIntegrityRun {
        return transactionManager.inTransaction(TenantContext(run.projectId)) { tx ->
            val sqlRun = """
                INSERT INTO business_financial_integrity_runs (
                    id, tenant_id, project_id, period_id, run_number, status, executed_by,
                    started_at, completed_at, total_assertions_count, passed_assertions_count,
                    warning_assertions_count, failed_assertions_count, blocked_assertions_count,
                    integrity_checksum, notes, idempotency_key, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    status = EXCLUDED.status,
                    completed_at = EXCLUDED.completed_at,
                    passed_assertions_count = EXCLUDED.passed_assertions_count,
                    warning_assertions_count = EXCLUDED.warning_assertions_count,
                    failed_assertions_count = EXCLUDED.failed_assertions_count,
                    blocked_assertions_count = EXCLUDED.blocked_assertions_count,
                    integrity_checksum = EXCLUDED.integrity_checksum,
                    notes = EXCLUDED.notes,
                    updated_at = EXCLUDED.updated_at
            """.trimIndent()

            tx.connection.prepareStatement(sqlRun).use { ps ->
                ps.setString(1, run.id)
                ps.setString(2, run.tenantId)
                ps.setString(3, run.projectId)
                ps.setString(4, run.periodId)
                ps.setString(5, run.runNumber)
                ps.setString(6, run.status.name)
                ps.setString(7, run.executedBy)
                ps.setLong(8, run.startedAt)
                if (run.completedAt != null) ps.setLong(9, run.completedAt) else ps.setNull(9, java.sql.Types.BIGINT)
                ps.setInt(10, run.totalAssertionsCount)
                ps.setInt(11, run.passedAssertionsCount)
                ps.setInt(12, run.warningAssertionsCount)
                ps.setInt(13, run.failedAssertionsCount)
                ps.setInt(14, run.blockedAssertionsCount)
                ps.setString(15, run.integrityChecksum)
                ps.setString(16, run.notes)
                ps.setString(17, run.idempotencyKey)
                ps.setLong(18, run.createdAt)
                ps.setLong(19, run.updatedAt)
                ps.executeUpdate()
            }

            if (run.assertions.isNotEmpty()) {
                val sqlAssertion = """
                    INSERT INTO business_financial_control_assertions (
                        id, tenant_id, project_id, run_id, period_id, assertion_type,
                        assertion_name, status, severity, expected_value, actual_value,
                        variance_value, explanation, recommended_action, source_entities_count,
                        evaluated_at, created_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (id) DO NOTHING
                """.trimIndent()

                tx.connection.prepareStatement(sqlAssertion).use { ps ->
                    for (a in run.assertions) {
                        ps.setString(1, a.id)
                        ps.setString(2, a.tenantId)
                        ps.setString(3, a.projectId)
                        ps.setString(4, a.runId)
                        ps.setString(5, a.periodId)
                        ps.setString(6, a.assertionType.name)
                        ps.setString(7, a.assertionName)
                        ps.setString(8, a.status.name)
                        ps.setString(9, a.severity.name)
                        ps.setString(10, a.expectedValue)
                        ps.setString(11, a.actualValue)
                        ps.setString(12, a.varianceValue)
                        ps.setString(13, a.explanation)
                        ps.setString(14, a.recommendedAction)
                        ps.setInt(15, a.sourceEntitiesCount)
                        ps.setLong(16, a.evaluatedAt)
                        ps.setLong(17, a.createdAt)
                        ps.addBatch()
                    }
                    ps.executeBatch()
                }
            }

            run
        }
    }

    override suspend fun getIntegrityRunById(
        tenantId: String,
        projectId: String,
        runId: String
    ): FinancialIntegrityRun? {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val sql = "SELECT * FROM business_financial_integrity_runs WHERE tenant_id = ? AND project_id = ? AND id = ?"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, projectId)
                ps.setString(3, runId)
                val rs = ps.executeQuery()
                if (rs.next()) {
                    val run = mapIntegrityRun(rs)
                    val runAssertions = getAssertionsByRunId(tenantId, projectId, runId)
                    run.copy(assertions = runAssertions)
                } else null
            }
        }
    }

    override suspend fun findRunByNumber(
        tenantId: String,
        projectId: String,
        runNumber: String
    ): FinancialIntegrityRun? {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val sql = "SELECT * FROM business_financial_integrity_runs WHERE tenant_id = ? AND project_id = ? AND run_number = ?"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, projectId)
                ps.setString(3, runNumber)
                val rs = ps.executeQuery()
                if (rs.next()) {
                    val run = mapIntegrityRun(rs)
                    val runAssertions = getAssertionsByRunId(tenantId, projectId, run.id)
                    run.copy(assertions = runAssertions)
                } else null
            }
        }
    }

    override suspend fun listIntegrityRuns(
        tenantId: String,
        projectId: String,
        filter: FinancialIntegrityRunFilter
    ): List<FinancialIntegrityRun> {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val conditions = mutableListOf("tenant_id = ?", "project_id = ?")
            val params = mutableListOf<Any>(tenantId, projectId)

            if (filter.periodId != null) {
                conditions.add("period_id = ?")
                params.add(filter.periodId)
            }
            if (filter.status != null) {
                conditions.add("status = ?")
                params.add(filter.status.name)
            }

            val sql = "SELECT * FROM business_financial_integrity_runs WHERE ${conditions.joinToString(" AND ")} ORDER BY started_at DESC LIMIT ? OFFSET ?"
            tx.connection.prepareStatement(sql).use { ps ->
                var idx = 1
                for (p in params) {
                    ps.setObject(idx++, p)
                }
                ps.setInt(idx++, filter.limit)
                ps.setInt(idx, filter.offset)
                val rs = ps.executeQuery()
                val list = mutableListOf<FinancialIntegrityRun>()
                while (rs.next()) {
                    list.add(mapIntegrityRun(rs))
                }
                list
            }
        }
    }

    override suspend fun getAssertionsByRunId(
        tenantId: String,
        projectId: String,
        runId: String
    ): List<FinancialControlAssertion> {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val sql = "SELECT * FROM business_financial_control_assertions WHERE tenant_id = ? AND project_id = ? AND run_id = ? ORDER BY id ASC"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, projectId)
                ps.setString(3, runId)
                val rs = ps.executeQuery()
                val list = mutableListOf<FinancialControlAssertion>()
                while (rs.next()) {
                    list.add(mapAssertion(rs))
                }
                list
            }
        }
    }

    override suspend fun savePeriodCloseCertificate(certificate: PeriodCloseCertificate): PeriodCloseCertificate {
        return transactionManager.inTransaction(TenantContext(certificate.projectId)) { tx ->
            val sql = """
                INSERT INTO business_financial_period_close_certificates (
                    id, tenant_id, project_id, period_id, period_code, final_run_id,
                    closed_by, closed_at, approved_by, approved_at, status,
                    total_recognized_expenses, total_settled_payables, total_ledger_debit,
                    total_ledger_credit, net_recognized_adjustments, certificate_checksum,
                    snapshot_payload_json, notes, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO NOTHING
            """.trimIndent()

            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, certificate.id)
                ps.setString(2, certificate.tenantId)
                ps.setString(3, certificate.projectId)
                ps.setString(4, certificate.periodId)
                ps.setString(5, certificate.periodCode)
                ps.setString(6, certificate.finalRunId)
                ps.setString(7, certificate.closedBy)
                ps.setLong(8, certificate.closedAt)
                ps.setString(9, certificate.approvedBy)
                ps.setLong(10, certificate.approvedAt)
                ps.setString(11, certificate.status)
                ps.setBigDecimal(12, certificate.totalRecognizedExpenses)
                ps.setBigDecimal(13, certificate.totalSettledPayables)
                ps.setBigDecimal(14, certificate.totalLedgerDebit)
                ps.setBigDecimal(15, certificate.totalLedgerCredit)
                ps.setBigDecimal(16, certificate.netRecognizedAdjustments)
                ps.setString(17, certificate.certificateChecksum)
                ps.setString(18, certificate.snapshotPayloadJson)
                ps.setString(19, certificate.notes)
                ps.setLong(20, certificate.createdAt)
                ps.executeUpdate()
            }
            certificate
        }
    }

    override suspend fun getPeriodCloseCertificate(
        tenantId: String,
        projectId: String,
        periodId: String
    ): PeriodCloseCertificate? {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val sql = "SELECT * FROM business_financial_period_close_certificates WHERE tenant_id = ? AND project_id = ? AND period_id = ?"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, projectId)
                ps.setString(3, periodId)
                val rs = ps.executeQuery()
                if (rs.next()) mapCertificate(rs) else null
            }
        }
    }

    override suspend fun listPeriodCloseCertificates(
        tenantId: String,
        projectId: String
    ): List<PeriodCloseCertificate> {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val sql = "SELECT * FROM business_financial_period_close_certificates WHERE tenant_id = ? AND project_id = ? ORDER BY closed_at DESC"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, projectId)
                val rs = ps.executeQuery()
                val list = mutableListOf<PeriodCloseCertificate>()
                while (rs.next()) {
                    list.add(mapCertificate(rs))
                }
                list
            }
        }
    }

    private fun mapIntegrityRun(rs: ResultSet): FinancialIntegrityRun {
        return FinancialIntegrityRun(
            id = rs.getString("id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            periodId = rs.getString("period_id"),
            runNumber = rs.getString("run_number"),
            status = FinancialIntegrityStatus.valueOf(rs.getString("status")),
            executedBy = rs.getString("executed_by"),
            startedAt = rs.getLong("started_at"),
            completedAt = rs.getObject("completed_at") as? Long,
            totalAssertionsCount = rs.getInt("total_assertions_count"),
            passedAssertionsCount = rs.getInt("passed_assertions_count"),
            warningAssertionsCount = rs.getInt("warning_assertions_count"),
            failedAssertionsCount = rs.getInt("failed_assertions_count"),
            blockedAssertionsCount = rs.getInt("blocked_assertions_count"),
            integrityChecksum = rs.getString("integrity_checksum"),
            notes = rs.getString("notes"),
            idempotencyKey = rs.getString("idempotency_key"),
            createdAt = rs.getLong("created_at"),
            updatedAt = rs.getLong("updated_at")
        )
    }

    private fun mapAssertion(rs: ResultSet): FinancialControlAssertion {
        return FinancialControlAssertion(
            id = rs.getString("id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            runId = rs.getString("run_id"),
            periodId = rs.getString("period_id"),
            assertionType = FinancialAssertionType.valueOf(rs.getString("assertion_type")),
            assertionName = rs.getString("assertion_name"),
            status = FinancialIntegrityStatus.valueOf(rs.getString("status")),
            severity = AssertionSeverity.valueOf(rs.getString("severity")),
            expectedValue = rs.getString("expected_value"),
            actualValue = rs.getString("actual_value"),
            varianceValue = rs.getString("variance_value"),
            explanation = rs.getString("explanation"),
            recommendedAction = rs.getString("recommended_action"),
            sourceEntitiesCount = rs.getInt("source_entities_count"),
            evaluatedAt = rs.getLong("evaluated_at"),
            createdAt = rs.getLong("created_at")
        )
    }

    private fun mapCertificate(rs: ResultSet): PeriodCloseCertificate {
        return PeriodCloseCertificate(
            id = rs.getString("id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            periodId = rs.getString("period_id"),
            periodCode = rs.getString("period_code"),
            finalRunId = rs.getString("final_run_id"),
            closedBy = rs.getString("closed_by"),
            closedAt = rs.getLong("closed_at"),
            approvedBy = rs.getString("approved_by"),
            approvedAt = rs.getLong("approved_at"),
            status = rs.getString("status"),
            totalRecognizedExpenses = rs.getBigDecimal("total_recognized_expenses"),
            totalSettledPayables = rs.getBigDecimal("total_settled_payables"),
            totalLedgerDebit = rs.getBigDecimal("total_ledger_debit"),
            totalLedgerCredit = rs.getBigDecimal("total_ledger_credit"),
            netRecognizedAdjustments = rs.getBigDecimal("net_recognized_adjustments"),
            certificateChecksum = rs.getString("certificate_checksum"),
            snapshotPayloadJson = rs.getString("snapshot_payload_json"),
            notes = rs.getString("notes"),
            createdAt = rs.getLong("created_at")
        )
    }
}

package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.preflight.PreflightDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.preflight.*
import java.sql.ResultSet

/**
 * PostgreSQL implementation of PreflightDataSource using TransactionManager & RLS.
 */
class PostgresPreflightDataSource(
    private val transactionManager: TransactionManager
) : PreflightDataSource {

    override suspend fun saveRun(run: PreflightRun): DomainResult<PreflightRun> {
        return try {
            transactionManager.inTransaction(TenantContext(run.tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = """
                    INSERT INTO preflight_runs (
                        preflight_run_id, tenant_id, job_id, artwork_id, artwork_version_id,
                        proof_id, status, overall_result, engine_version, idempotency_key,
                        requested_by, started_at, completed_at, summary, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, to_timestamp(? / 1000.0), to_timestamp(? / 1000.0))
                    ON CONFLICT (preflight_run_id) DO UPDATE SET
                        status = EXCLUDED.status,
                        overall_result = EXCLUDED.overall_result,
                        started_at = EXCLUDED.started_at,
                        completed_at = EXCLUDED.completed_at,
                        summary = EXCLUDED.summary,
                        updated_at = EXCLUDED.updated_at
                """.trimIndent()

                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, run.preflightRunId)
                    ps.setString(2, run.tenantId)
                    ps.setString(3, run.jobId)
                    ps.setString(4, run.artworkId)
                    ps.setString(5, run.artworkVersionId)
                    ps.setString(6, run.proofId)
                    ps.setString(7, run.status.name)
                    ps.setString(8, run.overallResult.name)
                    ps.setString(9, run.engineVersion)
                    ps.setString(10, run.idempotencyKey)
                    ps.setString(11, run.requestedBy)
                    if (run.startedAt != null) ps.setTimestamp(12, java.sql.Timestamp(run.startedAt)) else ps.setNull(12, java.sql.Types.TIMESTAMP)
                    if (run.completedAt != null) ps.setTimestamp(13, java.sql.Timestamp(run.completedAt)) else ps.setNull(13, java.sql.Types.TIMESTAMP)
                    ps.setString(14, run.summary)
                    ps.setLong(15, run.createdAt)
                    ps.setLong(16, run.updatedAt)
                    ps.executeUpdate()
                }
            }
            DomainResult.Success(run)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "save preflight run")
        }
    }

    override suspend fun getRunById(tenantId: String, runId: String): DomainResult<PreflightRun?> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = "SELECT * FROM preflight_runs WHERE tenant_id = ? AND preflight_run_id = ?"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setString(2, runId)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) mapRun(rs) else null
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "get preflight run by id")
        }
    }

    override suspend fun getRunByIdempotencyKey(
        tenantId: String,
        idempotencyKey: String
    ): DomainResult<PreflightRun?> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = "SELECT * FROM preflight_runs WHERE tenant_id = ? AND idempotency_key = ? ORDER BY created_at DESC LIMIT 1"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setString(2, idempotencyKey)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) mapRun(rs) else null
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "get preflight run by idempotency key")
        }
    }

    override suspend fun listRunsByArtwork(
        tenantId: String,
        artworkId: String,
        limit: Int
    ): DomainResult<List<PreflightRun>> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = "SELECT * FROM preflight_runs WHERE tenant_id = ? AND artwork_id = ? ORDER BY created_at DESC LIMIT ?"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setString(2, artworkId)
                    ps.setInt(3, limit)
                    ps.executeQuery().use { rs ->
                        val list = mutableListOf<PreflightRun>()
                        while (rs.next()) list.add(mapRun(rs))
                        list
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "list preflight runs by artwork")
        }
    }

    override suspend fun saveRuleExecutions(executions: List<PreflightRuleExecution>): DomainResult<List<PreflightRuleExecution>> {
        if (executions.isEmpty()) return DomainResult.Success(emptyList())
        return try {
            val tenantId = executions.first().tenantId
            transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = """
                    INSERT INTO preflight_rule_executions (
                        execution_id, tenant_id, preflight_run_id, rule_id,
                        rule_code, result, started_at, completed_at, error_message
                    ) VALUES (?, ?, ?, ?, ?, ?, to_timestamp(? / 1000.0), to_timestamp(? / 1000.0), ?)
                    ON CONFLICT (execution_id) DO NOTHING
                """.trimIndent()

                conn.prepareStatement(sql).use { ps ->
                    for (exec in executions) {
                        ps.setString(1, exec.executionId)
                        ps.setString(2, exec.tenantId)
                        ps.setString(3, exec.preflightRunId)
                        ps.setString(4, exec.ruleId)
                        ps.setString(5, exec.ruleCode)
                        ps.setString(6, exec.result.name)
                        ps.setLong(7, exec.startedAt)
                        ps.setLong(8, exec.completedAt)
                        ps.setString(9, exec.errorMessage)
                        ps.addBatch()
                    }
                    ps.executeBatch()
                }
            }
            DomainResult.Success(executions)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "save preflight rule executions")
        }
    }

    override suspend fun listRuleExecutionsByRun(
        tenantId: String,
        runId: String
    ): DomainResult<List<PreflightRuleExecution>> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = "SELECT * FROM preflight_rule_executions WHERE tenant_id = ? AND preflight_run_id = ? ORDER BY started_at ASC"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setString(2, runId)
                    ps.executeQuery().use { rs ->
                        val list = mutableListOf<PreflightRuleExecution>()
                        while (rs.next()) list.add(mapExecution(rs))
                        list
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "list preflight rule executions")
        }
    }

    override suspend fun saveFindings(findings: List<PreflightFinding>): DomainResult<List<PreflightFinding>> {
        if (findings.isEmpty()) return DomainResult.Success(emptyList())
        return try {
            val tenantId = findings.first().tenantId
            transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = """
                    INSERT INTO preflight_findings (
                        finding_id, tenant_id, preflight_run_id, execution_id,
                        rule_code, category, severity, message, expected_value, actual_value,
                        location_context, created_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, to_timestamp(? / 1000.0))
                    ON CONFLICT (finding_id) DO NOTHING
                """.trimIndent()

                conn.prepareStatement(sql).use { ps ->
                    for (f in findings) {
                        ps.setString(1, f.findingId)
                        ps.setString(2, f.tenantId)
                        ps.setString(3, f.preflightRunId)
                        ps.setString(4, f.executionId)
                        ps.setString(5, f.ruleCode)
                        ps.setString(6, f.category.name)
                        ps.setString(7, f.severity.name)
                        ps.setString(8, f.message)
                        ps.setString(9, f.expectedValue)
                        ps.setString(10, f.actualValue)
                        ps.setString(11, f.locationContext)
                        ps.setLong(12, f.createdAt)
                        ps.addBatch()
                    }
                    ps.executeBatch()
                }
            }
            DomainResult.Success(findings)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "save preflight findings")
        }
    }

    override suspend fun listFindingsByRun(
        tenantId: String,
        runId: String
    ): DomainResult<List<PreflightFinding>> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = "SELECT * FROM preflight_findings WHERE tenant_id = ? AND preflight_run_id = ? ORDER BY created_at ASC"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setString(2, runId)
                    ps.executeQuery().use { rs ->
                        val list = mutableListOf<PreflightFinding>()
                        while (rs.next()) list.add(mapFinding(rs))
                        list
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "list preflight findings")
        }
    }

    override suspend fun getFindingById(tenantId: String, findingId: String): DomainResult<PreflightFinding?> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = "SELECT * FROM preflight_findings WHERE tenant_id = ? AND finding_id = ?"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setString(2, findingId)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) mapFinding(rs) else null
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "get preflight finding by id")
        }
    }

    override suspend fun updateFinding(finding: PreflightFinding): DomainResult<PreflightFinding> {
        return try {
            transactionManager.inTransaction(TenantContext(finding.tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = """
                    UPDATE preflight_findings SET
                        status = ?,
                        acknowledged_by = ?,
                        acknowledged_at = ?,
                        resolved_at = ?,
                        resolved_by = ?,
                        waiver_reason = ?,
                        waived_by = ?,
                        waived_at = ?,
                        revalidation_run_id = ?
                    WHERE tenant_id = ? AND finding_id = ?
                """.trimIndent()

                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, finding.status.name)
                    ps.setString(2, finding.acknowledgedBy)
                    if (finding.acknowledgedAt != null) ps.setTimestamp(3, java.sql.Timestamp(finding.acknowledgedAt)) else ps.setNull(3, java.sql.Types.TIMESTAMP)
                    if (finding.resolvedAt != null) ps.setTimestamp(4, java.sql.Timestamp(finding.resolvedAt)) else ps.setNull(4, java.sql.Types.TIMESTAMP)
                    ps.setString(5, finding.resolvedBy)
                    ps.setString(6, finding.waiverReason)
                    ps.setString(7, finding.waivedBy)
                    if (finding.waivedAt != null) ps.setTimestamp(8, java.sql.Timestamp(finding.waivedAt)) else ps.setNull(8, java.sql.Types.TIMESTAMP)
                    ps.setString(9, finding.revalidationRunId)
                    ps.setString(10, finding.tenantId)
                    ps.setString(11, finding.findingId)
                    ps.executeUpdate()
                }
            }
            DomainResult.Success(finding)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "update preflight finding")
        }
    }

    override suspend fun saveCorrection(correction: PreflightFindingCorrection): DomainResult<PreflightFindingCorrection> {
        return try {
            transactionManager.inTransaction(TenantContext(correction.tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = """
                    INSERT INTO preflight_finding_corrections (
                        correction_id, tenant_id, finding_id, preflight_run_id,
                        correction_type, description, artwork_version_id,
                        submitted_by, submitted_at, revalidation_run_id
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, to_timestamp(? / 1000.0), ?)
                    ON CONFLICT (correction_id) DO NOTHING
                """.trimIndent()

                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, correction.correctionId)
                    ps.setString(2, correction.tenantId)
                    ps.setString(3, correction.findingId)
                    ps.setString(4, correction.preflightRunId)
                    ps.setString(5, correction.correctionType.name)
                    ps.setString(6, correction.description)
                    ps.setString(7, correction.artworkVersionId)
                    ps.setString(8, correction.submittedBy)
                    ps.setLong(9, correction.submittedAt)
                    ps.setString(10, correction.revalidationRunId)
                    ps.executeUpdate()
                }
            }
            DomainResult.Success(correction)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "save preflight finding correction")
        }
    }

    override suspend fun listCorrectionsForFinding(
        tenantId: String,
        findingId: String
    ): DomainResult<List<PreflightFindingCorrection>> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = "SELECT * FROM preflight_finding_corrections WHERE tenant_id = ? AND finding_id = ? ORDER BY submitted_at ASC"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setString(2, findingId)
                    ps.executeQuery().use { rs ->
                        val list = mutableListOf<PreflightFindingCorrection>()
                        while (rs.next()) list.add(mapCorrection(rs))
                        list
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "list preflight finding corrections")
        }
    }

    private fun mapRun(rs: ResultSet): PreflightRun {
        val startTs = rs.getTimestamp("started_at")
        val compTs = rs.getTimestamp("completed_at")
        val createdTs = rs.getTimestamp("created_at")
        val updatedTs = rs.getTimestamp("updated_at")
        return PreflightRun(
            preflightRunId = rs.getString("preflight_run_id"),
            tenantId = rs.getString("tenant_id"),
            jobId = rs.getString("job_id"),
            artworkId = rs.getString("artwork_id"),
            artworkVersionId = rs.getString("artwork_version_id"),
            proofId = rs.getString("proof_id"),
            status = try { PreflightRunStatus.valueOf(rs.getString("status")) } catch (_: Exception) { PreflightRunStatus.REQUESTED },
            overallResult = try { PreflightOverallResult.valueOf(rs.getString("overall_result")) } catch (_: Exception) { PreflightOverallResult.NOT_EVALUATED },
            engineVersion = rs.getString("engine_version") ?: "v1.0",
            idempotencyKey = rs.getString("idempotency_key"),
            requestedBy = rs.getString("requested_by"),
            startedAt = startTs?.time,
            completedAt = compTs?.time,
            summary = rs.getString("summary"),
            createdAt = createdTs?.time ?: System.currentTimeMillis(),
            updatedAt = updatedTs?.time ?: System.currentTimeMillis()
        )
    }

    private fun mapExecution(rs: ResultSet): PreflightRuleExecution {
        val startTs = rs.getTimestamp("started_at")
        val compTs = rs.getTimestamp("completed_at")
        return PreflightRuleExecution(
            executionId = rs.getString("execution_id"),
            tenantId = rs.getString("tenant_id"),
            preflightRunId = rs.getString("preflight_run_id"),
            ruleId = rs.getString("rule_id"),
            ruleCode = rs.getString("rule_code"),
            result = try { PreflightExecutionResult.valueOf(rs.getString("result")) } catch (_: Exception) { PreflightExecutionResult.ERROR },
            startedAt = startTs?.time ?: System.currentTimeMillis(),
            completedAt = compTs?.time ?: System.currentTimeMillis(),
            errorMessage = rs.getString("error_message")
        )
    }

    private fun mapFinding(rs: ResultSet): PreflightFinding {
        val createdTs = rs.getTimestamp("created_at")
        val ackTs = rs.getTimestamp("acknowledged_at")
        val resTs = rs.getTimestamp("resolved_at")
        val waivedTs = rs.getTimestamp("waived_at")
        return PreflightFinding(
            findingId = rs.getString("finding_id"),
            tenantId = rs.getString("tenant_id"),
            preflightRunId = rs.getString("preflight_run_id"),
            executionId = rs.getString("execution_id"),
            ruleCode = rs.getString("rule_code"),
            category = try { PreflightRuleCategory.valueOf(rs.getString("category")) } catch (_: Exception) { PreflightRuleCategory.DOCUMENT },
            severity = try { PreflightRuleSeverity.valueOf(rs.getString("severity")) } catch (_: Exception) { PreflightRuleSeverity.ERROR },
            message = rs.getString("message"),
            expectedValue = rs.getString("expected_value"),
            actualValue = rs.getString("actual_value"),
            locationContext = rs.getString("location_context"),
            status = try { PreflightFindingStatus.valueOf(rs.getString("status")) } catch (_: Exception) { PreflightFindingStatus.OPEN },
            acknowledgedBy = rs.getString("acknowledged_by"),
            acknowledgedAt = ackTs?.time,
            resolvedAt = resTs?.time,
            resolvedBy = rs.getString("resolved_by"),
            waiverReason = rs.getString("waiver_reason"),
            waivedBy = rs.getString("waived_by"),
            waivedAt = waivedTs?.time,
            revalidationRunId = rs.getString("revalidation_run_id"),
            createdAt = createdTs?.time ?: System.currentTimeMillis()
        )
    }

    private fun mapCorrection(rs: ResultSet): PreflightFindingCorrection {
        val subTs = rs.getTimestamp("submitted_at")
        return PreflightFindingCorrection(
            correctionId = rs.getString("correction_id"),
            tenantId = rs.getString("tenant_id"),
            findingId = rs.getString("finding_id"),
            preflightRunId = rs.getString("preflight_run_id"),
            correctionType = try { PreflightCorrectionType.valueOf(rs.getString("correction_type")) } catch (_: Exception) { PreflightCorrectionType.OTHER },
            description = rs.getString("description"),
            artworkVersionId = rs.getString("artwork_version_id"),
            submittedBy = rs.getString("submitted_by"),
            submittedAt = subTs?.time ?: System.currentTimeMillis(),
            revalidationRunId = rs.getString("revalidation_run_id")
        )
    }
}

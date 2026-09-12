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
            createdAt = createdTs?.time ?: System.currentTimeMillis()
        )
    }
}

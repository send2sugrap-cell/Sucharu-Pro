package com.sucharu.sucharupro.data.datasource.preflight

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.preflight.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe fake in-memory data source for Preflight Engine testing.
 */
class FakePreflightDataSource : PreflightDataSource {

    private val runs = ConcurrentHashMap<String, PreflightRun>()
    private val ruleExecutions = ConcurrentHashMap<String, PreflightRuleExecution>()
    private val findings = ConcurrentHashMap<String, PreflightFinding>()
    private val corrections = ConcurrentHashMap<String, PreflightFindingCorrection>()

    override suspend fun saveRun(run: PreflightRun): DomainResult<PreflightRun> {
        val key = "${run.tenantId}:${run.preflightRunId}"
        runs[key] = run
        return DomainResult.Success(run)
    }

    override suspend fun getRunById(tenantId: String, runId: String): DomainResult<PreflightRun?> {
        val key = "$tenantId:$runId"
        return DomainResult.Success(runs[key])
    }

    override suspend fun getRunByIdempotencyKey(
        tenantId: String,
        idempotencyKey: String
    ): DomainResult<PreflightRun?> {
        val found = runs.values.find { r -> r.tenantId == tenantId && r.idempotencyKey == idempotencyKey }
        return DomainResult.Success(found)
    }

    override suspend fun listRunsByArtwork(
        tenantId: String,
        artworkId: String,
        limit: Int
    ): DomainResult<List<PreflightRun>> {
        val filtered = runs.values.filter { r ->
            r.tenantId == tenantId && r.artworkId == artworkId
        }.sortedByDescending { it.createdAt }.take(limit)
        return DomainResult.Success(filtered)
    }

    override suspend fun saveRuleExecutions(executions: List<PreflightRuleExecution>): DomainResult<List<PreflightRuleExecution>> {
        for (exec in executions) {
            val key = "${exec.tenantId}:${exec.executionId}"
            ruleExecutions[key] = exec
        }
        return DomainResult.Success(executions)
    }

    override suspend fun listRuleExecutionsByRun(
        tenantId: String,
        runId: String
    ): DomainResult<List<PreflightRuleExecution>> {
        val filtered = ruleExecutions.values.filter { e ->
            e.tenantId == tenantId && e.preflightRunId == runId
        }.sortedBy { it.startedAt }
        return DomainResult.Success(filtered)
    }

    override suspend fun saveFindings(findings: List<PreflightFinding>): DomainResult<List<PreflightFinding>> {
        for (f in findings) {
            val key = "${f.tenantId}:${f.findingId}"
            this.findings[key] = f
        }
        return DomainResult.Success(findings)
    }

    override suspend fun listFindingsByRun(tenantId: String, runId: String): DomainResult<List<PreflightFinding>> {
        val filtered = findings.values.filter { f ->
            f.tenantId == tenantId && f.preflightRunId == runId
        }.sortedBy { it.createdAt }
        return DomainResult.Success(filtered)
    }

    override suspend fun getFindingById(tenantId: String, findingId: String): DomainResult<PreflightFinding?> {
        val key = "$tenantId:$findingId"
        return DomainResult.Success(findings[key])
    }

    override suspend fun updateFinding(finding: PreflightFinding): DomainResult<PreflightFinding> {
        val key = "${finding.tenantId}:${finding.findingId}"
        findings[key] = finding
        return DomainResult.Success(finding)
    }

    override suspend fun saveCorrection(correction: PreflightFindingCorrection): DomainResult<PreflightFindingCorrection> {
        val key = "${correction.tenantId}:${correction.correctionId}"
        corrections[key] = correction
        return DomainResult.Success(correction)
    }

    override suspend fun listCorrectionsForFinding(
        tenantId: String,
        findingId: String
    ): DomainResult<List<PreflightFindingCorrection>> {
        val filtered = corrections.values.filter { c ->
            c.tenantId == tenantId && c.findingId == findingId
        }.sortedBy { it.submittedAt }
        return DomainResult.Success(filtered)
    }
}

package com.sucharu.sucharupro.data.datasource.preflight

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.preflight.*

/**
 * Data Source interface for Preflight Engine persistence and finding governance.
 */
interface PreflightDataSource {
    suspend fun saveRun(run: PreflightRun): DomainResult<PreflightRun>
    suspend fun getRunById(tenantId: String, runId: String): DomainResult<PreflightRun?>
    suspend fun getRunByIdempotencyKey(tenantId: String, idempotencyKey: String): DomainResult<PreflightRun?>
    suspend fun listRunsByArtwork(tenantId: String, artworkId: String, limit: Int = 100): DomainResult<List<PreflightRun>>
    suspend fun saveRuleExecutions(executions: List<PreflightRuleExecution>): DomainResult<List<PreflightRuleExecution>>
    suspend fun listRuleExecutionsByRun(tenantId: String, runId: String): DomainResult<List<PreflightRuleExecution>>
    suspend fun saveFindings(findings: List<PreflightFinding>): DomainResult<List<PreflightFinding>>
    suspend fun listFindingsByRun(tenantId: String, runId: String): DomainResult<List<PreflightFinding>>
    suspend fun getFindingById(tenantId: String, findingId: String): DomainResult<PreflightFinding?>
    suspend fun updateFinding(finding: PreflightFinding): DomainResult<PreflightFinding>
    suspend fun saveCorrection(correction: PreflightFindingCorrection): DomainResult<PreflightFindingCorrection>
    suspend fun listCorrectionsForFinding(tenantId: String, findingId: String): DomainResult<List<PreflightFindingCorrection>>
}

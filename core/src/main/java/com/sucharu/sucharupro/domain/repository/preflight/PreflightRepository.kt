package com.sucharu.sucharupro.domain.repository.preflight

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.preflight.PreflightFinding
import com.sucharu.sucharupro.domain.preflight.PreflightRuleExecution
import com.sucharu.sucharupro.domain.preflight.PreflightRun

/**
 * Domain Repository interface for Preflight Engine operations.
 */
interface PreflightRepository {
    suspend fun saveRun(run: PreflightRun): DomainResult<PreflightRun>
    suspend fun getRunById(tenantId: String, runId: String): DomainResult<PreflightRun?>
    suspend fun getRunByIdempotencyKey(tenantId: String, idempotencyKey: String): DomainResult<PreflightRun?>
    suspend fun listRunsByArtwork(tenantId: String, artworkId: String, limit: Int = 100): DomainResult<List<PreflightRun>>
    suspend fun saveRuleExecutions(executions: List<PreflightRuleExecution>): DomainResult<List<PreflightRuleExecution>>
    suspend fun listRuleExecutionsByRun(tenantId: String, runId: String): DomainResult<List<PreflightRuleExecution>>
    suspend fun saveFindings(findings: List<PreflightFinding>): DomainResult<List<PreflightFinding>>
    suspend fun listFindingsByRun(tenantId: String, runId: String): DomainResult<List<PreflightFinding>>
}

package com.sucharu.sucharupro.domain.service.preflight

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.preflight.*

/**
 * Domain Service interface for Preflight operations and Finding Governance.
 */
interface PreflightService {
    suspend fun runPreflight(context: PreflightExecutionContext, requestedBy: String, idempotencyKey: String? = null): DomainResult<PreflightRun>
    suspend fun getPreflightRunDetails(tenantId: String, runId: String): DomainResult<PreflightRun?>
    suspend fun listFindingsForRun(tenantId: String, runId: String): DomainResult<List<PreflightFinding>>
    suspend fun listPreflightRunsForArtwork(tenantId: String, artworkId: String, limit: Int = 100): DomainResult<List<PreflightRun>>

    // Step 08 Governance
    suspend fun getFindingDetails(tenantId: String, findingId: String): DomainResult<PreflightFinding?>
    suspend fun acknowledgeFinding(tenantId: String, findingId: String, actorId: String): DomainResult<PreflightFinding>
    suspend fun flagCorrectionRequired(tenantId: String, findingId: String, actorId: String, reason: String? = null): DomainResult<PreflightFinding>
    suspend fun submitCorrection(
        tenantId: String,
        findingId: String,
        correctionType: PreflightCorrectionType,
        description: String,
        artworkVersionId: String?,
        actorId: String
    ): DomainResult<PreflightFindingCorrection>
    suspend fun revalidateFinding(tenantId: String, findingId: String, context: PreflightExecutionContext, actorId: String): DomainResult<PreflightFinding>
    suspend fun waiveFinding(tenantId: String, findingId: String, waiverReason: String, actorId: String): DomainResult<PreflightFinding>
    suspend fun listCorrectionsForFinding(tenantId: String, findingId: String): DomainResult<List<PreflightFindingCorrection>>

    // Step 09 Production Readiness Decision Gate
    suspend fun evaluateProductionReadiness(
        tenantId: String,
        artworkId: String,
        artworkVersionId: String? = null,
        proofId: String? = null,
        proofVersionId: String? = null,
        jobId: String? = null,
        preflightRunId: String? = null,
        evaluatorId: String
    ): DomainResult<PreflightProductionReadiness>
}

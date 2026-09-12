package com.sucharu.sucharupro.domain.service.preflight

import com.sucharu.sucharupro.domain.engine.preflight.PreflightEngine
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.preflight.*
import com.sucharu.sucharupro.domain.repository.preflight.PreflightRepository
import java.util.UUID

/**
 * Domain Service implementation for Preflight operations and Finding Governance.
 */
class PreflightServiceImpl(
    private val preflightEngine: PreflightEngine,
    private val preflightRepository: PreflightRepository
) : PreflightService {

    override suspend fun runPreflight(
        context: PreflightExecutionContext,
        requestedBy: String,
        idempotencyKey: String?
    ): DomainResult<PreflightRun> {
        return preflightEngine.runPreflight(context, requestedBy, idempotencyKey)
    }

    override suspend fun getPreflightRunDetails(
        tenantId: String,
        runId: String
    ): DomainResult<PreflightRun?> {
        return preflightRepository.getRunById(tenantId, runId)
    }

    override suspend fun listFindingsForRun(
        tenantId: String,
        runId: String
    ): DomainResult<List<PreflightFinding>> {
        return preflightRepository.listFindingsByRun(tenantId, runId)
    }

    override suspend fun listPreflightRunsForArtwork(
        tenantId: String,
        artworkId: String,
        limit: Int
    ): DomainResult<List<PreflightRun>> {
        return preflightRepository.listRunsByArtwork(tenantId, artworkId, limit)
    }

    override suspend fun getFindingDetails(
        tenantId: String,
        findingId: String
    ): DomainResult<PreflightFinding?> {
        return preflightRepository.getFindingById(tenantId, findingId)
    }

    override suspend fun acknowledgeFinding(
        tenantId: String,
        findingId: String,
        actorId: String
    ): DomainResult<PreflightFinding> {
        val findingRes = preflightRepository.getFindingById(tenantId, findingId)
        if (findingRes is DomainResult.Error) return findingRes
        val finding = (findingRes as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Preflight finding '$findingId' not found.")

        val valRes = PreflightFindingValidator.validateFindingStatusTransition(finding.status, PreflightFindingStatus.ACKNOWLEDGED)
        if (valRes is DomainResult.Error) return DomainResult.Error(message = valRes.message)

        val updated = finding.copy(
            status = PreflightFindingStatus.ACKNOWLEDGED,
            acknowledgedBy = actorId,
            acknowledgedAt = System.currentTimeMillis()
        )
        return preflightRepository.updateFinding(updated)
    }

    override suspend fun flagCorrectionRequired(
        tenantId: String,
        findingId: String,
        actorId: String,
        reason: String?
    ): DomainResult<PreflightFinding> {
        val findingRes = preflightRepository.getFindingById(tenantId, findingId)
        if (findingRes is DomainResult.Error) return findingRes
        val finding = (findingRes as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Preflight finding '$findingId' not found.")

        val valRes = PreflightFindingValidator.validateFindingStatusTransition(finding.status, PreflightFindingStatus.CORRECTION_REQUIRED)
        if (valRes is DomainResult.Error) return DomainResult.Error(message = valRes.message)

        val updated = finding.copy(
            status = PreflightFindingStatus.CORRECTION_REQUIRED
        )
        return preflightRepository.updateFinding(updated)
    }

    override suspend fun submitCorrection(
        tenantId: String,
        findingId: String,
        correctionType: PreflightCorrectionType,
        description: String,
        artworkVersionId: String?,
        actorId: String
    ): DomainResult<PreflightFindingCorrection> {
        val findingRes = preflightRepository.getFindingById(tenantId, findingId)
        if (findingRes is DomainResult.Error) return DomainResult.Error(message = findingRes.message)
        val finding = (findingRes as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Preflight finding '$findingId' not found.")

        val corrId = "CORR-" + UUID.randomUUID().toString().take(8).uppercase()
        val now = System.currentTimeMillis()

        val correction = PreflightFindingCorrection(
            correctionId = corrId,
            tenantId = tenantId,
            findingId = findingId,
            preflightRunId = finding.preflightRunId,
            correctionType = correctionType,
            description = description,
            artworkVersionId = artworkVersionId,
            submittedBy = actorId,
            submittedAt = now
        )

        val saveCorrRes = preflightRepository.saveCorrection(correction)
        if (saveCorrRes is DomainResult.Error) return saveCorrRes

        // Transition finding status to REVALIDATION_REQUIRED (NOT automatically RESOLVED!)
        val updatedFinding = finding.copy(
            status = PreflightFindingStatus.REVALIDATION_REQUIRED
        )
        preflightRepository.updateFinding(updatedFinding)

        return saveCorrRes
    }

    override suspend fun revalidateFinding(
        tenantId: String,
        findingId: String,
        context: PreflightExecutionContext,
        actorId: String
    ): DomainResult<PreflightFinding> {
        val findingRes = preflightRepository.getFindingById(tenantId, findingId)
        if (findingRes is DomainResult.Error) return findingRes
        val finding = (findingRes as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Preflight finding '$findingId' not found.")

        // 1. Execute NEW revalidation preflight run
        val runRes = preflightEngine.runPreflight(context, actorId)
        if (runRes is DomainResult.Error) return DomainResult.Error(message = runRes.message)
        val newRun = (runRes as DomainResult.Success).data

        // 2. Query findings from new run
        val newFindingsRes = preflightRepository.listFindingsByRun(tenantId, newRun.preflightRunId)
        val newFindings = (newFindingsRes as? DomainResult.Success)?.data ?: emptyList()

        // 3. Check if finding's ruleCode is still failing in the new run
        val stillFailing = newFindings.any { it.ruleCode == finding.ruleCode && (it.severity == PreflightRuleSeverity.ERROR || it.severity == PreflightRuleSeverity.WARNING) }

        val now = System.currentTimeMillis()
        val updatedFinding = if (!stillFailing) {
            finding.copy(
                status = PreflightFindingStatus.RESOLVED,
                resolvedBy = actorId,
                resolvedAt = now,
                revalidationRunId = newRun.preflightRunId
            )
        } else {
            finding.copy(
                status = PreflightFindingStatus.CORRECTION_REQUIRED,
                revalidationRunId = newRun.preflightRunId
            )
        }

        return preflightRepository.updateFinding(updatedFinding)
    }

    override suspend fun waiveFinding(
        tenantId: String,
        findingId: String,
        waiverReason: String,
        actorId: String
    ): DomainResult<PreflightFinding> {
        val valRes = PreflightFindingValidator.validateWaiver(waiverReason, actorId)
        if (valRes is DomainResult.Error) return DomainResult.Error(message = valRes.message)

        val findingRes = preflightRepository.getFindingById(tenantId, findingId)
        if (findingRes is DomainResult.Error) return findingRes
        val finding = (findingRes as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Preflight finding '$findingId' not found.")

        val updated = finding.copy(
            status = PreflightFindingStatus.WAIVED,
            waiverReason = waiverReason,
            waivedBy = actorId,
            waivedAt = System.currentTimeMillis()
        )
        return preflightRepository.updateFinding(updated)
    }

    override suspend fun listCorrectionsForFinding(
        tenantId: String,
        findingId: String
    ): DomainResult<List<PreflightFindingCorrection>> {
        return preflightRepository.listCorrectionsForFinding(tenantId, findingId)
    }

    override suspend fun evaluateProductionReadiness(
        tenantId: String,
        artworkId: String,
        artworkVersionId: String?,
        proofId: String?,
        proofVersionId: String?,
        jobId: String?,
        preflightRunId: String?,
        evaluatorId: String
    ): DomainResult<PreflightProductionReadiness> {
        val blockingReasons = mutableListOf<String>()

        // 1. Resolve PreflightRun
        val targetRun: PreflightRun? = if (!preflightRunId.isNullOrBlank()) {
            val runRes = preflightRepository.getRunById(tenantId, preflightRunId)
            if (runRes is DomainResult.Error) return runRes
            (runRes as DomainResult.Success).data
        } else {
            val runsRes = preflightRepository.listRunsByArtwork(tenantId, artworkId, limit = 10)
            if (runsRes is DomainResult.Error) return runsRes
            val runs = (runsRes as DomainResult.Success).data
            runs.firstOrNull { r ->
                (artworkVersionId == null || r.artworkVersionId == artworkVersionId) &&
                (proofId == null || r.proofId == proofId)
            } ?: runs.firstOrNull()
        }

        if (targetRun == null) {
            blockingReasons.add("No technical preflight run found for artwork '$artworkId'${if (artworkVersionId != null) " (version: $artworkVersionId)" else ""}.")
        } else {
            if (targetRun.status != PreflightRunStatus.COMPLETED) {
                blockingReasons.add("Preflight run '${targetRun.preflightRunId}' status is ${targetRun.status.name} (not COMPLETED).")
            }

            // Stale version check
            if (artworkVersionId != null && targetRun.artworkVersionId != null && targetRun.artworkVersionId != artworkVersionId) {
                blockingReasons.add("Preflight result is stale: Run '${targetRun.preflightRunId}' was executed for artwork version '${targetRun.artworkVersionId}', but target version is '$artworkVersionId'.")
            }

            // 2. Fetch findings for run
            val findingsRes = preflightRepository.listFindingsByRun(tenantId, targetRun.preflightRunId)
            val findings = (findingsRes as? DomainResult.Success)?.data ?: emptyList()

            var warningCount = 0
            var infoCount = 0
            var blockingFindingCount = 0

            for (f in findings) {
                when (f.severity) {
                    PreflightRuleSeverity.ERROR -> {
                        // Governed/resolved states: RESOLVED, ACCEPTED, WAIVED -> do NOT block
                        if (f.status != PreflightFindingStatus.RESOLVED &&
                            f.status != PreflightFindingStatus.ACCEPTED &&
                            f.status != PreflightFindingStatus.WAIVED
                        ) {
                            blockingFindingCount++
                            if (f.status == PreflightFindingStatus.CORRECTION_SUBMITTED || f.status == PreflightFindingStatus.REVALIDATION_REQUIRED) {
                                blockingReasons.add("Correction submitted for rule [${f.ruleCode}] requires technical revalidation before handoff.")
                            } else {
                                blockingReasons.add("Unresolved preflight error [${f.ruleCode}]: ${f.message} (Status: ${f.status.name})")
                            }
                        }
                    }
                    PreflightRuleSeverity.WARNING -> {
                        if (f.status != PreflightFindingStatus.RESOLVED &&
                            f.status != PreflightFindingStatus.ACCEPTED &&
                            f.status != PreflightFindingStatus.WAIVED
                        ) {
                            warningCount++
                        }
                    }
                    PreflightRuleSeverity.INFO -> {
                        infoCount++
                    }
                }
            }

            val decision = if (blockingReasons.isEmpty()) ProductionReadinessDecision.READY else ProductionReadinessDecision.BLOCKED
            val readinessId = "RDN-" + UUID.randomUUID().toString().take(8).uppercase()

            return DomainResult.Success(
                PreflightProductionReadiness(
                    readinessId = readinessId,
                    tenantId = tenantId,
                    artworkId = artworkId,
                    artworkVersionId = artworkVersionId ?: targetRun.artworkVersionId,
                    proofId = proofId ?: targetRun.proofId,
                    proofVersionId = proofVersionId,
                    jobId = jobId ?: targetRun.jobId,
                    preflightRunId = targetRun.preflightRunId,
                    decision = decision,
                    blockingFindingCount = blockingFindingCount,
                    warningCount = warningCount,
                    infoCount = infoCount,
                    blockingReasons = blockingReasons,
                    evaluatedBy = evaluatorId,
                    evaluatedAt = System.currentTimeMillis()
                )
            )
        }

        val readinessId = "RDN-" + UUID.randomUUID().toString().take(8).uppercase()
        return DomainResult.Success(
            PreflightProductionReadiness(
                readinessId = readinessId,
                tenantId = tenantId,
                artworkId = artworkId,
                artworkVersionId = artworkVersionId,
                proofId = proofId,
                proofVersionId = proofVersionId,
                jobId = jobId,
                preflightRunId = preflightRunId,
                decision = ProductionReadinessDecision.BLOCKED,
                blockingFindingCount = 1,
                warningCount = 0,
                infoCount = 0,
                blockingReasons = blockingReasons,
                evaluatedBy = evaluatorId,
                evaluatedAt = System.currentTimeMillis()
            )
        )
    }
}

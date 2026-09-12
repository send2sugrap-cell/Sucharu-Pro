package com.sucharu.sucharupro.domain.engine.preflight

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.preflight.*
import com.sucharu.sucharupro.domain.repository.preflight.PreflightRepository
import java.util.UUID

/**
 * Production implementation of the Preflight Engine executing rules deterministically.
 */
class PreflightEngineImpl(
    private val ruleRegistry: PreflightRuleRegistry,
    private val preflightRepository: PreflightRepository
) : PreflightEngine {

    override suspend fun runPreflight(
        context: PreflightExecutionContext,
        requestedBy: String,
        idempotencyKey: String?
    ): DomainResult<PreflightRun> {
        val tenantId = context.tenantId
        if (tenantId.isBlank() || context.artworkId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Artwork ID cannot be blank.")
        }

        // 1. Idempotency Check
        if (!idempotencyKey.isNullOrBlank()) {
            val existingCheck = preflightRepository.getRunByIdempotencyKey(tenantId, idempotencyKey)
            if (existingCheck is DomainResult.Success && existingCheck.data != null) {
                return DomainResult.Success(existingCheck.data)
            }
        }

        val runId = "PFRUN-" + UUID.randomUUID().toString().take(8).uppercase()
        val now = System.currentTimeMillis()

        // 2. Initial Run (REQUESTED)
        val initialRun = PreflightRun(
            preflightRunId = runId,
            tenantId = tenantId,
            jobId = context.jobId,
            artworkId = context.artworkId,
            artworkVersionId = context.artworkVersionId,
            proofId = context.proofId,
            status = PreflightRunStatus.REQUESTED,
            overallResult = PreflightOverallResult.NOT_EVALUATED,
            engineVersion = "v1.0",
            idempotencyKey = idempotencyKey,
            requestedBy = requestedBy,
            createdAt = now,
            updatedAt = now
        )
        val saveInitRes = preflightRepository.saveRun(initialRun)
        if (saveInitRes is DomainResult.Error) return saveInitRes

        // 3. Update Status to RUNNING
        val runningRun = initialRun.copy(
            status = PreflightRunStatus.RUNNING,
            startedAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        preflightRepository.saveRun(runningRun)

        // 4. Resolve Rules Deterministically
        val rules = ruleRegistry.getApplicableRules(context)
        val executions = mutableListOf<PreflightRuleExecution>()
        val findings = mutableListOf<PreflightFinding>()

        for (rule in rules) {
            val execId = "EXEC-" + UUID.randomUUID().toString().take(8).uppercase()
            val ruleStart = System.currentTimeMillis()

            try {
                val execRes = rule.execute(context)
                val ruleEnd = System.currentTimeMillis()

                val ruleExec = PreflightRuleExecution(
                    executionId = execId,
                    tenantId = tenantId,
                    preflightRunId = runId,
                    ruleId = rule.ruleId,
                    ruleCode = rule.ruleCode,
                    result = execRes.result,
                    startedAt = ruleStart,
                    completedAt = ruleEnd,
                    errorMessage = execRes.errorMessage
                )
                executions.add(ruleExec)

                for (f in execRes.findings) {
                    val findingWithRun = f.copy(
                        preflightRunId = runId,
                        executionId = execId,
                        tenantId = tenantId
                    )
                    findings.add(findingWithRun)
                }
            } catch (e: Exception) {
                val ruleEnd = System.currentTimeMillis()
                val ruleExec = PreflightRuleExecution(
                    executionId = execId,
                    tenantId = tenantId,
                    preflightRunId = runId,
                    ruleId = rule.ruleId,
                    ruleCode = rule.ruleCode,
                    result = PreflightExecutionResult.ERROR,
                    startedAt = ruleStart,
                    completedAt = ruleEnd,
                    errorMessage = "Rule execution error: ${e.message}"
                )
                executions.add(ruleExec)

                val errorFinding = PreflightFinding(
                    findingId = "FIND-" + UUID.randomUUID().toString().take(8).uppercase(),
                    tenantId = tenantId,
                    preflightRunId = runId,
                    executionId = execId,
                    ruleCode = rule.ruleCode,
                    category = rule.category,
                    severity = PreflightRuleSeverity.ERROR,
                    message = "Unhandled rule exception: ${e.message ?: "Unknown error"}",
                    createdAt = ruleEnd
                )
                findings.add(errorFinding)
            }
        }

        // 5. Aggregate Results
        val overallResult = PreflightResultAggregator.aggregate(executions, findings)
        val compTime = System.currentTimeMillis()

        // 6. Persist Executions & Findings
        if (executions.isNotEmpty()) {
            preflightRepository.saveRuleExecutions(executions)
        }
        if (findings.isNotEmpty()) {
            preflightRepository.saveFindings(findings)
        }

        // 7. Complete Run
        val completedRun = runningRun.copy(
            status = PreflightRunStatus.COMPLETED,
            overallResult = overallResult,
            completedAt = compTime,
            summary = "Executed ${executions.size} rules with ${findings.size} findings.",
            updatedAt = compTime
        )
        return preflightRepository.saveRun(completedRun)
    }
}

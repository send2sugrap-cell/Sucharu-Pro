package com.sucharu.sucharupro.data.observability.health

import com.sucharu.sucharupro.domain.observability.AiAgentIntegrationHealth
import com.sucharu.sucharupro.domain.observability.OperationalHealthStatus
import java.util.concurrent.atomic.AtomicLong

/**
 * Health evaluator for AI Agent notification boundary (INFRA-04 Step 09).
 */
class AiAgentHealthEvaluator {

    private val totalRequests = AtomicLong(0)
    private val totalDrafts = AtomicLong(0)
    private val totalExecutions = AtomicLong(0)
    private val totalDenials = AtomicLong(0)
    private val rateLimitBlocks = AtomicLong(0)
    private val credentialBlocks = AtomicLong(0)

    fun recordRequest(isAllowed: Boolean, isDraft: Boolean = false, isExecution: Boolean = false, isRateLimited: Boolean = false, isCredentialBlocked: Boolean = false) {
        totalRequests.incrementAndGet()
        if (isDraft) totalDrafts.incrementAndGet()
        if (isExecution) totalExecutions.incrementAndGet()
        if (!isAllowed) totalDenials.incrementAndGet()
        if (isRateLimited) rateLimitBlocks.incrementAndGet()
        if (isCredentialBlocked) credentialBlocks.incrementAndGet()
    }

    fun evaluate(pendingConfirmationsCount: Long = 0L): AiAgentIntegrationHealth {
        val issues = mutableListOf<String>()
        val reqs = totalRequests.get()
        val denies = totalDenials.get()
        val credBlocks = credentialBlocks.get()

        var status = OperationalHealthStatus.HEALTHY
        if (credBlocks > 10 || (reqs > 20 && (denies.toDouble() / reqs) > 0.5)) {
            status = OperationalHealthStatus.CRITICAL
            if (credBlocks > 10) issues.add("High rate of credential leak blocks from AI agent: $credBlocks")
            if (reqs > 20 && (denies.toDouble() / reqs) > 0.5) issues.add("Over 50% AI agent action denial rate")
        } else if (credBlocks > 0 || (reqs > 10 && (denies.toDouble() / reqs) > 0.2)) {
            status = OperationalHealthStatus.DEGRADED
            if (credBlocks > 0) issues.add("Credential leak attempts detected and blocked: $credBlocks")
            if (denies > 5) issues.add("Elevated AI agent action denial rate: $denies")
        }

        return AiAgentIntegrationHealth(
            status = status,
            totalRequests = reqs,
            totalDrafts = totalDrafts.get(),
            totalExecutions = totalExecutions.get(),
            totalDenials = denies,
            totalConfirmationsPending = pendingConfirmationsCount,
            rateLimitBlocks = rateLimitBlocks.get(),
            credentialBlocks = credBlocks,
            issues = issues
        )
    }

    fun reset() {
        totalRequests.set(0)
        totalDrafts.set(0)
        totalExecutions.set(0)
        totalDenials.set(0)
        rateLimitBlocks.set(0)
        credentialBlocks.set(0)
    }
}

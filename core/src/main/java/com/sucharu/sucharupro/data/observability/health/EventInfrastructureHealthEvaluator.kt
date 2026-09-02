package com.sucharu.sucharupro.data.observability.health

import com.sucharu.sucharupro.data.event.dispatcher.OutboxMetrics
import com.sucharu.sucharupro.domain.observability.EventInfrastructureHealth
import com.sucharu.sucharupro.domain.observability.OperationalHealthStatus
import com.sucharu.sucharupro.domain.observability.QueueHealth

/**
 * Health evaluator for Domain Events & Transactional Outbox (INFRA-04 Step 09).
 */
class EventInfrastructureHealthEvaluator(
    private val outboxMetrics: OutboxMetrics = OutboxMetrics()
) {

    fun evaluate(
        pendingOutboxCount: Long = 0L,
        processingOutboxCount: Long = 0L,
        deadLetterCount: Long = outboxMetrics.deadLetterCount,
        oldestPendingAgeMs: Long = 0L,
        oldestProcessingAgeMs: Long = 0L
    ): EventInfrastructureHealth {
        val issues = mutableListOf<String>()

        var queueStatus = OperationalHealthStatus.HEALTHY
        if (deadLetterCount > 50) {
            queueStatus = OperationalHealthStatus.CRITICAL
            issues.add("High dead-letter queue count: $deadLetterCount")
        } else if (deadLetterCount > 0 || pendingOutboxCount > 500 || oldestPendingAgeMs > 60_000) {
            queueStatus = OperationalHealthStatus.DEGRADED
            if (pendingOutboxCount > 500) issues.add("Outbox backlog high: $pendingOutboxCount pending events")
            if (oldestPendingAgeMs > 60_000) issues.add("Oldest pending event age exceeds 60s (${oldestPendingAgeMs / 1000}s)")
            if (deadLetterCount > 0) issues.add("Dead letters present in outbox: $deadLetterCount")
        }

        val queueHealth = QueueHealth(
            queueName = "transactional_outbox",
            pendingCount = pendingOutboxCount,
            processingCount = processingOutboxCount,
            retryCount = outboxMetrics.retriedCount,
            deadLetterCount = deadLetterCount,
            oldestPendingAgeMs = oldestPendingAgeMs,
            oldestProcessingAgeMs = oldestProcessingAgeMs,
            status = queueStatus
        )

        val totalPublished = outboxMetrics.publishedCount
        val avgLatency = outboxMetrics.averageLatencyMs

        val overallStatus = when {
            queueStatus == OperationalHealthStatus.CRITICAL -> OperationalHealthStatus.CRITICAL
            queueStatus == OperationalHealthStatus.DEGRADED || avgLatency > 2000.0 -> OperationalHealthStatus.DEGRADED
            else -> OperationalHealthStatus.HEALTHY
        }

        if (avgLatency > 2000.0) {
            issues.add("High average outbox dispatch latency: ${String.format("%.1f", avgLatency)}ms")
        }

        return EventInfrastructureHealth(
            status = overallStatus,
            outboxHealth = queueHealth,
            deadLetterCount = deadLetterCount,
            totalPublished = totalPublished,
            totalConsumed = totalPublished, // baseline consumer parity
            publicationLatencyMs = avgLatency,
            consumerLatencyMs = avgLatency,
            issues = issues
        )
    }
}

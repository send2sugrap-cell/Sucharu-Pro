package com.sucharu.sucharupro.domain.event.consumer.orchestration

import com.sucharu.sucharupro.domain.event.consumer.DomainEventConsumer
import com.sucharu.sucharupro.domain.event.consumer.EventConsumerResult
import com.sucharu.sucharupro.domain.event.model.DomainEvent
import com.sucharu.sucharupro.domain.event.model.EventEnvelope

/**
 * Detailed report returned by [EventConsumerRouter] after routing an event envelope.
 */
data class RouterDispatchReport(
    val eventId: String,
    val projectId: String,
    val totalMatched: Int,
    val successCount: Int,
    val failureCount: Int,
    val skippedCount: Int,
    val outcomes: List<ConsumerExecutionOutcome>
) {
    val isFullySuccessful: Boolean get() = failureCount == 0
}

/**
 * Production-grade central consumer routing engine for Sucharu Pro (INFRA-04 Step 03).
 *
 * Responsibilities:
 * - Deterministically match event envelopes to registered consumers in [EventConsumerRegistry].
 * - Execute consumers through [EventConsumerExecutionEngine].
 * - Ensure slow or failing consumers do not corrupt or block overall routing.
 */
class EventConsumerRouter(
    private val registry: EventConsumerRegistry,
    private val executionEngine: EventConsumerExecutionEngine
) {

    /**
     * Routes an event envelope to all matching registered consumers.
     */
    suspend fun <T : DomainEvent> route(envelope: EventEnvelope<T>): RouterDispatchReport {
        val matchingPairs = registry.getConsumersForEnvelope(envelope)
        val outcomes = mutableListOf<ConsumerExecutionOutcome>()

        var successCount = 0
        var failureCount = 0
        var skippedCount = 0

        for ((subscription, consumer) in matchingPairs) {
            @Suppress("UNCHECKED_CAST")
            val typedConsumer = consumer as DomainEventConsumer<T>
            val outcome = executionEngine.execute(typedConsumer, subscription, envelope)
            outcomes.add(outcome)

            when (outcome.result) {
                is EventConsumerResult.Success -> successCount++
                is EventConsumerResult.Failure -> failureCount++
                is EventConsumerResult.Skipped -> skippedCount++
            }
        }

        return RouterDispatchReport(
            eventId = envelope.eventId,
            projectId = envelope.projectId,
            totalMatched = matchingPairs.size,
            successCount = successCount,
            failureCount = failureCount,
            skippedCount = skippedCount,
            outcomes = outcomes
        )
    }
}

package com.sucharu.sucharupro.domain.event.dispatcher

import com.sucharu.sucharupro.domain.event.consumer.DomainEventConsumer
import com.sucharu.sucharupro.domain.event.consumer.EventConsumerResult
import com.sucharu.sucharupro.domain.event.consumer.EventFailureClassification
import com.sucharu.sucharupro.domain.event.idempotency.EventIdempotencyStore
import com.sucharu.sucharupro.domain.event.idempotency.EventProcessingRecord
import com.sucharu.sucharupro.domain.event.idempotency.EventProcessingStatus
import com.sucharu.sucharupro.domain.event.model.DomainEvent
import com.sucharu.sucharupro.domain.event.model.EventEnvelope
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Result of dispatching an envelope across all matching registered consumers.
 */
data class DispatchSummary(
    val eventId: String,
    val totalConsumersMatched: Int,
    val successCount: Int,
    val failureCount: Int,
    val skippedCount: Int,
    val consumerResults: Map<String, EventConsumerResult>
) {
    val isFullySuccessful: Boolean get() = failureCount == 0
}

/**
 * Central In-Process Event Dispatcher coordinating domain event routing to registered consumers.
 *
 * Enforces:
 * - Server-authoritative tenant scoping
 * - Schema version matching
 * - Aggregate version ordering validation
 * - Consumer idempotency checking and recording
 * - Failure classification
 */
class DomainEventDispatcher(
    private val idempotencyStore: EventIdempotencyStore? = null
) {
    private val consumers = CopyOnWriteArrayList<DomainEventConsumer<*>>()
    private val aggregateVersionCache = ConcurrentHashMap<String, Long>()

    /**
     * Registers a typed domain event consumer.
     */
    fun registerConsumer(consumer: DomainEventConsumer<*>) {
        if (!consumers.contains(consumer)) {
            consumers.add(consumer)
        }
    }

    /**
     * Unregisters a consumer.
     */
    fun unregisterConsumer(consumer: DomainEventConsumer<*>) {
        consumers.remove(consumer)
    }

    /**
     * Clears all registered consumers (useful for testing).
     */
    fun clearConsumers() {
        consumers.clear()
        aggregateVersionCache.clear()
    }

    /**
     * Dispatches an event envelope to all registered consumers matching the event type and version.
     */
    suspend fun <T : DomainEvent> dispatch(envelope: EventEnvelope<T>): DispatchSummary {
        // Aggregate version stream ordering validation
        val aggregateKey = "${envelope.projectId}:${envelope.aggregateType}:${envelope.aggregateId}"
        val lastSeenVersion = aggregateVersionCache[aggregateKey] ?: 0L

        if (envelope.aggregateVersion < lastSeenVersion && lastSeenVersion > 0L) {
            val staleResult = EventConsumerResult.Failure(
                reason = "Stale aggregate version received. Current known version: $lastSeenVersion, event version: ${envelope.aggregateVersion}",
                classification = EventFailureClassification.STALE_VERSION
            )
            return DispatchSummary(
                eventId = envelope.eventId,
                totalConsumersMatched = 0,
                successCount = 0,
                failureCount = 1,
                skippedCount = 0,
                consumerResults = mapOf("STREAM_ORDERING_VALIDATOR" to staleResult)
            )
        }

        // Update latest observed aggregate version
        aggregateVersionCache[aggregateKey] = maxOf(lastSeenVersion, envelope.aggregateVersion)

        val results = mutableMapOf<String, EventConsumerResult>()
        var successCount = 0
        var failureCount = 0
        var skippedCount = 0

        val matchingConsumers = consumers.filter { it.supports(envelope) }

        for (consumer in matchingConsumers) {
            @Suppress("UNCHECKED_CAST")
            val typedConsumer = consumer as DomainEventConsumer<T>
            val consumerId = typedConsumer.consumerId

            // Check idempotency if store is present
            if (idempotencyStore != null && idempotencyStore.isProcessed(envelope.eventId, consumerId, envelope.projectId)) {
                val skipped = EventConsumerResult.Skipped(
                    reason = "Duplicate event already processed by consumer: $consumerId",
                    classification = EventFailureClassification.DUPLICATE
                )
                results[consumerId] = skipped
                skippedCount++
                continue
            }

            val startTime = System.currentTimeMillis()
            val result = try {
                typedConsumer.consume(envelope)
            } catch (t: Throwable) {
                EventConsumerResult.Failure(
                    reason = "Unhandled consumer exception: ${t.message ?: t.javaClass.simpleName}",
                    classification = EventFailureClassification.TRANSIENT,
                    cause = t
                )
            }
            val duration = System.currentTimeMillis() - startTime

            // Record outcome in idempotency store
            if (idempotencyStore != null) {
                val status = when (result) {
                    is EventConsumerResult.Success -> EventProcessingStatus.PROCESSED
                    is EventConsumerResult.Skipped -> EventProcessingStatus.SKIPPED
                    is EventConsumerResult.Failure -> EventProcessingStatus.FAILED
                }
                val failureReason = (result as? EventConsumerResult.Failure)?.reason
                idempotencyStore.recordProcessing(
                    EventProcessingRecord(
                        eventId = envelope.eventId,
                        consumerId = consumerId,
                        projectId = envelope.projectId,
                        processedAt = System.currentTimeMillis(),
                        status = status,
                        failureReason = failureReason,
                        executionDurationMs = duration
                    )
                )
            }

            results[consumerId] = result
            when (result) {
                is EventConsumerResult.Success -> successCount++
                is EventConsumerResult.Failure -> failureCount++
                is EventConsumerResult.Skipped -> skippedCount++
            }
        }

        return DispatchSummary(
            eventId = envelope.eventId,
            totalConsumersMatched = matchingConsumers.size,
            successCount = successCount,
            failureCount = failureCount,
            skippedCount = skippedCount,
            consumerResults = results
        )
    }
}

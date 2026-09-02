package com.sucharu.sucharupro.data.event.dispatcher

import com.sucharu.sucharupro.data.event.model.PersistentOutboxRecord
import com.sucharu.sucharupro.data.event.model.RetryConfig
import com.sucharu.sucharupro.data.event.postgres.PostgresTransactionalOutboxStore
import com.sucharu.sucharupro.data.event.serialization.EventSerializationHelper
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.event.consumer.EventConsumerResult
import com.sucharu.sucharupro.domain.event.consumer.EventFailureClassification
import com.sucharu.sucharupro.domain.event.dispatcher.DomainEventDispatcher
import com.sucharu.sucharupro.domain.event.model.AggregateReference
import com.sucharu.sucharupro.domain.event.model.EventActor
import com.sucharu.sucharupro.domain.event.model.EventEnvelope
import com.sucharu.sucharupro.domain.event.model.EventTraceContext

/**
 * Summary result of an outbox dispatch cycle.
 */
data class OutboxDispatchSummary(
    val workerId: String,
    val claimedCount: Int,
    val publishedCount: Int,
    val retriedCount: Int,
    val deadLetterCount: Int,
    val skippedCount: Int,
    val executionDurationMs: Long
)

/**
 * Production-grade Reliable Outbox Dispatcher (INFRA-04 Step 02).
 *
 * Coordinates atomic batch claiming, aggregate-level ordering enforcement,
 * consumer dispatch, exponential backoff retries, and dead-letter quarantine.
 */
class OutboxDispatcher(
    private val outboxStore: PostgresTransactionalOutboxStore,
    private val domainEventDispatcher: DomainEventDispatcher,
    private val retryConfig: RetryConfig = RetryConfig(),
    val metrics: OutboxMetrics = OutboxMetrics()
) {

    // Aggregate-level tracking to prevent dispatching version N+1 if version N failed in the same cycle
    private val failedAggregatesInCycle = mutableSetOf<String>()

    /**
     * Executes one dispatch cycle for the given tenant and worker ID.
     */
    suspend fun dispatchBatch(
        tenantContext: TenantContext,
        workerId: String,
        batchLimit: Int = 20,
        leaseDurationMs: Long = 30000L
    ): OutboxDispatchSummary {
        val startTime = System.currentTimeMillis()
        failedAggregatesInCycle.clear()

        // 1. Claim pending records atomically with SKIP LOCKED
        val claimedRecords = outboxStore.claimPendingRecords(
            tenantContext = tenantContext,
            workerId = workerId,
            limit = batchLimit,
            leaseDurationMs = leaseDurationMs
        )

        metrics.recordClaimed(claimedRecords.size)

        var publishedCount = 0
        var retriedCount = 0
        var deadLetterCount = 0
        var skippedCount = 0

        for (record in claimedRecords) {
            val aggregateKey = "${record.aggregateType}:${record.aggregateId}"

            // If an earlier event for this aggregate failed in this batch, defer subsequent events to maintain ordering
            if (failedAggregatesInCycle.contains(aggregateKey)) {
                val nextAttempt = System.currentTimeMillis() + 2000L
                outboxStore.scheduleRetry(
                    tenantContext = tenantContext,
                    outboxId = record.outboxId,
                    nextAttemptAt = nextAttempt,
                    errorCode = "AGGREGATE_ORDERING_HOLD",
                    errorMessage = "Deferred: preceding aggregate version failed"
                )
                retriedCount++
                continue
            }

            val itemStartTime = System.currentTimeMillis()

            try {
                // 2. Deserialize EventEnvelope
                val envelope = try {
                    val payload = EventSerializationHelper.deserializePayload(record.eventType, record.payloadJson)
                    EventEnvelope(
                        eventId = record.eventId,
                        eventType = record.eventType,
                        eventVersion = record.eventVersion,
                        occurredAt = record.createdAt,
                        publishedAt = System.currentTimeMillis(),
                        projectId = record.projectId,
                        aggregateType = record.aggregateType,
                        aggregateId = record.aggregateId,
                        aggregateVersion = record.aggregateVersion,
                        actorType = record.actorType,
                        actorId = record.actorId,
                        principalType = record.principalType,
                        correlationId = record.correlationId,
                        causationId = record.causationId,
                        requestId = record.requestId,
                        source = record.source,
                        payload = payload,
                        metadata = record.metadata
                    )
                } catch (e: Exception) {
                    // Permanent deserialization error -> dead letter immediately
                    val duration = System.currentTimeMillis() - itemStartTime
                    outboxStore.moveToDeadLetter(
                        tenantContext = tenantContext,
                        outboxId = record.outboxId,
                        classification = EventFailureClassification.VALIDATION,
                        errorCode = "DESERIALIZATION_FAILED",
                        errorMessage = e.message ?: "Failed to deserialize event payload"
                    )
                    metrics.recordDeadLettered(duration)
                    deadLetterCount++
                    failedAggregatesInCycle.add(aggregateKey)
                    continue
                }

                // 3. Dispatch to registered domain consumers
                val dispatchSummary = domainEventDispatcher.dispatch(envelope)

                val itemDuration = System.currentTimeMillis() - itemStartTime

                if (dispatchSummary.isFullySuccessful) {
                    outboxStore.markPublished(tenantContext, record.outboxId)
                    metrics.recordPublished(itemDuration)
                    publishedCount++
                } else {
                    // Check consumer failures
                    val failures = dispatchSummary.consumerResults.values.filterIsInstance<EventConsumerResult.Failure>()
                    val isAnyRetryable = failures.any { it.isRetryable }

                    if (isAnyRetryable && record.attemptCount < retryConfig.maxAttempts) {
                        // Schedule exponential backoff retry
                        val delayMs = retryConfig.calculateDelayMs(record.attemptCount)
                        val nextAttemptAt = System.currentTimeMillis() + delayMs
                        val firstFailure = failures.firstOrNull()

                        outboxStore.scheduleRetry(
                            tenantContext = tenantContext,
                            outboxId = record.outboxId,
                            nextAttemptAt = nextAttemptAt,
                            errorCode = firstFailure?.classification?.name ?: "TRANSIENT_FAILURE",
                            errorMessage = firstFailure?.reason ?: "Consumer execution failed"
                        )
                        metrics.recordRetryScheduled(itemDuration)
                        retriedCount++
                        failedAggregatesInCycle.add(aggregateKey)
                    } else {
                        // Max attempts exceeded or permanent failure -> move to dead letter
                        val firstFailure = failures.firstOrNull()
                        val classification = firstFailure?.classification ?: EventFailureClassification.NON_RETRYABLE
                        outboxStore.moveToDeadLetter(
                            tenantContext = tenantContext,
                            outboxId = record.outboxId,
                            classification = classification,
                            errorCode = classification.name,
                            errorMessage = firstFailure?.reason ?: "Permanent failure or max retry attempts reached"
                        )
                        metrics.recordDeadLettered(itemDuration)
                        deadLetterCount++
                        failedAggregatesInCycle.add(aggregateKey)
                    }
                }
            } catch (ex: Throwable) {
                val itemDuration = System.currentTimeMillis() - itemStartTime
                if (record.attemptCount < retryConfig.maxAttempts) {
                    val delayMs = retryConfig.calculateDelayMs(record.attemptCount)
                    outboxStore.scheduleRetry(
                        tenantContext = tenantContext,
                        outboxId = record.outboxId,
                        nextAttemptAt = System.currentTimeMillis() + delayMs,
                        errorCode = "UNHANDLED_DISPATCHER_ERROR",
                        errorMessage = ex.message ?: "Dispatcher threw an unexpected exception"
                    )
                    metrics.recordRetryScheduled(itemDuration)
                    retriedCount++
                } else {
                    outboxStore.moveToDeadLetter(
                        tenantContext = tenantContext,
                        outboxId = record.outboxId,
                        classification = EventFailureClassification.NON_RETRYABLE,
                        errorCode = "UNHANDLED_EXCEPTION",
                        errorMessage = ex.message ?: "Dispatcher failed with unhandled exception"
                    )
                    metrics.recordDeadLettered(itemDuration)
                    deadLetterCount++
                }
                failedAggregatesInCycle.add(aggregateKey)
            }
        }

        return OutboxDispatchSummary(
            workerId = workerId,
            claimedCount = claimedRecords.size,
            publishedCount = publishedCount,
            retriedCount = retriedCount,
            deadLetterCount = deadLetterCount,
            skippedCount = skippedCount,
            executionDurationMs = System.currentTimeMillis() - startTime
        )
    }

    /**
     * Recovers expired worker leases for crashed workers.
     */
    suspend fun recoverExpiredLeases(tenantContext: TenantContext): Int {
        return outboxStore.recoverExpiredLeases(tenantContext)
    }
}

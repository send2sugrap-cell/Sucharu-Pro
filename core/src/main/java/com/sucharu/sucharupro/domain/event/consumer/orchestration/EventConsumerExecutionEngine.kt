package com.sucharu.sucharupro.domain.event.consumer.orchestration

import com.sucharu.sucharupro.data.event.postgres.IntegrationDeliveryRecord
import com.sucharu.sucharupro.data.event.postgres.IntegrationDeliveryRepository
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.event.consumer.DomainEventConsumer
import com.sucharu.sucharupro.domain.event.consumer.EventConsumerResult
import com.sucharu.sucharupro.domain.event.consumer.EventFailureClassification
import com.sucharu.sucharupro.domain.event.idempotency.EventIdempotencyStore
import com.sucharu.sucharupro.domain.event.idempotency.EventProcessingRecord
import com.sucharu.sucharupro.domain.event.idempotency.EventProcessingStatus
import com.sucharu.sucharupro.domain.event.model.DomainEvent
import com.sucharu.sucharupro.domain.event.model.EventEnvelope

/**
 * Production-grade execution engine for isolated, idempotent, failure-classified consumer invocations (INFRA-04 Step 03).
 */
class EventConsumerExecutionEngine(
    private val idempotencyStore: EventIdempotencyStore? = null,
    private val deliveryRepository: IntegrationDeliveryRepository? = null
) {

    /**
     * Executes a single consumer on an event envelope safely with idempotency and audit protection.
     */
    suspend fun <T : DomainEvent> execute(
        consumer: DomainEventConsumer<T>,
        subscription: ConsumerSubscription,
        envelope: EventEnvelope<T>
    ): ConsumerExecutionOutcome {
        val tenantContext = TenantContext(envelope.projectId)
        val consumerId = consumer.consumerId
        val eventId = envelope.eventId

        // 1. Check idempotency if required
        if (subscription.idempotencyRequired && idempotencyStore != null) {
            if (idempotencyStore.isProcessed(eventId, consumerId, envelope.projectId)) {
                val skippedResult = EventConsumerResult.Skipped(
                    reason = "Event already processed by consumer '$consumerId'",
                    classification = EventFailureClassification.DUPLICATE
                )
                return ConsumerExecutionOutcome(
                    consumerId = consumerId,
                    eventId = eventId,
                    projectId = envelope.projectId,
                    integrationType = subscription.integrationType,
                    result = skippedResult,
                    durationMs = 0L,
                    isDuplicate = true
                )
            }
        }

        // 2. Execute consumer safely
        val startTime = System.currentTimeMillis()
        val result = try {
            consumer.consume(envelope)
        } catch (t: Throwable) {
            EventConsumerResult.Failure(
                reason = "Consumer execution exception: ${t.message ?: t.javaClass.simpleName}",
                classification = EventFailureClassification.TRANSIENT,
                cause = t
            )
        }
        val duration = System.currentTimeMillis() - startTime

        // 3. Record outcome in EventIdempotencyStore
        if (idempotencyStore != null) {
            val status = when (result) {
                is EventConsumerResult.Success -> EventProcessingStatus.PROCESSED
                is EventConsumerResult.Skipped -> EventProcessingStatus.SKIPPED
                is EventConsumerResult.Failure -> EventProcessingStatus.FAILED
            }
            val failureReason = (result as? EventConsumerResult.Failure)?.reason
            idempotencyStore.recordProcessing(
                EventProcessingRecord(
                    eventId = eventId,
                    consumerId = consumerId,
                    projectId = envelope.projectId,
                    processedAt = System.currentTimeMillis(),
                    status = status,
                    failureReason = failureReason,
                    executionDurationMs = duration
                )
            )
        }

        // 4. Record persistent integration delivery record if repository present
        if (deliveryRepository != null && subscription.integrationType != IntegrationType.INTERNAL) {
            val deliveryStatus = when (result) {
                is EventConsumerResult.Success -> IntegrationDeliveryStatus.DELIVERED
                is EventConsumerResult.Skipped -> IntegrationDeliveryStatus.IGNORED_DUPLICATE
                is EventConsumerResult.Failure -> if (result.isRetryable) IntegrationDeliveryStatus.RETRY_SCHEDULED else IntegrationDeliveryStatus.FAILED
            }

            val deliveryRecord = IntegrationDeliveryRecord(
                projectId = envelope.projectId,
                eventId = eventId,
                consumerId = consumerId,
                integrationType = subscription.integrationType,
                destination = consumerId,
                status = deliveryStatus,
                attemptCount = 1,
                lastAttemptAt = System.currentTimeMillis(),
                deliveredAt = if (result is EventConsumerResult.Success) System.currentTimeMillis() else null,
                failureClassification = (result as? EventConsumerResult.Failure)?.classification,
                sanitizedError = (result as? EventConsumerResult.Failure)?.reason,
                correlationId = envelope.correlationId,
                requestId = envelope.requestId
            )
            deliveryRepository.recordDeliveryAttempt(deliveryRecord, tenantContext)
        }

        return ConsumerExecutionOutcome(
            consumerId = consumerId,
            eventId = eventId,
            projectId = envelope.projectId,
            integrationType = subscription.integrationType,
            result = result,
            durationMs = duration,
            isDuplicate = false
        )
    }
}

package com.sucharu.sucharupro.domain.event.consumer.orchestration

import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.auth.authorization.AuthorizationCapability
import com.sucharu.sucharupro.domain.event.consumer.DomainEventConsumer
import com.sucharu.sucharupro.domain.event.consumer.EventConsumerResult
import com.sucharu.sucharupro.domain.event.model.DomainEventType
import com.sucharu.sucharupro.domain.event.model.EventEnvelope

/**
 * High-level integration category.
 */
enum class IntegrationType {
    INTERNAL,
    NOTIFICATION,
    REAL_TIME,
    N8N,
    AI_AGENT
}

/**
 * Execution mode for consumer subscriptions.
 */
enum class ConsumerExecutionMode {
    SYNC,
    ASYNC
}

/**
 * Ordering requirement for aggregate event processing.
 */
enum class OrderingRequirement {
    AGGREGATE_STRICT,
    UNORDERED
}

/**
 * State of an integration delivery record.
 */
enum class IntegrationDeliveryStatus {
    PENDING,
    DELIVERED,
    RETRY_SCHEDULED,
    FAILED,
    DEAD_LETTERED,
    IGNORED_DUPLICATE
}

/**
 * Explicit subscription configuration for a registered domain event consumer (INFRA-04 Step 03).
 */
data class ConsumerSubscription(
    val consumerId: String,
    val supportedEventType: DomainEventType,
    val supportedVersion: String = supportedEventType.currentVersion,
    val integrationType: IntegrationType = IntegrationType.INTERNAL,
    val capabilityRequirement: AuthorizationCapability? = null,
    val executionMode: ConsumerExecutionMode = ConsumerExecutionMode.SYNC,
    val orderingRequirement: OrderingRequirement = OrderingRequirement.AGGREGATE_STRICT,
    val idempotencyRequired: Boolean = true,
    val maxRetries: Int = 3
) {
    init {
        require(consumerId.isNotBlank()) { "consumerId cannot be blank" }
        require(supportedVersion.isNotBlank()) { "supportedVersion cannot be blank" }
        require(maxRetries >= 0) { "maxRetries must be non-negative" }
    }
}

/**
 * Secure execution context passed to consumers during event dispatch.
 * Invariant: [projectId] and [actorId] strictly originate from the server-authoritative envelope.
 */
data class ConsumerExecutionContext(
    val projectId: String,
    val eventId: String,
    val eventType: DomainEventType,
    val eventVersion: String,
    val aggregateType: String,
    val aggregateId: String,
    val aggregateVersion: Long,
    val actorType: PrincipalType,
    val actorId: String,
    val principalType: PrincipalType,
    val correlationId: String,
    val causationId: String?,
    val requestId: String?,
    val consumerId: String,
    val integrationType: IntegrationType,
    val executionTimestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromEnvelope(
            envelope: EventEnvelope<*>,
            consumerId: String,
            integrationType: IntegrationType = IntegrationType.INTERNAL
        ): ConsumerExecutionContext {
            return ConsumerExecutionContext(
                projectId = envelope.projectId,
                eventId = envelope.eventId,
                eventType = envelope.eventType,
                eventVersion = envelope.eventVersion,
                aggregateType = envelope.aggregateType,
                aggregateId = envelope.aggregateId,
                aggregateVersion = envelope.aggregateVersion,
                actorType = envelope.actorType,
                actorId = envelope.actorId,
                principalType = envelope.principalType,
                correlationId = envelope.correlationId,
                causationId = envelope.causationId,
                requestId = envelope.requestId,
                consumerId = consumerId,
                integrationType = integrationType
            )
        }
    }
}

/**
 * Outcome of executing a consumer on an event envelope.
 */
data class ConsumerExecutionOutcome(
    val consumerId: String,
    val eventId: String,
    val projectId: String,
    val integrationType: IntegrationType,
    val result: EventConsumerResult,
    val durationMs: Long,
    val isDuplicate: Boolean = false
)

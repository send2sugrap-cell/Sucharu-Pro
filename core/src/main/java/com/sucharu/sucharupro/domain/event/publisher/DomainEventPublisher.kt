package com.sucharu.sucharupro.domain.event.publisher

import com.sucharu.sucharupro.domain.event.model.DomainEvent
import com.sucharu.sucharupro.domain.event.model.EventEnvelope

/**
 * Result of publishing an event through [DomainEventPublisher].
 */
sealed class PublishResult {
    data class Success(
        val eventId: String,
        val publishedAt: Long = System.currentTimeMillis()
    ) : PublishResult()

    data class Failure(
        val eventId: String,
        val reason: String,
        val cause: Throwable? = null
    ) : PublishResult()

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure
}

/**
 * Canonical Domain Event Publisher contract for Sucharu Pro (INFRA-04 Step 01).
 *
 * Responsibilities:
 * - Validate event and envelope integrity before dispatch.
 * - Preserve tenant boundary, correlation IDs, and causation metadata.
 * - Publish single or batch domain event envelopes.
 * - Remain completely decoupled from physical message brokers (Kafka, RabbitMQ, Redis, etc.).
 */
interface DomainEventPublisher {
    /**
     * Publishes a single immutable domain event envelope.
     */
    suspend fun <T : DomainEvent> publish(envelope: EventEnvelope<T>): PublishResult

    /**
     * Publishes a batch of domain event envelopes atomically where supported.
     */
    suspend fun publishAll(envelopes: List<EventEnvelope<*>>): List<PublishResult>
}

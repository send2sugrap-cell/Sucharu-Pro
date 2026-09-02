package com.sucharu.sucharupro.domain.event.consumer

import com.sucharu.sucharupro.domain.event.model.DomainEvent
import com.sucharu.sucharupro.domain.event.model.DomainEventType
import com.sucharu.sucharupro.domain.event.model.EventEnvelope

/**
 * Canonical domain event consumer abstraction for Sucharu Pro (INFRA-04 Step 01).
 *
 * Responsibilities:
 * - Declare unique consumer identity [consumerId] for idempotency tracking.
 * - Declare supported [supportedEventType] and [supportedVersion].
 * - Safely process the received [EventEnvelope] with zero side-effects on duplicate deliveries.
 * - Return structured [EventConsumerResult] indicating outcome and failure classification.
 */
interface DomainEventConsumer<T : DomainEvent> {
    /**
     * Globally unique, deterministic identifier for this consumer (e.g., "NotificationOrderCreatedConsumer").
     */
    val consumerId: String

    /**
     * The specific [DomainEventType] this consumer is registered to handle.
     */
    val supportedEventType: DomainEventType

    /**
     * Supported schema version (e.g., "v1").
     */
    val supportedVersion: String get() = supportedEventType.currentVersion

    /**
     * Consumes the typed domain event envelope.
     *
     * Invariants:
     * - Must remain idempotent across identical envelopes.
     * - Must not mutate unrelated business state.
     * - Must not bypass tenant or authorization boundaries.
     */
    suspend fun consume(envelope: EventEnvelope<T>): EventConsumerResult

    /**
     * Checks if this consumer supports the given envelope type and version.
     */
    fun supports(envelope: EventEnvelope<*>): Boolean {
        return envelope.eventType == supportedEventType && envelope.eventVersion == supportedVersion
    }
}

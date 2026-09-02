package com.sucharu.sucharupro.domain.event.model

/**
 * Base marker contract for all domain event payloads in Sucharu Pro.
 *
 * Domain events represent immutable facts that have already occurred within a bounded context.
 * They must never represent commands or mutating requests.
 */
interface DomainEvent {
    /**
     * Strongly typed domain event type classification.
     */
    val eventType: DomainEventType

    /**
     * Schema version for this event payload (e.g., "v1").
     */
    val eventVersion: String get() = eventType.currentVersion

    /**
     * Primary domain aggregate identifier associated with this event.
     */
    val aggregateId: String

    /**
     * Primary domain aggregate classification (e.g., "ORDER", "CUSTOMER", "INVOICE").
     */
    val aggregateType: String get() = eventType.category.name

    /**
     * Current monotonic version of the aggregate at the time this fact occurred.
     */
    val aggregateVersion: Long get() = 1L
}

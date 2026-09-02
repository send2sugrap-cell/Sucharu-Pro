package com.sucharu.sucharupro.domain.event.store

import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.event.model.EventEnvelope

/**
 * Append-only Event Store persistence abstraction for Sucharu Pro (INFRA-04 Step 01).
 *
 * Responsibilities:
 * - Persist immutable domain event envelopes with strict tenant isolation.
 * - Retrieve historical event streams by aggregate identity and correlation IDs.
 * - Enforce optimistic stream version checks to prevent out-of-order aggregate corruptions.
 */
interface EventStore {
    /**
     * Appends an immutable event envelope to the event store.
     *
     * Invariants:
     * - Must enforce [envelope.projectId] matching the authorized [tenantContext.projectId].
     * - Must reject duplicate [envelope.eventId].
     */
    suspend fun append(envelope: EventEnvelope<*>, tenantContext: TenantContext)

    /**
     * Appends multiple envelopes in an atomic batch.
     */
    suspend fun appendAll(envelopes: List<EventEnvelope<*>>, tenantContext: TenantContext)

    /**
     * Retrieves an event by its unique [eventId] strictly within the scoped [tenantContext].
     */
    suspend fun getById(eventId: String, tenantContext: TenantContext): EventEnvelope<*>?

    /**
     * Retrieves all events belonging to an aggregate stream, ordered by [EventEnvelope.aggregateVersion].
     */
    suspend fun getByAggregate(
        aggregateType: String,
        aggregateId: String,
        tenantContext: TenantContext
    ): List<EventEnvelope<*>>

    /**
     * Retrieves all events associated with a specific [correlationId] for tracing.
     */
    suspend fun getByCorrelationId(
        correlationId: String,
        tenantContext: TenantContext
    ): List<EventEnvelope<*>>
}

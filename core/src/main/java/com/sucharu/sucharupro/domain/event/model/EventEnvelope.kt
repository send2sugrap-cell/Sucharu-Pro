package com.sucharu.sucharupro.domain.event.model

import com.sucharu.sucharupro.data.api.model.PrincipalType
import java.util.UUID

/**
 * Immutable identity representing the actor who triggered or initiated the domain operation.
 */
data class EventActor(
    val actorId: String,
    val actorType: PrincipalType = PrincipalType.HUMAN,
    val principalType: PrincipalType = actorType
) {
    init {
        require(actorId.isNotBlank()) { "actorId cannot be blank" }
    }

    companion object {
        fun system(identifier: String = "SYSTEM"): EventActor =
            EventActor(actorId = identifier, actorType = PrincipalType.SYSTEM, principalType = PrincipalType.SYSTEM)

        fun aiAgent(agentId: String): EventActor =
            EventActor(actorId = agentId, actorType = PrincipalType.AI_AGENT, principalType = PrincipalType.AI_AGENT)

        fun human(userId: String): EventActor =
            EventActor(actorId = userId, actorType = PrincipalType.HUMAN, principalType = PrincipalType.HUMAN)
    }
}

/**
 * Distributed tracing metadata across workflows, causation chains, and originating API requests.
 */
data class EventTraceContext(
    val correlationId: String = UUID.randomUUID().toString(),
    val causationId: String? = null,
    val requestId: String? = null
) {
    init {
        require(correlationId.isNotBlank()) { "correlationId cannot be blank" }
    }

    /**
     * Creates a descendant trace context where this event's ID becomes the causation ID.
     */
    fun createChildContext(parentEventId: String): EventTraceContext =
        copy(correlationId = this.correlationId, causationId = parentEventId)
}

/**
 * Canonical aggregate reference associated with the domain event.
 */
data class AggregateReference(
    val aggregateType: String,
    val aggregateId: String,
    val aggregateVersion: Long = 1L
) {
    init {
        require(aggregateType.isNotBlank()) { "aggregateType cannot be blank" }
        require(aggregateId.isNotBlank()) { "aggregateId cannot be blank" }
        require(aggregateVersion >= 0L) { "aggregateVersion cannot be negative" }
    }
}

/**
 * Canonical, production-grade, immutable Event Envelope for Sucharu Pro (INFRA-04 Step 01).
 *
 * Encapsulates the strongly typed domain event [payload] with server-authoritative tenant scoping,
 * cryptographic-grade event identity, aggregate boundary, actor context, distributed tracing,
 * and immutable metadata.
 *
 * Rules:
 * - [projectId] MUST be derived from server-authoritative context (never trusted from raw client input).
 * - [eventId] MUST be globally unique and collision resistant.
 * - [occurredAt] represents the exact epoch millisecond when the domain fact occurred.
 * - [publishedAt] represents the epoch millisecond when the envelope was handed to the dispatcher.
 */
data class EventEnvelope<out T : DomainEvent>(
    val eventId: String = UUID.randomUUID().toString(),
    val eventType: DomainEventType,
    val eventVersion: String = eventType.currentVersion,
    val occurredAt: Long = System.currentTimeMillis(),
    val publishedAt: Long = occurredAt,
    val projectId: String,
    val aggregateType: String,
    val aggregateId: String,
    val aggregateVersion: Long = 1L,
    val actorType: PrincipalType = PrincipalType.HUMAN,
    val actorId: String,
    val principalType: PrincipalType = actorType,
    val correlationId: String = UUID.randomUUID().toString(),
    val causationId: String? = null,
    val requestId: String? = null,
    val source: String = "sucharu-pro-backend",
    val payload: T,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(eventId.isNotBlank()) { "eventId cannot be blank" }
        require(projectId.isNotBlank()) { "projectId cannot be blank; cross-tenant or un-scoped events are strictly prohibited." }
        require(aggregateType.isNotBlank()) { "aggregateType cannot be blank" }
        require(aggregateId.isNotBlank()) { "aggregateId cannot be blank" }
        require(actorId.isNotBlank()) { "actorId cannot be blank" }
        require(correlationId.isNotBlank()) { "correlationId cannot be blank" }
        require(eventVersion.isNotBlank()) { "eventVersion cannot be blank" }
        require(aggregateVersion >= 0L) { "aggregateVersion cannot be negative" }
    }

    /**
     * Extracts structured actor context.
     */
    val actor: EventActor
        get() = EventActor(actorId = actorId, actorType = actorType, principalType = principalType)

    /**
     * Extracts structured trace context.
     */
    val traceContext: EventTraceContext
        get() = EventTraceContext(correlationId = correlationId, causationId = causationId, requestId = requestId)

    /**
     * Extracts structured aggregate reference.
     */
    val aggregateReference: AggregateReference
        get() = AggregateReference(
            aggregateType = aggregateType,
            aggregateId = aggregateId,
            aggregateVersion = aggregateVersion
        )

    /**
     * Formatted canonical versioned signature (e.g. "OrderCreated:v1").
     */
    val versionedEventType: String get() = "${eventType.typeName}:$eventVersion"

    companion object {
        /**
         * Factory function to create a canonical [EventEnvelope] from a typed domain event and authoritative contexts.
         */
        fun <E : DomainEvent> create(
            payload: E,
            projectId: String,
            actor: EventActor,
            traceContext: EventTraceContext = EventTraceContext(),
            aggregateVersion: Long = payload.aggregateVersion,
            source: String = "sucharu-pro-backend",
            metadata: Map<String, String> = emptyMap(),
            eventId: String = UUID.randomUUID().toString(),
            occurredAt: Long = System.currentTimeMillis()
        ): EventEnvelope<E> {
            return EventEnvelope(
                eventId = eventId,
                eventType = payload.eventType,
                eventVersion = payload.eventVersion,
                occurredAt = occurredAt,
                publishedAt = System.currentTimeMillis(),
                projectId = projectId,
                aggregateType = payload.aggregateType,
                aggregateId = payload.aggregateId,
                aggregateVersion = aggregateVersion,
                actorType = actor.actorType,
                actorId = actor.actorId,
                principalType = actor.principalType,
                correlationId = traceContext.correlationId,
                causationId = traceContext.causationId,
                requestId = traceContext.requestId,
                source = source,
                payload = payload,
                metadata = metadata
            )
        }
    }
}

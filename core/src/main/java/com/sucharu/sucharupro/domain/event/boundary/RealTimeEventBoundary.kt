package com.sucharu.sucharupro.domain.event.boundary

import com.sucharu.sucharupro.domain.event.model.EventEnvelope

/**
 * Standard real-time streaming frame format for WebSocket / Server-Sent Events (SSE).
 */
data class RealTimeEventFrame(
    val frameId: String,
    val eventId: String,
    val eventType: String,
    val eventVersion: String,
    val projectId: String,
    val topic: String,
    val timestamp: Long,
    val aggregateType: String,
    val aggregateId: String,
    val correlationId: String,
    val payloadSummary: Map<String, String>
)

/**
 * Boundary contract preparing domain event envelopes for real-time dispatch (INFRA-04 Step 01).
 *
 * Provider-agnostic metadata preparation for future WebSocket / SSE infrastructure.
 */
object RealTimeEventBoundary {

    /**
     * Converts a domain event envelope into a real-time stream frame.
     */
    fun toStreamFrame(envelope: EventEnvelope<*>): RealTimeEventFrame {
        val topic = "tenant.${envelope.projectId}.${envelope.aggregateType.lowercase()}.${envelope.aggregateId}"
        return RealTimeEventFrame(
            frameId = java.util.UUID.randomUUID().toString(),
            eventId = envelope.eventId,
            eventType = envelope.eventType.typeName,
            eventVersion = envelope.eventVersion,
            projectId = envelope.projectId,
            topic = topic,
            timestamp = envelope.occurredAt,
            aggregateType = envelope.aggregateType,
            aggregateId = envelope.aggregateId,
            correlationId = envelope.correlationId,
            payloadSummary = envelope.metadata
        )
    }
}

package com.sucharu.sucharupro.domain.event.boundary

import com.sucharu.sucharupro.domain.event.model.DomainEventType
import com.sucharu.sucharupro.domain.event.model.EventEnvelope

/**
 * Sanitized, decoupled payload model for outgoing n8n automation webhooks.
 */
data class N8nWebhookPayload(
    val eventId: String,
    val eventType: String,
    val eventVersion: String,
    val projectId: String,
    val aggregateType: String,
    val aggregateId: String,
    val correlationId: String,
    val causationId: String?,
    val occurredAt: Long,
    val payloadSummary: Map<String, String>,
    val webhookSignature: String? = null
)

/**
 * Decoupled integration boundary for n8n workflow automations (INFRA-04 Step 01).
 *
 * Rules:
 * - Zero hardcoded URLs or external webhook endpoints in domain contracts.
 * - Zero credentials, passwords, or authentication tokens in webhook payload.
 * - Security events (e.g. passwords, sessions, login failures) are strictly blocked from external automations.
 */
object N8nIntegrationBoundary {

    private val blockedForN8n = setOf(
        DomainEventType.AUTH_SUCCEEDED,
        DomainEventType.AUTH_FAILED,
        DomainEventType.SESSION_CREATED,
        DomainEventType.SESSION_REVOKED,
        DomainEventType.AUTHZ_DENIED,
        DomainEventType.ACCOUNT_LOCKED,
        DomainEventType.PASSWORD_CHANGED
    )

    /**
     * Converts a domain event envelope into a sanitized n8n webhook payload.
     * Throws [IllegalArgumentException] if the event is a restricted security event.
     */
    fun toSanitizedWebhookPayload(envelope: EventEnvelope<*>): N8nWebhookPayload {
        require(!blockedForN8n.contains(envelope.eventType)) {
            "Event type '${envelope.eventType}' is a restricted security event and cannot be exported to n8n automation."
        }

        // Build sanitized payload map from metadata and aggregate info
        val summary = mutableMapOf(
            "aggregateType" to envelope.aggregateType,
            "aggregateId" to envelope.aggregateId,
            "aggregateVersion" to envelope.aggregateVersion.toString(),
            "source" to envelope.source
        )

        // Append non-sensitive metadata only
        envelope.metadata.forEach { (key, value) ->
            if (!key.contains("token", ignoreCase = true) &&
                !key.contains("secret", ignoreCase = true) &&
                !key.contains("auth", ignoreCase = true)
            ) {
                summary[key] = value
            }
        }

        return N8nWebhookPayload(
            eventId = envelope.eventId,
            eventType = envelope.eventType.typeName,
            eventVersion = envelope.eventVersion,
            projectId = envelope.projectId,
            aggregateType = envelope.aggregateType,
            aggregateId = envelope.aggregateId,
            correlationId = envelope.correlationId,
            causationId = envelope.causationId,
            occurredAt = envelope.occurredAt,
            payloadSummary = summary
        )
    }
}

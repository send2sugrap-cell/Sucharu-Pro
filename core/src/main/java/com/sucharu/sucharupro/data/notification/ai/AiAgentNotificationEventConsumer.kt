package com.sucharu.sucharupro.data.notification.ai

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.auth.authorization.AuthorizationCapability
import com.sucharu.sucharupro.data.notification.security.NotificationPayloadSanitizer
import com.sucharu.sucharupro.domain.event.consumer.DomainEventConsumer
import com.sucharu.sucharupro.domain.event.consumer.EventConsumerResult
import com.sucharu.sucharupro.domain.event.consumer.EventFailureClassification
import com.sucharu.sucharupro.domain.event.model.DomainEvent
import com.sucharu.sucharupro.domain.event.model.DomainEventType
import com.sucharu.sucharupro.domain.event.model.EventEnvelope
import com.sucharu.sucharupro.domain.notification.ai.AiNotificationAuditEvent
import com.sucharu.sucharupro.domain.notification.ai.AiNotificationAuditOperation
import com.sucharu.sucharupro.domain.notification.ai.AiNotificationEventView
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Production-grade event consumer providing data-minimized event views to AI Agents (INFRA-04 Step 08).
 *
 * Rules:
 * 1. Rejects sensitive/blocked event categories (auth failures, passwords, session revocation).
 * 2. Strict tenant isolation (event.projectId == principal.projectId).
 * 3. Builds data-minimized [AiNotificationEventView] without exposing raw DomainEvent or EventEnvelope.
 * 4. Strips all secret metadata before AI presentation.
 */
class AiAgentNotificationEventConsumer<T : DomainEvent>(
    override val supportedEventType: DomainEventType,
    override val supportedVersion: String = supportedEventType.currentVersion,
    override val consumerId: String = "ai_agent.notification.${supportedEventType.name.lowercase()}",
    private val targetAgentPrincipal: AuthenticatedPrincipal,
    private val securityBoundary: AiAgentNotificationSecurityBoundary = AiAgentNotificationSecurityBoundary(),
    private val auditService: AiNotificationAuditService? = null,
    private val onViewReceived: ((AiNotificationEventView) -> Unit)? = null
) : DomainEventConsumer<T> {

    private val _receivedViews = CopyOnWriteArrayList<AiNotificationEventView>()
    val receivedViews: List<AiNotificationEventView> get() = _receivedViews.toList()

    override suspend fun consume(envelope: EventEnvelope<T>): EventConsumerResult {
        // 1. Machine principal verification
        if (!targetAgentPrincipal.isAiAgent &&
            targetAgentPrincipal.principalType != PrincipalType.AI_AGENT &&
            targetAgentPrincipal.role != UserRole.AI_AGENT) {
            return EventConsumerResult.Failure(
                reason = "Consumer principal is not an AI_AGENT.",
                classification = EventFailureClassification.SECURITY
            )
        }

        // 2. Tenant isolation check
        if (targetAgentPrincipal.projectId != envelope.projectId) {
            return EventConsumerResult.Failure(
                reason = "Cross-tenant event consumption blocked: agent '${targetAgentPrincipal.projectId}' != event '${envelope.projectId}'.",
                classification = EventFailureClassification.SECURITY
            )
        }

        // 3. Sensitive event check
        if (securityBoundary.isEventBlockedForAi(envelope.eventType)) {
            auditService?.record(
                projectId = envelope.projectId,
                operation = AiNotificationAuditOperation.AI_NOTIFICATION_SENSITIVE_DATA_BLOCKED,
                decision = "BLOCKED",
                agentId = targetAgentPrincipal.userId,
                correlationId = envelope.correlationId,
                requestId = envelope.requestId ?: envelope.eventId,
                reasonCode = "SENSITIVE_EVENT_BLOCKED",
                safeSummary = "Blocked sensitive event type '${envelope.eventType}'"
            )
            return EventConsumerResult.Failure(
                reason = "Event type '${envelope.eventType}' is sensitive and strictly blocked from AI Agents.",
                classification = EventFailureClassification.SECURITY
            )
        }

        // 4. Capability verification
        val hasCap = targetAgentPrincipal.hasPermission(com.sucharu.sucharupro.data.api.model.UserPermission.ADMIN_ALL) ||
                targetAgentPrincipal.permissions.any { it.name == AuthorizationCapability.AI_READ_NOTIFICATION_CONTEXT.name } ||
                com.sucharu.sucharupro.data.auth.authorization.RoleCapabilityMatrix.hasCapability(
                    targetAgentPrincipal.role,
                    AuthorizationCapability.AI_READ_NOTIFICATION_CONTEXT
                )

        if (!hasCap) {
            return EventConsumerResult.Failure(
                reason = "AI Agent lacks mandatory capability 'AI_READ_NOTIFICATION_CONTEXT'.",
                classification = EventFailureClassification.SECURITY
            )
        }

        // 5. Build data-minimized view (sanitize metadata, strip secrets)
        val sanitizedMeta = NotificationPayloadSanitizer.sanitizeMetadata(envelope.metadata)
        val businessSummary = "Event ${envelope.eventType.name} for ${envelope.aggregateType}:${envelope.aggregateId}"

        val view = AiNotificationEventView(
            eventId = envelope.eventId,
            eventType = envelope.eventType,
            eventVersion = envelope.eventVersion,
            projectId = envelope.projectId,
            occurredAt = envelope.occurredAt,
            aggregateType = envelope.aggregateType,
            aggregateId = envelope.aggregateId,
            businessSummary = businessSummary,
            permittedMetadata = sanitizedMeta,
            correlationId = envelope.correlationId,
            allowedRecipientHint = envelope.actorId,
            allowedActionHints = listOf("CREATE_DRAFT", "REQUEST_SEND")
        )

        _receivedViews.add(view)
        onViewReceived?.invoke(view)

        auditService?.record(
            projectId = envelope.projectId,
            operation = AiNotificationAuditOperation.AI_NOTIFICATION_CONTEXT_READ,
            decision = "CONSUMED",
            agentId = targetAgentPrincipal.userId,
            correlationId = envelope.correlationId,
            requestId = envelope.requestId ?: envelope.eventId,
            safeSummary = "Consumed sanitized event view '${envelope.eventId}'"
        )

        return EventConsumerResult.Success(
            message = "AI Agent safely consumed event view for '${envelope.eventId}'"
        )
    }

    fun clear() = _receivedViews.clear()
}

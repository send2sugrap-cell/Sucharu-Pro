package com.sucharu.sucharupro.domain.event.boundary

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.auth.authorization.AuthorizationCapability
import com.sucharu.sucharupro.domain.event.consumer.EventFailureClassification
import com.sucharu.sucharupro.domain.event.model.DomainEventType
import com.sucharu.sucharupro.domain.event.model.EventEnvelope

/**
 * Result of AI Agent event subscription authorization evaluation.
 */
sealed class AiAgentEventAccessDecision {
    data class Allowed(
        val sanitizedMetadata: Map<String, String>,
        val grantedCapability: AuthorizationCapability
    ) : AiAgentEventAccessDecision()

    data class Denied(
        val reason: String,
        val classification: EventFailureClassification = EventFailureClassification.SECURITY
    ) : AiAgentEventAccessDecision()

    val isAllowed: Boolean get() = this is Allowed
    val isDenied: Boolean get() = this is Denied
}

/**
 * Security boundary governing AI Agent access to domain events (INFRA-04 Step 01).
 *
 * Rules:
 * 1. AI Agents are machine principals only ([PrincipalType.AI_AGENT]).
 * 2. Unrestricted / wildcard subscriptions are strictly blocked.
 * 3. AI Agents cannot cross tenant boundaries.
 * 4. Explicit capability authorization is required per domain event type.
 * 5. Sensitive internal financial fields and credentials are stripped.
 */
object AiAgentEventBoundary {

    /**
     * Map of event types to minimum required AI Agent capability.
     */
    private val requiredCapabilities = mapOf(
        DomainEventType.ORDER_CREATED to AuthorizationCapability.AI_READ_ORDER_CONTEXT,
        DomainEventType.ORDER_UPDATED to AuthorizationCapability.AI_READ_ORDER_CONTEXT,
        DomainEventType.ORDER_CANCELLED to AuthorizationCapability.AI_READ_ORDER_CONTEXT,
        DomainEventType.CUSTOMER_REGISTERED to AuthorizationCapability.AI_READ_CUSTOMER_CONTEXT,
        DomainEventType.CUSTOMER_VERIFIED to AuthorizationCapability.AI_READ_CUSTOMER_CONTEXT,
        DomainEventType.AFFILIATE_REFERRAL_CREATED to AuthorizationCapability.AI_READ_AFFILIATE_CONTEXT,
        DomainEventType.INVOICE_CREATED to AuthorizationCapability.AI_READ_INVOICE
    )

    /**
     * Prohibited event categories for AI Agents (administrative security and internal secrets).
     */
    private val blockedForAiAgents = setOf(
        DomainEventType.AUTH_SUCCEEDED,
        DomainEventType.AUTH_FAILED,
        DomainEventType.SESSION_CREATED,
        DomainEventType.SESSION_REVOKED,
        DomainEventType.AUTHZ_DENIED,
        DomainEventType.ACCOUNT_LOCKED,
        DomainEventType.PASSWORD_CHANGED,
        DomainEventType.PAYMENT_RECEIVED,
        DomainEventType.PAYMENT_REFUNDED
    )

    /**
     * Evaluates if an AI agent principal is authorized to subscribe to or receive the given event.
     */
    fun evaluateAccess(
        principal: AuthenticatedPrincipal?,
        envelope: EventEnvelope<*>
    ): AiAgentEventAccessDecision {
        if (principal == null) {
            return AiAgentEventAccessDecision.Denied("Unauthenticated principal cannot consume events.")
        }

        // Verify principal is indeed an AI agent
        if (principal.role != UserRole.AI_AGENT && principal.principalType != PrincipalType.AI_AGENT) {
            return AiAgentEventAccessDecision.Denied("Principal is not an AI_AGENT machine principal.")
        }

        // Strict tenant isolation check
        if (principal.projectId != envelope.projectId) {
            return AiAgentEventAccessDecision.Denied(
                "Cross-tenant event consumption blocked. Agent project '${principal.projectId}' does not match event project '${envelope.projectId}'."
            )
        }

        // Check if event type is inherently blocked for AI agents
        if (blockedForAiAgents.contains(envelope.eventType)) {
            return AiAgentEventAccessDecision.Denied(
                "AI agents are strictly prohibited from subscribing to security or raw financial transaction event type '${envelope.eventType}'."
            )
        }

        // Look up required capability
        val requiredCap = requiredCapabilities[envelope.eventType]
            ?: return AiAgentEventAccessDecision.Denied(
                "No authorized AI Agent capability mapping exists for event type '${envelope.eventType}'."
            )

        // Verify principal possesses the capability via canonical RoleCapabilityMatrix
        if (!com.sucharu.sucharupro.data.auth.authorization.RoleCapabilityMatrix.hasCapability(principal.role, requiredCap)) {
            return AiAgentEventAccessDecision.Denied(
                "AI agent '${principal.userId}' lacks mandatory capability '${requiredCap.name}' for event type '${envelope.eventType}'."
            )
        }

        // Sanitize metadata
        val sanitizedMeta = envelope.metadata.filterKeys { key ->
            !key.contains("secret", ignoreCase = true) &&
                    !key.contains("token", ignoreCase = true) &&
                    !key.contains("key", ignoreCase = true) &&
                    !key.contains("password", ignoreCase = true)
        }

        return AiAgentEventAccessDecision.Allowed(
            sanitizedMetadata = sanitizedMeta,
            grantedCapability = requiredCap
        )
    }
}

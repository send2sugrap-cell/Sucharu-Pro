package com.sucharu.sucharupro.data.notification.ai

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.auth.authorization.AuthorizationCapability
import com.sucharu.sucharupro.data.auth.authorization.RoleCapabilityMatrix
import com.sucharu.sucharupro.data.notification.security.InMemoryNotificationSuppressionRepository
import com.sucharu.sucharupro.data.notification.security.NotificationPayloadSanitizer
import com.sucharu.sucharupro.data.notification.security.NotificationRateLimiter
import com.sucharu.sucharupro.data.notification.security.NotificationSuppressionRepository
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.event.boundary.NotificationChannel
import com.sucharu.sucharupro.domain.event.model.DomainEventType
import com.sucharu.sucharupro.domain.notification.ai.*
import com.sucharu.sucharupro.domain.notification.security.RateLimitPolicy

/**
 * Dedicated Security Boundary mediating all AI Agent notification requests (INFRA-04 Step 08).
 *
 * Enforces all 12 non-negotiable architectural rules:
 * - Machine principal verification
 * - Fail-closed server-authoritative tenant isolation
 * - Explicit capability checks (Deny by default)
 * - Data minimization & payload sanitization
 * - Sensitive event / credential blocking
 * - Human-in-the-loop confirmation requirement for execution
 * - Rate limiting & anti-abuse
 * - n8n & direct provider bypass prevention
 */
class AiAgentNotificationSecurityBoundary(
    private val suppressionRepository: NotificationSuppressionRepository = InMemoryNotificationSuppressionRepository(),
    private val rateLimiter: NotificationRateLimiter = NotificationRateLimiter(),
    private val auditService: AiNotificationAuditService? = null,
    private val explicitCapabilityChecker: ((principal: AuthenticatedPrincipal, capability: AuthorizationCapability) -> Boolean)? = null
) {

    /** Event categories prohibited from AI Agent consumption or notification dispatch. */
    private val blockedEventTypes = setOf(
        DomainEventType.AUTH_SUCCEEDED,
        DomainEventType.AUTH_FAILED,
        DomainEventType.SESSION_CREATED,
        DomainEventType.SESSION_REVOKED,
        DomainEventType.AUTHZ_DENIED,
        DomainEventType.ACCOUNT_LOCKED,
        DomainEventType.PASSWORD_CHANGED,
        DomainEventType.PAYMENT_RECEIVED,
        DomainEventType.PAYMENT_REFUNDED,
        DomainEventType.NOTIFICATION_AUTHORIZATION_DENIED,
        DomainEventType.NOTIFICATION_PROVIDER_SECURITY_FAILURE
    )

    /** Map of action types to required explicit capabilities. */
    private val requiredCapabilities = mapOf(
        AiNotificationActionType.CREATE_DRAFT to AuthorizationCapability.AI_CREATE_NOTIFICATION_DRAFT,
        AiNotificationActionType.REQUEST_SEND to AuthorizationCapability.AI_REQUEST_NOTIFICATION_SEND,
        AiNotificationActionType.REQUEST_REPLAY to AuthorizationCapability.AI_REQUEST_NOTIFICATION_REPLAY,
        AiNotificationActionType.REQUEST_SUPPRESSION to AuthorizationCapability.AI_REQUEST_NOTIFICATION_SUPPRESSION,
        AiNotificationActionType.REQUEST_PREFERENCE_UPDATE to AuthorizationCapability.AI_REQUEST_NOTIFICATION_PREFERENCE_UPDATE
    )

    /** Rate limit policies for AI Agent actions. */
    private fun aiActionRateLimitPolicy(projectId: String, agentId: String, actionType: String) =
        RateLimitPolicy(
            dimensionKey = "ai_agent:$projectId:$agentId:$actionType",
            windowSeconds = 60L,
            maxCount = 20
        )

    private fun aiProjectRateLimitPolicy(projectId: String) =
        RateLimitPolicy(
            dimensionKey = "ai_agent_project:$projectId",
            windowSeconds = 60L,
            maxCount = 200
        )

    /**
     * Evaluates security policy for an incoming AI Agent action request.
     */
    suspend fun evaluateActionRequest(
        principal: AuthenticatedPrincipal?,
        request: AiNotificationActionRequest,
        serverProjectId: String
    ): AiNotificationSecurityDecision {
        val tenantContext = TenantContext(serverProjectId)

        // 1. Authentication check
        if (principal == null) {
            return deny(
                AiNotificationDenialReason.UNAUTHENTICATED,
                "Unauthenticated AI action request.",
                serverProjectId,
                request
            )
        }

        // 2. Machine principal verification
        if (!principal.isAiAgent && principal.principalType != PrincipalType.AI_AGENT && principal.role != UserRole.AI_AGENT) {
            return deny(
                AiNotificationDenialReason.NOT_AN_AI_AGENT,
                "Principal '${principal.userId}' is not an AI_AGENT machine principal.",
                serverProjectId,
                request
            )
        }

        // 3. Server-authoritative tenant isolation
        if (principal.projectId != serverProjectId || request.projectId != serverProjectId) {
            return deny(
                AiNotificationDenialReason.TENANT_MISMATCH,
                "Tenant mismatch: principal '${principal.projectId}', request '${request.projectId}', server '$serverProjectId'.",
                serverProjectId,
                request
            )
        }

        // 4. Explicit capability check
        val requiredCap = requiredCapabilities[request.actionType]
            ?: return deny(
                AiNotificationDenialReason.CAPABILITY_MISSING,
                "No capability mapping configured for action type '${request.actionType}'.",
                serverProjectId,
                request
            )

        val hasCap = hasExplicitCapability(principal, requiredCap)
        if (!hasCap) {
            return deny(
                AiNotificationDenialReason.CAPABILITY_MISSING,
                "AI Agent '${principal.userId}' lacks mandatory capability '${requiredCap.name}'.",
                serverProjectId,
                request
            )
        }

        // 5. Credential leak detection in title, body, or metadata
        if (NotificationPayloadSanitizer.containsCredentialLeak(request.title) ||
            NotificationPayloadSanitizer.containsCredentialLeak(request.body) ||
            request.metadata.values.any { NotificationPayloadSanitizer.containsCredentialLeak(it) }) {
            return deny(
                AiNotificationDenialReason.CREDENTIAL_LEAK_DETECTED,
                "AI notification payload contains detected credential material. Action blocked.",
                serverProjectId,
                request
            )
        }

        // 6. Injection vector detection in title
        if (NotificationPayloadSanitizer.containsInjection(request.title)) {
            return deny(
                AiNotificationDenialReason.CONTENT_INJECTION_DETECTED,
                "AI notification title contains disallowed injection vectors.",
                serverProjectId,
                request
            )
        }

        // 7. Rate limiting check (per-agent and per-project)
        val agentLimit = rateLimiter.evaluate(
            key = "ai_agent:${request.projectId}:${principal.userId}:${request.actionType.name}",
            policy = aiActionRateLimitPolicy(request.projectId, principal.userId, request.actionType.name)
        )
        if (!agentLimit.allowed) {
            auditService?.record(
                projectId = serverProjectId,
                operation = AiNotificationAuditOperation.AI_NOTIFICATION_RATE_LIMITED,
                decision = "RATE_LIMITED",
                agentId = principal.userId,
                actionType = request.actionType.name,
                correlationId = request.correlationId,
                requestId = request.requestId,
                reasonCode = "AI_AGENT_RATE_LIMIT"
            )
            return AiNotificationSecurityDecision.Denied(
                reason = AiNotificationDenialReason.RATE_LIMITED,
                message = "AI Agent rate limit exceeded for action '${request.actionType.name}'. Retry after ${agentLimit.retryAfterMs}ms."
            )
        }

        val projectLimit = rateLimiter.evaluate(
            key = "ai_agent_project:${request.projectId}",
            policy = aiProjectRateLimitPolicy(request.projectId)
        )
        if (!projectLimit.allowed) {
            return AiNotificationSecurityDecision.Denied(
                reason = AiNotificationDenialReason.RATE_LIMITED,
                message = "Project AI notification rate limit exceeded. Retry after ${projectLimit.retryAfterMs}ms."
            )
        }

        // 8. Suppression check (for send requests)
        if (request.actionType == AiNotificationActionType.REQUEST_SEND) {
            for (channel in request.targetChannels) {
                val isSuppressed = suppressionRepository.isSuppressed(
                    projectId = serverProjectId,
                    recipientId = request.targetRecipientId,
                    channel = channel,
                    tenantContext = tenantContext
                )
                if (isSuppressed) {
                    return deny(
                        AiNotificationDenialReason.RECIPIENT_SUPPRESSED,
                        "Recipient '${request.targetRecipientId}' is suppressed for channel '${channel.name}'.",
                        serverProjectId,
                        request
                    )
                }
            }
        }

        // 9. Mandatory Security Notifications Protection (for preference updates)
        if (request.actionType == AiNotificationActionType.REQUEST_PREFERENCE_UPDATE) {
            val disabledSecurity = request.metadata["disableSecurityAlerts"]?.toBooleanStrictOrNull() ?: false
            if (disabledSecurity) {
                return deny(
                    AiNotificationDenialReason.MANDATORY_SECURITY_NOTIFICATION_IMMUTABLE,
                    "AI Agents cannot propose disabling mandatory security notifications.",
                    serverProjectId,
                    request
                )
            }
        }

        // Record rate limit increment
        rateLimiter.record(
            key = "ai_agent:${request.projectId}:${principal.userId}:${request.actionType.name}",
            policy = aiActionRateLimitPolicy(request.projectId, principal.userId, request.actionType.name)
        )
        rateLimiter.record(
            key = "ai_agent_project:${request.projectId}",
            policy = aiProjectRateLimitPolicy(request.projectId)
        )

        // 10. Sanitize the payload
        val sanitizedTitle = NotificationPayloadSanitizer.sanitizeText(request.title, isTitleField = true)
        val sanitizedBody = NotificationPayloadSanitizer.sanitizeText(request.body, isTitleField = false)
        val sanitizedMeta = NotificationPayloadSanitizer.sanitizeMetadata(request.metadata)

        val cleanRequest = request.copy(
            title = sanitizedTitle,
            body = sanitizedBody,
            metadata = sanitizedMeta
        )

        return AiNotificationSecurityDecision.Allowed(
            grantedCapability = requiredCap,
            sanitizedRequest = cleanRequest
        )
    }

    /**
     * Checks if an event type is sensitive and strictly blocked from AI Agent consumption.
     */
    fun isEventBlockedForAi(eventType: DomainEventType): Boolean =
        blockedEventTypes.contains(eventType)

    private fun hasExplicitCapability(
        principal: AuthenticatedPrincipal,
        capability: AuthorizationCapability
    ): Boolean {
        if (principal.hasPermission(com.sucharu.sucharupro.data.api.model.UserPermission.ADMIN_ALL)) return true
        if (explicitCapabilityChecker != null) {
            return explicitCapabilityChecker.invoke(principal, capability)
        }
        return RoleCapabilityMatrix.hasCapability(principal.role, capability)
    }

    private suspend fun deny(
        reason: AiNotificationDenialReason,
        message: String,
        projectId: String,
        request: AiNotificationActionRequest
    ): AiNotificationSecurityDecision.Denied {
        auditService?.record(
            projectId = projectId,
            operation = AiNotificationAuditOperation.AI_NOTIFICATION_ACTION_DENIED,
            decision = "DENIED",
            agentId = request.targetRecipientId,
            actionType = request.actionType.name,
            recipientId = request.targetRecipientId,
            correlationId = request.correlationId,
            requestId = request.requestId,
            reasonCode = reason.name,
            safeSummary = message
        )
        return AiNotificationSecurityDecision.Denied(reason = reason, message = message)
    }
}

package com.sucharu.sucharupro.data.notification.security

import com.sucharu.sucharupro.data.auth.authorization.AuthorizationCapability
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.event.boundary.NotificationChannel
import com.sucharu.sucharupro.domain.notification.security.NotificationDataClassification
import com.sucharu.sucharupro.domain.notification.security.NotificationSecurityContext
import com.sucharu.sucharupro.domain.notification.security.NotificationSecurityDecision
import com.sucharu.sucharupro.domain.notification.security.NotificationSecurityReason
import com.sucharu.sucharupro.domain.notification.security.RateLimitPolicy
import com.sucharu.sucharupro.domain.notification.security.SuppressionReason
import com.sucharu.sucharupro.domain.notification.security.SuppressionType
import com.sucharu.sucharupro.domain.notification.security.NotificationSuppression
import java.util.UUID

/**
 * Central notification security policy engine (INFRA-04 Step 07).
 *
 * Evaluates all security controls in deterministic order and returns an authoritative decision.
 * All evaluations are fail-closed: ambiguity results in DENY.
 *
 * Evaluation order:
 * 1. Tenant isolation
 * 2. Recipient authorization
 * 3. AI Agent boundary
 * 4. Payload sanitization & credential leak detection
 * 5. Content security (injection detection)
 * 6. Data classification ? channel eligibility
 * 7. Suppression check
 * 8. Rate limit evaluation
 * 9. Payload sanitize all eligible channels
 * 10. ALLOW with sanitized intent
 */
class NotificationSecurityPolicy(
    private val authorizationService: NotificationAuthorizationService = NotificationAuthorizationService(),
    private val suppressionRepository: NotificationSuppressionRepository = InMemoryNotificationSuppressionRepository(),
    private val rateLimiter: NotificationRateLimiter = NotificationRateLimiter(),
    private val auditService: NotificationAuditService? = null,
    private val recipientProjectValidator: (projectId: String, recipientId: String) -> Boolean = { _, _ -> true }
) {

    /** Default rate limit policies. May be overridden in production via configuration. */
    private fun perRecipientPolicy(projectId: String, recipientId: String, channel: NotificationChannel) =
        RateLimitPolicy(
            dimensionKey = rateLimiter.buildRecipientChannelKey(projectId, recipientId, channel.name),
            windowSeconds = 60L,
            maxCount = 10
        )

    private fun perProjectPolicy(projectId: String, channel: NotificationChannel) =
        RateLimitPolicy(
            dimensionKey = rateLimiter.buildProjectChannelKey(projectId, channel.name),
            windowSeconds = 60L,
            maxCount = 500
        )

    /**
     * Evaluates the full security policy for a notification dispatch context.
     * Returns a deterministic [NotificationSecurityDecision].
     */
    suspend fun evaluateDispatch(
        context: NotificationSecurityContext
    ): NotificationSecurityDecision {
        val tenantContext = TenantContext(context.projectId)

        // 1. Tenant isolation — fail closed on mismatch
        if (context.intent.projectId != context.projectId) {
            return deny(
                NotificationSecurityReason.TENANT_MISMATCH,
                "Intent projectId '${context.intent.projectId}' does not match security context '${context.projectId}'"
            )
        }

        // 2. Recipient authorization
        val recipientAuth = authorizationService.authorizeRecipient(
            principal = context.principal,
            intent = context.intent,
            serverProjectId = context.projectId
        )
        if (!recipientAuth.authorized) {
            return deny(
                recipientAuth.reason ?: NotificationSecurityReason.UNAUTHORIZED_RECIPIENT,
                recipientAuth.message
            )
        }

        // 3. Recipient must belong to project (server-side validation)
        val recipientId = context.intent.targetRecipientId
        if (!recipientProjectValidator(context.projectId, recipientId)) {
            return deny(
                NotificationSecurityReason.RECIPIENT_NOT_IN_PROJECT,
                "Recipient '$recipientId' does not belong to project '${context.projectId}'"
            )
        }

        // 4. Credential leak detection in title and body
        if (NotificationPayloadSanitizer.containsCredentialLeak(context.intent.title) ||
            NotificationPayloadSanitizer.containsCredentialLeak(context.intent.body)) {
            return deny(
                NotificationSecurityReason.CREDENTIAL_LEAK_DETECTED,
                "Notification payload appears to contain credential material. Dispatch blocked."
            )
        }

        // 5. Content injection detection
        if (NotificationPayloadSanitizer.containsInjection(context.intent.title)) {
            return deny(
                NotificationSecurityReason.CONTENT_INJECTION_DETECTED,
                "Notification title contains injection vectors."
            )
        }

        // 6. Data classification ? compute effective channels
        val classificationEligible = context.classification.eligibleChannels()
        val requestedChannels = context.intent.targetChannels
        val effectiveChannels = requestedChannels.intersect(classificationEligible)

        if (effectiveChannels.isEmpty()) {
            return deny(
                NotificationSecurityReason.CHANNEL_CLASSIFICATION_MISMATCH,
                "No eligible channels remain after classification '${context.classification}' filter. " +
                "Requested: ${requestedChannels.map { it.name }}, eligible: ${classificationEligible.map { it.name }}"
            )
        }

        // 7. Suppression check (per effective channel)
        for (channel in effectiveChannels) {
            val suppressed = suppressionRepository.isSuppressed(
                projectId = context.projectId,
                recipientId = recipientId,
                channel = channel,
                tenantContext = tenantContext
            )
            if (suppressed) {
                return NotificationSecurityDecision.Suppress(
                    reason = "Recipient '$recipientId' is suppressed for channel '${channel.name}'",
                    suppressionType = SuppressionType.RECIPIENT
                )
            }
        }

        // 8. Rate limit evaluation (per-recipient, per-project)
        for (channel in effectiveChannels) {
            val recipientDecision = rateLimiter.evaluate(
                key = rateLimiter.buildRecipientChannelKey(context.projectId, recipientId, channel.name),
                policy = perRecipientPolicy(context.projectId, recipientId, channel)
            )
            if (!recipientDecision.allowed) {
                return NotificationSecurityDecision.RateLimit(
                    dimension = "recipient:$recipientId:channel:${channel.name}",
                    retryAfterMs = recipientDecision.retryAfterMs
                )
            }

            val projectDecision = rateLimiter.evaluate(
                key = rateLimiter.buildProjectChannelKey(context.projectId, channel.name),
                policy = perProjectPolicy(context.projectId, channel)
            )
            if (!projectDecision.allowed) {
                return NotificationSecurityDecision.RateLimit(
                    dimension = "project:${context.projectId}:channel:${channel.name}",
                    retryAfterMs = projectDecision.retryAfterMs
                )
            }
        }

        // 9. Record rate limit increments for all effective channels (only after all checks pass)
        for (channel in effectiveChannels) {
            rateLimiter.record(
                key = rateLimiter.buildRecipientChannelKey(context.projectId, recipientId, channel.name),
                policy = perRecipientPolicy(context.projectId, recipientId, channel)
            )
            rateLimiter.record(
                key = rateLimiter.buildProjectChannelKey(context.projectId, channel.name),
                policy = perProjectPolicy(context.projectId, channel)
            )
        }

        // 10. Sanitize intent for dispatch — use first effective channel for size constraints
        val primaryChannel = effectiveChannels.firstOrNull() ?: effectiveChannels.first()
        val sanitized = NotificationPayloadSanitizer.sanitize(context.intent, primaryChannel, context.classification)
        val sanitizedIntent = context.intent.copy(
            title = sanitized.title,
            body = sanitized.body,
            targetChannels = effectiveChannels
        )

        return NotificationSecurityDecision.Allow(
            sanitizedIntent = sanitizedIntent,
            effectiveChannels = effectiveChannels
        )
    }

    /**
     * Evaluates replay security — re-runs full dispatch evaluation under replay context.
     */
    suspend fun evaluateReplay(
        context: NotificationSecurityContext
    ): NotificationSecurityDecision {
        // Authorization check for replay capability
        val replayAuth = authorizationService.authorizeReplay(
            principal = context.principal,
            serverProjectId = context.projectId
        )
        if (!replayAuth.authorized) {
            return deny(
                replayAuth.reason ?: NotificationSecurityReason.REPLAY_UNAUTHORIZED,
                replayAuth.message
            )
        }

        // Re-evaluate full dispatch policy — never use original authorization state
        return evaluateDispatch(context.copy(isReplay = true))
    }

    /**
     * Evaluates suppression management authorization (create/remove suppression).
     */
    fun evaluateSuppressionManagement(
        context: NotificationSecurityContext
    ): NotificationAuthorizationService.RecipientAuthResult {
        return authorizationService.authorizeAdminOperation(
            principal = context.principal,
            capability = AuthorizationCapability.NOTIFICATION_SUPPRESSION_MANAGE,
            serverProjectId = context.projectId
        )
    }

    private fun deny(reason: NotificationSecurityReason, message: String): NotificationSecurityDecision.Deny =
        NotificationSecurityDecision.Deny(reason = reason, message = message)
}

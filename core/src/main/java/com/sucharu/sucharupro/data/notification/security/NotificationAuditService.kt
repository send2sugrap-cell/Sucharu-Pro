package com.sucharu.sucharupro.data.notification.security

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.notification.security.NotificationSecurityAuditEvent
import com.sucharu.sucharupro.domain.notification.security.NotificationSecurityContext
import com.sucharu.sucharupro.domain.notification.security.NotificationSecurityDecision
import com.sucharu.sucharupro.domain.notification.security.NotificationSecurityOperation
import java.util.UUID

/**
 * Production-grade notification security audit service (INFRA-04 Step 07).
 *
 * Centralizes all audit record creation. Enforces:
 * - No secrets logged in any field
 * - Append-only writes via [NotificationAuditRepository]
 * - Tenant isolation on every write
 */
class NotificationAuditService(
    private val auditRepository: NotificationAuditRepository
) {

    /**
     * Records the outcome of a security policy evaluation for a notification dispatch.
     */
    suspend fun recordDecision(
        context: NotificationSecurityContext,
        decision: NotificationSecurityDecision
    ) {
        val tenantContext = TenantContext(context.projectId)
        val (operation, decisionStr, reason) = when (decision) {
            is NotificationSecurityDecision.Allow -> Triple(
                NotificationSecurityOperation.NOTIFICATION_ALLOWED, "ALLOW", null
            )
            is NotificationSecurityDecision.Deny -> Triple(
                NotificationSecurityOperation.NOTIFICATION_DENIED, "DENY", decision.reason.name
            )
            is NotificationSecurityDecision.Suppress -> Triple(
                NotificationSecurityOperation.NOTIFICATION_SUPPRESSED, "SUPPRESS", decision.reason
            )
            is NotificationSecurityDecision.RateLimit -> Triple(
                NotificationSecurityOperation.RATE_LIMIT_TRIGGERED, "RATE_LIMIT", decision.dimension
            )
            is NotificationSecurityDecision.RequireConfirmation -> Triple(
                NotificationSecurityOperation.NOTIFICATION_DENIED, "REQUIRE_CONFIRMATION", decision.reason
            )
        }

        val safeDetails = buildSafeDetails(context)

        val event = NotificationSecurityAuditEvent(
            auditId = "nsa-${UUID.randomUUID().toString().take(12)}",
            projectId = context.projectId,
            operation = operation,
            decision = decisionStr,
            reason = reason,
            eventId = context.intent.eventId,
            actorId = context.principal?.userId,
            actorRole = context.principal?.role?.name,
            correlationId = context.correlationId,
            requestId = context.requestId,
            recipientId = context.intent.targetRecipientId,
            safeDetails = safeDetails
        )
        tryAppend(event, tenantContext)
    }

    /**
     * Records a privileged administrative operation (suppression create/remove, policy change, etc.).
     * Always audited regardless of outcome.
     */
    suspend fun recordPrivilegedOperation(
        principal: AuthenticatedPrincipal,
        operation: NotificationSecurityOperation,
        projectId: String,
        safeDetails: Map<String, String> = emptyMap()
    ) {
        val tenantContext = TenantContext(projectId)
        val event = NotificationSecurityAuditEvent(
            auditId = "nsa-${UUID.randomUUID().toString().take(12)}",
            projectId = projectId,
            operation = operation,
            decision = "EXECUTED",
            actorId = principal.userId,
            actorRole = principal.role.name,
            safeDetails = safeDetails
        )
        tryAppend(event, tenantContext)
    }

    /**
     * Records a replay request and its authorization outcome.
     */
    suspend fun recordReplay(
        context: NotificationSecurityContext,
        authorized: Boolean,
        reason: String? = null
    ) {
        val tenantContext = TenantContext(context.projectId)
        val operation = if (authorized)
            NotificationSecurityOperation.REPLAY_EXECUTED
        else
            NotificationSecurityOperation.REPLAY_DENIED

        val event = NotificationSecurityAuditEvent(
            auditId = "nsa-${UUID.randomUUID().toString().take(12)}",
            projectId = context.projectId,
            operation = operation,
            decision = if (authorized) "AUTHORIZED" else "DENIED",
            reason = reason,
            eventId = context.originalEventId,
            actorId = context.principal?.userId,
            actorRole = context.principal?.role?.name,
            correlationId = context.correlationId,
            requestId = context.requestId,
            recipientId = context.intent.targetRecipientId
        )
        tryAppend(event, tenantContext)
    }

    /**
     * Records a provider callback security validation result.
     */
    suspend fun recordCallbackValidation(
        projectId: String,
        idempotencyKey: String,
        result: CallbackValidationResult
    ) {
        val tenantContext = TenantContext(projectId)
        val operation = when {
            result.isReplay -> NotificationSecurityOperation.CALLBACK_REPLAYED
            result.isValid -> NotificationSecurityOperation.CALLBACK_SIGNATURE_VALID
            else -> NotificationSecurityOperation.CALLBACK_SIGNATURE_INVALID
        }
        val event = NotificationSecurityAuditEvent(
            auditId = "nsa-${UUID.randomUUID().toString().take(12)}",
            projectId = projectId,
            operation = operation,
            decision = if (result.isValid) "VALID" else "INVALID",
            safeDetails = mapOf(
                "idempotencyKey" to idempotencyKey,
                "signatureValid" to result.signatureValid.toString(),
                "timestampValid" to result.timestampValid.toString(),
                "isReplay" to result.isReplay.toString()
            )
        )
        tryAppend(event, tenantContext)
    }

    /**
     * Builds a safe details map from the security context — MUST NOT include any secret values.
     */
    private fun buildSafeDetails(context: NotificationSecurityContext): Map<String, String> {
        return mapOf(
            "classification" to context.classification.name,
            "eventType" to context.intent.eventType.name,
            "isReplay" to context.isReplay.toString(),
            "channelCount" to context.intent.targetChannels.size.toString()
        )
    }

    private suspend fun tryAppend(event: NotificationSecurityAuditEvent, tenantContext: TenantContext) {
        try {
            auditRepository.appendAuditEvent(event, tenantContext)
        } catch (_: Throwable) {
            // Audit failures must never block notification operations.
            // In production: emit a metric/alert for audit write failure.
        }
    }
}

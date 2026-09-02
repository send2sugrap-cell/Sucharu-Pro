package com.sucharu.sucharupro.data.notification.ai

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.auth.authorization.AuthorizationCapability
import com.sucharu.sucharupro.data.auth.authorization.BackendAuthorizationService
import com.sucharu.sucharupro.data.event.integration.notification.NotificationPreferences
import com.sucharu.sucharupro.data.notification.security.NotificationPayloadSanitizer
import com.sucharu.sucharupro.domain.event.boundary.NotificationChannel
import com.sucharu.sucharupro.domain.notification.ai.AiNotificationAuditOperation
import com.sucharu.sucharupro.domain.notification.ai.AiNotificationConversationContext

sealed class AiReadResult<out T> {
    data class Success<T>(val data: T) : AiReadResult<T>()
    data class Denied(val reason: String, val code: String) : AiReadResult<Nothing>()
}

data class SafeNotificationSummary(
    val notificationId: String,
    val projectId: String,
    val recipientId: String,
    val sanitizedTitle: String,
    val sanitizedBodySnippet: String,
    val status: String,
    val timestamp: Long
)

data class SafeDeliveryState(
    val notificationId: String,
    val projectId: String,
    val channelsAttempted: List<NotificationChannel>,
    val status: String,
    val deliveredAt: Long?
)

/**
 * Production-grade controlled conversational read service for AI Agents (INFRA-04 Step 08).
 *
 * Rules:
 * 1. Requires explicit capability (`AI_READ_NOTIFICATION_STATUS`, `AI_READ_NOTIFICATION_HISTORY`, `AI_READ_NOTIFICATION_CONTEXT`).
 * 2. Strict tenant isolation (server-authoritative).
 * 3. Sanitizes all output; data-minimized fields only.
 * 4. Never exposes SQL, raw repository queries, or global un-scoped dumps.
 */
class AiNotificationReadService(
    private val authorizationService: BackendAuthorizationService = BackendAuthorizationService(),
    private val auditService: AiNotificationAuditService? = null,
    private val notificationLookup: (projectId: String, notificationId: String) -> SafeNotificationSummary? = { _, _ -> null },
    private val deliveryStateLookup: (projectId: String, notificationId: String) -> SafeDeliveryState? = { _, _ -> null },
    private val historyLookup: (projectId: String, entityRef: String, limit: Int) -> List<SafeNotificationSummary> = { _, _, _ -> emptyList() },
    private val preferenceLookup: (projectId: String, recipientId: String) -> NotificationPreferences? = { p, r -> NotificationPreferences(recipientId = r, projectId = p) }
) {

    suspend fun getNotificationStatus(
        notificationId: String,
        principal: AuthenticatedPrincipal?,
        serverProjectId: String
    ): AiReadResult<SafeNotificationSummary> {
        val authCheck = checkAuth(principal, serverProjectId, AuthorizationCapability.AI_READ_NOTIFICATION_STATUS)
        if (authCheck != null) return authCheck

        val summary = notificationLookup(serverProjectId, notificationId)
            ?: return AiReadResult.Denied("Notification '$notificationId' not found.", "NOT_FOUND")

        auditService?.record(
            projectId = serverProjectId,
            operation = AiNotificationAuditOperation.AI_NOTIFICATION_CONTEXT_READ,
            decision = "SUCCESS",
            agentId = principal!!.userId,
            recipientId = summary.recipientId,
            correlationId = "read-status-$notificationId",
            requestId = "read-$notificationId",
            safeSummary = "Read status for notification '$notificationId'"
        )

        return AiReadResult.Success(
            summary.copy(
                sanitizedTitle = NotificationPayloadSanitizer.sanitizeText(summary.sanitizedTitle, isTitleField = true),
                sanitizedBodySnippet = NotificationPayloadSanitizer.sanitizeText(summary.sanitizedBodySnippet, isTitleField = false)
            )
        )
    }

    suspend fun getNotificationDeliveryState(
        notificationId: String,
        principal: AuthenticatedPrincipal?,
        serverProjectId: String
    ): AiReadResult<SafeDeliveryState> {
        val authCheck = checkAuth(principal, serverProjectId, AuthorizationCapability.AI_READ_NOTIFICATION_STATUS)
        if (authCheck != null) return authCheck

        val state = deliveryStateLookup(serverProjectId, notificationId)
            ?: return AiReadResult.Denied("Delivery state for '$notificationId' not found.", "NOT_FOUND")

        return AiReadResult.Success(state)
    }

    suspend fun getNotificationHistory(
        entityRef: String,
        principal: AuthenticatedPrincipal?,
        serverProjectId: String,
        limit: Int = 10
    ): AiReadResult<List<SafeNotificationSummary>> {
        val authCheck = checkAuth(principal, serverProjectId, AuthorizationCapability.AI_READ_NOTIFICATION_HISTORY)
        if (authCheck != null) return authCheck

        // Enforce max page limit (cannot dump entire tenant history)
        val boundedLimit = limit.coerceIn(1, 20)
        val history = historyLookup(serverProjectId, entityRef, boundedLimit)

        val sanitizedList = history.map { item ->
            item.copy(
                sanitizedTitle = NotificationPayloadSanitizer.sanitizeText(item.sanitizedTitle, isTitleField = true),
                sanitizedBodySnippet = NotificationPayloadSanitizer.sanitizeText(item.sanitizedBodySnippet, isTitleField = false)
            )
        }

        auditService?.record(
            projectId = serverProjectId,
            operation = AiNotificationAuditOperation.AI_NOTIFICATION_CONTEXT_READ,
            decision = "SUCCESS",
            agentId = principal!!.userId,
            correlationId = "read-history-$entityRef",
            requestId = "read-history",
            safeSummary = "Read ${sanitizedList.size} history items for '$entityRef'"
        )

        return AiReadResult.Success(sanitizedList)
    }

    suspend fun getNotificationPreferences(
        recipientId: String,
        principal: AuthenticatedPrincipal?,
        serverProjectId: String
    ): AiReadResult<NotificationPreferences> {
        val authCheck = checkAuth(principal, serverProjectId, AuthorizationCapability.AI_READ_NOTIFICATION_CONTEXT)
        if (authCheck != null) return authCheck

        val prefs = preferenceLookup(serverProjectId, recipientId)
            ?: NotificationPreferences(recipientId = recipientId, projectId = serverProjectId)

        return AiReadResult.Success(prefs)
    }

    private fun checkAuth(
        principal: AuthenticatedPrincipal?,
        serverProjectId: String,
        capability: AuthorizationCapability
    ): AiReadResult.Denied? {
        if (principal == null) {
            return AiReadResult.Denied("Unauthenticated request.", "UNAUTHENTICATED")
        }

        if (principal.projectId != serverProjectId) {
            return AiReadResult.Denied(
                "Cross-tenant access blocked. Principal '${principal.projectId}' != server '$serverProjectId'.",
                "TENANT_MISMATCH"
            )
        }

        // Machine principal check
        if (!principal.isAiAgent && principal.principalType != PrincipalType.AI_AGENT && principal.role != UserRole.AI_AGENT) {
            return AiReadResult.Denied("Caller is not an AI_AGENT machine principal.", "NOT_AN_AI_AGENT")
        }

        // Capability check (explicit permission or matrix)
        val hasCap = principal.hasPermission(com.sucharu.sucharupro.data.api.model.UserPermission.ADMIN_ALL) ||
                principal.permissions.any { it.name == capability.name } ||
                com.sucharu.sucharupro.data.auth.authorization.RoleCapabilityMatrix.hasCapability(principal.role, capability)

        if (!hasCap) {
            return AiReadResult.Denied(
                "AI Agent '${principal.userId}' lacks mandatory capability '${capability.name}'.",
                "CAPABILITY_MISSING"
            )
        }

        return null
    }
}

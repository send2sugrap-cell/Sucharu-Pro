package com.sucharu.sucharupro.domain.notification.ai

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.auth.authorization.AuthorizationCapability
import com.sucharu.sucharupro.domain.event.boundary.NotificationChannel
import com.sucharu.sucharupro.domain.event.model.DomainEventType
import java.util.UUID

// ============================================================
// AI NOTIFICATION EVENT VIEW (DATA-MINIMIZED)
// ============================================================

/**
 * Data-minimized, sanitized, safe event view provided to AI agents (INFRA-04 Step 08).
 *
 * Guaranteed to contain:
 * - No raw EventEnvelope
 * - No credentials, passwords, tokens, API keys, or secrets
 * - No unrestricted financial transactions or auth credentials
 * - No stack traces or internal DB metadata
 */
data class AiNotificationEventView(
    val eventId: String,
    val eventType: DomainEventType,
    val eventVersion: String,
    val projectId: String,
    val occurredAt: Long,
    val aggregateType: String,
    val aggregateId: String,
    val businessSummary: String,
    val permittedMetadata: Map<String, String>,
    val correlationId: String,
    val allowedRecipientHint: String? = null,
    val allowedActionHints: List<String> = emptyList()
)

// ============================================================
// CONVERSATIONAL NOTIFICATION CONTEXT
// ============================================================

/**
 * Scoped conversational context for an AI interaction session (INFRA-04 Step 08).
 *
 * Enforces strict containment:
 * - Scoped to a specific conversation, project, and entity
 * - Strictly prevents tenant-wide or global history dumps
 */
data class AiNotificationConversationContext(
    val conversationId: String,
    val projectId: String,
    val entityReference: String,
    val relatedNotificationId: String? = null,
    val eventSummary: String? = null,
    val notificationStatus: String? = null,
    val permittedActions: Set<AiNotificationActionType> = emptySet(),
    val confirmationRequiredFor: Set<AiNotificationActionType> = emptySet(),
    val correlationId: String,
    val createdAt: Long = System.currentTimeMillis()
)

// ============================================================
// ACTION TYPES & REQUEST PIPELINE
// ============================================================

/**
 * Permitted action categories for AI Agent notification requests (INFRA-04 Step 08).
 *
 * Strict separation:
 * CREATE_DRAFT (read/generation only) vs REQUEST_SEND (requires execution authorization).
 */
enum class AiNotificationActionType {
    CREATE_DRAFT,
    REQUEST_SEND,
    REQUEST_REPLAY,
    REQUEST_SUPPRESSION,
    REQUEST_PREFERENCE_UPDATE
}

/**
 * Request submitted by an AI Agent machine principal (INFRA-04 Step 08).
 */
data class AiNotificationActionRequest(
    val requestId: String = UUID.randomUUID().toString(),
    val projectId: String,
    val actionType: AiNotificationActionType,
    val targetRecipientId: String,
    val targetChannels: Set<NotificationChannel> = setOf(NotificationChannel.IN_APP),
    val title: String,
    val body: String,
    val draftId: String? = null,
    val confirmationId: String? = null,
    val idempotencyKey: String,
    val correlationId: String,
    val metadata: Map<String, String> = emptyMap(),
    val requestedAt: Long = System.currentTimeMillis()
)

/**
 * Result of an AI Notification action request evaluation or execution (INFRA-04 Step 08).
 */
sealed class AiNotificationActionResult {
    data class DraftCreated(
        val draftId: String,
        val projectId: String,
        val sanitizedTitle: String,
        val sanitizedBody: String,
        val targetChannels: Set<NotificationChannel>,
        val correlationId: String
    ) : AiNotificationActionResult()

    data class ExecutionSubmitted(
        val actionId: String,
        val projectId: String,
        val status: String,
        val message: String,
        val correlationId: String
    ) : AiNotificationActionResult()

    data class RequiresConfirmation(
        val confirmationId: String,
        val actionType: AiNotificationActionType,
        val reason: String,
        val requiredRole: String,
        val expiresAt: Long
    ) : AiNotificationActionResult()

    data class Denied(
        val reasonCode: String,
        val message: String
    ) : AiNotificationActionResult()

    data class RateLimited(
        val dimension: String,
        val retryAfterMs: Long
    ) : AiNotificationActionResult()
}

// ============================================================
// SECURITY DECISIONS
// ============================================================

/**
 * Explicit, deterministic security decisions produced by the AI Agent boundary (INFRA-04 Step 08).
 */
sealed class AiNotificationSecurityDecision {
    data class Allowed(
        val grantedCapability: AuthorizationCapability,
        val sanitizedRequest: AiNotificationActionRequest
    ) : AiNotificationSecurityDecision()

    data class RequiresHumanConfirmation(
        val reason: String,
        val requiredRole: String,
        val confirmationId: String
    ) : AiNotificationSecurityDecision()

    data class Denied(
        val reason: AiNotificationDenialReason,
        val message: String
    ) : AiNotificationSecurityDecision()

    val isAllowed: Boolean get() = this is Allowed
    val isDenied: Boolean get() = this is Denied
}

enum class AiNotificationDenialReason {
    NOT_AN_AI_AGENT,
    UNAUTHENTICATED,
    TENANT_MISMATCH,
    CAPABILITY_MISSING,
    SENSITIVE_EVENT_BLOCKED,
    CREDENTIAL_LEAK_DETECTED,
    CONTENT_INJECTION_DETECTED,
    RATE_LIMITED,
    RECIPIENT_SUPPRESSED,
    INVALID_CONFIRMATION,
    CONFIRMATION_EXPIRED,
    CONFIRMATION_WRONG_ACTION,
    CONFIRMATION_SELF_APPROVED,
    N8N_DIRECT_ACCESS_DENIED,
    PROVIDER_DIRECT_ACCESS_DENIED,
    MANDATORY_SECURITY_NOTIFICATION_IMMUTABLE
}

// ============================================================
// HUMAN CONFIRMATION LIFECYCLE
// ============================================================

enum class AiConfirmationStatus(val isTerminal: Boolean) {
    PENDING(isTerminal = false),
    APPROVED(isTerminal = true),
    REJECTED(isTerminal = true),
    EXPIRED(isTerminal = true),
    CANCELLED(isTerminal = true)
}

/**
 * Cryptographically-secure human confirmation record for high-impact AI notification operations.
 */
data class AiNotificationConfirmationRequest(
    val confirmationId: String = UUID.randomUUID().toString(),
    val projectId: String,
    val actionType: AiNotificationActionType,
    val requestedByAgentId: String,
    val payloadSummary: String,
    val targetRecipientId: String,
    val status: AiConfirmationStatus = AiConfirmationStatus.PENDING,
    val approvedByHumanId: String? = null,
    val approverRole: String? = null,
    val approvedAt: Long? = null,
    val rejectionReason: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (30 * 60 * 1000L) // 30 mins default
)

// ============================================================
// AUDIT EVENT
// ============================================================

enum class AiNotificationAuditOperation {
    AI_NOTIFICATION_CONTEXT_READ,
    AI_NOTIFICATION_DRAFT_CREATED,
    AI_NOTIFICATION_ACTION_REQUESTED,
    AI_NOTIFICATION_ACTION_DENIED,
    AI_NOTIFICATION_CONFIRMATION_REQUIRED,
    AI_NOTIFICATION_CONFIRMED,
    AI_NOTIFICATION_REJECTED,
    AI_NOTIFICATION_EXECUTED,
    AI_NOTIFICATION_RATE_LIMITED,
    AI_NOTIFICATION_SENSITIVE_DATA_BLOCKED,
    AI_NOTIFICATION_PREFERENCE_UPDATE_PROPOSED,
    AI_NOTIFICATION_IDEMPOTENCY_REPLAY
}

/**
 * Immutable append-only audit event for AI Agent notification interactions.
 */
data class AiNotificationAuditEvent(
    val auditId: String = "ai-audit-${UUID.randomUUID().toString().take(12)}",
    val projectId: String,
    val operation: AiNotificationAuditOperation,
    val decision: String,
    val agentId: String,
    val actionType: String? = null,
    val recipientId: String? = null,
    val correlationId: String,
    val requestId: String,
    val confirmationId: String? = null,
    val reasonCode: String? = null,
    val safeSummary: String? = null,
    val occurredAt: Long = System.currentTimeMillis()
)

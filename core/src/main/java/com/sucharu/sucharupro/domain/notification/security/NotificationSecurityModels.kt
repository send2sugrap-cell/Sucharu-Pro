package com.sucharu.sucharupro.domain.notification.security

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.domain.event.boundary.NotificationChannel
import com.sucharu.sucharupro.domain.event.boundary.NotificationIntent

// ============================================================
// DATA CLASSIFICATION
// ============================================================

/**
 * Sensitivity classification for notification content and metadata (INFRA-04 Step 07).
 */
enum class NotificationDataClassification {
    PUBLIC,
    INTERNAL,
    CONFIDENTIAL,
    SENSITIVE,
    RESTRICTED;

    fun eligibleChannels(): Set<NotificationChannel> = when (this) {
        PUBLIC -> setOf(NotificationChannel.IN_APP, NotificationChannel.EMAIL, NotificationChannel.SMS, NotificationChannel.PUSH)
        INTERNAL -> setOf(NotificationChannel.IN_APP)
        CONFIDENTIAL -> setOf(NotificationChannel.IN_APP, NotificationChannel.EMAIL)
        SENSITIVE -> setOf(NotificationChannel.IN_APP, NotificationChannel.EMAIL)
        RESTRICTED -> setOf(NotificationChannel.IN_APP, NotificationChannel.EMAIL)
    }
}

// ============================================================
// SECURITY CONTEXT
// ============================================================

data class NotificationSecurityContext(
    val principal: AuthenticatedPrincipal?,
    val projectId: String,
    val intent: NotificationIntent,
    val classification: NotificationDataClassification,
    val correlationId: String,
    val requestId: String,
    val isReplay: Boolean = false,
    val originalEventId: String? = null
)

// ============================================================
// SECURITY DECISION
// ============================================================

sealed class NotificationSecurityDecision {
    data class Allow(
        val sanitizedIntent: NotificationIntent,
        val effectiveChannels: Set<NotificationChannel>
    ) : NotificationSecurityDecision()

    data class Deny(
        val reason: NotificationSecurityReason,
        val message: String
    ) : NotificationSecurityDecision()

    data class Suppress(
        val reason: String,
        val suppressionType: SuppressionType
    ) : NotificationSecurityDecision()

    data class RateLimit(
        val dimension: String,
        val retryAfterMs: Long
    ) : NotificationSecurityDecision()

    data class RequireConfirmation(
        val reason: String,
        val confirmationToken: String
    ) : NotificationSecurityDecision()

    val isAllow: Boolean get() = this is Allow
    val isDeny: Boolean get() = this is Deny
}

// ============================================================
// SECURITY REASON CODES
// ============================================================

enum class NotificationSecurityReason {
    TENANT_MISMATCH,
    UNAUTHENTICATED_PRINCIPAL,
    UNAUTHORIZED_RECIPIENT,
    RECIPIENT_NOT_IN_PROJECT,
    RECIPIENT_CHANNEL_NOT_ELIGIBLE,
    CHANNEL_CLASSIFICATION_MISMATCH,
    SUPPRESSED_RECIPIENT,
    RATE_LIMITED,
    INVALID_CHANNEL,
    CREDENTIAL_LEAK_DETECTED,
    CONTENT_INJECTION_DETECTED,
    ABUSE_DETECTED,
    INVALID_PROVIDER_SIGNATURE,
    REPLAY_UNAUTHORIZED,
    REPLAY_SUPPRESSED,
    REPLAY_RATE_LIMITED,
    AI_AGENT_DENIED,
    MISSING_CAPABILITY,
    INVALID_SECURITY_CONTEXT,
    PAYLOAD_TOO_LARGE
}

// ============================================================
// SUPPRESSION MODELS
// ============================================================

enum class SuppressionReason {
    USER_REQUESTED,
    SECURITY_BLOCK,
    BOUNCE,
    PROVIDER_FAILURE,
    ABUSE_DETECTED,
    ADMIN_BLOCK,
    INVALID_DESTINATION
}

enum class SuppressionType {
    RECIPIENT,
    DESTINATION,
    CHANNEL,
    GLOBAL
}

data class NotificationSuppression(
    val suppressionId: String,
    val projectId: String,
    val recipientId: String,
    val channel: NotificationChannel?,
    val reason: SuppressionReason,
    val suppressionType: SuppressionType,
    val createdBy: String,
    val expiresAt: Long? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val removedAt: Long? = null,
    val removedBy: String? = null
)

// ============================================================
// RATE LIMIT MODELS
// ============================================================

data class RateLimitPolicy(
    val dimensionKey: String,
    val windowSeconds: Long,
    val maxCount: Int
)

data class RateLimitDecision(
    val allowed: Boolean,
    val remaining: Int,
    val retryAfterMs: Long = 0L,
    val windowResetMs: Long = 0L
)

// ============================================================
// ABUSE SIGNAL
// ============================================================

enum class AbuseSignalSeverity { LOW, MEDIUM, HIGH, CRITICAL }

data class NotificationAbuseSignal(
    val signalType: String,
    val description: String,
    val severity: AbuseSignalSeverity,
    val recipientId: String?,
    val projectId: String,
    val detectedAt: Long = System.currentTimeMillis()
)

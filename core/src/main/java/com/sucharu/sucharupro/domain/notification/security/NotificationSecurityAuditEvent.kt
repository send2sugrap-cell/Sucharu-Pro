package com.sucharu.sucharupro.domain.notification.security

/**
 * Canonical notification security audit operations (INFRA-04 Step 07).
 * Every security-relevant action produces one of these records.
 */
enum class NotificationSecurityOperation {
    NOTIFICATION_ALLOWED,
    NOTIFICATION_DENIED,
    NOTIFICATION_SUPPRESSED,
    RATE_LIMIT_TRIGGERED,
    ABUSE_DETECTED,
    RECIPIENT_AUTH_FAILURE,
    PROVIDER_SECURITY_FAILURE,
    REPLAY_REQUESTED,
    REPLAY_DENIED,
    REPLAY_EXECUTED,
    SUPPRESSION_CREATED,
    SUPPRESSION_REMOVED,
    PRIVILEGED_OVERRIDE,
    SECURITY_POLICY_CHANGE,
    CALLBACK_SIGNATURE_VALID,
    CALLBACK_SIGNATURE_INVALID,
    CALLBACK_REPLAYED
}

/**
 * Immutable, append-only notification security audit event (INFRA-04 Step 07).
 *
 * SECURITY RULES:
 * - Never log secrets, tokens, passwords, or raw credential values.
 * - Never log full notification body if classification is SENSITIVE or RESTRICTED.
 * - Records are immutable after creation; corrections require a new compensating record.
 */
data class NotificationSecurityAuditEvent(
    val auditId: String,
    val projectId: String,
    val operation: NotificationSecurityOperation,
    val decision: String,
    val reason: String? = null,
    val eventId: String? = null,
    val notificationId: String? = null,
    val actorId: String? = null,
    val actorRole: String? = null,
    val channel: String? = null,
    val recipientId: String? = null,
    val correlationId: String? = null,
    val requestId: String? = null,
    /** Safe, sanitized details only — must NOT contain secrets or payloads. */
    val safeDetails: Map<String, String> = emptyMap(),
    val occurredAt: Long = System.currentTimeMillis()
)

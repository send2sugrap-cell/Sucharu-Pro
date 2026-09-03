package com.sucharu.sucharupro.domain.model.affiliate

import com.sucharu.sucharupro.domain.model.notification.NotificationChannel
import com.sucharu.sucharupro.domain.model.notification.NotificationType

/**
 * Operational communication type classification for affiliate-facing communications.
 * Maps directly to canonical NotificationType entries (Module 20 Step 04).
 */
enum class AffiliateCommunicationType(
    val defaultLabel: String,
    val canonicalNotificationType: NotificationType,
    val isMandatory: Boolean = false
) {
    APPLICATION(
        "Application Status",
        NotificationType.AFFILIATE_APPLICATION_STATUS
    ),
    ENROLLMENT(
        "Enrollment Status",
        NotificationType.AFFILIATE_ENROLLMENT_STATUS
    ),
    PROFILE(
        "Profile Update",
        NotificationType.AFFILIATE_PROFILE_UPDATE
    ),
    VERIFICATION(
        "Verification Result",
        NotificationType.AFFILIATE_VERIFICATION_RESULT
    ),
    PROGRAM(
        "Program Notice",
        NotificationType.AFFILIATE_PROGRAM_NOTICE
    ),
    GOVERNANCE(
        "Governance Notice",
        NotificationType.AFFILIATE_GOVERNANCE_NOTICE,
        isMandatory = true
    ),
    SECURITY(
        "Security Notice",
        NotificationType.AFFILIATE_SECURITY_NOTICE,
        isMandatory = true
    ),
    SYSTEM(
        "System Notice",
        NotificationType.SYSTEM_ALERT,
        isMandatory = true
    )
}

/**
 * Deterministic state machine for affiliate communication delivery lifecycle.
 */
enum class AffiliateCommunicationStatus {
    CREATED,
    QUEUED,
    PROCESSING,
    DELIVERED,
    READ,
    FAILED,
    CANCELLED
}

/**
 * Represents the fully resolved intent to deliver an affiliate notification.
 * Produced by AffiliateCommunicationPolicyEngine after evaluating preferences.
 */
data class AffiliateNotificationIntent(
    val tenantId: String,
    val affiliateId: String,
    val recipientUserId: String,
    val communicationType: AffiliateCommunicationType,
    val canonicalNotificationType: NotificationType,
    val channels: List<NotificationChannel>,
    val title: String,
    val message: String,
    val referenceType: String? = null,
    val referenceId: String? = null,
    val idempotencyKey: String,
    val correlationId: String
)

/**
 * Affiliate Communication Record — Affiliate-scoped tracking/audit record for each communication.
 * Stored separately from the canonical Notification record for domain isolation.
 * References canonicalNotificationId as the delivery record in the canonical notification system.
 */
data class AffiliateCommunicationRecord(
    val communicationId: String,
    val tenantId: String,
    val affiliateId: String,
    val recipientUserId: String,
    val communicationType: AffiliateCommunicationType,
    val subject: String,
    val bodyPreview: String? = null,
    val channelsJson: String, // JSON array: ["IN_APP","EMAIL"]
    val status: AffiliateCommunicationStatus = AffiliateCommunicationStatus.CREATED,
    val canonicalNotificationId: String? = null,
    val referenceType: String? = null,
    val referenceId: String? = null,
    val idempotencyKey: String,
    val correlationId: String,
    val version: Long = 1L,
    val createdAt: Long = System.currentTimeMillis(),
    val scheduledAt: Long? = null,
    val deliveredAt: Long? = null,
    val readAt: Long? = null,
    val failureReason: String? = null
) {
    val isRead: Boolean get() = status == AffiliateCommunicationStatus.READ
    val isDelivered: Boolean get() = status in setOf(AffiliateCommunicationStatus.DELIVERED, AffiliateCommunicationStatus.READ)
    val isPending: Boolean get() = status in setOf(AffiliateCommunicationStatus.CREATED, AffiliateCommunicationStatus.QUEUED, AffiliateCommunicationStatus.PROCESSING)
}

/**
 * Per-affiliate, per-communication-type notification channel preferences.
 * Enforces mandatory type rules: governance, security, and system communications
 * cannot have in-app delivery disabled.
 */
data class AffiliateNotificationPreference(
    val preferenceId: String,
    val tenantId: String,
    val affiliateId: String,
    val userId: String,
    val communicationType: AffiliateCommunicationType,
    val inAppEnabled: Boolean = true,
    val pushEnabled: Boolean = true,
    val emailEnabled: Boolean = false,
    val smsEnabled: Boolean = false,
    val version: Long = 1L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val effectiveChannels: List<NotificationChannel>
        get() = buildList {
            if (inAppEnabled || communicationType.isMandatory) add(NotificationChannel.IN_APP)
            if (pushEnabled) add(NotificationChannel.PUSH)
            if (emailEnabled) add(NotificationChannel.EMAIL)
            if (smsEnabled) add(NotificationChannel.SMS)
        }
}

/**
 * Append-only cryptographic audit record for affiliate communication governance actions.
 * Maintains SHA-256 hash chain for tamper evidence.
 */
data class AffiliateCommunicationAuditRecord(
    val auditId: String,
    val tenantId: String,
    val affiliateId: String,
    val communicationId: String,
    val actorUserId: String,
    val actorRole: String,
    val actorType: AffiliateActorType,
    val action: String,
    val previousStatus: String?,
    val newStatus: String,
    val reason: String? = null,
    val correlationId: String,
    val recordHash: String,
    val previousAuditHash: String? = null,
    val chainHash: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Governance summary metrics for affiliate communication management (admin view).
 */
data class AffiliateNotificationGovernanceSummary(
    val tenantId: String,
    val totalCommunications: Long,
    val deliveredCount: Long,
    val readCount: Long,
    val failedCount: Long,
    val cancelledCount: Long,
    val pendingCount: Long,
    val unreadCount: Long,
    val byType: Map<String, Long> = emptyMap()
)

/**
 * Per-affiliate communication summary (affiliate self-view).
 */
data class AffiliateUnreadSummary(
    val affiliateId: String,
    val totalUnread: Long,
    val byType: Map<String, Long> = emptyMap()
)

/**
 * Module 20 Step 04 Downstream AI Governance Handoff Contract (v1.0.0).
 * Sealed, immutable advisory contract for Modules 21, 22, 23, 24 AI agents.
 * Read-only — AI agents MUST NOT mutate notification state through this contract.
 */
data class Module20Step04AffiliateCommunicationHandoffContract(
    val contractVersion: String = "v1.0.0",
    val tenantId: String,
    val affiliateId: String,
    val userId: String,
    val totalNotifications: Long,
    val unreadCount: Long,
    val deliveredCount: Long,
    val failedCount: Long,
    val lastNotificationAt: Long?,
    val communicationSummary: Map<String, Long>,
    val preferenceSummary: Map<String, Boolean>,
    val governanceStatus: String,
    val isReadOnly: Boolean = true,
    val allowedAiActions: List<String> = listOf(
        "EXPLAIN_NOTIFICATION_CONTENT",
        "SUMMARIZE_AFFILIATE_COMMUNICATIONS",
        "IDENTIFY_UNREAD_OPERATIONAL_NOTICES",
        "EXPLAIN_VERIFICATION_STATUS_MESSAGE",
        "EXPLAIN_ENROLLMENT_STATUS_MESSAGE",
        "RECOMMEND_HUMAN_FOLLOWUP",
        "INSPECT_PREFERENCE_SUMMARY"
    ),
    val forbiddenAiActions: List<String> = listOf(
        "SEND_UNRESTRICTED_MESSAGE",
        "IMPERSONATE_ADMINISTRATOR",
        "CHANGE_NOTIFICATION_PREFERENCES_WITHOUT_AUTH",
        "ALTER_NOTIFICATION_STATE_WITHOUT_TOOL",
        "BYPASS_RBAC",
        "BYPASS_ROW_LEVEL_SECURITY",
        "ALTER_AUDIT_HISTORY",
        "APPROVE_AFFILIATE_VERIFICATION",
        "CHANGE_AFFILIATE_LIFECYCLE",
        "DELETE_NOTIFICATION_RECORDS",
        "EMIT_GOVERNANCE_NOTICE_DIRECTLY",
        "DISCARD_MANDATORY_NOTIFICATIONS",
        "SUPPRESS_SECURITY_ALERTS",
        "BYPASS_AUDIT_LOGGING"
    ),
    val integritySealHash: String,
    val generatedAt: Long = System.currentTimeMillis()
)

package com.sucharu.sucharupro.domain.service.affiliate

import com.sucharu.sucharupro.domain.model.affiliate.*
import com.sucharu.sucharupro.domain.model.notification.NotificationChannel
import java.security.MessageDigest

/**
 * Deterministic Communication Policy Engine for Affiliate Notification Resolution.
 *
 * Responsibilities:
 * 1. Resolves the canonical NotificationType for a given AffiliateCommunicationType
 * 2. Resolves the effective delivery channels from affiliate preferences
 * 3. Enforces mandatory notification type rules (governance, security cannot disable in-app)
 * 4. Generates deterministic idempotency keys for communications
 * 5. Generates integrity hashes for AI Handoff Contracts
 * 6. Maintains the SHA-256 audit chain for communication audit records
 *
 * Module 20 Step 04.
 */
object AffiliateCommunicationPolicyEngine {

    const val GENESIS_AFFILIATE_COMM_AUDIT_BLOCK =
        "0000000000000000000000000000000000000000000000000000000000000000"

    // ─────────────────────────────────────────────────────────────────
    // Channel Resolution
    // ─────────────────────────────────────────────────────────────────

    /**
     * Resolves the effective notification channels for a communication intent
     * given the affiliate's stored preferences. Mandatory types always include IN_APP.
     */
    fun resolveChannels(
        communicationType: AffiliateCommunicationType,
        preference: AffiliateNotificationPreference?
    ): List<NotificationChannel> {
        if (preference == null) {
            // Default: mandatory types use IN_APP; others use IN_APP + PUSH
            return if (communicationType.isMandatory) {
                listOf(NotificationChannel.IN_APP)
            } else {
                listOf(NotificationChannel.IN_APP, NotificationChannel.PUSH)
            }
        }
        return preference.effectiveChannels.ifEmpty {
            listOf(NotificationChannel.IN_APP)
        }
    }

    /**
     * Builds a resolved AffiliateNotificationIntent from communication parameters and preferences.
     */
    fun buildIntent(
        tenantId: String,
        affiliateId: String,
        recipientUserId: String,
        communicationType: AffiliateCommunicationType,
        title: String,
        message: String,
        referenceType: String? = null,
        referenceId: String? = null,
        preference: AffiliateNotificationPreference? = null,
        idempotencyKey: String,
        correlationId: String
    ): AffiliateNotificationIntent {
        val channels = resolveChannels(communicationType, preference)
        return AffiliateNotificationIntent(
            tenantId = tenantId,
            affiliateId = affiliateId,
            recipientUserId = recipientUserId,
            communicationType = communicationType,
            canonicalNotificationType = communicationType.canonicalNotificationType,
            channels = channels,
            title = title,
            message = message,
            referenceType = referenceType,
            referenceId = referenceId,
            idempotencyKey = idempotencyKey,
            correlationId = correlationId
        )
    }

    // ─────────────────────────────────────────────────────────────────
    // Idempotency Key Generation
    // ─────────────────────────────────────────────────────────────────

    /**
     * Generates a deterministic idempotency key for a communication.
     * Format: sha256(tenantId:affiliateId:communicationType:correlationId)
     */
    fun generateIdempotencyKey(
        tenantId: String,
        affiliateId: String,
        communicationType: AffiliateCommunicationType,
        correlationId: String
    ): String {
        val payload = "$tenantId:$affiliateId:${communicationType.name}:$correlationId"
        return sha256(payload).take(32)
    }

    // ─────────────────────────────────────────────────────────────────
    // Audit Chain Computation
    // ─────────────────────────────────────────────────────────────────

    /**
     * Computes a deterministic SHA-256 hash for a communication audit record.
     */
    fun computeAuditRecordHash(
        tenantId: String,
        auditId: String,
        affiliateId: String,
        communicationId: String,
        actorUserId: String,
        action: String,
        previousStatus: String?,
        newStatus: String,
        correlationId: String,
        timestamp: Long
    ): String {
        val payload =
            "$tenantId|$auditId|$affiliateId|$communicationId|$actorUserId|$action|$previousStatus|$newStatus|$correlationId|$timestamp"
        return sha256(payload)
    }

    /**
     * Computes chained SHA-256 hash connecting the previous audit block.
     */
    fun computeAuditChainHash(previousChainHash: String?, recordHash: String): String {
        val prev = previousChainHash ?: GENESIS_AFFILIATE_COMM_AUDIT_BLOCK
        return sha256("$prev:$recordHash")
    }

    // ─────────────────────────────────────────────────────────────────
    // Handoff Contract Synthesis
    // ─────────────────────────────────────────────────────────────────

    /**
     * Synthesizes an immutable, integrity-sealed AI Governance Handoff Contract
     * for affiliate communication context (Module 20 Step 04).
     */
    fun synthesizeHandoffContract(
        tenantId: String,
        affiliateId: String,
        userId: String,
        communications: List<AffiliateCommunicationRecord>,
        preferences: List<AffiliateNotificationPreference>
    ): Module20Step04AffiliateCommunicationHandoffContract {
        val unreadCount = communications.count { !it.isRead }.toLong()
        val deliveredCount = communications.count { it.isDelivered }.toLong()
        val failedCount = communications.count { it.status == AffiliateCommunicationStatus.FAILED }.toLong()
        val totalCount = communications.size.toLong()
        val lastNotificationAt = communications.maxOfOrNull { it.createdAt }

        val byType = communications
            .groupBy { it.communicationType.name }
            .mapValues { (_, list) -> list.size.toLong() }

        val prefSummary = preferences.associate { pref ->
            pref.communicationType.name to pref.inAppEnabled
        }

        val governanceStatus = when {
            failedCount > 0 && unreadCount == 0L -> "ISSUES_DETECTED"
            unreadCount > 10 -> "HIGH_UNREAD"
            unreadCount > 0 -> "HAS_UNREAD"
            else -> "FULLY_CURRENT"
        }

        val sealPayload = "$tenantId:$affiliateId:$userId:$totalCount:$unreadCount:$deliveredCount:$failedCount"
        val sealHash = sha256(sealPayload)

        return Module20Step04AffiliateCommunicationHandoffContract(
            tenantId = tenantId,
            affiliateId = affiliateId,
            userId = userId,
            totalNotifications = totalCount,
            unreadCount = unreadCount,
            deliveredCount = deliveredCount,
            failedCount = failedCount,
            lastNotificationAt = lastNotificationAt,
            communicationSummary = byType,
            preferenceSummary = prefSummary,
            governanceStatus = governanceStatus,
            isReadOnly = true,
            integritySealHash = sealHash
        )
    }

    // ─────────────────────────────────────────────────────────────────
    // Default Message Builders
    // ─────────────────────────────────────────────────────────────────

    /**
     * Builds default notification title + message for well-known affiliate events.
     */
    fun buildDefaultNotificationContent(
        communicationType: AffiliateCommunicationType,
        referenceType: String? = null,
        referenceId: String? = null
    ): Pair<String, String> {
        return when (communicationType) {
            AffiliateCommunicationType.APPLICATION ->
                "Application Status Updated" to
                        "Your affiliate application status has been updated. Please review the details in your dashboard."

            AffiliateCommunicationType.ENROLLMENT ->
                "Enrollment Status Changed" to
                        "Your enrollment status in an affiliate program has changed. Please check your enrollments."

            AffiliateCommunicationType.PROFILE ->
                "Profile Action Required" to
                        "Your affiliate profile requires your attention. Please review and update your profile information."

            AffiliateCommunicationType.VERIFICATION ->
                "Verification Result Available" to
                        "A verification result is available for your affiliate profile. Please review the verification details."

            AffiliateCommunicationType.PROGRAM ->
                "Program Update" to
                        "There is an update regarding an affiliate program you participate in. Please review the notice."

            AffiliateCommunicationType.GOVERNANCE ->
                "Governance Notice" to
                        "An important governance notice requires your attention. Please log in to review this mandatory notice."

            AffiliateCommunicationType.SECURITY ->
                "Security Notice" to
                        "A security notice has been issued for your affiliate account. Immediate review is required."

            AffiliateCommunicationType.SYSTEM ->
                "System Notice" to
                        "A system notice has been issued. Please review the details in your affiliate dashboard."
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Internal Utilities
    // ─────────────────────────────────────────────────────────────────

    fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

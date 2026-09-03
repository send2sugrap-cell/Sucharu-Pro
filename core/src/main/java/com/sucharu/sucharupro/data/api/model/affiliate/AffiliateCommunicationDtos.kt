package com.sucharu.sucharupro.data.api.model.affiliate

/**
 * Request DTO for emitting an affiliate communication (admin/system-triggered).
 */
data class EmitAffiliateCommunicationRequestDto(
    val affiliateId: String,
    val communicationType: String,
    val title: String? = null,
    val message: String? = null,
    val referenceType: String? = null,
    val referenceId: String? = null,
    val idempotencyKey: String? = null
)

/**
 * Request DTO for updating an affiliate notification preference for a specific communication type.
 */
data class UpdateAffiliateNotificationPreferenceRequestDto(
    val communicationType: String,
    val inAppEnabled: Boolean = true,
    val pushEnabled: Boolean = true,
    val emailEnabled: Boolean = false,
    val smsEnabled: Boolean = false
)

/**
 * Response DTO for an affiliate communication record.
 */
data class AffiliateCommunicationResponseDto(
    val communicationId: String,
    val tenantId: String,
    val affiliateId: String,
    val recipientUserId: String,
    val communicationType: String,
    val subject: String,
    val bodyPreview: String?,
    val channelsJson: String,
    val status: String,
    val canonicalNotificationId: String?,
    val referenceType: String?,
    val referenceId: String?,
    val idempotencyKey: String,
    val correlationId: String,
    val version: Long,
    val createdAt: Long,
    val deliveredAt: Long?,
    val readAt: Long?,
    val failureReason: String?
) {
    val title: String get() = subject
    val message: String get() = bodyPreview ?: ""
    val isRead: Boolean get() = readAt != null || status == "READ"
}

/**
 * Response DTO for the unread notification count summary.
 */
data class AffiliateUnreadSummaryResponseDto(
    val affiliateId: String,
    val totalUnread: Long,
    val byType: Map<String, Long>
)

/**
 * Response DTO for a single notification preference entry.
 */
data class AffiliateNotificationPreferenceResponseDto(
    val preferenceId: String,
    val tenantId: String,
    val affiliateId: String,
    val userId: String,
    val communicationType: String,
    val isMandatory: Boolean,
    val inAppEnabled: Boolean,
    val pushEnabled: Boolean,
    val emailEnabled: Boolean,
    val smsEnabled: Boolean,
    val version: Long,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Response DTO for communication governance summary (admin/manager view).
 */
data class AffiliateNotificationGovernanceSummaryResponseDto(
    val tenantId: String,
    val totalCommunications: Long,
    val deliveredCount: Long,
    val readCount: Long,
    val failedCount: Long,
    val cancelledCount: Long,
    val pendingCount: Long,
    val unreadCount: Long,
    val byType: Map<String, Long>
)

/**
 * Response DTO for an affiliate communication audit record.
 */
data class AffiliateCommunicationAuditResponseDto(
    val auditId: String,
    val tenantId: String,
    val affiliateId: String,
    val communicationId: String,
    val actorUserId: String,
    val actorRole: String,
    val actorType: String,
    val action: String,
    val previousStatus: String?,
    val newStatus: String,
    val reason: String?,
    val correlationId: String,
    val recordHash: String,
    val previousAuditHash: String?,
    val chainHash: String,
    val timestamp: Long
)

/**
 * Response DTO for the mark-all-read operation.
 */
data class MarkAllReadResponseDto(
    val affiliateId: String,
    val markedCount: Int,
    val success: Boolean = true
)

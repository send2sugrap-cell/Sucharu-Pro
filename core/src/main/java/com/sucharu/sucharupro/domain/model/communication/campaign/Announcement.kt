package com.sucharu.sucharupro.domain.model.communication.campaign

import com.sucharu.sucharupro.domain.model.notification.NotificationChannel

/**
 * Organizational announcement model (Module 10 Step 07).
 */
data class Announcement(
    val announcementId: String,
    val announcementNo: String,
    val projectId: String,
    val title: String,
    val content: String,
    val priority: CampaignPriority = CampaignPriority.NORMAL,
    val audienceType: CampaignAudienceType = CampaignAudienceType.ALL_PROJECT_USERS,
    val targetCriteria: CampaignAudienceCriteria = CampaignAudienceCriteria(),
    val channel: NotificationChannel = NotificationChannel.IN_APP,
    val status: CampaignStatus = CampaignStatus.DRAFT,
    val scheduledAt: Long? = null,
    val expiresAt: Long? = null,
    val acknowledgementRequired: Boolean = false,
    val createdBy: String,
    val approvedBy: String? = null,
    val publishedBy: String? = null,
    val cancelledBy: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val publishedAt: Long? = null,
    val cancelledAt: Long? = null,
    val idempotencyKey: String? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(announcementId.isNotBlank()) { "Announcement ID cannot be blank." }
        require(announcementNo.isNotBlank()) { "Announcement Number cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(title.isNotBlank()) { "Announcement title cannot be blank." }
        require(content.isNotBlank()) { "Announcement content cannot be blank." }
        require(createdBy.isNotBlank()) { "Created By cannot be blank." }
        require(createdAt > 0) { "Creation timestamp must be positive." }
        require(updatedAt >= createdAt) { "Updated timestamp cannot precede creation timestamp." }

        if (expiresAt != null && expiresAt <= createdAt) {
            require(expiresAt > createdAt) { "Announcement expiry date must be in the future." }
        }
    }

    val isExpired: Boolean
        get() = expiresAt != null && expiresAt < System.currentTimeMillis()
}

package com.sucharu.sucharupro.domain.model.communication.campaign

import com.sucharu.sucharupro.domain.model.notification.NotificationChannel

/**
 * Core aggregate root representing a Communication Campaign in Sucharu Pro ERP (Module 10 Step 07).
 *
 * Implements immutable history, strong project isolation, RBAC lifecycle control,
 * and metadata security guards.
 */
data class Campaign(
    val campaignId: String,
    val campaignNo: String,
    val projectId: String,
    val title: String,
    val description: String = "",
    val campaignType: CampaignType = CampaignType.GENERAL,
    val status: CampaignStatus = CampaignStatus.DRAFT,
    val priority: CampaignPriority = CampaignPriority.NORMAL,
    val audienceType: CampaignAudienceType = CampaignAudienceType.ALL_PROJECT_USERS,
    val targetCriteria: CampaignAudienceCriteria = CampaignAudienceCriteria(),
    val communicationChannel: NotificationChannel = NotificationChannel.IN_APP,
    val content: String,
    val templateId: String? = null,
    val scheduledAt: Long? = null,
    val startsAt: Long? = null,
    val endsAt: Long? = null,
    val createdBy: String,
    val updatedBy: String? = null,
    val approvedBy: String? = null,
    val publishedBy: String? = null,
    val cancelledBy: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val submittedAt: Long? = null,
    val approvedAt: Long? = null,
    val publishedAt: Long? = null,
    val completedAt: Long? = null,
    val cancelledAt: Long? = null,
    val rejectionReason: String? = null,
    val idempotencyKey: String? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(campaignId.isNotBlank()) { "Campaign ID cannot be blank." }
        require(campaignNo.isNotBlank()) { "Campaign Number cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(title.isNotBlank()) { "Campaign title cannot be blank." }
        require(content.isNotBlank()) { "Campaign content cannot be blank." }
        require(createdBy.isNotBlank()) { "Created By cannot be blank." }
        require(createdAt > 0) { "Creation timestamp must be positive." }
        require(updatedAt >= createdAt) { "Updated timestamp cannot precede creation timestamp." }

        if (startsAt != null && endsAt != null) {
            require(endsAt >= startsAt) { "Campaign endsAt cannot precede startsAt." }
        }

        // Security check: sensitive keys are prohibited
        val forbiddenKeys = listOf("password", "token", "secret", "cvv", "card_number", "pin", "api_key", "bearer")
        for (key in metadata.keys) {
            val lower = key.lowercase()
            require(forbiddenKeys.none { lower.contains(it) }) {
                "Sensitive key '$key' is prohibited in campaign metadata."
            }
        }
    }

    val isTerminal: Boolean
        get() = status.isTerminal

    val isPublished: Boolean
        get() = status == CampaignStatus.PUBLISHED || publishedAt != null
}

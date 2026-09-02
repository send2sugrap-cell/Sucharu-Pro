package com.sucharu.sucharupro.domain.model.communication.campaign

import com.sucharu.sucharupro.domain.model.notification.NotificationChannel

/**
 * Immediate or scheduled targeted broadcast communication (Module 10 Step 07).
 */
data class BroadcastMessage(
    val broadcastId: String,
    val broadcastNo: String,
    val projectId: String,
    val title: String,
    val message: String,
    val priority: CampaignPriority = CampaignPriority.HIGH,
    val audienceType: CampaignAudienceType = CampaignAudienceType.ROLE,
    val targetCriteria: CampaignAudienceCriteria = CampaignAudienceCriteria(),
    val channels: Set<NotificationChannel> = setOf(NotificationChannel.IN_APP),
    val status: CampaignStatus = CampaignStatus.DRAFT,
    val scheduledAt: Long? = null,
    val sentAt: Long? = null,
    val createdBy: String,
    val publishedBy: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val idempotencyKey: String? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(broadcastId.isNotBlank()) { "Broadcast ID cannot be blank." }
        require(broadcastNo.isNotBlank()) { "Broadcast Number cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(title.isNotBlank()) { "Broadcast title cannot be blank." }
        require(message.isNotBlank()) { "Broadcast message cannot be blank." }
        require(createdBy.isNotBlank()) { "Created By cannot be blank." }
    }
}

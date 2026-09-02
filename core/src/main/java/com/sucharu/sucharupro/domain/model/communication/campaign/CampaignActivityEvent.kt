package com.sucharu.sucharupro.domain.model.communication.campaign

/**
 * Immutable append-only audit event for Campaign, Announcement, and Broadcast operations (Module 10 Step 07).
 */
data class CampaignActivityEvent(
    val eventId: String,
    val projectId: String,
    val campaignId: String,
    val eventType: CampaignActivityEventType,
    val actorUserId: String,
    val summary: String,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(eventId.isNotBlank()) { "Event ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(campaignId.isNotBlank()) { "Campaign ID cannot be blank." }
        require(actorUserId.isNotBlank()) { "Actor User ID cannot be blank." }
        require(summary.isNotBlank()) { "Summary cannot be blank." }
    }
}

enum class CampaignActivityEventType(val defaultLabel: String) {
    CAMPAIGN_CREATED("Campaign Created"),
    CAMPAIGN_UPDATED("Campaign Updated"),
    CAMPAIGN_SUBMITTED("Campaign Submitted for Approval"),
    CAMPAIGN_APPROVED("Campaign Approved"),
    CAMPAIGN_REJECTED("Campaign Rejected"),
    CAMPAIGN_SCHEDULED("Campaign Scheduled"),
    CAMPAIGN_PUBLISHED("Campaign Published"),
    CAMPAIGN_COMPLETED("Campaign Completed"),
    CAMPAIGN_CANCELLED("Campaign Cancelled"),
    AUDIENCE_RESOLVED("Audience Resolved"),
    RECIPIENT_DISPATCHED("Recipient Dispatched"),
    RECIPIENT_DELIVERED("Recipient Delivered"),
    RECIPIENT_READ("Recipient Read"),
    RECIPIENT_ACKNOWLEDGED("Recipient Acknowledged"),
    DELIVERY_FAILED("Delivery Failed"),
    ANNOUNCEMENT_CREATED("Announcement Created"),
    ANNOUNCEMENT_PUBLISHED("Announcement Published"),
    ANNOUNCEMENT_EXPIRED("Announcement Expired"),
    ANNOUNCEMENT_ACKNOWLEDGED("Announcement Acknowledged"),
    BROADCAST_DISPATCHED("Broadcast Dispatched")
}

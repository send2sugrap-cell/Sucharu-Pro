package com.sucharu.sucharupro.domain.model.communication.campaign

import com.sucharu.sucharupro.domain.model.notification.NotificationStatus

/**
 * Resolved recipient tracking entity for a campaign/broadcast dispatch (Module 10 Step 07).
 */
data class CampaignRecipient(
    val recipientId: String,
    val campaignId: String,
    val projectId: String,
    val recipientType: String, // "CUSTOMER", "VENDOR", "USER", "STAFF"
    val recipientEntityId: String, // Customer ID, Vendor ID, or User ID
    val userId: String,
    val notificationId: String? = null,
    val deliveryStatus: NotificationStatus = NotificationStatus.DRAFT,
    val readStatus: Boolean = false,
    val acknowledgementStatus: Boolean = false,
    val resolvedAt: Long = System.currentTimeMillis(),
    val deliveredAt: Long? = null,
    val readAt: Long? = null,
    val acknowledgedAt: Long? = null,
    val failureReason: String? = null
) {
    init {
        require(recipientId.isNotBlank()) { "Recipient ID cannot be blank." }
        require(campaignId.isNotBlank()) { "Campaign ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(recipientEntityId.isNotBlank()) { "Recipient Entity ID cannot be blank." }
        require(userId.isNotBlank()) { "User ID cannot be blank." }
    }
}

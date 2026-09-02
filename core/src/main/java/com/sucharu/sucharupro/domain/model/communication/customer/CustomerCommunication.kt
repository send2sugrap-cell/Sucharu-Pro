package com.sucharu.sucharupro.domain.model.communication.customer

import com.sucharu.sucharupro.domain.model.notification.NotificationPriority

/**
 * Core aggregate root representing a Customer Communication (Module 10 Step 02).
 *
 * References the canonical [Notification] created by Module 10 Step 01.
 */
data class CustomerCommunication(
    val communicationId: String,
    val communicationNo: String,
    val projectId: String,
    val customerId: String,
    val recipientUserId: String,
    val communicationType: CustomerCommunicationType,
    val notificationId: String,
    val title: String,
    val message: String,
    val referenceType: String? = null,
    val referenceId: String? = null,
    val priority: NotificationPriority = NotificationPriority.NORMAL,
    val status: CustomerCommunicationStatus = CustomerCommunicationStatus.DRAFT,
    val scheduledAt: Long? = null,
    val groupKey: String? = null,
    val idempotencyKey: String? = null,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val sentAt: Long? = null,
    val deliveredAt: Long? = null,
    val readAt: Long? = null,
    val acknowledgedAt: Long? = null,
    val cancelledAt: Long? = null,
    val failureReason: String? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(communicationId.isNotBlank()) { "Communication ID cannot be blank." }
        require(communicationNo.isNotBlank()) { "Communication Number cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(customerId.isNotBlank()) { "Customer ID cannot be blank." }
        require(recipientUserId.isNotBlank()) { "Recipient User ID cannot be blank." }
        require(notificationId.isNotBlank()) { "Canonical Notification ID cannot be blank." }
        require(title.isNotBlank()) { "Title cannot be blank." }
        require(message.isNotBlank()) { "Message cannot be blank." }
        require(createdBy.isNotBlank()) { "Created By identifier cannot be blank." }
        require(createdAt > 0) { "Creation timestamp must be positive." }
        require(updatedAt >= createdAt) { "Updated timestamp cannot precede creation timestamp." }

        // Metadata safety check: Ensure no sensitive keys are stored
        val forbiddenKeys = listOf("password", "token", "secret", "cvv", "card_number", "pin", "api_key")
        for (key in metadata.keys) {
            val lower = key.lowercase()
            require(forbiddenKeys.none { lower.contains(it) }) {
                "Sensitive key '$key' is prohibited in communication metadata."
            }
        }
    }

    val isRead: Boolean
        get() = status == CustomerCommunicationStatus.READ || status == CustomerCommunicationStatus.ACKNOWLEDGED || readAt != null

    val isAcknowledged: Boolean
        get() = status == CustomerCommunicationStatus.ACKNOWLEDGED || acknowledgedAt != null

    val isDelivered: Boolean
        get() = status == CustomerCommunicationStatus.DELIVERED || isRead || deliveredAt != null
}

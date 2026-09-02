package com.sucharu.sucharupro.domain.model.notification

/**
 * Core aggregate root representing a notification in Sucharu Pro ERP (Module 10 Step 01).
 *
 * Implements immutable delivery history, strong project isolation, recipient containment,
 * and deterministic state transitions.
 */
data class Notification(
    val notificationId: String,
    val notificationNo: String,
    val projectId: String,
    val recipientUserId: String,
    val recipientType: String = "USER", // USER, CUSTOMER, VENDOR, STAFF
    val senderUserId: String? = null,
    val notificationType: NotificationType,
    val channel: NotificationChannel,
    val priority: NotificationPriority = NotificationPriority.NORMAL,
    val status: NotificationStatus = NotificationStatus.DRAFT,
    val title: String,
    val message: String,
    val referenceType: String? = null,
    val referenceId: String? = null,
    val templateId: String? = null,
    val groupKey: String? = null,
    val idempotencyKey: String? = null,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val queuedAt: Long? = null,
    val processedAt: Long? = null,
    val sentAt: Long? = null,
    val deliveredAt: Long? = null,
    val readAt: Long? = null,
    val cancelledAt: Long? = null,
    val failureReason: String? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(notificationId.isNotBlank()) { "Notification ID cannot be blank." }
        require(notificationNo.isNotBlank()) { "Notification Number cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(recipientUserId.isNotBlank()) { "Recipient User ID cannot be blank." }
        require(title.isNotBlank()) { "Notification title cannot be blank." }
        require(message.isNotBlank()) { "Notification message cannot be blank." }
        require(createdBy.isNotBlank()) { "Created By cannot be blank." }
        require(createdAt > 0) { "Creation timestamp must be positive." }
        require(updatedAt >= createdAt) { "Updated timestamp cannot precede creation timestamp." }

        // Metadata safety checks: Never store passwords, auth secrets, or payment credentials
        val forbiddenKeys = listOf("password", "token", "secret", "cvv", "card_number", "pin", "api_key")
        for (key in metadata.keys) {
            val lower = key.lowercase()
            require(forbiddenKeys.none { lower.contains(it) }) {
                "Sensitive key '$key' is strictly prohibited in notification metadata."
            }
        }
    }

    val isRead: Boolean
        get() = status == NotificationStatus.READ || readAt != null

    val isDelivered: Boolean
        get() = status == NotificationStatus.DELIVERED || status == NotificationStatus.READ || deliveredAt != null

    val isFailed: Boolean
        get() = status == NotificationStatus.FAILED
}

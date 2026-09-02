package com.sucharu.sucharupro.domain.model.notification

/**
 * Generic business event trigger payload for requesting notifications (Module 10 Step 01).
 */
data class NotificationTrigger(
    val projectId: String,
    val notificationType: NotificationType,
    val recipientUserId: String,
    val recipientType: String = "USER",
    val title: String,
    val message: String,
    val referenceType: String? = null,
    val referenceId: String? = null,
    val priority: NotificationPriority = NotificationPriority.NORMAL,
    val preferredChannel: NotificationChannel? = null,
    val idempotencyKey: String? = null,
    val groupKey: String? = null,
    val actorUserId: String,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(recipientUserId.isNotBlank()) { "Recipient User ID cannot be blank." }
        require(title.isNotBlank()) { "Title cannot be blank." }
        require(message.isNotBlank()) { "Message cannot be blank." }
        require(actorUserId.isNotBlank()) { "Actor User ID cannot be blank." }
    }
}

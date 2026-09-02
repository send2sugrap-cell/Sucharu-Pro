package com.sucharu.sucharupro.domain.model.notification

/**
 * Immutable append-only audit event for notification operations (Module 10 Step 01).
 */
data class NotificationActivityEvent(
    val eventId: String,
    val projectId: String,
    val notificationId: String,
    val eventType: NotificationActivityEventType,
    val actorUserId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap(),
    val description: String
) {
    init {
        require(eventId.isNotBlank()) { "Event ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(notificationId.isNotBlank()) { "Notification ID cannot be blank." }
        require(actorUserId.isNotBlank()) { "Actor User ID cannot be blank." }
        require(timestamp > 0) { "Timestamp must be positive." }
        require(description.isNotBlank()) { "Description cannot be blank." }
    }
}

enum class NotificationActivityEventType(val defaultLabel: String) {
    NOTIFICATION_CREATED("Notification Created"),
    NOTIFICATION_QUEUED("Notification Queued"),
    NOTIFICATION_PROCESSING("Notification Processing"),
    NOTIFICATION_SENT("Notification Sent"),
    NOTIFICATION_DELIVERED("Notification Delivered"),
    NOTIFICATION_READ("Notification Read / Acknowledged"),
    NOTIFICATION_FAILED("Notification Delivery Failed"),
    NOTIFICATION_RETRIED("Notification Retried"),
    NOTIFICATION_CANCELLED("Notification Cancelled"),
    PREFERENCE_CREATED("Preference Created"),
    PREFERENCE_UPDATED("Preference Updated"),
    TEMPLATE_CREATED("Template Created"),
    TEMPLATE_VERSION_CREATED("Template Version Created")
}

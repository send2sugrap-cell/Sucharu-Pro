package com.sucharu.sucharupro.domain.model.notification

/**
 * Immutable record of a single delivery dispatch attempt (Module 10 Step 01).
 */
data class NotificationDeliveryAttempt(
    val attemptId: String,
    val projectId: String,
    val notificationId: String,
    val channel: NotificationChannel,
    val attemptNumber: Int,
    val status: NotificationStatus,
    val provider: String = "LOCAL_IN_MEMORY",
    val providerMessageId: String? = null,
    val requestedAt: Long = System.currentTimeMillis(),
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val failureCode: String? = null,
    val failureMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    init {
        require(attemptId.isNotBlank()) { "Attempt ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(notificationId.isNotBlank()) { "Notification ID cannot be blank." }
        require(attemptNumber >= 1) { "Attempt number must be >= 1." }
        require(createdAt > 0) { "Creation timestamp must be positive." }
    }
}

package com.sucharu.sucharupro.domain.model.notification

/**
 * User channel preferences per notification category/type (Module 10 Step 01).
 */
data class NotificationPreference(
    val preferenceId: String,
    val projectId: String,
    val userId: String,
    val notificationType: NotificationType,
    val channel: NotificationChannel,
    val enabled: Boolean,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(preferenceId.isNotBlank()) { "Preference ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(userId.isNotBlank()) { "User ID cannot be blank." }
        require(createdAt > 0) { "Creation timestamp must be positive." }
        require(updatedAt >= createdAt) { "Updated timestamp cannot precede creation timestamp." }

        // In-App channel cannot be disabled for mandatory system/security alerts
        if (channel == NotificationChannel.IN_APP && notificationType.isMandatory) {
            require(enabled) { "In-App notifications cannot be disabled for mandatory '${notificationType.defaultLabel}'." }
        }
    }
}

package com.sucharu.sucharupro.domain.model.notification

/**
 * Immutable versioned notification template (Module 10 Step 01).
 */
data class NotificationTemplate(
    val templateId: String,
    val projectId: String,
    val templateCode: String,
    val notificationType: NotificationType,
    val channel: NotificationChannel,
    val titleTemplate: String,
    val messageTemplate: String,
    val languageCode: String = "en",
    val active: Boolean = true,
    val version: Int = 1,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(templateId.isNotBlank()) { "Template ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(templateCode.isNotBlank()) { "Template code cannot be blank." }
        require(titleTemplate.isNotBlank()) { "Title template cannot be blank." }
        require(messageTemplate.isNotBlank()) { "Message template cannot be blank." }
        require(version >= 1) { "Template version must be >= 1." }
        require(createdAt > 0) { "Creation timestamp must be positive." }
        require(updatedAt >= createdAt) { "Updated timestamp cannot precede creation timestamp." }
    }
}

package com.sucharu.sucharupro.domain.model.notification

/**
 * Aggregated summary and metrics for notifications in a project scope (Module 10 Step 01).
 */
data class NotificationSummary(
    val projectId: String,
    val totalCount: Int = 0,
    val unreadCount: Int = 0,
    val readCount: Int = 0,
    val queuedCount: Int = 0,
    val processingCount: Int = 0,
    val sentCount: Int = 0,
    val deliveredCount: Int = 0,
    val failedCount: Int = 0,
    val cancelledCount: Int = 0,
    val countsByPriority: Map<NotificationPriority, Int> = emptyMap(),
    val countsByType: Map<NotificationType, Int> = emptyMap(),
    val countsByChannel: Map<NotificationChannel, Int> = emptyMap()
)

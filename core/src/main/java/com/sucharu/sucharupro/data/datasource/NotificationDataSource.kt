package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.notification.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Source contract for notifications, attempts, preferences, templates, and audit events (Module 10 Step 01).
 */
interface NotificationDataSource {

    suspend fun saveNotification(notification: Notification)

    suspend fun getNotificationById(projectId: String, notificationId: String): Notification?

    suspend fun getNotificationByNo(projectId: String, notificationNo: String): Notification?

    suspend fun getByIdempotencyKey(projectId: String, idempotencyKey: String): Notification?

    suspend fun getByDuplicateCriteria(
        projectId: String,
        recipientUserId: String,
        notificationType: NotificationType,
        referenceType: String?,
        referenceId: String?,
        groupKey: String?
    ): Notification?

    fun observeNotificationsByProject(projectId: String): Flow<List<Notification>>

    fun observeNotificationsByUser(projectId: String, userId: String): Flow<List<Notification>>

    suspend fun getNotificationsByReference(
        projectId: String,
        referenceType: String,
        referenceId: String
    ): List<Notification>

    suspend fun generateNotificationNumber(projectId: String): String

    // Delivery attempts
    suspend fun saveDeliveryAttempt(attempt: NotificationDeliveryAttempt)

    suspend fun getDeliveryAttempts(projectId: String, notificationId: String): List<NotificationDeliveryAttempt>

    // Preferences
    suspend fun savePreference(preference: NotificationPreference)

    suspend fun getPreferencesByUser(projectId: String, userId: String): List<NotificationPreference>

    suspend fun getPreference(projectId: String, preferenceId: String): NotificationPreference?

    // Templates
    suspend fun saveTemplate(template: NotificationTemplate)

    suspend fun getTemplateById(projectId: String, templateId: String): NotificationTemplate?

    suspend fun getTemplatesByCode(projectId: String, templateCode: String): List<NotificationTemplate>

    suspend fun getAllTemplates(projectId: String): List<NotificationTemplate>

    // Activity Events
    suspend fun recordActivityEvent(event: NotificationActivityEvent)

    suspend fun getActivityEvents(projectId: String, notificationId: String): List<NotificationActivityEvent>

    fun observeActivityEvents(projectId: String): Flow<List<NotificationActivityEvent>>
}

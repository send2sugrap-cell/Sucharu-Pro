package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.notification.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Calendar

/**
 * Concurrency-safe in-memory Fake implementation of NotificationDataSource (Module 10 Step 01).
 */
class FakeNotificationDataSource : NotificationDataSource {

    private val mutex = Mutex()
    private val notificationsState = MutableStateFlow<Map<String, Notification>>(emptyMap())
    private val deliveryAttemptsState = MutableStateFlow<List<NotificationDeliveryAttempt>>(emptyList())
    private val preferencesState = MutableStateFlow<Map<String, NotificationPreference>>(emptyMap())
    private val templatesState = MutableStateFlow<Map<String, NotificationTemplate>>(emptyMap())
    private val activityEventsState = MutableStateFlow<List<NotificationActivityEvent>>(emptyList())

    private var sequenceCounter = 0

    override suspend fun saveNotification(notification: Notification): Unit = mutex.withLock {
        notificationsState.update { current ->
            current + (notification.notificationId to notification)
        }
    }

    override suspend fun getNotificationById(projectId: String, notificationId: String): Notification? = mutex.withLock {
        notificationsState.value[notificationId]?.takeIf { it.projectId == projectId }
    }

    override suspend fun getNotificationByNo(projectId: String, notificationNo: String): Notification? = mutex.withLock {
        notificationsState.value.values.firstOrNull { it.projectId == projectId && it.notificationNo == notificationNo }
    }

    override suspend fun getByIdempotencyKey(projectId: String, idempotencyKey: String): Notification? = mutex.withLock {
        notificationsState.value.values.firstOrNull { it.projectId == projectId && it.idempotencyKey == idempotencyKey }
    }

    override suspend fun getByDuplicateCriteria(
        projectId: String,
        recipientUserId: String,
        notificationType: NotificationType,
        referenceType: String?,
        referenceId: String?,
        groupKey: String?
    ): Notification? = mutex.withLock {
        notificationsState.value.values.firstOrNull { n ->
            n.projectId == projectId &&
                    n.recipientUserId == recipientUserId &&
                    n.notificationType == notificationType &&
                    n.referenceType == referenceType &&
                    n.referenceId == referenceId &&
                    n.groupKey == groupKey &&
                    (n.status == NotificationStatus.QUEUED || n.status == NotificationStatus.PROCESSING || n.status == NotificationStatus.SENT || n.status == NotificationStatus.DELIVERED)
        }
    }

    override fun observeNotificationsByProject(projectId: String): Flow<List<Notification>> {
        return notificationsState.map { map ->
            map.values.filter { it.projectId == projectId }.sortedByDescending { it.createdAt }
        }
    }

    override fun observeNotificationsByUser(projectId: String, userId: String): Flow<List<Notification>> {
        return notificationsState.map { map ->
            map.values
                .filter { it.projectId == projectId && it.recipientUserId == userId }
                .sortedByDescending { it.createdAt }
        }
    }

    override suspend fun getNotificationsByReference(
        projectId: String,
        referenceType: String,
        referenceId: String
    ): List<Notification> = mutex.withLock {
        notificationsState.value.values
            .filter { it.projectId == projectId && it.referenceType == referenceType && it.referenceId == referenceId }
            .sortedByDescending { it.createdAt }
    }

    override suspend fun generateNotificationNumber(projectId: String): String = mutex.withLock {
        sequenceCounter++
        val year = Calendar.getInstance().get(Calendar.YEAR)
        "NTF-$year-%05d".format(sequenceCounter)
    }

    override suspend fun saveDeliveryAttempt(attempt: NotificationDeliveryAttempt): Unit = mutex.withLock {
        deliveryAttemptsState.update { current ->
            current + attempt
        }
    }

    override suspend fun getDeliveryAttempts(projectId: String, notificationId: String): List<NotificationDeliveryAttempt> = mutex.withLock {
        deliveryAttemptsState.value
            .filter { it.projectId == projectId && it.notificationId == notificationId }
            .sortedBy { it.attemptNumber }
    }

    override suspend fun savePreference(preference: NotificationPreference): Unit = mutex.withLock {
        preferencesState.update { current ->
            current + (preference.preferenceId to preference)
        }
    }

    override suspend fun getPreferencesByUser(projectId: String, userId: String): List<NotificationPreference> = mutex.withLock {
        preferencesState.value.values
            .filter { it.projectId == projectId && it.userId == userId }
            .sortedBy { it.notificationType.name }
    }

    override suspend fun getPreference(projectId: String, preferenceId: String): NotificationPreference? = mutex.withLock {
        preferencesState.value[preferenceId]?.takeIf { it.projectId == projectId }
    }

    override suspend fun saveTemplate(template: NotificationTemplate): Unit = mutex.withLock {
        templatesState.update { current ->
            current + (template.templateId to template)
        }
    }

    override suspend fun getTemplateById(projectId: String, templateId: String): NotificationTemplate? = mutex.withLock {
        templatesState.value[templateId]?.takeIf { it.projectId == projectId }
    }

    override suspend fun getTemplatesByCode(projectId: String, templateCode: String): List<NotificationTemplate> = mutex.withLock {
        templatesState.value.values
            .filter { it.projectId == projectId && it.templateCode == templateCode }
            .sortedByDescending { it.version }
    }

    override suspend fun getAllTemplates(projectId: String): List<NotificationTemplate> = mutex.withLock {
        templatesState.value.values
            .filter { it.projectId == projectId }
            .sortedBy { it.templateCode }
    }

    override suspend fun recordActivityEvent(event: NotificationActivityEvent): Unit = mutex.withLock {
        activityEventsState.update { current ->
            current + event
        }
    }

    override suspend fun getActivityEvents(projectId: String, notificationId: String): List<NotificationActivityEvent> = mutex.withLock {
        activityEventsState.value
            .filter { it.projectId == projectId && it.notificationId == notificationId }
            .sortedByDescending { it.timestamp }
    }

    override fun observeActivityEvents(projectId: String): Flow<List<NotificationActivityEvent>> {
        return activityEventsState.map { list ->
            list.filter { it.projectId == projectId }.sortedByDescending { it.timestamp }
        }
    }
}

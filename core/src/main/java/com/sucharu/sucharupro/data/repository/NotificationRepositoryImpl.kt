package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.NotificationDataSource
import com.sucharu.sucharupro.data.notification.NotificationDeliveryServiceImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.notification.*
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.notification.NotificationDeliveryService
import com.sucharu.sucharupro.domain.repository.notification.NotificationRepository
import com.sucharu.sucharupro.domain.validation.notification.NotificationAuthorizationValidator
import com.sucharu.sucharupro.domain.validation.notification.NotificationLifecycleValidator
import com.sucharu.sucharupro.domain.validation.notification.NotificationValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Production-grade implementation of NotificationRepository (Module 10 Step 01).
 *
 * Implements Mutex concurrency locking, project & recipient isolation, idempotency deduplication,
 * immutable delivery attempts, and append-only audit activity logging.
 */
class NotificationRepositoryImpl(
    private val notificationDataSource: NotificationDataSource,
    private val deliveryService: NotificationDeliveryService = NotificationDeliveryServiceImpl()
) : NotificationRepository {

    private val mutex = Mutex()

    override suspend fun createNotification(
        projectId: String,
        recipientUserId: String,
        recipientType: String,
        notificationType: NotificationType,
        channel: NotificationChannel,
        priority: NotificationPriority,
        title: String,
        message: String,
        referenceType: String?,
        referenceId: String?,
        templateId: String?,
        groupKey: String?,
        idempotencyKey: String?,
        metadata: Map<String, String>,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Notification> = mutex.withLock {
        if (projectId.isBlank()) {
            return@withLock DomainResult.Error(message = "Project ID cannot be blank.")
        }

        // 1. Idempotency Check
        if (!idempotencyKey.isNullOrBlank()) {
            val existing = notificationDataSource.getByIdempotencyKey(projectId, idempotencyKey)
            if (existing != null) {
                return@withLock DomainResult.Success(existing)
            }
        }

        // 2. Duplicate Detection
        val duplicate = notificationDataSource.getByDuplicateCriteria(
            projectId = projectId,
            recipientUserId = recipientUserId,
            notificationType = notificationType,
            referenceType = referenceType,
            referenceId = referenceId,
            groupKey = groupKey
        )
        if (duplicate != null) {
            return@withLock DomainResult.Success(duplicate)
        }

        // 3. Number generation & creation
        val notificationId = UUID.randomUUID().toString()
        val notificationNo = notificationDataSource.generateNotificationNumber(projectId)
        val now = System.currentTimeMillis()

        val notification = Notification(
            notificationId = notificationId,
            notificationNo = notificationNo,
            projectId = projectId,
            recipientUserId = recipientUserId,
            recipientType = recipientType,
            senderUserId = actorId,
            notificationType = notificationType,
            channel = channel,
            priority = priority,
            status = NotificationStatus.DRAFT,
            title = title,
            message = message,
            referenceType = referenceType,
            referenceId = referenceId,
            templateId = templateId,
            groupKey = groupKey,
            idempotencyKey = idempotencyKey,
            createdBy = actorId,
            createdAt = now,
            updatedAt = now,
            metadata = metadata
        )

        val validationResult = NotificationValidator.validateNotification(notification)
        if (validationResult is DomainResult.Error) {
            return@withLock validationResult
        }

        notificationDataSource.saveNotification(notification)

        // Record audit
        notificationDataSource.recordActivityEvent(
            NotificationActivityEvent(
                eventId = UUID.randomUUID().toString(),
                projectId = projectId,
                notificationId = notificationId,
                eventType = NotificationActivityEventType.NOTIFICATION_CREATED,
                actorUserId = actorId,
                timestamp = now,
                description = "Notification $notificationNo created for recipient $recipientUserId."
            )
        )

        DomainResult.Success(notification)
    }

    override suspend fun getNotification(
        projectId: String,
        notificationId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Notification> = mutex.withLock {
        val notification = notificationDataSource.getNotificationById(projectId, notificationId)
            ?: return@withLock DomainResult.Error(message = "Notification '$notificationId' not found.")

        val authResult = NotificationAuthorizationValidator.validateNotificationView(
            notification = notification,
            requestProjectId = projectId,
            actorId = actorId,
            callerRole = callerRole
        )
        if (authResult is DomainResult.Error) return@withLock authResult

        DomainResult.Success(notification)
    }

    override suspend fun getByNotificationNo(
        projectId: String,
        notificationNo: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Notification> = mutex.withLock {
        val notification = notificationDataSource.getNotificationByNo(projectId, notificationNo)
            ?: return@withLock DomainResult.Error(message = "Notification with number '$notificationNo' not found.")

        val authResult = NotificationAuthorizationValidator.validateNotificationView(
            notification = notification,
            requestProjectId = projectId,
            actorId = actorId,
            callerRole = callerRole
        )
        if (authResult is DomainResult.Error) return@withLock authResult

        DomainResult.Success(notification)
    }

    override suspend fun getUserNotifications(
        projectId: String,
        targetUserId: String,
        category: NotificationCategory?,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<Notification>> = mutex.withLock {
        val authResult = NotificationAuthorizationValidator.validateUserQuery(targetUserId, projectId, actorId, callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        val all = notificationDataSource.observeNotificationsByUser(projectId, targetUserId).first()
        val filtered = if (category != null) {
            all.filter { it.notificationType.category == category }
        } else all

        DomainResult.Success(filtered)
    }

    override suspend fun getUnreadNotifications(
        projectId: String,
        targetUserId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<Notification>> = mutex.withLock {
        val authResult = NotificationAuthorizationValidator.validateUserQuery(targetUserId, projectId, actorId, callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        val all = notificationDataSource.observeNotificationsByUser(projectId, targetUserId).first()
        val unread = all.filter { !it.isRead && it.status != NotificationStatus.CANCELLED }

        DomainResult.Success(unread)
    }

    override fun observeUserNotifications(
        projectId: String,
        targetUserId: String,
        callerRole: UserRole
    ): Flow<List<Notification>> {
        return notificationDataSource.observeNotificationsByUser(projectId, targetUserId)
    }

    override fun observeUnreadCount(
        projectId: String,
        targetUserId: String,
        callerRole: UserRole
    ): Flow<Int> {
        return notificationDataSource.observeNotificationsByUser(projectId, targetUserId)
            .map { list -> list.count { !it.isRead && it.status != NotificationStatus.CANCELLED } }
    }

    override suspend fun getByReference(
        projectId: String,
        referenceType: String,
        referenceId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<Notification>> = mutex.withLock {
        val list = notificationDataSource.getNotificationsByReference(projectId, referenceType, referenceId)
        DomainResult.Success(list)
    }

    override suspend fun markQueued(
        projectId: String,
        notificationId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Notification> = mutex.withLock {
        val current = notificationDataSource.getNotificationById(projectId, notificationId)
            ?: return@withLock DomainResult.Error(message = "Notification '$notificationId' not found.")

        val transResult = NotificationLifecycleValidator.validateTransition(current.status, NotificationStatus.QUEUED)
        if (transResult is DomainResult.Error) return@withLock transResult

        val now = System.currentTimeMillis()
        val updated = current.copy(
            status = NotificationStatus.QUEUED,
            queuedAt = now,
            updatedAt = now
        )
        notificationDataSource.saveNotification(updated)

        notificationDataSource.recordActivityEvent(
            NotificationActivityEvent(
                eventId = UUID.randomUUID().toString(),
                projectId = projectId,
                notificationId = notificationId,
                eventType = NotificationActivityEventType.NOTIFICATION_QUEUED,
                actorUserId = actorId,
                timestamp = now,
                description = "Notification ${updated.notificationNo} queued for delivery."
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun markProcessing(
        projectId: String,
        notificationId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Notification> = mutex.withLock {
        val current = notificationDataSource.getNotificationById(projectId, notificationId)
            ?: return@withLock DomainResult.Error(message = "Notification '$notificationId' not found.")

        val transResult = NotificationLifecycleValidator.validateTransition(current.status, NotificationStatus.PROCESSING)
        if (transResult is DomainResult.Error) return@withLock transResult

        val now = System.currentTimeMillis()
        val updated = current.copy(
            status = NotificationStatus.PROCESSING,
            processedAt = now,
            updatedAt = now
        )
        notificationDataSource.saveNotification(updated)

        notificationDataSource.recordActivityEvent(
            NotificationActivityEvent(
                eventId = UUID.randomUUID().toString(),
                projectId = projectId,
                notificationId = notificationId,
                eventType = NotificationActivityEventType.NOTIFICATION_PROCESSING,
                actorUserId = actorId,
                timestamp = now,
                description = "Notification ${updated.notificationNo} processing dispatch."
            )
        )

        // Trigger delivery service attempt
        val attempts = notificationDataSource.getDeliveryAttempts(projectId, notificationId)
        val attemptNumber = attempts.size + 1
        val deliveryResult = deliveryService.processDelivery(updated, attemptNumber)

        if (deliveryResult is DomainResult.Success) {
            val attempt = deliveryResult.data
            notificationDataSource.saveDeliveryAttempt(attempt)

            if (attempt.status == NotificationStatus.DELIVERED) {
                val deliveredNotification = updated.copy(
                    status = NotificationStatus.DELIVERED,
                    sentAt = attempt.startedAt ?: now,
                    deliveredAt = attempt.completedAt ?: now,
                    updatedAt = now
                )
                notificationDataSource.saveNotification(deliveredNotification)
                notificationDataSource.recordActivityEvent(
                    NotificationActivityEvent(
                        eventId = UUID.randomUUID().toString(),
                        projectId = projectId,
                        notificationId = notificationId,
                        eventType = NotificationActivityEventType.NOTIFICATION_DELIVERED,
                        actorUserId = actorId,
                        timestamp = now,
                        description = "Notification ${deliveredNotification.notificationNo} delivered successfully via ${attempt.channel.defaultLabel}."
                    )
                )
                return@withLock DomainResult.Success(deliveredNotification)
            } else if (attempt.status == NotificationStatus.FAILED) {
                val failedNotification = updated.copy(
                    status = NotificationStatus.FAILED,
                    failureReason = attempt.failureMessage,
                    updatedAt = now
                )
                notificationDataSource.saveNotification(failedNotification)
                notificationDataSource.recordActivityEvent(
                    NotificationActivityEvent(
                        eventId = UUID.randomUUID().toString(),
                        projectId = projectId,
                        notificationId = notificationId,
                        eventType = NotificationActivityEventType.NOTIFICATION_FAILED,
                        actorUserId = actorId,
                        timestamp = now,
                        description = "Notification ${failedNotification.notificationNo} delivery failed: ${attempt.failureMessage}."
                    )
                )
                return@withLock DomainResult.Success(failedNotification)
            }
        }

        DomainResult.Success(updated)
    }

    override suspend fun markSent(
        projectId: String,
        notificationId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Notification> = mutex.withLock {
        val current = notificationDataSource.getNotificationById(projectId, notificationId)
            ?: return@withLock DomainResult.Error(message = "Notification '$notificationId' not found.")

        val transResult = NotificationLifecycleValidator.validateTransition(current.status, NotificationStatus.SENT)
        if (transResult is DomainResult.Error) return@withLock transResult

        val now = System.currentTimeMillis()
        val updated = current.copy(
            status = NotificationStatus.SENT,
            sentAt = now,
            updatedAt = now
        )
        notificationDataSource.saveNotification(updated)

        notificationDataSource.recordActivityEvent(
            NotificationActivityEvent(
                eventId = UUID.randomUUID().toString(),
                projectId = projectId,
                notificationId = notificationId,
                eventType = NotificationActivityEventType.NOTIFICATION_SENT,
                actorUserId = actorId,
                timestamp = now,
                description = "Notification ${updated.notificationNo} sent to channel ${updated.channel.defaultLabel}."
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun markDelivered(
        projectId: String,
        notificationId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Notification> = mutex.withLock {
        val current = notificationDataSource.getNotificationById(projectId, notificationId)
            ?: return@withLock DomainResult.Error(message = "Notification '$notificationId' not found.")

        val transResult = NotificationLifecycleValidator.validateTransition(current.status, NotificationStatus.DELIVERED)
        if (transResult is DomainResult.Error) return@withLock transResult

        val now = System.currentTimeMillis()
        val updated = current.copy(
            status = NotificationStatus.DELIVERED,
            deliveredAt = now,
            updatedAt = now
        )
        notificationDataSource.saveNotification(updated)

        notificationDataSource.recordActivityEvent(
            NotificationActivityEvent(
                eventId = UUID.randomUUID().toString(),
                projectId = projectId,
                notificationId = notificationId,
                eventType = NotificationActivityEventType.NOTIFICATION_DELIVERED,
                actorUserId = actorId,
                timestamp = now,
                description = "Notification ${updated.notificationNo} marked as delivered."
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun markRead(
        projectId: String,
        notificationId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Notification> = mutex.withLock {
        val current = notificationDataSource.getNotificationById(projectId, notificationId)
            ?: return@withLock DomainResult.Error(message = "Notification '$notificationId' not found.")

        val authResult = NotificationAuthorizationValidator.validateNotificationView(current, projectId, actorId, callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        if (current.status == NotificationStatus.READ) {
            return@withLock DomainResult.Success(current)
        }

        val transResult = NotificationLifecycleValidator.validateTransition(current.status, NotificationStatus.READ)
        if (transResult is DomainResult.Error) return@withLock transResult

        val now = System.currentTimeMillis()
        val updated = current.copy(
            status = NotificationStatus.READ,
            readAt = now,
            updatedAt = now
        )
        notificationDataSource.saveNotification(updated)

        notificationDataSource.recordActivityEvent(
            NotificationActivityEvent(
                eventId = UUID.randomUUID().toString(),
                projectId = projectId,
                notificationId = notificationId,
                eventType = NotificationActivityEventType.NOTIFICATION_READ,
                actorUserId = actorId,
                timestamp = now,
                description = "Notification ${updated.notificationNo} read by user $actorId."
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun markAllAsRead(
        projectId: String,
        targetUserId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Int> = mutex.withLock {
        val authResult = NotificationAuthorizationValidator.validateUserQuery(targetUserId, projectId, actorId, callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        val userNotifications = notificationDataSource.observeNotificationsByUser(projectId, targetUserId).first()
        var updatedCount = 0
        val now = System.currentTimeMillis()

        for (n in userNotifications) {
            if (!n.isRead && n.status != NotificationStatus.CANCELLED) {
                val updated = n.copy(
                    status = NotificationStatus.READ,
                    readAt = now,
                    updatedAt = now
                )
                notificationDataSource.saveNotification(updated)
                updatedCount++
            }
        }

        DomainResult.Success(updatedCount)
    }

    override suspend fun retryNotification(
        projectId: String,
        notificationId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Notification> = mutex.withLock {
        val current = notificationDataSource.getNotificationById(projectId, notificationId)
            ?: return@withLock DomainResult.Error(message = "Notification '$notificationId' not found.")

        if (current.status != NotificationStatus.FAILED) {
            return@withLock DomainResult.Error(message = "Only FAILED notifications can be retried. Current status: ${current.status.defaultLabel}")
        }

        val now = System.currentTimeMillis()
        val queued = current.copy(
            status = NotificationStatus.QUEUED,
            queuedAt = now,
            failureReason = null,
            updatedAt = now
        )
        notificationDataSource.saveNotification(queued)

        notificationDataSource.recordActivityEvent(
            NotificationActivityEvent(
                eventId = UUID.randomUUID().toString(),
                projectId = projectId,
                notificationId = notificationId,
                eventType = NotificationActivityEventType.NOTIFICATION_RETRIED,
                actorUserId = actorId,
                timestamp = now,
                description = "Notification ${queued.notificationNo} queued for retry."
            )
        )

        // Process delivery immediately
        val attempts = notificationDataSource.getDeliveryAttempts(projectId, notificationId)
        val attemptNumber = attempts.size + 1
        val deliveryResult = deliveryService.processDelivery(queued, attemptNumber)

        if (deliveryResult is DomainResult.Success) {
            val attempt = deliveryResult.data
            notificationDataSource.saveDeliveryAttempt(attempt)

            if (attempt.status == NotificationStatus.DELIVERED) {
                val delivered = queued.copy(
                    status = NotificationStatus.DELIVERED,
                    sentAt = attempt.startedAt ?: now,
                    deliveredAt = attempt.completedAt ?: now,
                    updatedAt = now
                )
                notificationDataSource.saveNotification(delivered)
                return@withLock DomainResult.Success(delivered)
            } else if (attempt.status == NotificationStatus.FAILED) {
                val failed = queued.copy(
                    status = NotificationStatus.FAILED,
                    failureReason = attempt.failureMessage,
                    updatedAt = now
                )
                notificationDataSource.saveNotification(failed)
                return@withLock DomainResult.Success(failed)
            }
        }

        DomainResult.Success(queued)
    }

    override suspend fun cancelNotification(
        projectId: String,
        notificationId: String,
        reason: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Notification> = mutex.withLock {
        val current = notificationDataSource.getNotificationById(projectId, notificationId)
            ?: return@withLock DomainResult.Error(message = "Notification '$notificationId' not found.")

        val transResult = NotificationLifecycleValidator.validateTransition(current.status, NotificationStatus.CANCELLED)
        if (transResult is DomainResult.Error) return@withLock transResult

        val now = System.currentTimeMillis()
        val updated = current.copy(
            status = NotificationStatus.CANCELLED,
            cancelledAt = now,
            failureReason = reason,
            updatedAt = now
        )
        notificationDataSource.saveNotification(updated)

        notificationDataSource.recordActivityEvent(
            NotificationActivityEvent(
                eventId = UUID.randomUUID().toString(),
                projectId = projectId,
                notificationId = notificationId,
                eventType = NotificationActivityEventType.NOTIFICATION_CANCELLED,
                actorUserId = actorId,
                timestamp = now,
                description = "Notification ${updated.notificationNo} cancelled: $reason."
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun getDeliveryAttempts(
        projectId: String,
        notificationId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<NotificationDeliveryAttempt>> = mutex.withLock {
        val attempts = notificationDataSource.getDeliveryAttempts(projectId, notificationId)
        DomainResult.Success(attempts)
    }

    override suspend fun getSummary(
        projectId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<NotificationSummary> = mutex.withLock {
        val authResult = NotificationAuthorizationValidator.validateAdminDashboardAccess(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        val all = notificationDataSource.observeNotificationsByProject(projectId).first()

        val total = all.size
        val unread = all.count { !it.isRead && it.status != NotificationStatus.CANCELLED }
        val read = all.count { it.isRead }
        val queued = all.count { it.status == NotificationStatus.QUEUED }
        val processing = all.count { it.status == NotificationStatus.PROCESSING }
        val sent = all.count { it.status == NotificationStatus.SENT }
        val delivered = all.count { it.status == NotificationStatus.DELIVERED }
        val failed = all.count { it.status == NotificationStatus.FAILED }
        val cancelled = all.count { it.status == NotificationStatus.CANCELLED }

        val byPriority = all.groupingBy { it.priority }.eachCount()
        val byType = all.groupingBy { it.notificationType }.eachCount()
        val byChannel = all.groupingBy { it.channel }.eachCount()

        val summary = NotificationSummary(
            projectId = projectId,
            totalCount = total,
            unreadCount = unread,
            readCount = read,
            queuedCount = queued,
            processingCount = processing,
            sentCount = sent,
            deliveredCount = delivered,
            failedCount = failed,
            cancelledCount = cancelled,
            countsByPriority = byPriority,
            countsByType = byType,
            countsByChannel = byChannel
        )

        DomainResult.Success(summary)
    }

    override suspend fun createPreference(
        preference: NotificationPreference,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<NotificationPreference> = mutex.withLock {
        notificationDataSource.savePreference(preference)

        notificationDataSource.recordActivityEvent(
            NotificationActivityEvent(
                eventId = UUID.randomUUID().toString(),
                projectId = preference.projectId,
                notificationId = "PREF-${preference.preferenceId}",
                eventType = NotificationActivityEventType.PREFERENCE_CREATED,
                actorUserId = actorId,
                description = "Preference created for type ${preference.notificationType.name} and channel ${preference.channel.name}."
            )
        )

        DomainResult.Success(preference)
    }

    override suspend fun updatePreference(
        projectId: String,
        preferenceId: String,
        enabled: Boolean,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<NotificationPreference> = mutex.withLock {
        val existing = notificationDataSource.getPreference(projectId, preferenceId)
            ?: return@withLock DomainResult.Error(message = "Preference '$preferenceId' not found.")

        if (existing.channel == NotificationChannel.IN_APP && existing.notificationType.isMandatory && !enabled) {
            return@withLock DomainResult.Error(message = "Mandatory in-app notifications cannot be disabled.")
        }

        val updated = existing.copy(
            enabled = enabled,
            updatedAt = System.currentTimeMillis()
        )
        notificationDataSource.savePreference(updated)

        notificationDataSource.recordActivityEvent(
            NotificationActivityEvent(
                eventId = UUID.randomUUID().toString(),
                projectId = projectId,
                notificationId = "PREF-$preferenceId",
                eventType = NotificationActivityEventType.PREFERENCE_UPDATED,
                actorUserId = actorId,
                description = "Preference $preferenceId updated: enabled=$enabled."
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun getPreferences(
        projectId: String,
        targetUserId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<NotificationPreference>> = mutex.withLock {
        val authResult = NotificationAuthorizationValidator.validateUserQuery(targetUserId, projectId, actorId, callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        val prefs = notificationDataSource.getPreferencesByUser(projectId, targetUserId)
        DomainResult.Success(prefs)
    }

    override suspend fun createTemplate(
        template: NotificationTemplate,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<NotificationTemplate> = mutex.withLock {
        val authResult = NotificationAuthorizationValidator.validateTemplateManagement(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        notificationDataSource.saveTemplate(template)

        notificationDataSource.recordActivityEvent(
            NotificationActivityEvent(
                eventId = UUID.randomUUID().toString(),
                projectId = template.projectId,
                notificationId = "TMPL-${template.templateId}",
                eventType = NotificationActivityEventType.TEMPLATE_CREATED,
                actorUserId = actorId,
                description = "Template ${template.templateCode} v${template.version} created."
            )
        )

        DomainResult.Success(template)
    }

    override suspend fun createTemplateVersion(
        projectId: String,
        templateCode: String,
        titleTemplate: String,
        messageTemplate: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<NotificationTemplate> = mutex.withLock {
        val authResult = NotificationAuthorizationValidator.validateTemplateManagement(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        val existingVersions = notificationDataSource.getTemplatesByCode(projectId, templateCode)
        if (existingVersions.isEmpty()) {
            return@withLock DomainResult.Error(message = "Template code '$templateCode' does not exist. Create base template first.")
        }
        val latest = existingVersions.maxByOrNull { it.version }!!
        val now = System.currentTimeMillis()

        val newVersion = latest.copy(
            templateId = UUID.randomUUID().toString(),
            titleTemplate = titleTemplate,
            messageTemplate = messageTemplate,
            version = latest.version + 1,
            createdBy = actorId,
            createdAt = now,
            updatedAt = now
        )

        notificationDataSource.saveTemplate(newVersion)

        notificationDataSource.recordActivityEvent(
            NotificationActivityEvent(
                eventId = UUID.randomUUID().toString(),
                projectId = projectId,
                notificationId = "TMPL-${newVersion.templateId}",
                eventType = NotificationActivityEventType.TEMPLATE_VERSION_CREATED,
                actorUserId = actorId,
                timestamp = now,
                description = "Template $templateCode upgraded to version ${newVersion.version}."
            )
        )

        DomainResult.Success(newVersion)
    }

    override suspend fun getTemplate(
        projectId: String,
        templateId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<NotificationTemplate> = mutex.withLock {
        val template = notificationDataSource.getTemplateById(projectId, templateId)
            ?: return@withLock DomainResult.Error(message = "Template '$templateId' not found.")
        DomainResult.Success(template)
    }

    override suspend fun getTemplates(
        projectId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<NotificationTemplate>> = mutex.withLock {
        val templates = notificationDataSource.getAllTemplates(projectId)
        DomainResult.Success(templates)
    }

    override suspend fun getActivityEvents(
        projectId: String,
        notificationId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<NotificationActivityEvent>> = mutex.withLock {
        val events = notificationDataSource.getActivityEvents(projectId, notificationId)
        DomainResult.Success(events)
    }

    override fun observeActivityEvents(
        projectId: String,
        callerRole: UserRole
    ): Flow<List<NotificationActivityEvent>> {
        return notificationDataSource.observeActivityEvents(projectId)
    }
}

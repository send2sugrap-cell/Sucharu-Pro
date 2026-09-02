package com.sucharu.sucharupro.domain.repository.notification

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.notification.*
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for the canonical Notification System (Module 10 Step 01).
 */
interface NotificationRepository {

    suspend fun createNotification(
        projectId: String,
        recipientUserId: String,
        recipientType: String = "USER",
        notificationType: NotificationType,
        channel: NotificationChannel = NotificationChannel.IN_APP,
        priority: NotificationPriority = NotificationPriority.NORMAL,
        title: String,
        message: String,
        referenceType: String? = null,
        referenceId: String? = null,
        templateId: String? = null,
        groupKey: String? = null,
        idempotencyKey: String? = null,
        metadata: Map<String, String> = emptyMap(),
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Notification>

    suspend fun getNotification(
        projectId: String,
        notificationId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Notification>

    suspend fun getByNotificationNo(
        projectId: String,
        notificationNo: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Notification>

    suspend fun getUserNotifications(
        projectId: String,
        targetUserId: String,
        category: NotificationCategory? = null,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<Notification>>

    suspend fun getUnreadNotifications(
        projectId: String,
        targetUserId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<Notification>>

    fun observeUserNotifications(
        projectId: String,
        targetUserId: String,
        callerRole: UserRole
    ): Flow<List<Notification>>

    fun observeUnreadCount(
        projectId: String,
        targetUserId: String,
        callerRole: UserRole
    ): Flow<Int>

    suspend fun getByReference(
        projectId: String,
        referenceType: String,
        referenceId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<Notification>>

    suspend fun markQueued(
        projectId: String,
        notificationId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Notification>

    suspend fun markProcessing(
        projectId: String,
        notificationId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Notification>

    suspend fun markSent(
        projectId: String,
        notificationId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Notification>

    suspend fun markDelivered(
        projectId: String,
        notificationId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Notification>

    suspend fun markRead(
        projectId: String,
        notificationId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Notification>

    suspend fun markAllAsRead(
        projectId: String,
        targetUserId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Int>

    suspend fun retryNotification(
        projectId: String,
        notificationId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Notification>

    suspend fun cancelNotification(
        projectId: String,
        notificationId: String,
        reason: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Notification>

    suspend fun getDeliveryAttempts(
        projectId: String,
        notificationId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<NotificationDeliveryAttempt>>

    suspend fun getSummary(
        projectId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<NotificationSummary>

    suspend fun createPreference(
        preference: NotificationPreference,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<NotificationPreference>

    suspend fun updatePreference(
        projectId: String,
        preferenceId: String,
        enabled: Boolean,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<NotificationPreference>

    suspend fun getPreferences(
        projectId: String,
        targetUserId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<NotificationPreference>>

    suspend fun createTemplate(
        template: NotificationTemplate,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<NotificationTemplate>

    suspend fun createTemplateVersion(
        projectId: String,
        templateCode: String,
        titleTemplate: String,
        messageTemplate: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<NotificationTemplate>

    suspend fun getTemplate(
        projectId: String,
        templateId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<NotificationTemplate>

    suspend fun getTemplates(
        projectId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<NotificationTemplate>>

    suspend fun getActivityEvents(
        projectId: String,
        notificationId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<NotificationActivityEvent>>

    fun observeActivityEvents(
        projectId: String,
        callerRole: UserRole
    ): Flow<List<NotificationActivityEvent>>
}

package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.communication.customer.InMemoryCustomerCommunicationScheduler
import com.sucharu.sucharupro.data.datasource.CustomerCommunicationDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.customer.*
import com.sucharu.sucharupro.domain.model.notification.NotificationChannel
import com.sucharu.sucharupro.domain.model.notification.NotificationPriority
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.communication.customer.CustomerCommunicationRepository
import com.sucharu.sucharupro.domain.repository.notification.NotificationRepository
import com.sucharu.sucharupro.domain.validation.communication.customer.CustomerCommunicationAuthorizationValidator
import com.sucharu.sucharupro.domain.validation.communication.customer.CustomerCommunicationLifecycleValidator
import com.sucharu.sucharupro.domain.validation.communication.customer.CustomerCommunicationValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Production-grade repository implementation for Customer Communication & Engagement Management (Module 10 Step 02).
 *
 * Integrates with canonical [NotificationRepository] from Module 10 Step 01.
 */
class CustomerCommunicationRepositoryImpl(
    private val dataSource: CustomerCommunicationDataSource,
    private val notificationRepository: NotificationRepository,
    private val scheduler: CustomerCommunicationScheduler = InMemoryCustomerCommunicationScheduler()
) : CustomerCommunicationRepository {

    private val mutex = Mutex()

    override suspend fun createCommunication(
        projectId: String,
        customerId: String,
        recipientUserId: String?,
        communicationType: CustomerCommunicationType,
        channel: NotificationChannel,
        priority: NotificationPriority,
        title: String,
        message: String,
        referenceType: String?,
        referenceId: String?,
        scheduledAt: Long?,
        groupKey: String?,
        idempotencyKey: String?,
        metadata: Map<String, String>,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerCommunication> = mutex.withLock {
        val recipientRes = CustomerCommunicationRecipientResolver.resolve(projectId, customerId, recipientUserId)
        if (recipientRes is DomainResult.Error) return@withLock recipientRes
        val (cId, rUserId, pId) = (recipientRes as DomainResult.Success).data

        // 1. Idempotency Check
        if (!idempotencyKey.isNullOrBlank()) {
            val existing = dataSource.getByIdempotencyKey(pId, idempotencyKey)
            if (existing != null) {
                return@withLock DomainResult.Success(existing)
            }
        }

        // 2. Duplicate Detection
        val duplicate = dataSource.getByDuplicateCriteria(
            projectId = pId,
            customerId = cId,
            communicationType = communicationType,
            referenceType = referenceType,
            referenceId = referenceId,
            groupKey = groupKey
        )
        if (duplicate != null) {
            return@withLock DomainResult.Success(duplicate)
        }

        // 3. Create Canonical Notification in Module 10 Step 01
        val notifRes = notificationRepository.createNotification(
            projectId = pId,
            recipientUserId = rUserId,
            recipientType = "CUSTOMER",
            notificationType = communicationType.canonicalNotificationType,
            channel = channel,
            priority = priority,
            title = title,
            message = message,
            referenceType = referenceType,
            referenceId = referenceId,
            groupKey = groupKey,
            idempotencyKey = idempotencyKey?.let { "NOTIF-$it" },
            metadata = metadata,
            actorId = actorId,
            callerRole = callerRole
        )
        if (notifRes is DomainResult.Error) return@withLock notifRes
        val canonicalNotif = (notifRes as DomainResult.Success).data

        // 4. Create Customer Communication Aggregate Root
        val communicationId = UUID.randomUUID().toString()
        val communicationNo = dataSource.generateCommunicationNumber(pId)
        val now = System.currentTimeMillis()

        val initialStatus = if (scheduledAt != null && scheduledAt > now) {
            CustomerCommunicationStatus.SCHEDULED
        } else {
            CustomerCommunicationStatus.DRAFT
        }

        val communication = CustomerCommunication(
            communicationId = communicationId,
            communicationNo = communicationNo,
            projectId = pId,
            customerId = cId,
            recipientUserId = rUserId,
            communicationType = communicationType,
            notificationId = canonicalNotif.notificationId,
            title = title,
            message = message,
            referenceType = referenceType,
            referenceId = referenceId,
            priority = priority,
            status = initialStatus,
            scheduledAt = scheduledAt,
            groupKey = groupKey,
            idempotencyKey = idempotencyKey,
            createdBy = actorId,
            createdAt = now,
            updatedAt = now,
            metadata = metadata
        )

        val validationResult = CustomerCommunicationValidator.validate(communication)
        if (validationResult is DomainResult.Error) return@withLock validationResult

        dataSource.saveCommunication(communication)

        // Record history
        dataSource.recordHistory(
            CustomerCommunicationHistory(
                historyId = UUID.randomUUID().toString(),
                projectId = pId,
                customerId = cId,
                communicationId = communicationId,
                eventType = "COMMUNICATION_CREATED",
                previousStatus = null,
                newStatus = initialStatus,
                actorUserId = actorId,
                timestamp = now,
                notes = "Customer communication $communicationNo created."
            )
        )

        if (initialStatus == CustomerCommunicationStatus.SCHEDULED && scheduledAt != null) {
            scheduler.schedule(pId, communicationId, scheduledAt)
        }

        DomainResult.Success(communication)
    }

    override suspend fun getCommunication(
        projectId: String,
        communicationId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerCommunication> = mutex.withLock {
        val comm = dataSource.getCommunicationById(projectId, communicationId)
            ?: return@withLock DomainResult.Error(message = "Customer communication '$communicationId' not found.")

        val authResult = CustomerCommunicationAuthorizationValidator.validateView(comm, projectId, actorId, callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        DomainResult.Success(comm)
    }

    override suspend fun getByCommunicationNo(
        projectId: String,
        communicationNo: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerCommunication> = mutex.withLock {
        val comm = dataSource.getCommunicationByNo(projectId, communicationNo)
            ?: return@withLock DomainResult.Error(message = "Customer communication with number '$communicationNo' not found.")

        val authResult = CustomerCommunicationAuthorizationValidator.validateView(comm, projectId, actorId, callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        DomainResult.Success(comm)
    }

    override suspend fun getCustomerCommunications(
        projectId: String,
        targetCustomerId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<CustomerCommunication>> = mutex.withLock {
        val authResult = CustomerCommunicationAuthorizationValidator.validateCustomerQuery(targetCustomerId, projectId, actorId, callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        val list = dataSource.observeCommunicationsByCustomer(projectId, targetCustomerId).first()
        DomainResult.Success(list)
    }

    override suspend fun getUnreadCommunications(
        projectId: String,
        targetCustomerId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<CustomerCommunication>> = mutex.withLock {
        val authResult = CustomerCommunicationAuthorizationValidator.validateCustomerQuery(targetCustomerId, projectId, actorId, callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        val all = dataSource.observeCommunicationsByCustomer(projectId, targetCustomerId).first()
        val unread = all.filter { !it.isRead && it.status != CustomerCommunicationStatus.CANCELLED }
        DomainResult.Success(unread)
    }

    override fun observeCustomerCommunications(
        projectId: String,
        targetCustomerId: String,
        callerRole: UserRole
    ): Flow<List<CustomerCommunication>> {
        return dataSource.observeCommunicationsByCustomer(projectId, targetCustomerId)
    }

    override fun observeUnreadCount(
        projectId: String,
        targetCustomerId: String,
        callerRole: UserRole
    ): Flow<Int> {
        return dataSource.observeCommunicationsByCustomer(projectId, targetCustomerId)
            .map { list -> list.count { !it.isRead && it.status != CustomerCommunicationStatus.CANCELLED } }
    }

    override suspend fun scheduleCommunication(
        projectId: String,
        communicationId: String,
        scheduledAt: Long,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerCommunication> = mutex.withLock {
        val current = dataSource.getCommunicationById(projectId, communicationId)
            ?: return@withLock DomainResult.Error(message = "Communication '$communicationId' not found.")

        val transResult = CustomerCommunicationLifecycleValidator.validateTransition(current.status, CustomerCommunicationStatus.SCHEDULED)
        if (transResult is DomainResult.Error) return@withLock transResult

        val now = System.currentTimeMillis()
        val updated = current.copy(
            status = CustomerCommunicationStatus.SCHEDULED,
            scheduledAt = scheduledAt,
            updatedAt = now
        )
        dataSource.saveCommunication(updated)
        scheduler.schedule(projectId, communicationId, scheduledAt)

        dataSource.recordHistory(
            CustomerCommunicationHistory(
                historyId = UUID.randomUUID().toString(),
                projectId = projectId,
                customerId = current.customerId,
                communicationId = communicationId,
                eventType = "COMMUNICATION_SCHEDULED",
                previousStatus = current.status,
                newStatus = CustomerCommunicationStatus.SCHEDULED,
                actorUserId = actorId,
                timestamp = now,
                notes = "Communication scheduled for timestamp $scheduledAt."
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun queueCommunication(
        projectId: String,
        communicationId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerCommunication> = mutex.withLock {
        val current = dataSource.getCommunicationById(projectId, communicationId)
            ?: return@withLock DomainResult.Error(message = "Communication '$communicationId' not found.")

        val transResult = CustomerCommunicationLifecycleValidator.validateTransition(current.status, CustomerCommunicationStatus.QUEUED)
        if (transResult is DomainResult.Error) return@withLock transResult

        val now = System.currentTimeMillis()
        val updated = current.copy(
            status = CustomerCommunicationStatus.QUEUED,
            updatedAt = now
        )
        dataSource.saveCommunication(updated)

        // Queue in Canonical Notification Engine
        notificationRepository.markQueued(projectId, current.notificationId, actorId, callerRole)
        val notifProc = notificationRepository.markProcessing(projectId, current.notificationId, actorId, callerRole)

        val finalStatus = if (notifProc is DomainResult.Success && notifProc.data.isDelivered) {
            CustomerCommunicationStatus.DELIVERED
        } else if (notifProc is DomainResult.Success && notifProc.data.isFailed) {
            CustomerCommunicationStatus.FAILED
        } else {
            CustomerCommunicationStatus.SENT
        }

        val dispatched = updated.copy(
            status = finalStatus,
            sentAt = now,
            deliveredAt = if (finalStatus == CustomerCommunicationStatus.DELIVERED) now else null,
            failureReason = if (finalStatus == CustomerCommunicationStatus.FAILED) {
                (notifProc as? DomainResult.Error)?.message ?: "Notification delivery failed"
            } else null,
            updatedAt = now
        )
        dataSource.saveCommunication(dispatched)

        dataSource.recordHistory(
            CustomerCommunicationHistory(
                historyId = UUID.randomUUID().toString(),
                projectId = projectId,
                customerId = current.customerId,
                communicationId = communicationId,
                eventType = "COMMUNICATION_DISPATCHED",
                previousStatus = current.status,
                newStatus = finalStatus,
                actorUserId = actorId,
                timestamp = now,
                notes = "Communication dispatched via Notification engine with status ${finalStatus.name}."
            )
        )

        DomainResult.Success(dispatched)
    }

    override suspend fun markRead(
        projectId: String,
        communicationId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerCommunication> = mutex.withLock {
        val current = dataSource.getCommunicationById(projectId, communicationId)
            ?: return@withLock DomainResult.Error(message = "Communication '$communicationId' not found.")

        val authResult = CustomerCommunicationAuthorizationValidator.validateView(current, projectId, actorId, callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        if (current.isRead) {
            return@withLock DomainResult.Success(current)
        }

        val transResult = CustomerCommunicationLifecycleValidator.validateTransition(current.status, CustomerCommunicationStatus.READ)
        if (transResult is DomainResult.Error) return@withLock transResult

        val now = System.currentTimeMillis()
        val updated = current.copy(
            status = CustomerCommunicationStatus.READ,
            readAt = now,
            updatedAt = now
        )
        dataSource.saveCommunication(updated)

        // Mark in Canonical Notification Engine
        notificationRepository.markRead(projectId, current.notificationId, actorId, callerRole)

        // Record history & engagement
        dataSource.recordHistory(
            CustomerCommunicationHistory(
                historyId = UUID.randomUUID().toString(),
                projectId = projectId,
                customerId = current.customerId,
                communicationId = communicationId,
                eventType = "COMMUNICATION_READ",
                previousStatus = current.status,
                newStatus = CustomerCommunicationStatus.READ,
                actorUserId = actorId,
                timestamp = now,
                notes = "Communication marked as read."
            )
        )

        dataSource.recordEngagementEvent(
            CustomerEngagementEvent(
                eventId = UUID.randomUUID().toString(),
                projectId = projectId,
                customerId = current.customerId,
                communicationId = communicationId,
                eventType = CustomerEngagementEventType.COMMUNICATION_READ,
                timestamp = now
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun markAcknowledged(
        projectId: String,
        communicationId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerCommunication> = mutex.withLock {
        val current = dataSource.getCommunicationById(projectId, communicationId)
            ?: return@withLock DomainResult.Error(message = "Communication '$communicationId' not found.")

        val authResult = CustomerCommunicationAuthorizationValidator.validateView(current, projectId, actorId, callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        if (current.status == CustomerCommunicationStatus.ACKNOWLEDGED) {
            return@withLock DomainResult.Success(current)
        }

        val transResult = CustomerCommunicationLifecycleValidator.validateTransition(current.status, CustomerCommunicationStatus.ACKNOWLEDGED)
        if (transResult is DomainResult.Error) return@withLock transResult

        val now = System.currentTimeMillis()
        val updated = current.copy(
            status = CustomerCommunicationStatus.ACKNOWLEDGED,
            acknowledgedAt = now,
            readAt = current.readAt ?: now,
            updatedAt = now
        )
        dataSource.saveCommunication(updated)

        // Mark read in Canonical Notification Engine if not read
        notificationRepository.markRead(projectId, current.notificationId, actorId, callerRole)

        // Record history & engagement
        dataSource.recordHistory(
            CustomerCommunicationHistory(
                historyId = UUID.randomUUID().toString(),
                projectId = projectId,
                customerId = current.customerId,
                communicationId = communicationId,
                eventType = "COMMUNICATION_ACKNOWLEDGED",
                previousStatus = current.status,
                newStatus = CustomerCommunicationStatus.ACKNOWLEDGED,
                actorUserId = actorId,
                timestamp = now,
                notes = "Customer acknowledged communication."
            )
        )

        dataSource.recordEngagementEvent(
            CustomerEngagementEvent(
                eventId = UUID.randomUUID().toString(),
                projectId = projectId,
                customerId = current.customerId,
                communicationId = communicationId,
                eventType = CustomerEngagementEventType.COMMUNICATION_ACKNOWLEDGED,
                timestamp = now
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun cancelCommunication(
        projectId: String,
        communicationId: String,
        reason: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerCommunication> = mutex.withLock {
        val current = dataSource.getCommunicationById(projectId, communicationId)
            ?: return@withLock DomainResult.Error(message = "Communication '$communicationId' not found.")

        val transResult = CustomerCommunicationLifecycleValidator.validateTransition(current.status, CustomerCommunicationStatus.CANCELLED)
        if (transResult is DomainResult.Error) return@withLock transResult

        val now = System.currentTimeMillis()
        val updated = current.copy(
            status = CustomerCommunicationStatus.CANCELLED,
            cancelledAt = now,
            failureReason = reason,
            updatedAt = now
        )
        dataSource.saveCommunication(updated)
        scheduler.cancelScheduled(projectId, communicationId)

        // Cancel in canonical Notification Engine
        notificationRepository.cancelNotification(projectId, current.notificationId, reason, actorId, callerRole)

        dataSource.recordHistory(
            CustomerCommunicationHistory(
                historyId = UUID.randomUUID().toString(),
                projectId = projectId,
                customerId = current.customerId,
                communicationId = communicationId,
                eventType = "COMMUNICATION_CANCELLED",
                previousStatus = current.status,
                newStatus = CustomerCommunicationStatus.CANCELLED,
                actorUserId = actorId,
                timestamp = now,
                notes = "Communication cancelled: $reason."
            )
        )

        DomainResult.Success(updated)
    }

    override suspend fun getHistory(
        projectId: String,
        communicationId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<CustomerCommunicationHistory>> = mutex.withLock {
        val history = dataSource.getHistory(projectId, communicationId)
        DomainResult.Success(history)
    }

    override suspend fun recordEngagement(
        projectId: String,
        customerId: String,
        communicationId: String,
        eventType: CustomerEngagementEventType,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerEngagementEvent> = mutex.withLock {
        val event = CustomerEngagementEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = projectId,
            customerId = customerId,
            communicationId = communicationId,
            eventType = eventType,
            timestamp = System.currentTimeMillis()
        )
        dataSource.recordEngagementEvent(event)
        DomainResult.Success(event)
    }

    override suspend fun getEngagementEvents(
        projectId: String,
        targetCustomerId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<CustomerEngagementEvent>> = mutex.withLock {
        val authResult = CustomerCommunicationAuthorizationValidator.validateCustomerQuery(targetCustomerId, projectId, actorId, callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        val events = dataSource.getEngagementEventsByCustomer(projectId, targetCustomerId)
        DomainResult.Success(events)
    }

    override suspend fun getEngagementSummary(
        projectId: String,
        targetCustomerId: String?,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerEngagementSummary> = mutex.withLock {
        val allComms = if (targetCustomerId != null) {
            dataSource.observeCommunicationsByCustomer(projectId, targetCustomerId).first()
        } else {
            dataSource.observeCommunicationsByProject(projectId).first()
        }

        val sent = allComms.count { it.status == CustomerCommunicationStatus.SENT || it.status == CustomerCommunicationStatus.DELIVERED || it.isRead }
        val delivered = allComms.count { it.isDelivered }
        val read = allComms.count { it.isRead }
        val acked = allComms.count { it.isAcknowledged }

        val readRate = if (delivered > 0) (read.toDouble() / delivered) * 100.0 else 0.0
        val ackRate = if (read > 0) (acked.toDouble() / read) * 100.0 else 0.0

        val events = if (targetCustomerId != null) {
            dataSource.getEngagementEventsByCustomer(projectId, targetCustomerId)
        } else {
            dataSource.observeEngagementEvents(projectId).first()
        }

        val offerViews = events.count { it.eventType == CustomerEngagementEventType.OFFER_VIEWED }
        val annViews = events.count { it.eventType == CustomerEngagementEventType.ANNOUNCEMENT_VIEWED }
        val lastEvent = events.maxByOrNull { it.timestamp }?.timestamp

        DomainResult.Success(
            CustomerEngagementSummary(
                projectId = projectId,
                customerId = targetCustomerId,
                messagesSent = sent,
                messagesDelivered = delivered,
                messagesRead = read,
                messagesAcknowledged = acked,
                readRatePercent = Math.round(readRate * 100.0) / 100.0,
                acknowledgementRatePercent = Math.round(ackRate * 100.0) / 100.0,
                offerViews = offerViews,
                announcementViews = annViews,
                lastEngagementAt = lastEvent
            )
        )
    }

    override suspend fun getSummary(
        projectId: String,
        targetCustomerId: String?,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerCommunicationSummary> = mutex.withLock {
        val all = if (targetCustomerId != null) {
            val authResult = CustomerCommunicationAuthorizationValidator.validateCustomerQuery(targetCustomerId, projectId, actorId, callerRole)
            if (authResult is DomainResult.Error) return@withLock authResult
            dataSource.observeCommunicationsByCustomer(projectId, targetCustomerId).first()
        } else {
            dataSource.observeCommunicationsByProject(projectId).first()
        }

        val total = all.size
        val unread = all.count { !it.isRead && it.status != CustomerCommunicationStatus.CANCELLED }
        val read = all.count { it.isRead }
        val acked = all.count { it.isAcknowledged }
        val scheduled = all.count { it.status == CustomerCommunicationStatus.SCHEDULED }
        val sent = all.count { it.status == CustomerCommunicationStatus.SENT }
        val delivered = all.count { it.status == CustomerCommunicationStatus.DELIVERED }
        val failed = all.count { it.status == CustomerCommunicationStatus.FAILED }

        val byType = all.groupingBy { it.communicationType }.eachCount()
        val byPriority = all.groupingBy { it.priority }.eachCount()

        DomainResult.Success(
            CustomerCommunicationSummary(
                projectId = projectId,
                customerId = targetCustomerId,
                totalCount = total,
                unreadCount = unread,
                readCount = read,
                acknowledgedCount = acked,
                scheduledCount = scheduled,
                sentCount = sent,
                deliveredCount = delivered,
                failedCount = failed,
                countsByType = byType,
                countsByPriority = byPriority
            )
        )
    }

    override suspend fun getByReference(
        projectId: String,
        referenceType: String,
        referenceId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<CustomerCommunication>> = mutex.withLock {
        val list = dataSource.getCommunicationsByReference(projectId, referenceType, referenceId)
        DomainResult.Success(list)
    }
}

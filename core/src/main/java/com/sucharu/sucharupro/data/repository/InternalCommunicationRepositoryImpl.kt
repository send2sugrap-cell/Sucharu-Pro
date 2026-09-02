package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.communication.internal.InMemoryInternalCommunicationScheduler
import com.sucharu.sucharupro.data.datasource.InternalCommunicationDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.internal.*
import com.sucharu.sucharupro.domain.model.notification.NotificationChannel
import com.sucharu.sucharupro.domain.model.notification.NotificationPriority
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.communication.internal.InternalCommunicationRepository
import com.sucharu.sucharupro.domain.repository.notification.NotificationRepository
import com.sucharu.sucharupro.domain.validation.communication.internal.InternalCommunicationAuthorizationValidator
import com.sucharu.sucharupro.domain.validation.communication.internal.InternalCommunicationLifecycleValidator
import com.sucharu.sucharupro.domain.validation.communication.internal.InternalCommunicationValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Production-grade repository implementation for Internal Staff & Team Communication Management (Module 10 Step 03).
 */
class InternalCommunicationRepositoryImpl(
    private val dataSource: InternalCommunicationDataSource,
    private val notificationRepository: NotificationRepository,
    private val scheduler: InternalCommunicationScheduler = InMemoryInternalCommunicationScheduler()
) : InternalCommunicationRepository {

    private val mutex = Mutex()

    // ── Private unlocked implementations (called from within mutex.withLock) ──────

    private suspend fun doCreateCommunication(
        projectId: String,
        senderUserId: String,
        senderRole: UserRole,
        recipientType: InternalCommunicationRecipientType,
        recipientUserIds: Set<String> = emptySet(),
        recipientRole: UserRole? = null,
        teamId: String? = null,
        departmentId: String? = null,
        communicationType: InternalCommunicationType,
        priority: InternalCommunicationPriority = InternalCommunicationPriority.NORMAL,
        subject: String,
        message: String,
        referenceType: String? = null,
        referenceId: String? = null,
        threadId: String? = null,
        parentCommunicationId: String? = null,
        requiresAcknowledgement: Boolean = false,
        scheduledAt: Long? = null,
        idempotencyKey: String? = null,
        metadata: Map<String, String> = emptyMap(),
        actorId: String,
        callerRole: UserRole
    ): DomainResult<InternalCommunication> {
        val authResult = InternalCommunicationAuthorizationValidator.validateInternalUser(callerRole)
        if (authResult is DomainResult.Error) return authResult

        if (projectId.isBlank()) return DomainResult.Error(message = "Project ID cannot be blank.")
        if (subject.isBlank()) return DomainResult.Error(message = "Subject cannot be blank")
        if (message.isBlank()) return DomainResult.Error(message = "Message cannot be blank")

        val prohibitedKeys = setOf("api_key", "password", "secret", "token", "private_key", "auth_token")
        for (key in metadata.keys) {
            if (prohibitedKeys.any { key.contains(it, ignoreCase = true) }) {
                return DomainResult.Error(message = "Sensitive key '$key' is prohibited in communication metadata.")
            }
        }

        // 1. Idempotency Check
        if (!idempotencyKey.isNullOrBlank()) {
            val existing = dataSource.getByIdempotencyKey(projectId, idempotencyKey)
            if (existing != null) {
                return DomainResult.Success(existing)
            }
        }

        val communicationId = UUID.randomUUID().toString()
        val communicationNo = dataSource.generateCommunicationNumber(projectId)
        val now = System.currentTimeMillis()

        val initialStatus = if (scheduledAt != null && scheduledAt > now) {
            InternalCommunicationStatus.SCHEDULED
        } else {
            InternalCommunicationStatus.DRAFT
        }

        val communication = InternalCommunication(
            communicationId = communicationId,
            communicationNo = communicationNo,
            projectId = projectId,
            senderUserId = senderUserId,
            senderRole = senderRole,
            recipientType = recipientType,
            recipientUserIds = recipientUserIds,
            recipientRole = recipientRole,
            teamId = teamId,
            departmentId = departmentId,
            communicationType = communicationType,
            priority = priority,
            status = initialStatus,
            subject = subject,
            message = message,
            referenceType = referenceType,
            referenceId = referenceId,
            threadId = threadId,
            parentCommunicationId = parentCommunicationId,
            requiresAcknowledgement = requiresAcknowledgement,
            scheduledAt = scheduledAt,
            sentAt = null,
            deliveredAt = null,
            readAt = null,
            acknowledgedAt = null,
            createdBy = actorId,
            updatedBy = actorId,
            createdAt = now,
            updatedAt = now,
            idempotencyKey = idempotencyKey,
            metadata = metadata
        )

        val validationResult = InternalCommunicationValidator.validate(communication)
        if (validationResult is DomainResult.Error) return validationResult

        dataSource.saveCommunication(communication)

        // Save recipient records
        recipientUserIds.forEach { rUid ->
            dataSource.saveRecipient(
                InternalCommunicationRecipient(
                    recipientId = UUID.randomUUID().toString(),
                    communicationId = communicationId,
                    projectId = projectId,
                    recipientType = recipientType,
                    recipientUserId = rUid,
                    recipientRole = recipientRole,
                    teamId = teamId,
                    departmentId = departmentId,
                    recipientStatus = initialStatus
                )
            )
        }

        // Record Activity Event
        dataSource.recordActivityEvent(
            InternalCommunicationActivityEvent(
                eventId = UUID.randomUUID().toString(),
                projectId = projectId,
                communicationId = communicationId,
                eventType = "COMMUNICATION_CREATED",
                previousStatus = null,
                newStatus = initialStatus,
                actorUserId = actorId,
                timestamp = now,
                notes = "Communication $communicationNo created."
            )
        )

        if (initialStatus == InternalCommunicationStatus.SCHEDULED && scheduledAt != null) {
            scheduler.schedule(projectId, communicationId, scheduledAt)
        }

        return DomainResult.Success(communication)
    }

    override suspend fun createCommunication(
        projectId: String,
        senderUserId: String,
        senderRole: UserRole,
        recipientType: InternalCommunicationRecipientType,
        recipientUserIds: Set<String>,
        recipientRole: UserRole?,
        teamId: String?,
        departmentId: String?,
        communicationType: InternalCommunicationType,
        priority: InternalCommunicationPriority,
        subject: String,
        message: String,
        referenceType: String?,
        referenceId: String?,
        threadId: String?,
        parentCommunicationId: String?,
        requiresAcknowledgement: Boolean,
        scheduledAt: Long?,
        idempotencyKey: String?,
        metadata: Map<String, String>,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<InternalCommunication> = mutex.withLock {
        doCreateCommunication(
            projectId = projectId, senderUserId = senderUserId, senderRole = senderRole,
            recipientType = recipientType, recipientUserIds = recipientUserIds,
            recipientRole = recipientRole, teamId = teamId, departmentId = departmentId,
            communicationType = communicationType, priority = priority, subject = subject,
            message = message, referenceType = referenceType, referenceId = referenceId,
            threadId = threadId, parentCommunicationId = parentCommunicationId,
            requiresAcknowledgement = requiresAcknowledgement, scheduledAt = scheduledAt,
            idempotencyKey = idempotencyKey, metadata = metadata,
            actorId = actorId, callerRole = callerRole
        )
    }

    override suspend fun getCommunication(
        projectId: String,
        communicationId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<InternalCommunication> = mutex.withLock {
        val comm = dataSource.getCommunicationById(projectId, communicationId)
            ?: return@withLock DomainResult.Error(message = "Internal communication '$communicationId' not found.")

        val authResult = InternalCommunicationAuthorizationValidator.validateView(comm, projectId, actorId, callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        DomainResult.Success(comm)
    }

    override suspend fun getByCommunicationNo(
        projectId: String,
        communicationNo: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<InternalCommunication> = mutex.withLock {
        val comm = dataSource.getCommunicationByNo(projectId, communicationNo)
            ?: return@withLock DomainResult.Error(message = "Communication number '$communicationNo' not found.")

        val authResult = InternalCommunicationAuthorizationValidator.validateView(comm, projectId, actorId, callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        DomainResult.Success(comm)
    }

    override suspend fun getCommunications(
        projectId: String,
        targetUserId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<InternalCommunication>> = mutex.withLock {
        val authResult = InternalCommunicationAuthorizationValidator.validateInternalUser(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        val list = dataSource.observeCommunicationsForUser(projectId, targetUserId).first()
        DomainResult.Success(list)
    }

    override fun observeCommunications(
        projectId: String,
        targetUserId: String,
        callerRole: UserRole
    ): Flow<List<InternalCommunication>> {
        return dataSource.observeCommunicationsForUser(projectId, targetUserId)
    }

    override suspend fun getUnreadCommunications(
        projectId: String,
        targetUserId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<InternalCommunication>> = mutex.withLock {
        val list = dataSource.observeCommunicationsForUser(projectId, targetUserId).first()
        val unread = list.filter { !it.isRead && it.status != InternalCommunicationStatus.ARCHIVED && it.status != InternalCommunicationStatus.CANCELLED }
        DomainResult.Success(unread)
    }

    override fun observeUnreadCount(
        projectId: String,
        targetUserId: String,
        callerRole: UserRole
    ): Flow<Int> {
        return dataSource.observeCommunicationsForUser(projectId, targetUserId)
            .map { list -> list.count { !it.isRead && it.status != InternalCommunicationStatus.ARCHIVED && it.status != InternalCommunicationStatus.CANCELLED } }
    }

    override suspend fun scheduleCommunication(
        projectId: String,
        communicationId: String,
        scheduledAt: Long,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<InternalCommunication> = mutex.withLock {
        val comm = dataSource.getCommunicationById(projectId, communicationId)
            ?: return@withLock DomainResult.Error(message = "Communication '$communicationId' not found.")

        val transResult = InternalCommunicationLifecycleValidator.validateTransition(comm.status, InternalCommunicationStatus.SCHEDULED)
        if (transResult is DomainResult.Error) return@withLock transResult

        val now = System.currentTimeMillis()
        val updated = comm.copy(
            status = InternalCommunicationStatus.SCHEDULED,
            scheduledAt = scheduledAt,
            updatedBy = actorId,
            updatedAt = now
        )
        dataSource.saveCommunication(updated)
        scheduler.schedule(projectId, communicationId, scheduledAt)

        dataSource.recordActivityEvent(
            InternalCommunicationActivityEvent(
                eventId = UUID.randomUUID().toString(),
                projectId = projectId,
                communicationId = communicationId,
                eventType = "COMMUNICATION_SCHEDULED",
                previousStatus = comm.status,
                newStatus = InternalCommunicationStatus.SCHEDULED,
                actorUserId = actorId,
                timestamp = now,
                notes = "Communication scheduled for timestamp $scheduledAt."
            )
        )

        DomainResult.Success(updated)
    }

    private suspend fun doQueueCommunication(
        projectId: String,
        communicationId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<InternalCommunication> {
        val comm = dataSource.getCommunicationById(projectId, communicationId)
            ?: return DomainResult.Error(message = "Communication '$communicationId' not found.")

        val transResult = InternalCommunicationLifecycleValidator.validateTransition(comm.status, InternalCommunicationStatus.QUEUED)
        if (transResult is DomainResult.Error) return transResult

        val now = System.currentTimeMillis()
        val queued = comm.copy(
            status = InternalCommunicationStatus.QUEUED,
            updatedBy = actorId,
            updatedAt = now
        )
        dataSource.saveCommunication(queued)

        // Dispatch notifications to individual recipients in canonical Notification Engine
        comm.recipientUserIds.forEach { rUid ->
            notificationRepository.createNotification(
                projectId = projectId,
                recipientUserId = rUid,
                recipientType = "INTERNAL_STAFF",
                notificationType = comm.communicationType.canonicalNotificationType,
                channel = NotificationChannel.IN_APP,
                priority = when (comm.priority) {
                    InternalCommunicationPriority.LOW -> NotificationPriority.LOW
                    InternalCommunicationPriority.NORMAL -> NotificationPriority.NORMAL
                    InternalCommunicationPriority.HIGH -> NotificationPriority.HIGH
                    InternalCommunicationPriority.URGENT,
                    InternalCommunicationPriority.CRITICAL -> NotificationPriority.URGENT
                },
                title = comm.subject,
                message = comm.message,
                referenceType = comm.referenceType,
                referenceId = comm.referenceId,
                groupKey = comm.threadId,
                actorId = actorId,
                callerRole = callerRole
            )
        }

        val sent = queued.copy(
            status = InternalCommunicationStatus.SENT,
            sentAt = now,
            deliveredAt = now,
            updatedAt = now
        )
        dataSource.saveCommunication(sent)

        dataSource.recordActivityEvent(
            InternalCommunicationActivityEvent(
                eventId = UUID.randomUUID().toString(),
                projectId = projectId,
                communicationId = communicationId,
                eventType = "COMMUNICATION_SENT",
                previousStatus = comm.status,
                newStatus = InternalCommunicationStatus.SENT,
                actorUserId = actorId,
                timestamp = now,
                notes = "Communication dispatched to ${comm.recipientUserIds.size} recipient(s)."
            )
        )

        return DomainResult.Success(sent)
    }

    override suspend fun queueCommunication(
        projectId: String,
        communicationId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<InternalCommunication> = mutex.withLock {
        doQueueCommunication(projectId, communicationId, actorId, callerRole)
    }

    override suspend fun markRead(
        projectId: String,
        communicationId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<InternalCommunication> = mutex.withLock {
        val comm = dataSource.getCommunicationById(projectId, communicationId)
            ?: return@withLock DomainResult.Error(message = "Communication '$communicationId' not found.")

        if (comm.isRead) {
            return@withLock DomainResult.Success(comm)
        }

        val now = System.currentTimeMillis()
        val readComm = comm.copy(
            status = if (comm.status == InternalCommunicationStatus.ACKNOWLEDGED) comm.status else InternalCommunicationStatus.READ,
            readAt = now,
            updatedBy = actorId,
            updatedAt = now
        )
        dataSource.saveCommunication(readComm)

        dataSource.recordActivityEvent(
            InternalCommunicationActivityEvent(
                eventId = UUID.randomUUID().toString(),
                projectId = projectId,
                communicationId = communicationId,
                eventType = "COMMUNICATION_READ",
                previousStatus = comm.status,
                newStatus = readComm.status,
                actorUserId = actorId,
                timestamp = now,
                notes = "Communication read by user $actorId."
            )
        )

        DomainResult.Success(readComm)
    }

    override suspend fun acknowledge(
        projectId: String,
        communicationId: String,
        notes: String?,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<InternalCommunicationAcknowledgement> = mutex.withLock {
        val comm = dataSource.getCommunicationById(projectId, communicationId)
            ?: return@withLock DomainResult.Error(message = "Communication '$communicationId' not found.")

        val ackId = dataSource.generateAcknowledgementNumber(projectId)
        val now = System.currentTimeMillis()

        val ack = InternalCommunicationAcknowledgement(
            acknowledgementId = ackId,
            projectId = projectId,
            communicationId = communicationId,
            recipientUserId = actorId,
            acknowledgedAt = now,
            notes = notes
        )
        dataSource.saveAcknowledgement(ack)

        val ackComm = comm.copy(
            status = InternalCommunicationStatus.ACKNOWLEDGED,
            acknowledgedAt = now,
            readAt = comm.readAt ?: now,
            updatedBy = actorId,
            updatedAt = now
        )
        dataSource.saveCommunication(ackComm)

        dataSource.recordActivityEvent(
            InternalCommunicationActivityEvent(
                eventId = UUID.randomUUID().toString(),
                projectId = projectId,
                communicationId = communicationId,
                eventType = "COMMUNICATION_ACKNOWLEDGED",
                previousStatus = comm.status,
                newStatus = InternalCommunicationStatus.ACKNOWLEDGED,
                actorUserId = actorId,
                timestamp = now,
                notes = "Acknowledged by $actorId. Notes: $notes"
            )
        )

        DomainResult.Success(ack)
    }

    override suspend fun cancelCommunication(
        projectId: String,
        communicationId: String,
        reason: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<InternalCommunication> = mutex.withLock {
        val comm = dataSource.getCommunicationById(projectId, communicationId)
            ?: return@withLock DomainResult.Error(message = "Communication '$communicationId' not found.")

        val transResult = InternalCommunicationLifecycleValidator.validateTransition(comm.status, InternalCommunicationStatus.CANCELLED)
        if (transResult is DomainResult.Error) return@withLock transResult

        val now = System.currentTimeMillis()
        val cancelled = comm.copy(
            status = InternalCommunicationStatus.CANCELLED,
            updatedBy = actorId,
            updatedAt = now
        )
        dataSource.saveCommunication(cancelled)
        scheduler.cancelScheduled(projectId, communicationId)

        dataSource.recordActivityEvent(
            InternalCommunicationActivityEvent(
                eventId = UUID.randomUUID().toString(),
                projectId = projectId,
                communicationId = communicationId,
                eventType = "COMMUNICATION_CANCELLED",
                previousStatus = comm.status,
                newStatus = InternalCommunicationStatus.CANCELLED,
                actorUserId = actorId,
                timestamp = now,
                notes = "Cancelled: $reason"
            )
        )

        DomainResult.Success(cancelled)
    }

    override suspend fun archiveCommunication(
        projectId: String,
        communicationId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<InternalCommunication> = mutex.withLock {
        val comm = dataSource.getCommunicationById(projectId, communicationId)
            ?: return@withLock DomainResult.Error(message = "Communication '$communicationId' not found.")

        val now = System.currentTimeMillis()
        val archived = comm.copy(
            status = InternalCommunicationStatus.ARCHIVED,
            updatedBy = actorId,
            updatedAt = now
        )
        dataSource.saveCommunication(archived)

        dataSource.recordActivityEvent(
            InternalCommunicationActivityEvent(
                eventId = UUID.randomUUID().toString(),
                projectId = projectId,
                communicationId = communicationId,
                eventType = "COMMUNICATION_ARCHIVED",
                previousStatus = comm.status,
                newStatus = InternalCommunicationStatus.ARCHIVED,
                actorUserId = actorId,
                timestamp = now,
                notes = "Archived by $actorId"
            )
        )

        DomainResult.Success(archived)
    }

    override suspend fun createThread(
        projectId: String,
        subject: String,
        initialMessage: String,
        senderUserId: String,
        senderRole: UserRole,
        recipientType: InternalCommunicationRecipientType,
        recipientUserIds: Set<String>,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<InternalCommunicationThread> = mutex.withLock {
        val threadId = dataSource.generateThreadNumber(projectId)
        val now = System.currentTimeMillis()

        val rootCommRes = doCreateCommunication(
            projectId = projectId,
            senderUserId = senderUserId,
            senderRole = senderRole,
            recipientType = recipientType,
            recipientUserIds = recipientUserIds,
            communicationType = InternalCommunicationType.TEAM_MESSAGE,
            subject = subject,
            message = initialMessage,
            threadId = threadId,
            actorId = actorId,
            callerRole = callerRole
        )
        if (rootCommRes is DomainResult.Error) return@withLock rootCommRes
        val rootComm = (rootCommRes as DomainResult.Success).data

        val thread = InternalCommunicationThread(
            threadId = threadId,
            projectId = projectId,
            rootCommunicationId = rootComm.communicationId,
            subject = subject,
            participantUserIds = recipientUserIds + senderUserId,
            createdBy = actorId,
            createdAt = now,
            updatedAt = now
        )
        dataSource.saveThread(thread)

        DomainResult.Success(thread)
    }

    override suspend fun replyToThread(
        projectId: String,
        threadId: String,
        replyMessage: String,
        senderUserId: String,
        senderRole: UserRole,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<InternalCommunication> = mutex.withLock {
        val thread = dataSource.getThreadById(projectId, threadId)
            ?: return@withLock DomainResult.Error(message = "Thread '$threadId' not found.")

        if (thread.isArchived) {
            return@withLock DomainResult.Error(message = "Cannot reply to an archived thread.")
        }

        val otherParticipants = thread.participantUserIds - senderUserId

        val replyRes = doCreateCommunication(
            projectId = projectId,
            senderUserId = senderUserId,
            senderRole = senderRole,
            recipientType = InternalCommunicationRecipientType.USER,
            recipientUserIds = otherParticipants,
            communicationType = InternalCommunicationType.TEAM_MESSAGE,
            subject = "Re: ${thread.subject}",
            message = replyMessage,
            threadId = threadId,
            parentCommunicationId = thread.rootCommunicationId,
            actorId = actorId,
            callerRole = callerRole
        )
        if (replyRes is DomainResult.Error) return@withLock replyRes
        val reply = (replyRes as DomainResult.Success).data

        val now = System.currentTimeMillis()
        val updatedThread = thread.copy(
            participantUserIds = thread.participantUserIds + senderUserId,
            updatedAt = now
        )
        dataSource.saveThread(updatedThread)

        DomainResult.Success(reply)
    }

    override suspend fun getThread(
        projectId: String,
        threadId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<InternalCommunicationThread> = mutex.withLock {
        val thread = dataSource.getThreadById(projectId, threadId)
            ?: return@withLock DomainResult.Error(message = "Thread '$threadId' not found.")

        DomainResult.Success(thread)
    }

    override suspend fun getThreadMessages(
        projectId: String,
        threadId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<InternalCommunication>> = mutex.withLock {
        val messages = dataSource.getThreadMessages(projectId, threadId)
        DomainResult.Success(messages)
    }

    override suspend fun createMention(
        projectId: String,
        communicationId: String,
        mentionedUserId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<InternalCommunicationMention> = mutex.withLock {
        val mentionId = dataSource.generateMentionNumber(projectId)
        val now = System.currentTimeMillis()

        val mention = InternalCommunicationMention(
            mentionId = mentionId,
            projectId = projectId,
            communicationId = communicationId,
            mentionedUserId = mentionedUserId,
            mentionedBy = actorId,
            createdAt = now
        )
        dataSource.saveMention(mention)

        // Create mention notification in Step 01 engine
        notificationRepository.createNotification(
            projectId = projectId,
            recipientUserId = mentionedUserId,
            recipientType = "INTERNAL_STAFF",
            notificationType = InternalCommunicationType.MENTION.canonicalNotificationType,
            channel = NotificationChannel.IN_APP,
            priority = NotificationPriority.HIGH,
            title = "You were mentioned in a team discussion",
            message = "User $actorId mentioned you in communication $communicationId.",
            actorId = actorId,
            callerRole = callerRole
        )

        dataSource.recordActivityEvent(
            InternalCommunicationActivityEvent(
                eventId = UUID.randomUUID().toString(),
                projectId = projectId,
                communicationId = communicationId,
                eventType = "COMMUNICATION_MENTIONED",
                previousStatus = null,
                newStatus = null,
                actorUserId = actorId,
                timestamp = now,
                notes = "User $mentionedUserId mentioned."
            )
        )

        DomainResult.Success(mention)
    }

    override suspend fun getMentions(
        projectId: String,
        targetUserId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<InternalCommunicationMention>> = mutex.withLock {
        val mentions = dataSource.getMentionsForUser(projectId, targetUserId)
        DomainResult.Success(mentions)
    }

    override suspend fun broadcastCommunication(
        projectId: String,
        recipientType: InternalCommunicationRecipientType,
        recipientRole: UserRole?,
        teamId: String?,
        departmentId: String?,
        priority: InternalCommunicationPriority,
        subject: String,
        message: String,
        requiresAcknowledgement: Boolean,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<InternalCommunication> = mutex.withLock {
        val broadCheck = InternalCommunicationAuthorizationValidator.validateBroadcast(recipientType, callerRole)
        if (broadCheck is DomainResult.Error) return@withLock broadCheck

        // Use private unlocked helpers to avoid re-acquiring the mutex (self-deadlock)
        val createRes = doCreateCommunication(
            projectId = projectId,
            senderUserId = actorId,
            senderRole = callerRole,
            recipientType = recipientType,
            recipientUserIds = emptySet(),
            recipientRole = recipientRole,
            teamId = teamId,
            departmentId = departmentId,
            communicationType = InternalCommunicationType.GENERAL_ANNOUNCEMENT,
            priority = priority,
            subject = subject,
            message = message,
            referenceType = null,
            referenceId = null,
            threadId = null,
            parentCommunicationId = null,
            requiresAcknowledgement = requiresAcknowledgement,
            scheduledAt = null,
            idempotencyKey = null,
            metadata = emptyMap(),
            actorId = actorId,
            callerRole = callerRole
        )
        if (createRes is DomainResult.Error) return@withLock createRes
        val comm = (createRes as DomainResult.Success).data

        // Queue & broadcast (unlocked — we already hold the mutex)
        doQueueCommunication(projectId, comm.communicationId, actorId, callerRole)
    }

    override suspend fun getSummary(
        projectId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<InternalCommunicationSummary> = mutex.withLock {
        val all = dataSource.observeCommunicationsByProject(projectId).first()
        val total = all.size
        val unread = all.count { !it.isRead && it.status != InternalCommunicationStatus.ARCHIVED && it.status != InternalCommunicationStatus.CANCELLED }
        val urgent = all.count { it.priority == InternalCommunicationPriority.URGENT }
        val critical = all.count { it.priority == InternalCommunicationPriority.CRITICAL }
        val pendingAck = all.count { it.requiresAcknowledgement && !it.isAcknowledged }
        val failed = all.count { it.status == InternalCommunicationStatus.FAILED }
        val team = all.count { it.communicationType == InternalCommunicationType.TEAM_MESSAGE }
        val dept = all.count { it.communicationType == InternalCommunicationType.DEPARTMENT_MESSAGE }
        val direct = all.count { it.communicationType == InternalCommunicationType.DIRECT_MESSAGE }

        DomainResult.Success(
            InternalCommunicationSummary(
                projectId = projectId,
                totalMessages = total,
                unreadMessages = unread,
                urgentMessages = urgent,
                criticalMessages = critical,
                pendingAcknowledgements = pendingAck,
                teamMessages = team,
                departmentMessages = dept,
                directMessages = direct,
                failedMessages = failed
            )
        )
    }

    override suspend fun getActivityHistory(
        projectId: String,
        communicationId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<InternalCommunicationActivityEvent>> = mutex.withLock {
        val events = dataSource.getActivityEvents(projectId, communicationId)
        DomainResult.Success(events)
    }
}

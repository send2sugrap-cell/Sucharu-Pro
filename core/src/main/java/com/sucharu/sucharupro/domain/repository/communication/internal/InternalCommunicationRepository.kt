package com.sucharu.sucharupro.domain.repository.communication.internal

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.internal.*
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for Internal Staff & Team Communication Management (Module 10 Step 03).
 */
interface InternalCommunicationRepository {

    suspend fun createCommunication(
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
    ): DomainResult<InternalCommunication>

    suspend fun getCommunication(
        projectId: String,
        communicationId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<InternalCommunication>

    suspend fun getByCommunicationNo(
        projectId: String,
        communicationNo: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<InternalCommunication>

    suspend fun getCommunications(
        projectId: String,
        targetUserId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<InternalCommunication>>

    fun observeCommunications(
        projectId: String,
        targetUserId: String,
        callerRole: UserRole
    ): Flow<List<InternalCommunication>>

    suspend fun getUnreadCommunications(
        projectId: String,
        targetUserId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<InternalCommunication>>

    fun observeUnreadCount(
        projectId: String,
        targetUserId: String,
        callerRole: UserRole
    ): Flow<Int>

    suspend fun scheduleCommunication(
        projectId: String,
        communicationId: String,
        scheduledAt: Long,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<InternalCommunication>

    suspend fun queueCommunication(
        projectId: String,
        communicationId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<InternalCommunication>

    suspend fun markRead(
        projectId: String,
        communicationId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<InternalCommunication>

    suspend fun acknowledge(
        projectId: String,
        communicationId: String,
        notes: String? = null,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<InternalCommunicationAcknowledgement>

    suspend fun cancelCommunication(
        projectId: String,
        communicationId: String,
        reason: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<InternalCommunication>

    suspend fun archiveCommunication(
        projectId: String,
        communicationId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<InternalCommunication>

    // Thread Operations
    suspend fun createThread(
        projectId: String,
        subject: String,
        initialMessage: String,
        senderUserId: String,
        senderRole: UserRole,
        recipientType: InternalCommunicationRecipientType,
        recipientUserIds: Set<String>,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<InternalCommunicationThread>

    suspend fun replyToThread(
        projectId: String,
        threadId: String,
        replyMessage: String,
        senderUserId: String,
        senderRole: UserRole,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<InternalCommunication>

    suspend fun getThread(
        projectId: String,
        threadId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<InternalCommunicationThread>

    suspend fun getThreadMessages(
        projectId: String,
        threadId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<InternalCommunication>>

    // Mention Operations
    suspend fun createMention(
        projectId: String,
        communicationId: String,
        mentionedUserId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<InternalCommunicationMention>

    suspend fun getMentions(
        projectId: String,
        targetUserId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<InternalCommunicationMention>>

    // Broadcast Operations
    suspend fun broadcastCommunication(
        projectId: String,
        recipientType: InternalCommunicationRecipientType,
        recipientRole: UserRole? = null,
        teamId: String? = null,
        departmentId: String? = null,
        priority: InternalCommunicationPriority = InternalCommunicationPriority.HIGH,
        subject: String,
        message: String,
        requiresAcknowledgement: Boolean = false,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<InternalCommunication>

    // Summary & History
    suspend fun getSummary(
        projectId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<InternalCommunicationSummary>

    suspend fun getActivityHistory(
        projectId: String,
        communicationId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<InternalCommunicationActivityEvent>>
}

package com.sucharu.sucharupro.domain.repository.communication.customer

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.customer.*
import com.sucharu.sucharupro.domain.model.notification.NotificationChannel
import com.sucharu.sucharupro.domain.model.notification.NotificationPriority
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for Customer Communication & Engagement Management (Module 10 Step 02).
 */
interface CustomerCommunicationRepository {

    suspend fun createCommunication(
        projectId: String,
        customerId: String,
        recipientUserId: String? = null,
        communicationType: CustomerCommunicationType,
        channel: NotificationChannel = NotificationChannel.IN_APP,
        priority: NotificationPriority = NotificationPriority.NORMAL,
        title: String,
        message: String,
        referenceType: String? = null,
        referenceId: String? = null,
        scheduledAt: Long? = null,
        groupKey: String? = null,
        idempotencyKey: String? = null,
        metadata: Map<String, String> = emptyMap(),
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerCommunication>

    suspend fun getCommunication(
        projectId: String,
        communicationId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerCommunication>

    suspend fun getByCommunicationNo(
        projectId: String,
        communicationNo: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerCommunication>

    suspend fun getCustomerCommunications(
        projectId: String,
        targetCustomerId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<CustomerCommunication>>

    suspend fun getUnreadCommunications(
        projectId: String,
        targetCustomerId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<CustomerCommunication>>

    fun observeCustomerCommunications(
        projectId: String,
        targetCustomerId: String,
        callerRole: UserRole
    ): Flow<List<CustomerCommunication>>

    fun observeUnreadCount(
        projectId: String,
        targetCustomerId: String,
        callerRole: UserRole
    ): Flow<Int>

    suspend fun scheduleCommunication(
        projectId: String,
        communicationId: String,
        scheduledAt: Long,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerCommunication>

    suspend fun queueCommunication(
        projectId: String,
        communicationId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerCommunication>

    suspend fun markRead(
        projectId: String,
        communicationId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerCommunication>

    suspend fun markAcknowledged(
        projectId: String,
        communicationId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerCommunication>

    suspend fun cancelCommunication(
        projectId: String,
        communicationId: String,
        reason: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerCommunication>

    suspend fun getHistory(
        projectId: String,
        communicationId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<CustomerCommunicationHistory>>

    suspend fun recordEngagement(
        projectId: String,
        customerId: String,
        communicationId: String,
        eventType: CustomerEngagementEventType,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerEngagementEvent>

    suspend fun getEngagementEvents(
        projectId: String,
        targetCustomerId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<CustomerEngagementEvent>>

    suspend fun getEngagementSummary(
        projectId: String,
        targetCustomerId: String?,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerEngagementSummary>

    suspend fun getSummary(
        projectId: String,
        targetCustomerId: String?,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<CustomerCommunicationSummary>

    suspend fun getByReference(
        projectId: String,
        referenceType: String,
        referenceId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<CustomerCommunication>>
}

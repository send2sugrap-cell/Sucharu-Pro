package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeInternalCommunicationDataSource
import com.sucharu.sucharupro.data.datasource.FakeNotificationDataSource
import com.sucharu.sucharupro.data.repository.InternalCommunicationRepositoryImpl
import com.sucharu.sucharupro.data.repository.NotificationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.internal.InternalCommunicationPriority
import com.sucharu.sucharupro.domain.model.communication.internal.InternalCommunicationRecipientType
import com.sucharu.sucharupro.domain.model.communication.internal.InternalCommunicationStatus
import com.sucharu.sucharupro.domain.model.communication.internal.InternalCommunicationType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class InternalCommunicationLifecycleTest {

    private lateinit var repository: InternalCommunicationRepositoryImpl

    @Before
    fun setUp() {
        val commDs = FakeInternalCommunicationDataSource()
        val notifDs = FakeNotificationDataSource()
        val notifRepo = NotificationRepositoryImpl(notifDs)
        repository = InternalCommunicationRepositoryImpl(commDs, notifRepo)
    }

    @Test
    fun `test valid message lifecycle transition to DELIVERED READ ACKNOWLEDGED`() = runBlocking {
        val createRes = repository.createCommunication(
            projectId = "PRJ-01",
            senderUserId = "USER-01",
            senderRole = UserRole.STAFF,
            recipientType = InternalCommunicationRecipientType.USER,
            recipientUserIds = setOf("USER-02"),
            communicationType = InternalCommunicationType.DIRECT_MESSAGE,
            priority = InternalCommunicationPriority.URGENT,
            subject = "Urgent Notice",
            message = "Please acknowledge ASAP",
            requiresAcknowledgement = true,
            actorId = "USER-01",
            callerRole = UserRole.STAFF
        )
        assertTrue(createRes is DomainResult.Success)
        val comm = (createRes as DomainResult.Success).data

        val queueRes = repository.queueCommunication(
            projectId = "PRJ-01",
            communicationId = comm.communicationId,
            actorId = "USER-01",
            callerRole = UserRole.STAFF
        )
        assertTrue(queueRes is DomainResult.Success)
        val sentComm = (queueRes as DomainResult.Success).data
        assertEquals(InternalCommunicationStatus.SENT, sentComm.status)

        val markReadRes = repository.markRead(
            projectId = "PRJ-01",
            communicationId = comm.communicationId,
            actorId = "USER-02",
            callerRole = UserRole.STAFF
        )
        assertTrue(markReadRes is DomainResult.Success)
        assertEquals(InternalCommunicationStatus.READ, (markReadRes as DomainResult.Success).data.status)

        val ackRes = repository.acknowledge(
            projectId = "PRJ-01",
            communicationId = comm.communicationId,
            notes = "Acknowledged and received",
            actorId = "USER-02",
            callerRole = UserRole.STAFF
        )
        assertTrue(ackRes is DomainResult.Success)

        val updatedComm = (repository.getCommunication("PRJ-01", comm.communicationId, "USER-02", UserRole.STAFF) as DomainResult.Success).data
        assertEquals(InternalCommunicationStatus.ACKNOWLEDGED, updatedComm.status)
        assertNotNull(updatedComm.acknowledgedAt)
    }

    @Test
    fun `test cancel communication changes status to CANCELLED`() = runBlocking {
        val schedAt = System.currentTimeMillis() + 3600000
        val createRes = repository.createCommunication(
            projectId = "PRJ-01",
            senderUserId = "USER-01",
            senderRole = UserRole.ADMIN,
            recipientType = InternalCommunicationRecipientType.USER,
            recipientUserIds = setOf("USER-02"),
            communicationType = InternalCommunicationType.GENERAL_ANNOUNCEMENT,
            subject = "Scheduled Maintenance",
            message = "Maintenance tomorrow",
            scheduledAt = schedAt,
            actorId = "USER-01",
            callerRole = UserRole.ADMIN
        )
        val comm = (createRes as DomainResult.Success).data
        assertEquals(InternalCommunicationStatus.SCHEDULED, comm.status)

        val cancelRes = repository.cancelCommunication(
            projectId = "PRJ-01",
            communicationId = comm.communicationId,
            reason = "Maintenance postponed",
            actorId = "USER-01",
            callerRole = UserRole.ADMIN
        )
        assertTrue(cancelRes is DomainResult.Success)
        assertEquals(InternalCommunicationStatus.CANCELLED, (cancelRes as DomainResult.Success).data.status)
    }
}

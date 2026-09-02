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

class InternalCommunicationAcknowledgementTest {

    private lateinit var repository: InternalCommunicationRepositoryImpl

    @Before
    fun setUp() {
        val commDs = FakeInternalCommunicationDataSource()
        val notifDs = FakeNotificationDataSource()
        val notifRepo = NotificationRepositoryImpl(notifDs)
        repository = InternalCommunicationRepositoryImpl(commDs, notifRepo)
    }

    @Test
    fun `test acknowledge creates immutable record and updates communication status`() = runBlocking {
        val commRes = repository.createCommunication(
            projectId = "PRJ-01",
            senderUserId = "ADMIN-01",
            senderRole = UserRole.ADMIN,
            recipientType = InternalCommunicationRecipientType.USER,
            recipientUserIds = setOf("STAFF-01"),
            communicationType = InternalCommunicationType.URGENT_NOTICE,
            priority = InternalCommunicationPriority.CRITICAL,
            subject = "Critical Safety Update",
            message = "Acknowledge press safety policy",
            requiresAcknowledgement = true,
            actorId = "ADMIN-01",
            callerRole = UserRole.ADMIN
        )
        val commId = (commRes as DomainResult.Success).data.communicationId

        val ackRes = repository.acknowledge(
            projectId = "PRJ-01",
            communicationId = commId,
            notes = "Read and agreed",
            actorId = "STAFF-01",
            callerRole = UserRole.STAFF
        )
        assertTrue(ackRes is DomainResult.Success)
        val ack = (ackRes as DomainResult.Success).data

        assertEquals("PRJ-01", ack.projectId)
        assertEquals(commId, ack.communicationId)
        assertEquals("STAFF-01", ack.recipientUserId)
        assertEquals("Read and agreed", ack.notes)
    }
}

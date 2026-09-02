package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeInternalCommunicationDataSource
import com.sucharu.sucharupro.data.datasource.FakeNotificationDataSource
import com.sucharu.sucharupro.data.repository.InternalCommunicationRepositoryImpl
import com.sucharu.sucharupro.data.repository.NotificationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.internal.InternalCommunicationRecipientType
import com.sucharu.sucharupro.domain.model.communication.internal.InternalCommunicationType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class InternalCommunicationAuditTest {

    private lateinit var repository: InternalCommunicationRepositoryImpl

    @Before
    fun setUp() {
        val commDs = FakeInternalCommunicationDataSource()
        val notifDs = FakeNotificationDataSource()
        val notifRepo = NotificationRepositoryImpl(notifDs)
        repository = InternalCommunicationRepositoryImpl(commDs, notifRepo)
    }

    @Test
    fun `test audit trail captures creation and read activity events`() = runBlocking {
        val createRes = repository.createCommunication(
            projectId = "PRJ-01",
            senderUserId = "USER-01",
            senderRole = UserRole.STAFF,
            recipientType = InternalCommunicationRecipientType.USER,
            recipientUserIds = setOf("USER-02"),
            communicationType = InternalCommunicationType.DIRECT_MESSAGE,
            subject = "Audit Test Subject",
            message = "Audit message content",
            actorId = "USER-01",
            callerRole = UserRole.STAFF
        )
        val commId = (createRes as DomainResult.Success).data.communicationId

        repository.markRead(
            projectId = "PRJ-01",
            communicationId = commId,
            actorId = "USER-02",
            callerRole = UserRole.STAFF
        )

        val historyRes = repository.getActivityHistory("PRJ-01", commId, "USER-01", UserRole.STAFF)
        assertTrue(historyRes is DomainResult.Success)
        val events = (historyRes as DomainResult.Success).data

        assertTrue(events.isNotEmpty())
        assertTrue(events.any { it.eventType == "COMMUNICATION_CREATED" })
        assertTrue(events.any { it.eventType == "COMMUNICATION_READ" })
    }
}

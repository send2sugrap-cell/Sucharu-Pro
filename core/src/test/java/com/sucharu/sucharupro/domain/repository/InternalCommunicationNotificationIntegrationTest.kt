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

class InternalCommunicationNotificationIntegrationTest {

    private lateinit var repository: InternalCommunicationRepositoryImpl
    private lateinit var notifRepo: NotificationRepositoryImpl

    @Before
    fun setUp() {
        val commDs = FakeInternalCommunicationDataSource()
        val notifDs = FakeNotificationDataSource()
        notifRepo = NotificationRepositoryImpl(notifDs)
        repository = InternalCommunicationRepositoryImpl(commDs, notifRepo)
    }

    @Test
    fun `test creating direct message automatically dispatches canonical notification`() = runBlocking {
        val commRes = repository.createCommunication(
            projectId = "PRJ-01",
            senderUserId = "SENDER-01",
            senderRole = UserRole.STAFF,
            recipientType = InternalCommunicationRecipientType.USER,
            recipientUserIds = setOf("RECIPIENT-01"),
            communicationType = InternalCommunicationType.DIRECT_MESSAGE,
            subject = "Notification Trigger Test",
            message = "Body text",
            actorId = "SENDER-01",
            callerRole = UserRole.STAFF
        )
        assertTrue(commRes is DomainResult.Success)
        val comm = (commRes as DomainResult.Success).data
        repository.queueCommunication(
            projectId = "PRJ-01",
            communicationId = comm.communicationId,
            actorId = "SENDER-01",
            callerRole = UserRole.STAFF
        )

        val notificationsRes = notifRepo.getUserNotifications(
            projectId = "PRJ-01",
            targetUserId = "RECIPIENT-01",
            actorId = "RECIPIENT-01",
            callerRole = UserRole.STAFF
        )
        assertTrue(notificationsRes is DomainResult.Success)
        val notifications = (notificationsRes as DomainResult.Success).data
        assertTrue(notifications.isNotEmpty())
        assertEquals("Notification Trigger Test", notifications[0].title)
    }
}

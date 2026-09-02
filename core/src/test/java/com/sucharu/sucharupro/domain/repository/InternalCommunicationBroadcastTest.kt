package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeInternalCommunicationDataSource
import com.sucharu.sucharupro.data.datasource.FakeNotificationDataSource
import com.sucharu.sucharupro.data.repository.InternalCommunicationRepositoryImpl
import com.sucharu.sucharupro.data.repository.NotificationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.internal.InternalCommunicationPriority
import com.sucharu.sucharupro.domain.model.communication.internal.InternalCommunicationRecipientType
import com.sucharu.sucharupro.domain.model.communication.internal.InternalCommunicationType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class InternalCommunicationBroadcastTest {

    private lateinit var repository: InternalCommunicationRepositoryImpl

    @Before
    fun setUp() {
        val commDs = FakeInternalCommunicationDataSource()
        val notifDs = FakeNotificationDataSource()
        val notifRepo = NotificationRepositoryImpl(notifDs)
        repository = InternalCommunicationRepositoryImpl(commDs, notifRepo)
    }

    @Test
    fun `test admin broadcast communication succeeds and sets type to GENERAL_ANNOUNCEMENT`() = runBlocking {
        val broadcastRes = repository.broadcastCommunication(
            projectId = "PRJ-01",
            recipientType = InternalCommunicationRecipientType.PROJECT,
            priority = InternalCommunicationPriority.HIGH,
            subject = "All Staff Announcement",
            message = "Holiday schedule announcement",
            actorId = "ADMIN-01",
            callerRole = UserRole.ADMIN
        )
        assertTrue(broadcastRes is DomainResult.Success)
        val comm = (broadcastRes as DomainResult.Success).data

        assertEquals(InternalCommunicationType.GENERAL_ANNOUNCEMENT, comm.communicationType)
        assertEquals(InternalCommunicationRecipientType.PROJECT, comm.recipientType)
        assertEquals(InternalCommunicationPriority.HIGH, comm.priority)
    }
}

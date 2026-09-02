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

class InternalCommunicationDuplicateTest {

    private lateinit var repository: InternalCommunicationRepositoryImpl

    @Before
    fun setUp() {
        val commDs = FakeInternalCommunicationDataSource()
        val notifDs = FakeNotificationDataSource()
        val notifRepo = NotificationRepositoryImpl(notifDs)
        repository = InternalCommunicationRepositoryImpl(commDs, notifRepo)
    }

    @Test
    fun `test creating distinct messages generates unique sequential communication numbers`() = runBlocking {
        val res1 = repository.createCommunication(
            projectId = "PRJ-01",
            senderUserId = "USER-01",
            senderRole = UserRole.STAFF,
            recipientType = InternalCommunicationRecipientType.USER,
            recipientUserIds = setOf("USER-02"),
            communicationType = InternalCommunicationType.DIRECT_MESSAGE,
            subject = "Message One",
            message = "Body 1",
            actorId = "USER-01",
            callerRole = UserRole.STAFF
        )
        val res2 = repository.createCommunication(
            projectId = "PRJ-01",
            senderUserId = "USER-01",
            senderRole = UserRole.STAFF,
            recipientType = InternalCommunicationRecipientType.USER,
            recipientUserIds = setOf("USER-02"),
            communicationType = InternalCommunicationType.DIRECT_MESSAGE,
            subject = "Message Two",
            message = "Body 2",
            actorId = "USER-01",
            callerRole = UserRole.STAFF
        )
        assertTrue(res1 is DomainResult.Success)
        assertTrue(res2 is DomainResult.Success)

        val comm1 = (res1 as DomainResult.Success).data
        val comm2 = (res2 as DomainResult.Success).data
        assertNotEquals(comm1.communicationId, comm2.communicationId)
        assertNotEquals(comm1.communicationNo, comm2.communicationNo)
    }
}

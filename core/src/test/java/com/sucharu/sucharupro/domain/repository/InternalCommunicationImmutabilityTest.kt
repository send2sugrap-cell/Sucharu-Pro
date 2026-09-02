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

class InternalCommunicationImmutabilityTest {

    private lateinit var repository: InternalCommunicationRepositoryImpl

    @Before
    fun setUp() {
        val commDs = FakeInternalCommunicationDataSource()
        val notifDs = FakeNotificationDataSource()
        val notifRepo = NotificationRepositoryImpl(notifDs)
        repository = InternalCommunicationRepositoryImpl(commDs, notifRepo)
    }

    @Test
    fun `test original sent communication properties remain immutable after sending`() = runBlocking {
        val createRes = repository.createCommunication(
            projectId = "PRJ-01",
            senderUserId = "USER-01",
            senderRole = UserRole.STAFF,
            recipientType = InternalCommunicationRecipientType.USER,
            recipientUserIds = setOf("USER-02"),
            communicationType = InternalCommunicationType.DIRECT_MESSAGE,
            subject = "Original Subject",
            message = "Original Message Content",
            actorId = "USER-01",
            callerRole = UserRole.STAFF
        )
        val comm = (createRes as DomainResult.Success).data

        // Verify subject and message body
        assertEquals("Original Subject", comm.subject)
        assertEquals("Original Message Content", comm.message)

        // Reading does not mutate content or subject
        val readRes = repository.markRead("PRJ-01", comm.communicationId, "USER-02", UserRole.STAFF)
        val readComm = (readRes as DomainResult.Success).data
        assertEquals("Original Subject", readComm.subject)
        assertEquals("Original Message Content", readComm.message)
    }
}

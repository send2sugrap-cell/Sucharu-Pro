package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeInternalCommunicationDataSource
import com.sucharu.sucharupro.data.datasource.FakeNotificationDataSource
import com.sucharu.sucharupro.data.repository.InternalCommunicationRepositoryImpl
import com.sucharu.sucharupro.data.repository.NotificationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.internal.InternalCommunicationRecipientType
import com.sucharu.sucharupro.domain.model.communication.internal.InternalCommunicationStatus
import com.sucharu.sucharupro.domain.model.communication.internal.InternalCommunicationType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class InternalCommunicationReadReceiptTest {

    private lateinit var repository: InternalCommunicationRepositoryImpl

    @Before
    fun setUp() {
        val commDs = FakeInternalCommunicationDataSource()
        val notifDs = FakeNotificationDataSource()
        val notifRepo = NotificationRepositoryImpl(notifDs)
        repository = InternalCommunicationRepositoryImpl(commDs, notifRepo)
    }

    @Test
    fun `test markRead updates status to READ and sets readAt timestamp`() = runBlocking {
        val commRes = repository.createCommunication(
            projectId = "PRJ-01",
            senderUserId = "USER-01",
            senderRole = UserRole.STAFF,
            recipientType = InternalCommunicationRecipientType.USER,
            recipientUserIds = setOf("USER-02"),
            communicationType = InternalCommunicationType.DIRECT_MESSAGE,
            subject = "Direct Message",
            message = "Hello test",
            actorId = "USER-01",
            callerRole = UserRole.STAFF
        )
        val commId = (commRes as DomainResult.Success).data.communicationId

        val markReadRes = repository.markRead(
            projectId = "PRJ-01",
            communicationId = commId,
            actorId = "USER-02",
            callerRole = UserRole.STAFF
        )
        assertTrue(markReadRes is DomainResult.Success)
        val readComm = (markReadRes as DomainResult.Success).data
        assertEquals(InternalCommunicationStatus.READ, readComm.status)
        assertNotNull(readComm.readAt)
    }
}

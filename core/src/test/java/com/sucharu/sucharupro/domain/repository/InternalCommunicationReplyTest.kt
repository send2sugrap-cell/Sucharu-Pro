package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeInternalCommunicationDataSource
import com.sucharu.sucharupro.data.datasource.FakeNotificationDataSource
import com.sucharu.sucharupro.data.repository.InternalCommunicationRepositoryImpl
import com.sucharu.sucharupro.data.repository.NotificationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.internal.InternalCommunicationRecipientType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class InternalCommunicationReplyTest {

    private lateinit var repository: InternalCommunicationRepositoryImpl

    @Before
    fun setUp() {
        val commDs = FakeInternalCommunicationDataSource()
        val notifDs = FakeNotificationDataSource()
        val notifRepo = NotificationRepositoryImpl(notifDs)
        repository = InternalCommunicationRepositoryImpl(commDs, notifRepo)
    }

    @Test
    fun `test replyToThread adds reply message linked to existing thread`() = runBlocking {
        val threadRes = repository.createThread(
            projectId = "PRJ-01",
            subject = "QC Discussion",
            initialMessage = "Check color calibration",
            senderUserId = "QC-01",
            senderRole = UserRole.QC_INSPECTOR,
            recipientType = InternalCommunicationRecipientType.USER,
            recipientUserIds = setOf("PROD-01"),
            actorId = "QC-01",
            callerRole = UserRole.QC_INSPECTOR
        )
        val threadId = (threadRes as DomainResult.Success).data.threadId

        val replyRes = repository.replyToThread(
            projectId = "PRJ-01",
            threadId = threadId,
            replyMessage = "Color calibration is calibrated now.",
            senderUserId = "PROD-01",
            senderRole = UserRole.STAFF,
            actorId = "PROD-01",
            callerRole = UserRole.STAFF
        )
        assertTrue(replyRes is DomainResult.Success)
        val reply = (replyRes as DomainResult.Success).data
        assertEquals(threadId, reply.threadId)

        val messagesRes = repository.getThreadMessages("PRJ-01", threadId, "QC-01", UserRole.QC_INSPECTOR)
        assertTrue(messagesRes is DomainResult.Success)
        assertEquals(2, (messagesRes as DomainResult.Success).data.size)
    }
}

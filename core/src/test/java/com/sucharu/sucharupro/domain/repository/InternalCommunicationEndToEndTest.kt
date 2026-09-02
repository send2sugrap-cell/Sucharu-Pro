package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeInternalCommunicationDataSource
import com.sucharu.sucharupro.data.datasource.FakeNotificationDataSource
import com.sucharu.sucharupro.data.notification.InMemoryNotificationDeliveryProvider
import com.sucharu.sucharupro.data.notification.NotificationDeliveryServiceImpl
import com.sucharu.sucharupro.data.repository.InternalCommunicationRepositoryImpl
import com.sucharu.sucharupro.data.repository.NotificationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.internal.*
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.communication.internal.InternalCommunicationRepository
import com.sucharu.sucharupro.domain.repository.notification.NotificationRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InternalCommunicationEndToEndTest {

    private lateinit var internalCommunicationDataSource: FakeInternalCommunicationDataSource
    private lateinit var notificationDataSource: FakeNotificationDataSource
    private lateinit var notificationRepository: NotificationRepository
    private lateinit var repository: InternalCommunicationRepository

    private val projectId = "PRJ-INT-E2E"
    private val adminActorId = "ADMIN-001"
    private val adminRole = UserRole.ADMIN

    @Before
    fun setUp() {
        internalCommunicationDataSource = FakeInternalCommunicationDataSource()
        notificationDataSource = FakeNotificationDataSource()
        val deliveryService = NotificationDeliveryServiceImpl(listOf(InMemoryNotificationDeliveryProvider()))
        notificationRepository = NotificationRepositoryImpl(notificationDataSource, deliveryService)
        repository = InternalCommunicationRepositoryImpl(internalCommunicationDataSource, notificationRepository)
    }

    @Test
    fun `full end to end lifecycle for direct message, thread reply, mention, acknowledgement, and archiving`() = runBlocking {
        // 1. Create Thread
        val threadRes = repository.createThread(
            projectId = projectId,
            subject = "Rush Order #888 Packaging Discussion",
            initialMessage = "Boxes need UV coating before cutting.",
            senderUserId = "OPERATOR-01",
            senderRole = UserRole.STAFF,
            recipientType = InternalCommunicationRecipientType.USER,
            recipientUserIds = setOf("OPERATOR-02", "QC-INSPECTOR-01"),
            actorId = "OPERATOR-01",
            callerRole = UserRole.STAFF
        )
        assertTrue(threadRes is DomainResult.Success)
        val thread = (threadRes as DomainResult.Success).data

        // 2. Dispatch root message
        repository.queueCommunication(projectId, thread.rootCommunicationId, "OPERATOR-01", UserRole.STAFF)

        // 3. Reply to Thread
        val replyRes = repository.replyToThread(
            projectId = projectId,
            threadId = thread.threadId,
            replyMessage = "UV coating completed. Proceeding to die cutter.",
            senderUserId = "OPERATOR-02",
            senderRole = UserRole.STAFF,
            actorId = "OPERATOR-02",
            callerRole = UserRole.STAFF
        )
        assertTrue(replyRes is DomainResult.Success)
        val reply = (replyRes as DomainResult.Success).data

        // 4. Mention QC Inspector
        val mentionRes = repository.createMention(
            projectId = projectId,
            communicationId = reply.communicationId,
            mentionedUserId = "QC-INSPECTOR-01",
            actorId = "OPERATOR-02",
            callerRole = UserRole.STAFF
        )
        assertTrue(mentionRes is DomainResult.Success)

        // 5. QC Inspector reads and acknowledges
        repository.markRead(projectId, reply.communicationId, "QC-INSPECTOR-01", UserRole.QC_INSPECTOR)
        val ackRes = repository.acknowledge(
            projectId = projectId,
            communicationId = reply.communicationId,
            notes = "Samples inspected and passed.",
            actorId = "QC-INSPECTOR-01",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(ackRes is DomainResult.Success)

        // 6. Verify Summary Metrics
        val summaryRes = repository.getSummary(projectId, adminActorId, adminRole)
        assertTrue(summaryRes is DomainResult.Success)
        val summary = (summaryRes as DomainResult.Success).data
        assertEquals(2, summary.totalMessages)

        // 7. Archive message
        val archiveRes = repository.archiveCommunication(projectId, reply.communicationId, adminActorId, adminRole)
        assertTrue(archiveRes is DomainResult.Success)
        assertEquals(InternalCommunicationStatus.ARCHIVED, (archiveRes as DomainResult.Success).data.status)
    }
}

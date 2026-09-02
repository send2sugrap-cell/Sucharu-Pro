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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class InternalCommunicationRepositoryTest {

    private lateinit var internalCommunicationDataSource: FakeInternalCommunicationDataSource
    private lateinit var notificationDataSource: FakeNotificationDataSource
    private lateinit var notificationRepository: NotificationRepository
    private lateinit var repository: InternalCommunicationRepository

    private val projectId = "PRJ-INTERNAL-TEST"
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
    fun `createCommunication generates sequential ICM number and audit event`() = runBlocking {
        val res = repository.createCommunication(
            projectId = projectId,
            senderUserId = "STAFF-01",
            senderRole = UserRole.STAFF,
            recipientType = InternalCommunicationRecipientType.USER,
            recipientUserIds = setOf("STAFF-02"),
            communicationType = InternalCommunicationType.DIRECT_MESSAGE,
            subject = "Cutting Machine Status",
            message = "Blade replaced and calibrated.",
            actorId = "STAFF-01",
            callerRole = UserRole.STAFF
        )

        assertTrue(res is DomainResult.Success)
        val comm = (res as DomainResult.Success).data
        assertTrue(comm.communicationNo.startsWith("ICM-"))
        assertEquals(InternalCommunicationStatus.DRAFT, comm.status)

        val historyRes = repository.getActivityHistory(projectId, comm.communicationId, "STAFF-01", UserRole.STAFF)
        assertTrue(historyRes is DomainResult.Success)
        val history = (historyRes as DomainResult.Success).data
        assertTrue(history.any { it.eventType == "COMMUNICATION_CREATED" })
    }

    @Test
    fun `idempotency test guarantees single creation`() = runBlocking {
        val idemKey = "ICM-IDEM-99"

        val first = repository.createCommunication(
            projectId = projectId,
            senderUserId = "STAFF-01",
            senderRole = UserRole.STAFF,
            recipientType = InternalCommunicationRecipientType.USER,
            recipientUserIds = setOf("STAFF-02"),
            communicationType = InternalCommunicationType.DIRECT_MESSAGE,
            subject = "Urgent Notice",
            message = "Paper order arrived",
            idempotencyKey = idemKey,
            actorId = "STAFF-01",
            callerRole = UserRole.STAFF
        )

        val second = repository.createCommunication(
            projectId = projectId,
            senderUserId = "STAFF-01",
            senderRole = UserRole.STAFF,
            recipientType = InternalCommunicationRecipientType.USER,
            recipientUserIds = setOf("STAFF-02"),
            communicationType = InternalCommunicationType.DIRECT_MESSAGE,
            subject = "Urgent Notice (Duplicate)",
            message = "Paper order arrived",
            idempotencyKey = idemKey,
            actorId = "STAFF-01",
            callerRole = UserRole.STAFF
        )

        assertEquals((first as DomainResult.Success).data.communicationId, (second as DomainResult.Success).data.communicationId)
    }

    @Test
    fun `concurrency test with 25 coroutines creates distinct safe messages`() = runBlocking {
        val count = 25
        val results = mutableListOf<DomainResult<InternalCommunication>>()

        coroutineScope {
            val jobs = (1..count).map { i ->
                async(Dispatchers.Default) {
                    repository.createCommunication(
                        projectId = projectId,
                        senderUserId = "STAFF-$i",
                        senderRole = UserRole.STAFF,
                        recipientType = InternalCommunicationRecipientType.USER,
                        recipientUserIds = setOf("STAFF-TARGET"),
                        communicationType = InternalCommunicationType.DIRECT_MESSAGE,
                        subject = "Sync #$i",
                        message = "Message from worker $i",
                        actorId = "STAFF-$i",
                        callerRole = UserRole.STAFF
                    )
                }
            }
            results.addAll(jobs.awaitAll())
        }

        assertTrue(results.all { it is DomainResult.Success })
        val numbers = results.map { (it as DomainResult.Success).data.communicationNo }
        assertEquals(count, numbers.toSet().size)
    }

    @Test
    fun `thread creation and reply workflow`() = runBlocking {
        val threadRes = repository.createThread(
            projectId = projectId,
            subject = "Job #505 Production Scheduling",
            initialMessage = "When will the lamination finish?",
            senderUserId = "PLANNER-01",
            senderRole = UserRole.STAFF,
            recipientType = InternalCommunicationRecipientType.USER,
            recipientUserIds = setOf("OPERATOR-01"),
            actorId = "PLANNER-01",
            callerRole = UserRole.STAFF
        )
        assertTrue(threadRes is DomainResult.Success)
        val thread = (threadRes as DomainResult.Success).data
        assertTrue(thread.threadId.startsWith("THR-"))

        val replyRes = repository.replyToThread(
            projectId = projectId,
            threadId = thread.threadId,
            replyMessage = "Lamination scheduled for 3:00 PM today.",
            senderUserId = "OPERATOR-01",
            senderRole = UserRole.STAFF,
            actorId = "OPERATOR-01",
            callerRole = UserRole.STAFF
        )
        assertTrue(replyRes is DomainResult.Success)

        val msgsRes = repository.getThreadMessages(projectId, thread.threadId, "PLANNER-01", UserRole.STAFF)
        assertTrue(msgsRes is DomainResult.Success)
        assertEquals(2, (msgsRes as DomainResult.Success).data.size)
    }

    @Test
    fun `mention creates mention record and notification in Step 01 engine`() = runBlocking {
        val commRes = repository.createCommunication(
            projectId = projectId,
            senderUserId = "DESIGNER-01",
            senderRole = UserRole.DESIGNER,
            recipientType = InternalCommunicationRecipientType.USER,
            recipientUserIds = setOf("QC-01"),
            communicationType = InternalCommunicationType.DESIGN_DISCUSSION,
            subject = "Proof Review",
            message = "Please review artwork proof.",
            actorId = "DESIGNER-01",
            callerRole = UserRole.DESIGNER
        )
        val comm = (commRes as DomainResult.Success).data

        val mentionRes = repository.createMention(
            projectId = projectId,
            communicationId = comm.communicationId,
            mentionedUserId = "QC-01",
            actorId = "DESIGNER-01",
            callerRole = UserRole.DESIGNER
        )
        assertTrue(mentionRes is DomainResult.Success)
        val mention = (mentionRes as DomainResult.Success).data
        assertTrue(mention.mentionId.startsWith("MNT-"))

        val mentionsRes = repository.getMentions(projectId, "QC-01", "QC-01", UserRole.QC_INSPECTOR)
        assertTrue(mentionsRes is DomainResult.Success)
        assertEquals(1, (mentionsRes as DomainResult.Success).data.size)
    }

    @Test
    fun `acknowledgement creates immutable ACK record and updates communication state`() = runBlocking {
        val commRes = repository.createCommunication(
            projectId = projectId,
            senderUserId = adminActorId,
            senderRole = adminRole,
            recipientType = InternalCommunicationRecipientType.USER,
            recipientUserIds = setOf("OPERATOR-02"),
            communicationType = InternalCommunicationType.URGENT_NOTICE,
            priority = InternalCommunicationPriority.CRITICAL,
            subject = "Immediate Machine Maintenance",
            message = "Stop Press #2 immediately for maintenance inspection.",
            requiresAcknowledgement = true,
            actorId = adminActorId,
            callerRole = adminRole
        )
        val comm = (commRes as DomainResult.Success).data

        repository.queueCommunication(projectId, comm.communicationId, adminActorId, adminRole)

        val ackRes = repository.acknowledge(projectId, comm.communicationId, "Acknowledged and stopped press #2", "OPERATOR-02", UserRole.STAFF)
        assertTrue(ackRes is DomainResult.Success)
        val ack = (ackRes as DomainResult.Success).data
        assertTrue(ack.acknowledgementId.startsWith("ACK-"))

        val updatedComm = repository.getCommunication(projectId, comm.communicationId, "OPERATOR-02", UserRole.STAFF)
        assertTrue(updatedComm is DomainResult.Success)
        assertTrue((updatedComm as DomainResult.Success).data.isAcknowledged)
    }
}

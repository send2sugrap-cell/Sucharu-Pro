package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeCustomerCommunicationDataSource
import com.sucharu.sucharupro.data.datasource.FakeNotificationDataSource
import com.sucharu.sucharupro.data.notification.InMemoryNotificationDeliveryProvider
import com.sucharu.sucharupro.data.notification.NotificationDeliveryServiceImpl
import com.sucharu.sucharupro.data.repository.CustomerCommunicationRepositoryImpl
import com.sucharu.sucharupro.data.repository.NotificationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.customer.*
import com.sucharu.sucharupro.domain.model.notification.NotificationPriority
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.communication.customer.CustomerCommunicationRepository
import com.sucharu.sucharupro.domain.repository.notification.NotificationRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CustomerCommunicationEndToEndTest {

    private lateinit var customerCommunicationDataSource: FakeCustomerCommunicationDataSource
    private lateinit var notificationDataSource: FakeNotificationDataSource
    private lateinit var notificationRepository: NotificationRepository
    private lateinit var repository: CustomerCommunicationRepository

    private val projectId = "PRJ-E2E-TEST"
    private val adminActorId = "ADMIN-001"
    private val adminRole = UserRole.ADMIN
    private val customerId = "CUST-E2E-1"

    @Before
    fun setUp() {
        customerCommunicationDataSource = FakeCustomerCommunicationDataSource()
        notificationDataSource = FakeNotificationDataSource()
        val deliveryService = NotificationDeliveryServiceImpl(listOf(InMemoryNotificationDeliveryProvider()))
        notificationRepository = NotificationRepositoryImpl(notificationDataSource, deliveryService)
        repository = CustomerCommunicationRepositoryImpl(customerCommunicationDataSource, notificationRepository)
    }

    @Test
    fun `full end to end customer communication lifecycle from draft to acknowledged and engagement metric update`() = runBlocking {
        // 1. Evaluate policy
        val policy = CustomerCommunicationPolicy.evaluateEvent("DESIGN_APPROVAL_REQUIRED")
        assertTrue(policy.shouldCommunicate)

        // 2. Create communication
        val createRes = repository.createCommunication(
            projectId = projectId,
            customerId = customerId,
            communicationType = policy.communicationType,
            priority = policy.defaultPriority,
            title = "Design Approval for Order #777",
            message = "Your customized box packaging artwork is ready for review.",
            referenceType = "DESIGN_JOB",
            referenceId = "DJ-777",
            actorId = adminActorId,
            callerRole = adminRole
        )
        assertTrue(createRes is DomainResult.Success)
        val comm = (createRes as DomainResult.Success).data
        assertEquals(CustomerCommunicationStatus.DRAFT, comm.status)

        // 3. Queue and deliver via Canonical Notification Engine
        val queueRes = repository.queueCommunication(projectId, comm.communicationId, adminActorId, adminRole)
        assertTrue(queueRes is DomainResult.Success)
        val dispatchedComm = (queueRes as DomainResult.Success).data
        assertEquals(CustomerCommunicationStatus.DELIVERED, dispatchedComm.status)
        assertTrue(dispatchedComm.isDelivered)

        // 4. Customer reads communication
        val readRes = repository.markRead(projectId, comm.communicationId, customerId, UserRole.CUSTOMER)
        assertTrue(readRes is DomainResult.Success)
        val readComm = (readRes as DomainResult.Success).data
        assertEquals(CustomerCommunicationStatus.READ, readComm.status)
        assertTrue(readComm.isRead)

        // 5. Customer acknowledges communication
        val ackRes = repository.markAcknowledged(projectId, comm.communicationId, customerId, UserRole.CUSTOMER)
        assertTrue(ackRes is DomainResult.Success)
        val ackComm = (ackRes as DomainResult.Success).data
        assertEquals(CustomerCommunicationStatus.ACKNOWLEDGED, ackComm.status)
        assertTrue(ackComm.isAcknowledged)

        // 6. Verify Engagement Analytics
        val engSummaryRes = repository.getEngagementSummary(projectId, customerId, customerId, UserRole.CUSTOMER)
        assertTrue(engSummaryRes is DomainResult.Success)
        val engSummary = (engSummaryRes as DomainResult.Success).data
        assertEquals(1, engSummary.messagesDelivered)
        assertEquals(1, engSummary.messagesRead)
        assertEquals(1, engSummary.messagesAcknowledged)
        assertEquals(100.0, engSummary.readRatePercent, 0.01)
        assertEquals(100.0, engSummary.acknowledgementRatePercent, 0.01)

        // 7. Verify History audit trail
        val histRes = repository.getHistory(projectId, comm.communicationId, customerId, UserRole.CUSTOMER)
        assertTrue(histRes is DomainResult.Success)
        val history = (histRes as DomainResult.Success).data
        assertEquals(4, history.size) // Created -> Dispatched -> Read -> Acknowledged
    }
}

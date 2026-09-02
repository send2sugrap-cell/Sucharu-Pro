package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeCustomerCommunicationDataSource
import com.sucharu.sucharupro.data.datasource.FakeNotificationDataSource
import com.sucharu.sucharupro.data.notification.InMemoryNotificationDeliveryProvider
import com.sucharu.sucharupro.data.notification.NotificationDeliveryServiceImpl
import com.sucharu.sucharupro.data.repository.CustomerCommunicationRepositoryImpl
import com.sucharu.sucharupro.data.repository.NotificationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.customer.*
import com.sucharu.sucharupro.domain.model.notification.NotificationChannel
import com.sucharu.sucharupro.domain.model.notification.NotificationPriority
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.communication.customer.CustomerCommunicationRepository
import com.sucharu.sucharupro.domain.repository.notification.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CustomerCommunicationRepositoryTest {

    private lateinit var customerCommunicationDataSource: FakeCustomerCommunicationDataSource
    private lateinit var notificationDataSource: FakeNotificationDataSource
    private lateinit var notificationRepository: NotificationRepository
    private lateinit var repository: CustomerCommunicationRepository

    private val projectId = "PRJ-COMM-TEST"
    private val adminActorId = "ADMIN-001"
    private val adminRole = UserRole.ADMIN

    @Before
    fun setUp() {
        customerCommunicationDataSource = FakeCustomerCommunicationDataSource()
        notificationDataSource = FakeNotificationDataSource()
        val deliveryService = NotificationDeliveryServiceImpl(listOf(InMemoryNotificationDeliveryProvider()))
        notificationRepository = NotificationRepositoryImpl(notificationDataSource, deliveryService)
        repository = CustomerCommunicationRepositoryImpl(customerCommunicationDataSource, notificationRepository)
    }

    @Test
    fun `createCommunication generates formatted number and creates canonical Notification`() = runBlocking {
        val res = repository.createCommunication(
            projectId = projectId,
            customerId = "CUST-001",
            communicationType = CustomerCommunicationType.ORDER_UPDATE,
            channel = NotificationChannel.IN_APP,
            priority = NotificationPriority.NORMAL,
            title = "Order #100 Confirmed",
            message = "Your print order #100 has been verified.",
            actorId = adminActorId,
            callerRole = adminRole
        )

        assertTrue(res is DomainResult.Success)
        val comm = (res as DomainResult.Success).data
        assertTrue(comm.communicationNo.startsWith("CCM-"))
        assertEquals("CUST-001", comm.customerId)
        assertEquals(CustomerCommunicationStatus.DRAFT, comm.status)

        // Verify canonical notification was created
        val notifRes = notificationRepository.getNotification(projectId, comm.notificationId, adminActorId, adminRole)
        assertTrue(notifRes is DomainResult.Success)
        val notif = (notifRes as DomainResult.Success).data
        assertEquals("CUSTOMER", notif.recipientType)
        assertEquals("CUST-001", notif.recipientUserId)
    }

    @Test
    fun `idempotency test guarantees single creation on duplicate calls`() = runBlocking {
        val idemKey = "COMM-IDEM-001"

        val first = repository.createCommunication(
            projectId = projectId,
            customerId = "CUST-001",
            communicationType = CustomerCommunicationType.PAYMENT_RECEIVED,
            title = "Payment Acknowledged",
            message = "5,000 BDT received.",
            idempotencyKey = idemKey,
            actorId = adminActorId,
            callerRole = adminRole
        )
        assertTrue(first is DomainResult.Success)

        val second = repository.createCommunication(
            projectId = projectId,
            customerId = "CUST-001",
            communicationType = CustomerCommunicationType.PAYMENT_RECEIVED,
            title = "Payment Acknowledged (Repeat)",
            message = "5,000 BDT received.",
            idempotencyKey = idemKey,
            actorId = adminActorId,
            callerRole = adminRole
        )
        assertTrue(second is DomainResult.Success)

        assertEquals((first as DomainResult.Success).data.communicationId, (second as DomainResult.Success).data.communicationId)
        assertEquals(first.data.communicationNo, second.data.communicationNo)
    }

    @Test
    fun `concurrency test with 25 coroutines guarantees thread safety and distinct numbers`() = runBlocking {
        val coroutines = 25
        val results = mutableListOf<DomainResult<CustomerCommunication>>()

        coroutineScope {
            val jobs = (1..coroutines).map { i ->
                async(Dispatchers.Default) {
                    repository.createCommunication(
                        projectId = projectId,
                        customerId = "CUST-$i",
                        communicationType = CustomerCommunicationType.GENERAL_MESSAGE,
                        title = "Notice #$i",
                        message = "Message for customer $i",
                        actorId = adminActorId,
                        callerRole = adminRole
                    )
                }
            }
            results.addAll(jobs.awaitAll())
        }

        assertTrue(results.all { it is DomainResult.Success })
        val numbers = results.map { (it as DomainResult.Success).data.communicationNo }
        assertEquals(coroutines, numbers.toSet().size)
    }

    @Test
    fun `customer isolation prevents Customer A from querying Customer B communications`() = runBlocking {
        // Create comm for Customer A
        repository.createCommunication(
            projectId = projectId,
            customerId = "CUST-A",
            communicationType = CustomerCommunicationType.ORDER_UPDATE,
            title = "Order for A",
            message = "Your order is ready",
            actorId = adminActorId,
            callerRole = adminRole
        )

        // Customer B tries to query Customer A's communications
        val crossQuery = repository.getCustomerCommunications(
            projectId = projectId,
            targetCustomerId = "CUST-A",
            actorId = "CUST-B",
            callerRole = UserRole.CUSTOMER
        )
        assertTrue(crossQuery is DomainResult.Error)

        // Customer A querying own communications
        val ownQuery = repository.getCustomerCommunications(
            projectId = projectId,
            targetCustomerId = "CUST-A",
            actorId = "CUST-A",
            callerRole = UserRole.CUSTOMER
        )
        assertTrue(ownQuery is DomainResult.Success)
        assertEquals(1, (ownQuery as DomainResult.Success).data.size)
    }

    @Test
    fun `acknowledgement marks status and creates engagement event`() = runBlocking {
        val createRes = repository.createCommunication(
            projectId = projectId,
            customerId = "CUST-002",
            communicationType = CustomerCommunicationType.APPROVAL_REQUEST,
            title = "Approve Design #456",
            message = "Please review artwork proof.",
            actorId = adminActorId,
            callerRole = adminRole
        )
        val comm = (createRes as DomainResult.Success).data

        repository.queueCommunication(projectId, comm.communicationId, adminActorId, adminRole)

        val ackRes = repository.markAcknowledged(projectId, comm.communicationId, "CUST-002", UserRole.CUSTOMER)
        assertTrue(ackRes is DomainResult.Success)
        val ackComm = (ackRes as DomainResult.Success).data
        assertEquals(CustomerCommunicationStatus.ACKNOWLEDGED, ackComm.status)
        assertTrue(ackComm.isAcknowledged)

        // Verify engagement event recorded
        val eventsRes = repository.getEngagementEvents(projectId, "CUST-002", "CUST-002", UserRole.CUSTOMER)
        assertTrue(eventsRes is DomainResult.Success)
        val events = (eventsRes as DomainResult.Success).data
        assertTrue(events.any { it.eventType == CustomerEngagementEventType.COMMUNICATION_ACKNOWLEDGED })
    }
}

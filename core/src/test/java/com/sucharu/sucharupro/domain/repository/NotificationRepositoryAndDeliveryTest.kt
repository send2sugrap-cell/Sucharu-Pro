package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeNotificationDataSource
import com.sucharu.sucharupro.data.notification.InMemoryNotificationDeliveryProvider
import com.sucharu.sucharupro.data.notification.NotificationDeliveryServiceImpl
import com.sucharu.sucharupro.data.repository.NotificationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.notification.*
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.notification.NotificationRepository
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class NotificationRepositoryAndDeliveryTest {

    private lateinit var dataSource: FakeNotificationDataSource
    private lateinit var repository: NotificationRepository

    private val projectId = "PRJ-NOTIF-TEST"
    private val actorId = "ADMIN-001"
    private val adminRole = UserRole.ADMIN

    @Before
    fun setUp() {
        dataSource = FakeNotificationDataSource()
        val deliveryService = NotificationDeliveryServiceImpl(
            listOf(InMemoryNotificationDeliveryProvider())
        )
        repository = NotificationRepositoryImpl(dataSource, deliveryService)
    }

    @Test
    fun `createNotification creates draft and records activity audit event`() = runBlocking {
        val res = repository.createNotification(
            projectId = projectId,
            recipientUserId = "USER-101",
            notificationType = NotificationType.ORDER_CREATED,
            title = "Order #500 Created",
            message = "Your order #500 is in processing.",
            actorId = actorId,
            callerRole = adminRole
        )

        assertTrue(res is DomainResult.Success)
        val notif = (res as DomainResult.Success).data
        assertEquals(NotificationStatus.DRAFT, notif.status)
        assertTrue(notif.notificationNo.startsWith("NTF-"))

        val eventsRes = repository.getActivityEvents(projectId, notif.notificationId, actorId, adminRole)
        assertTrue(eventsRes is DomainResult.Success)
        val events = (eventsRes as DomainResult.Success).data
        assertTrue(events.any { it.eventType == NotificationActivityEventType.NOTIFICATION_CREATED })
    }

    @Test
    fun `idempotency test returns existing notification without creating duplicates`() = runBlocking {
        val idemKey = "IDEM-KEY-999"

        val firstRes = repository.createNotification(
            projectId = projectId,
            recipientUserId = "USER-101",
            notificationType = NotificationType.PAYMENT_RECEIVED,
            title = "Payment Received",
            message = "Payment of 5000 BDT acknowledged.",
            idempotencyKey = idemKey,
            actorId = actorId,
            callerRole = adminRole
        )
        assertTrue(firstRes is DomainResult.Success)
        val firstNotif = (firstRes as DomainResult.Success).data

        val secondRes = repository.createNotification(
            projectId = projectId,
            recipientUserId = "USER-101",
            notificationType = NotificationType.PAYMENT_RECEIVED,
            title = "Payment Received (Duplicate call)",
            message = "Payment of 5000 BDT acknowledged.",
            idempotencyKey = idemKey,
            actorId = actorId,
            callerRole = adminRole
        )
        assertTrue(secondRes is DomainResult.Success)
        val secondNotif = (secondRes as DomainResult.Success).data

        assertEquals(firstNotif.notificationId, secondNotif.notificationId)
        assertEquals(firstNotif.notificationNo, secondNotif.notificationNo)
    }

    @Test
    fun `concurrency test with 25 concurrent operations guarantees safety and uniqueness`() = runBlocking {
        val coroutineCount = 25
        val results = mutableListOf<DomainResult<Notification>>()

        coroutineScope {
            val jobs = (1..coroutineCount).map { i ->
                async(Dispatchers.Default) {
                    repository.createNotification(
                        projectId = projectId,
                        recipientUserId = "USER-$i",
                        notificationType = NotificationType.GENERAL,
                        title = "Bulk Alert #$i",
                        message = "Message content for worker $i",
                        actorId = actorId,
                        callerRole = adminRole
                    )
                }
            }
            results.addAll(jobs.awaitAll())
        }

        assertTrue(results.all { it is DomainResult.Success })
        val numbers = results.map { (it as DomainResult.Success).data.notificationNo }
        assertEquals(coroutineCount, numbers.toSet().size) // All distinct
    }

    @Test
    fun `delivery failure and successful retry execution`() = runBlocking {
        val failingProvider = InMemoryNotificationDeliveryProvider(
            providerName = "FAILING_SIMULATOR",
            shouldSimulateFailure = true
        )
        val failingService = NotificationDeliveryServiceImpl(listOf(failingProvider))
        val failingRepo = NotificationRepositoryImpl(dataSource, failingService)

        val createRes = failingRepo.createNotification(
            projectId = projectId,
            recipientUserId = "USER-202",
            notificationType = NotificationType.STOCK_LOW,
            title = "Stock Alert",
            message = "Paper stock low",
            actorId = actorId,
            callerRole = adminRole
        )
        val notif = (createRes as DomainResult.Success).data

        failingRepo.markQueued(projectId, notif.notificationId, actorId, adminRole)
        val procRes = failingRepo.markProcessing(projectId, notif.notificationId, actorId, adminRole)
        assertTrue(procRes is DomainResult.Success)
        val failedNotif = (procRes as DomainResult.Success).data
        assertEquals(NotificationStatus.FAILED, failedNotif.status)

        // Switch to working service and retry
        val workingService = NotificationDeliveryServiceImpl(listOf(InMemoryNotificationDeliveryProvider(shouldSimulateFailure = false)))
        val workingRepo = NotificationRepositoryImpl(dataSource, workingService)

        val retryRes = workingRepo.retryNotification(projectId, notif.notificationId, actorId, adminRole)
        assertTrue(retryRes is DomainResult.Success)
        val deliveredNotif = (retryRes as DomainResult.Success).data
        assertEquals(NotificationStatus.DELIVERED, deliveredNotif.status)
    }

    @Test
    fun `notification template versioning creates immutable historical versions`() = runBlocking {
        val template = NotificationTemplate(
            templateId = "tmpl-01",
            projectId = projectId,
            templateCode = "ORDER_CONFIRMATION",
            notificationType = NotificationType.ORDER_CREATED,
            channel = NotificationChannel.IN_APP,
            titleTemplate = "Order Created",
            messageTemplate = "Your order {orderNo} is confirmed.",
            version = 1,
            createdBy = actorId
        )

        repository.createTemplate(template, actorId, adminRole)

        val v2Res = repository.createTemplateVersion(
            projectId = projectId,
            templateCode = "ORDER_CONFIRMATION",
            titleTemplate = "Order #{orderNo} Confirmed",
            messageTemplate = "Dear Customer, your order #{orderNo} has been confirmed.",
            actorId = actorId,
            callerRole = adminRole
        )
        assertTrue(v2Res is DomainResult.Success)
        val v2 = (v2Res as DomainResult.Success).data
        assertEquals(2, v2.version)

        // Historical v1 preserved
        val v1 = repository.getTemplate(projectId, "tmpl-01", actorId, adminRole)
        assertTrue(v1 is DomainResult.Success)
        assertEquals(1, (v1 as DomainResult.Success).data.version)
    }

    @Test
    fun `end to end notification lifecycle flow`() = runBlocking {
        // 1. Create Draft
        val createRes = repository.createNotification(
            projectId = projectId,
            recipientUserId = "USER-303",
            notificationType = NotificationType.DELIVERY_DELIVERED,
            title = "Package Delivered",
            message = "Challan #CH-999 delivered to your address.",
            actorId = actorId,
            callerRole = adminRole
        )
        val notif = (createRes as DomainResult.Success).data
        assertEquals(NotificationStatus.DRAFT, notif.status)

        // 2. Queue
        val queueRes = repository.markQueued(projectId, notif.notificationId, actorId, adminRole)
        assertEquals(NotificationStatus.QUEUED, (queueRes as DomainResult.Success).data.status)

        // 3. Process & Deliver
        val procRes = repository.markProcessing(projectId, notif.notificationId, actorId, adminRole)
        assertEquals(NotificationStatus.DELIVERED, (procRes as DomainResult.Success).data.status)

        // 4. Read
        val readRes = repository.markRead(projectId, notif.notificationId, "USER-303", adminRole)
        assertEquals(NotificationStatus.READ, (readRes as DomainResult.Success).data.status)

        // 5. Verify Summary
        val summaryRes = repository.getSummary(projectId, actorId, adminRole)
        assertTrue(summaryRes is DomainResult.Success)
        val summary = (summaryRes as DomainResult.Success).data
        assertEquals(1, summary.totalCount)
        assertEquals(1, summary.readCount)
        assertEquals(0, summary.unreadCount)
    }
}

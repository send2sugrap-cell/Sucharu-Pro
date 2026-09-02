package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.notification.InMemoryNotificationDeliveryProvider
import com.sucharu.sucharupro.data.notification.NotificationDeliveryServiceImpl
import com.sucharu.sucharupro.data.repository.FinancialTransactionRepositoryImpl
import com.sucharu.sucharupro.data.repository.NotificationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.FinancialEntryType
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionType
import com.sucharu.sucharupro.domain.model.notification.NotificationChannel
import com.sucharu.sucharupro.domain.model.notification.NotificationPriority
import com.sucharu.sucharupro.domain.model.notification.NotificationType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.notification.NotificationRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NotificationBoundaryTest {

    private lateinit var notificationDataSource: FakeNotificationDataSource
    private lateinit var notificationRepository: NotificationRepository

    private lateinit var financialTransactionDataSource: FakeFinancialTransactionDataSource
    private lateinit var financialTransactionRepository: FinancialTransactionRepository

    private val projectId = "PRJ-BOUNDARY-TEST"
    private val actorId = "ADMIN-001"
    private val adminRole = UserRole.ADMIN

    @Before
    fun setUp() {
        notificationDataSource = FakeNotificationDataSource()
        notificationRepository = NotificationRepositoryImpl(
            notificationDataSource = notificationDataSource,
            deliveryService = NotificationDeliveryServiceImpl(listOf(InMemoryNotificationDeliveryProvider()))
        )

        financialTransactionDataSource = FakeFinancialTransactionDataSource()
        financialTransactionRepository = FinancialTransactionRepositoryImpl(financialTransactionDataSource)
    }

    @Test
    fun `Notification layer does not mutate financial records`() = runBlocking {
        // Setup initial financial transaction in Module 09
        val txRes = financialTransactionRepository.createTransaction(
            projectId = projectId,
            transactionType = FinancialTransactionType.SALE,
            entryType = FinancialEntryType.DEBIT,
            amount = Money(50000),
            referenceType = FinancialReferenceType.ORDER,
            referenceId = "ord-boundary-1",
            description = "Sales revenue",
            actorId = actorId,
            callerRole = adminRole
        )
        assertTrue(txRes is DomainResult.Success)
        val initialTx = (txRes as DomainResult.Success).data
        val initialTxList = financialTransactionRepository.observeTransactions(projectId, adminRole).first()
        assertEquals(1, initialTxList.size)

        // Perform multiple notification operations referencing the transaction
        val notifRes = notificationRepository.createNotification(
            projectId = projectId,
            recipientUserId = "USER-FINANCE-01",
            notificationType = NotificationType.PAYMENT_RECEIVED,
            channel = NotificationChannel.IN_APP,
            priority = NotificationPriority.HIGH,
            title = "Payment Processed",
            message = "Payment of 50000 BDT acknowledged for Order ord-boundary-1",
            referenceType = "FINANCIAL_TRANSACTION",
            referenceId = initialTx.transactionId,
            actorId = actorId,
            callerRole = adminRole
        )
        assertTrue(notifRes is DomainResult.Success)
        val notif = (notifRes as DomainResult.Success).data

        notificationRepository.markQueued(projectId, notif.notificationId, actorId, adminRole)
        notificationRepository.markProcessing(projectId, notif.notificationId, actorId, adminRole)
        notificationRepository.markRead(projectId, notif.notificationId, "USER-FINANCE-01", adminRole)

        // Verify that financial transaction in Module 09 is completely untouched
        val postNotifTxList = financialTransactionRepository.observeTransactions(projectId, adminRole).first()
        assertEquals(1, postNotifTxList.size)
        assertEquals(initialTx.amount, postNotifTxList.first().amount)
        assertEquals(initialTx.transactionStatus, postNotifTxList.first().transactionStatus)
        assertEquals(initialTx.description, postNotifTxList.first().description)
    }
}

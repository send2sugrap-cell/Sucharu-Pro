package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.notification.InMemoryNotificationDeliveryProvider
import com.sucharu.sucharupro.data.notification.NotificationDeliveryServiceImpl
import com.sucharu.sucharupro.data.repository.FinancialTransactionRepositoryImpl
import com.sucharu.sucharupro.data.repository.InternalCommunicationRepositoryImpl
import com.sucharu.sucharupro.data.repository.NotificationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.communication.internal.InternalCommunicationPriority
import com.sucharu.sucharupro.domain.model.communication.internal.InternalCommunicationRecipientType
import com.sucharu.sucharupro.domain.model.communication.internal.InternalCommunicationType
import com.sucharu.sucharupro.domain.model.finance.FinancialEntryType
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.communication.internal.InternalCommunicationRepository
import com.sucharu.sucharupro.domain.repository.notification.NotificationRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InternalCommunicationBoundaryTest {

    private lateinit var internalCommunicationDataSource: FakeInternalCommunicationDataSource
    private lateinit var notificationDataSource: FakeNotificationDataSource
    private lateinit var notificationRepository: NotificationRepository
    private lateinit var internalCommunicationRepository: InternalCommunicationRepository

    private lateinit var financialTransactionDataSource: FakeFinancialTransactionDataSource
    private lateinit var financialTransactionRepository: FinancialTransactionRepository

    private val projectId = "PRJ-INTERNAL-BOUNDARY"
    private val adminActorId = "ADMIN-001"
    private val adminRole = UserRole.ADMIN

    @Before
    fun setUp() {
        internalCommunicationDataSource = FakeInternalCommunicationDataSource()
        notificationDataSource = FakeNotificationDataSource()
        val deliveryService = NotificationDeliveryServiceImpl(listOf(InMemoryNotificationDeliveryProvider()))
        notificationRepository = NotificationRepositoryImpl(notificationDataSource, deliveryService)
        internalCommunicationRepository = InternalCommunicationRepositoryImpl(internalCommunicationDataSource, notificationRepository)

        financialTransactionDataSource = FakeFinancialTransactionDataSource()
        financialTransactionRepository = FinancialTransactionRepositoryImpl(financialTransactionDataSource)
    }

    @Test
    fun `Internal communication referencing financial transaction does not mutate financial balance or transaction`() = runBlocking {
        val txRes = financialTransactionRepository.createTransaction(
            projectId = projectId,
            transactionType = FinancialTransactionType.EXPENSE,
            entryType = FinancialEntryType.CREDIT,
            amount = Money(15000),
            referenceType = FinancialReferenceType.EXPENSE,
            referenceId = "exp-001",
            description = "Ink batch purchase expense",
            actorId = adminActorId,
            callerRole = adminRole
        )
        assertTrue(txRes is DomainResult.Success)
        val initialTx = (txRes as DomainResult.Success).data

        val commRes = internalCommunicationRepository.createCommunication(
            projectId = projectId,
            senderUserId = "ACCOUNTS-01",
            senderRole = UserRole.ACCOUNTS,
            recipientType = InternalCommunicationRecipientType.USER,
            recipientUserIds = setOf("MANAGER-01"),
            communicationType = InternalCommunicationType.FINANCE_DISCUSSION,
            priority = InternalCommunicationPriority.HIGH,
            subject = "Ink Purchase Approval",
            message = "Approved voucher for ink batch purchase.",
            referenceType = "FINANCIAL_TRANSACTION",
            referenceId = initialTx.transactionId,
            actorId = "ACCOUNTS-01",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(commRes is DomainResult.Success)
        val comm = (commRes as DomainResult.Success).data

        internalCommunicationRepository.queueCommunication(projectId, comm.communicationId, "ACCOUNTS-01", UserRole.ACCOUNTS)
        internalCommunicationRepository.markRead(projectId, comm.communicationId, "MANAGER-01", UserRole.MANAGER)
        internalCommunicationRepository.acknowledge(projectId, comm.communicationId, "Verified voucher", "MANAGER-01", UserRole.MANAGER)

        // Verify financial transactions are untouched
        val postTxs = financialTransactionRepository.observeTransactions(projectId, adminRole).first()
        assertEquals(1, postTxs.size)
        assertEquals(initialTx.amount, postTxs.first().amount)
        assertEquals(initialTx.transactionStatus, postTxs.first().transactionStatus)
        assertEquals(initialTx.description, postTxs.first().description)
    }
}

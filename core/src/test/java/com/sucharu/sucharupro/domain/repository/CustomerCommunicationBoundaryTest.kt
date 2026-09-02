package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.notification.InMemoryNotificationDeliveryProvider
import com.sucharu.sucharupro.data.notification.NotificationDeliveryServiceImpl
import com.sucharu.sucharupro.data.repository.CustomerCommunicationRepositoryImpl
import com.sucharu.sucharupro.data.repository.FinancialTransactionRepositoryImpl
import com.sucharu.sucharupro.data.repository.NotificationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.communication.customer.CustomerCommunicationType
import com.sucharu.sucharupro.domain.model.finance.FinancialEntryType
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.communication.customer.CustomerCommunicationRepository
import com.sucharu.sucharupro.domain.repository.notification.NotificationRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CustomerCommunicationBoundaryTest {

    private lateinit var customerCommunicationDataSource: FakeCustomerCommunicationDataSource
    private lateinit var notificationDataSource: FakeNotificationDataSource
    private lateinit var notificationRepository: NotificationRepository
    private lateinit var customerCommunicationRepository: CustomerCommunicationRepository

    private lateinit var financialTransactionDataSource: FakeFinancialTransactionDataSource
    private lateinit var financialTransactionRepository: FinancialTransactionRepository

    private val projectId = "PRJ-BOUNDARY-02"
    private val adminActorId = "ADMIN-001"
    private val adminRole = UserRole.ADMIN

    @Before
    fun setUp() {
        customerCommunicationDataSource = FakeCustomerCommunicationDataSource()
        notificationDataSource = FakeNotificationDataSource()
        val deliveryService = NotificationDeliveryServiceImpl(listOf(InMemoryNotificationDeliveryProvider()))
        notificationRepository = NotificationRepositoryImpl(notificationDataSource, deliveryService)
        customerCommunicationRepository = CustomerCommunicationRepositoryImpl(customerCommunicationDataSource, notificationRepository)

        financialTransactionDataSource = FakeFinancialTransactionDataSource()
        financialTransactionRepository = FinancialTransactionRepositoryImpl(financialTransactionDataSource)
    }

    @Test
    fun `Customer communication actions do not mutate financial transactions or balances`() = runBlocking {
        // 1. Create financial transaction
        val txRes = financialTransactionRepository.createTransaction(
            projectId = projectId,
            transactionType = FinancialTransactionType.SALE,
            entryType = FinancialEntryType.DEBIT,
            amount = Money(25000),
            referenceType = FinancialReferenceType.ORDER,
            referenceId = "ord-boundary-02",
            description = "Print order invoice",
            actorId = adminActorId,
            callerRole = adminRole
        )
        assertTrue(txRes is DomainResult.Success)
        val initialTx = (txRes as DomainResult.Success).data
        val initialList = financialTransactionRepository.observeTransactions(projectId, adminRole).first()
        assertEquals(1, initialList.size)

        // 2. Dispatch Customer Communication referencing the transaction
        val commRes = customerCommunicationRepository.createCommunication(
            projectId = projectId,
            customerId = "CUST-B02",
            communicationType = CustomerCommunicationType.PAYMENT_DUE,
            title = "Payment Due Notice",
            message = "Invoice for Order #ord-boundary-02 is due for payment.",
            referenceType = "FINANCIAL_TRANSACTION",
            referenceId = initialTx.transactionId,
            actorId = adminActorId,
            callerRole = adminRole
        )
        assertTrue(commRes is DomainResult.Success)
        val comm = (commRes as DomainResult.Success).data

        customerCommunicationRepository.queueCommunication(projectId, comm.communicationId, adminActorId, adminRole)
        customerCommunicationRepository.markRead(projectId, comm.communicationId, "CUST-B02", UserRole.CUSTOMER)
        customerCommunicationRepository.markAcknowledged(projectId, comm.communicationId, "CUST-B02", UserRole.CUSTOMER)

        // 3. Verify Financial Record remains completely unmutated
        val postList = financialTransactionRepository.observeTransactions(projectId, adminRole).first()
        assertEquals(1, postList.size)
        assertEquals(initialTx.amount, postList.first().amount)
        assertEquals(initialTx.transactionStatus, postList.first().transactionStatus)
        assertEquals(initialTx.description, postList.first().description)
    }
}

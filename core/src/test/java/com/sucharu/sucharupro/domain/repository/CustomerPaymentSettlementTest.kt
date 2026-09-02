package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeCustomerPaymentDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerReceivableDataSource
import com.sucharu.sucharupro.data.datasource.FakeFinancialTransactionDataSource
import com.sucharu.sucharupro.data.repository.CustomerPaymentRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerReceivableRepositoryImpl
import com.sucharu.sucharupro.data.repository.FinancialTransactionRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.CustomerPaymentMethod
import com.sucharu.sucharupro.domain.model.finance.CustomerPaymentStatus
import com.sucharu.sucharupro.domain.model.finance.CustomerReceivableStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerPaymentSettlementTest {

    private lateinit var paymentDataSource: FakeCustomerPaymentDataSource
    private lateinit var receivableDataSource: FakeCustomerReceivableDataSource
    private lateinit var financeTransactionDataSource: FakeFinancialTransactionDataSource

    private lateinit var receivableRepository: CustomerReceivableRepository
    private lateinit var financialTransactionRepository: FinancialTransactionRepository
    private lateinit var paymentRepository: CustomerPaymentRepository

    @Before
    fun setUp() {
        paymentDataSource = FakeCustomerPaymentDataSource()
        receivableDataSource = FakeCustomerReceivableDataSource()
        financeTransactionDataSource = FakeFinancialTransactionDataSource()

        receivableRepository = CustomerReceivableRepositoryImpl(receivableDataSource)
        financialTransactionRepository = FinancialTransactionRepositoryImpl(financeTransactionDataSource)
        paymentRepository = CustomerPaymentRepositoryImpl(
            paymentDataSource,
            receivableRepository,
            financialTransactionRepository
        )
    }

    @Test
    fun `partial payment reduces receivable outstanding and sets status to PARTIALLY_SETTLED`() = runBlocking {
        // Create 100,000 receivable
        val recRes = receivableRepository.createReceivable(
            projectId = "PRJ-01",
            customerId = "CUST-001",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-001",
            originalAmount = Money(BigDecimal("100000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "100k Invoice",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        val recId = (recRes as DomainResult.Success).data.receivableId

        // Create and Post 30,000 payment
        val payRes = paymentRepository.createPayment(
            projectId = "PRJ-01",
            customerId = "CUST-001",
            receivableId = recId,
            amount = Money(BigDecimal("30000.00")),
            currency = "BDT",
            paymentMethod = CustomerPaymentMethod.BANK_TRANSFER,
            paymentReference = "EFT-1001",
            notes = "30% advance payment",
            actorId = "staff-1",
            callerRole = UserRole.STAFF
        )
        assertTrue(payRes is DomainResult.Success)
        val payId = (payRes as DomainResult.Success).data.paymentId

        val postRes = paymentRepository.postPayment(payId, "BANK_ACCOUNT", "acct-1", UserRole.ACCOUNTS)
        assertTrue(postRes is DomainResult.Success)
        val postedPayment = (postRes as DomainResult.Success).data
        assertEquals(CustomerPaymentStatus.POSTED, postedPayment.status)

        // Verify updated receivable
        val updatedRecRes = receivableRepository.getReceivableById(recId, UserRole.ACCOUNTS)
        assertTrue(updatedRecRes is DomainResult.Success)
        val updatedRec = (updatedRecRes as DomainResult.Success).data
        assertEquals(Money(BigDecimal("30000.00")), updatedRec.settledAmount)
        assertEquals(Money(BigDecimal("70000.00")), updatedRec.outstandingAmount)
        assertEquals(CustomerReceivableStatus.PARTIALLY_SETTLED, updatedRec.status)
    }

    @Test
    fun `full payment reduces outstanding to zero and sets receivable status to SETTLED`() = runBlocking {
        // Create 50,000 receivable
        val recRes = receivableRepository.createReceivable(
            projectId = "PRJ-01",
            customerId = "CUST-001",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-002",
            originalAmount = Money(BigDecimal("50000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "50k Invoice",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        val recId = (recRes as DomainResult.Success).data.receivableId

        // Pay full 50,000
        val payRes = paymentRepository.createPayment(
            projectId = "PRJ-01",
            customerId = "CUST-001",
            receivableId = recId,
            amount = Money(BigDecimal("50000.00")),
            currency = "BDT",
            paymentMethod = CustomerPaymentMethod.CASH,
            actorId = "staff-1",
            callerRole = UserRole.STAFF
        )
        val payId = (payRes as DomainResult.Success).data.paymentId

        paymentRepository.postPayment(payId, "CASH_IN_HAND", "acct-1", UserRole.ACCOUNTS)

        val updatedRec = (receivableRepository.getReceivableById(recId, UserRole.ACCOUNTS) as DomainResult.Success).data
        assertEquals(Money(BigDecimal("50000.00")), updatedRec.settledAmount)
        assertEquals(Money.ZERO, updatedRec.outstandingAmount)
        assertEquals(CustomerReceivableStatus.SETTLED, updatedRec.status)
    }

    @Test
    fun `overpayment exceeding outstanding receivable balance is rejected`() = runBlocking {
        val recRes = receivableRepository.createReceivable(
            projectId = "PRJ-01",
            customerId = "CUST-001",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-003",
            originalAmount = Money(BigDecimal("10000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "10k Invoice",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        val recId = (recRes as DomainResult.Success).data.receivableId

        val payRes = paymentRepository.createPayment(
            projectId = "PRJ-01",
            customerId = "CUST-001",
            receivableId = recId,
            amount = Money(BigDecimal("15000.00")),
            currency = "BDT",
            paymentMethod = CustomerPaymentMethod.CASH,
            actorId = "staff-1",
            callerRole = UserRole.STAFF
        )

        assertTrue(payRes is DomainResult.Error)
    }
}

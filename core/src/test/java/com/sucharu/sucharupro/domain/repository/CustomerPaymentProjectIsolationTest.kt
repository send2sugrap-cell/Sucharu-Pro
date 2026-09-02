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
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerPaymentProjectIsolationTest {

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
    fun `payment and receipt streams strictly enforce tenant project scoping`() = runBlocking {
        // Setup Project A
        val rA = receivableRepository.createReceivable(
            projectId = "PRJ-A",
            customerId = "CUST-001",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-A",
            originalAmount = Money(BigDecimal("10000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Desc A",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        val recAId = (rA as DomainResult.Success).data.receivableId
        paymentRepository.createPayment(
            projectId = "PRJ-A",
            customerId = "CUST-001",
            receivableId = recAId,
            amount = Money(BigDecimal("10000.00")),
            currency = "BDT",
            paymentMethod = CustomerPaymentMethod.CASH,
            paymentReference = null,
            paymentDate = System.currentTimeMillis(),
            idempotencyKey = null,
            notes = null,
            actorId = "staff-1",
            callerRole = UserRole.STAFF
        )

        // Setup Project B
        val rB = receivableRepository.createReceivable(
            projectId = "PRJ-B",
            customerId = "CUST-001",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-B",
            originalAmount = Money(BigDecimal("20000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Desc B",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        val recBId = (rB as DomainResult.Success).data.receivableId
        paymentRepository.createPayment(
            projectId = "PRJ-B",
            customerId = "CUST-001",
            receivableId = recBId,
            amount = Money(BigDecimal("20000.00")),
            currency = "BDT",
            paymentMethod = CustomerPaymentMethod.CASH,
            paymentReference = null,
            paymentDate = System.currentTimeMillis(),
            idempotencyKey = null,
            notes = null,
            actorId = "staff-1",
            callerRole = UserRole.STAFF
        )

        val prjAPayments = paymentRepository.observePayments("PRJ-A", UserRole.ACCOUNTS).first()
        val prjBPayments = paymentRepository.observePayments("PRJ-B", UserRole.ACCOUNTS).first()

        assertEquals(1, prjAPayments.size)
        assertEquals("PRJ-A", prjAPayments[0].projectId)
        assertEquals(Money(BigDecimal("10000.00")), prjAPayments[0].amount)

        assertEquals(1, prjBPayments.size)
        assertEquals("PRJ-B", prjBPayments[0].projectId)
        assertEquals(Money(BigDecimal("20000.00")), prjBPayments[0].amount)
    }
}

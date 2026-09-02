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
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerPaymentIdempotencyTest {

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
    fun `repeated submission with same idempotency key returns existing payment without creating duplicates`() = runBlocking {
        val recRes = receivableRepository.createReceivable(
            projectId = "PRJ-01",
            customerId = "CUST-001",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-IDEM",
            originalAmount = Money(BigDecimal("40000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Idempotency test invoice",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        val recId = (recRes as DomainResult.Success).data.receivableId

        val idempotencyKey = "IDEM-KEY-12345"

        // First attempt
        val p1Res = paymentRepository.createPayment(
            projectId = "PRJ-01",
            customerId = "CUST-001",
            receivableId = recId,
            amount = Money(BigDecimal("15000.00")),
            currency = "BDT",
            paymentMethod = CustomerPaymentMethod.CASH,
            idempotencyKey = idempotencyKey,
            actorId = "staff-1",
            callerRole = UserRole.STAFF
        )
        assertTrue(p1Res is DomainResult.Success)
        val p1 = (p1Res as DomainResult.Success).data

        // Repeated network retry attempt with identical key
        val p2Res = paymentRepository.createPayment(
            projectId = "PRJ-01",
            customerId = "CUST-001",
            receivableId = recId,
            amount = Money(BigDecimal("15000.00")),
            currency = "BDT",
            paymentMethod = CustomerPaymentMethod.CASH,
            idempotencyKey = idempotencyKey,
            actorId = "staff-1",
            callerRole = UserRole.STAFF
        )
        assertTrue(p2Res is DomainResult.Success)
        val p2 = (p2Res as DomainResult.Success).data

        // Verified same payment returned
        assertEquals(p1.paymentId, p2.paymentId)
        assertEquals(p1.paymentNo, p2.paymentNo)
    }
}

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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerPaymentConcurrencyTest {

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
    fun `concurrent payment postings preserve non-reentrant mutex and receivable invariants`() = runBlocking {
        // Create 100,000 receivable
        val recRes = receivableRepository.createReceivable(
            projectId = "PRJ-01",
            customerId = "CUST-001",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-CONCUR",
            originalAmount = Money(BigDecimal("100000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Concurrency test invoice",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        val recId = (recRes as DomainResult.Success).data.receivableId

        // Create 10 payments of 10,000 each
        val payments = (1..10).map { idx ->
            val pRes = paymentRepository.createPayment(
                projectId = "PRJ-01",
                customerId = "CUST-001",
                receivableId = recId,
                amount = Money(BigDecimal("10000.00")),
                currency = "BDT",
                paymentMethod = CustomerPaymentMethod.CASH,
                actorId = "staff-$idx",
                callerRole = UserRole.STAFF
            )
            (pRes as DomainResult.Success).data
        }

        // Concurrently post all 10 payments (using admin to bypass separation of duties for all)
        val deferredPosts = payments.map { p ->
            async {
                paymentRepository.postPayment(
                    paymentId = p.paymentId,
                    accountHead = "CASH_IN_HAND",
                    actorId = "admin-1",
                    callerRole = UserRole.ADMIN
                )
            }
        }

        val results = deferredPosts.awaitAll()
        results.forEach {
            assertTrue(it is DomainResult.Success)
        }

        val finalRec = (receivableRepository.getReceivableById(recId, UserRole.ACCOUNTS) as DomainResult.Success).data
        assertEquals(Money(BigDecimal("100000.00")), finalRec.settledAmount)
        assertEquals(Money.ZERO, finalRec.outstandingAmount)
    }
}

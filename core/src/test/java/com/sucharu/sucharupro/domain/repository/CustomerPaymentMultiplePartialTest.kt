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
import com.sucharu.sucharupro.domain.model.finance.CustomerReceivableStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerPaymentMultiplePartialTest {

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
    fun `multiple partial payments accumulate correctly towards total settlement`() = runBlocking {
        val recRes = receivableRepository.createReceivable(
            projectId = "PRJ-01",
            customerId = "CUST-001",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-MULTI",
            originalAmount = Money(BigDecimal("90000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Multi-partial invoice",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        val recId = (recRes as DomainResult.Success).data.receivableId

        // Payment 1: 30,000
        val p1Res = paymentRepository.createPayment(
            projectId = "PRJ-01",
            customerId = "CUST-001",
            receivableId = recId,
            amount = Money(BigDecimal("30000.00")),
            currency = "BDT",
            paymentMethod = CustomerPaymentMethod.BANK_TRANSFER,
            paymentReference = "TRX-1",
            actorId = "staff-1",
            callerRole = UserRole.STAFF
        )
        val p1Id = (p1Res as DomainResult.Success).data.paymentId
        paymentRepository.postPayment(p1Id, "BANK_ACCOUNT", "acct-1", UserRole.ACCOUNTS)

        // Payment 2: 40,000
        val p2Res = paymentRepository.createPayment(
            projectId = "PRJ-01",
            customerId = "CUST-001",
            receivableId = recId,
            amount = Money(BigDecimal("40000.00")),
            currency = "BDT",
            paymentMethod = CustomerPaymentMethod.MOBILE_BANKING,
            paymentReference = "TRX-2",
            actorId = "staff-1",
            callerRole = UserRole.STAFF
        )
        val p2Id = (p2Res as DomainResult.Success).data.paymentId
        paymentRepository.postPayment(p2Id, "MOBILE_WALLET", "acct-1", UserRole.ACCOUNTS)

        // Payment 3: 20,000 (Final)
        val p3Res = paymentRepository.createPayment(
            projectId = "PRJ-01",
            customerId = "CUST-001",
            receivableId = recId,
            amount = Money(BigDecimal("20000.00")),
            currency = "BDT",
            paymentMethod = CustomerPaymentMethod.CASH,
            actorId = "staff-1",
            callerRole = UserRole.STAFF
        )
        val p3Id = (p3Res as DomainResult.Success).data.paymentId
        paymentRepository.postPayment(p3Id, "CASH_IN_HAND", "acct-1", UserRole.ACCOUNTS)

        val updatedRec = (receivableRepository.getReceivableById(recId, UserRole.ACCOUNTS) as DomainResult.Success).data
        assertEquals(Money(BigDecimal("90000.00")), updatedRec.settledAmount)
        assertEquals(Money.ZERO, updatedRec.outstandingAmount)
        assertEquals(CustomerReceivableStatus.SETTLED, updatedRec.status)
    }
}

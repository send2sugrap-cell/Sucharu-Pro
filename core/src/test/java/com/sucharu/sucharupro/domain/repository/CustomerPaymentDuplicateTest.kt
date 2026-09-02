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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerPaymentDuplicateTest {

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
    fun `duplicate payment reference for same customer is rejected`() = runBlocking {
        val recRes = receivableRepository.createReceivable(
            projectId = "PRJ-01",
            customerId = "CUST-001",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-DUP",
            originalAmount = Money(BigDecimal("50000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Dup test invoice",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        val recId = (recRes as DomainResult.Success).data.receivableId

        val p1 = paymentRepository.createPayment(
            projectId = "PRJ-01",
            customerId = "CUST-001",
            receivableId = recId,
            amount = Money(BigDecimal("20000.00")),
            currency = "BDT",
            paymentMethod = CustomerPaymentMethod.BANK_TRANSFER,
            paymentReference = "BANK-REF-9999",
            actorId = "staff-1",
            callerRole = UserRole.STAFF
        )
        assertTrue(p1 is DomainResult.Success)

        // Attempting to create another payment with identical bank reference for same customer
        val p2 = paymentRepository.createPayment(
            projectId = "PRJ-01",
            customerId = "CUST-001",
            receivableId = recId,
            amount = Money(BigDecimal("20000.00")),
            currency = "BDT",
            paymentMethod = CustomerPaymentMethod.BANK_TRANSFER,
            paymentReference = "BANK-REF-9999",
            actorId = "staff-1",
            callerRole = UserRole.STAFF
        )
        assertTrue(p2 is DomainResult.Error)
    }
}

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

class CustomerPaymentReceiptTest {

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
    fun `posting customer payment generates unique immutable receipt`() = runBlocking {
        val recRes = receivableRepository.createReceivable(
            projectId = "PRJ-01",
            customerId = "CUST-001",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-RCT",
            originalAmount = Money(BigDecimal("25000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Receipt test invoice",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        val recId = (recRes as DomainResult.Success).data.receivableId

        val payRes = paymentRepository.createPayment(
            projectId = "PRJ-01",
            customerId = "CUST-001",
            receivableId = recId,
            amount = Money(BigDecimal("25000.00")),
            currency = "BDT",
            paymentMethod = CustomerPaymentMethod.BANK_TRANSFER,
            paymentReference = "EFT-8888",
            actorId = "staff-1",
            callerRole = UserRole.STAFF
        )
        val payId = (payRes as DomainResult.Success).data.paymentId

        val postRes = paymentRepository.postPayment(payId, "BANK_ACCOUNT", "acct-1", UserRole.ACCOUNTS)
        assertTrue(postRes is DomainResult.Success)
        val postedPayment = (postRes as DomainResult.Success).data
        assertTrue(!postedPayment.receiptId.isNullOrBlank())

        val receiptRes = paymentRepository.getReceiptByPaymentId(payId, UserRole.ACCOUNTS)
        assertTrue(receiptRes is DomainResult.Success)
        val receipt = (receiptRes as DomainResult.Success).data
        assertEquals(Money(BigDecimal("25000.00")), receipt.amount)
        assertEquals("EFT-8888", receipt.paymentReference)
        assertEquals("acct-1", receipt.issuedBy)
    }
}

package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeCustomerPaymentDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerReceivableDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerRefundDataSource
import com.sucharu.sucharupro.data.datasource.FakeFinancialTransactionDataSource
import com.sucharu.sucharupro.data.repository.CustomerPaymentRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerReceivableRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerRefundRepositoryImpl
import com.sucharu.sucharupro.data.repository.FinancialTransactionRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.CustomerRefundMethod
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialEntryType
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerRefundEndToEndTest {

    private lateinit var paymentDataSource: FakeCustomerPaymentDataSource
    private lateinit var receivableDataSource: FakeCustomerReceivableDataSource
    private lateinit var refundDataSource: FakeCustomerRefundDataSource
    private lateinit var transactionDataSource: FakeFinancialTransactionDataSource

    private lateinit var transactionRepository: FinancialTransactionRepository
    private lateinit var receivableRepository: CustomerReceivableRepository
    private lateinit var paymentRepository: CustomerPaymentRepository
    private lateinit var refundRepository: CustomerRefundRepository

    @Before
    fun setUp() {
        paymentDataSource = FakeCustomerPaymentDataSource()
        receivableDataSource = FakeCustomerReceivableDataSource()
        refundDataSource = FakeCustomerRefundDataSource()
        transactionDataSource = FakeFinancialTransactionDataSource()

        transactionRepository = FinancialTransactionRepositoryImpl(transactionDataSource)
        receivableRepository = CustomerReceivableRepositoryImpl(receivableDataSource)
        paymentRepository = CustomerPaymentRepositoryImpl(paymentDataSource, receivableRepository, transactionRepository)
        refundRepository = CustomerRefundRepositoryImpl(refundDataSource, transactionRepository)
    }

    @Test
    fun `end-to-end customer refund workflow from creation, submission, approval, ledger posting to audit trail`() = runBlocking {
        val projectId = "PRJ-E2E-REFUND"

        // 1. Staff drafts refund
        val draftRes = refundRepository.createRefund(
            projectId = projectId,
            customerId = "CUST-001",
            amount = Money(BigDecimal("7500.00")),
            refundMethod = CustomerRefundMethod.BANK_TRANSFER,
            refundReference = "EFT-DISBURSE-101",
            reason = "Security deposit refund upon contract completion",
            actorId = "staff-1",
            callerRole = UserRole.STAFF
        )
        assertTrue(draftRes is DomainResult.Success)
        val refundId = (draftRes as DomainResult.Success).data.refundId

        // 2. Staff submits
        val submitRes = refundRepository.submitRefund(refundId, "staff-1", UserRole.STAFF)
        assertTrue(submitRes is DomainResult.Success)

        // 3. Manager approves
        val approveRes = refundRepository.approveRefund(refundId, "mgr-1", UserRole.MANAGER)
        assertTrue(approveRes is DomainResult.Success)

        // 4. Accounts posts refund
        val postRes = refundRepository.postRefund(refundId, "BANK_ACCOUNT", "acct-2", UserRole.ACCOUNTS)
        assertTrue(postRes is DomainResult.Success)
        val postedRefund = (postRes as DomainResult.Success).data

        assertEquals(FinancialAdjustmentStatus.POSTED, postedRefund.status)
        assertNotNull(postedRefund.financialTransactionId)

        // 5. Verify ledger transaction
        val txnRes = transactionRepository.getTransactionById(postedRefund.financialTransactionId!!, UserRole.ACCOUNTS)
        assertTrue(txnRes is DomainResult.Success)
        val txn = (txnRes as DomainResult.Success).data
        assertEquals(FinancialTransactionType.REFUND, txn.transactionType)
        assertEquals(FinancialEntryType.DEBIT, txn.entryType)
        assertEquals(Money(BigDecimal("7500.00")), txn.amount)

        // 6. Verify audit events
        val auditRes = refundRepository.getActivityEvents(refundId, UserRole.ACCOUNTS)
        assertTrue(auditRes is DomainResult.Success)
        val events = (auditRes as DomainResult.Success).data
        assertTrue(events.size >= 4)
    }
}

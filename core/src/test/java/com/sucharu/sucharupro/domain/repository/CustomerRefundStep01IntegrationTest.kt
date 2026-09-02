package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeCustomerRefundDataSource
import com.sucharu.sucharupro.data.datasource.FakeFinancialTransactionDataSource
import com.sucharu.sucharupro.data.repository.CustomerRefundRepositoryImpl
import com.sucharu.sucharupro.data.repository.FinancialTransactionRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.CustomerRefundMethod
import com.sucharu.sucharupro.domain.model.finance.FinancialEntryType
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerRefundStep01IntegrationTest {

    private lateinit var refundDataSource: FakeCustomerRefundDataSource
    private lateinit var transactionDataSource: FakeFinancialTransactionDataSource
    private lateinit var transactionRepository: FinancialTransactionRepository
    private lateinit var refundRepository: CustomerRefundRepository

    @Before
    fun setUp() {
        refundDataSource = FakeCustomerRefundDataSource()
        transactionDataSource = FakeFinancialTransactionDataSource()
        transactionRepository = FinancialTransactionRepositoryImpl(transactionDataSource)
        refundRepository = CustomerRefundRepositoryImpl(refundDataSource, transactionRepository)
    }

    @Test
    fun `posting customer refund generates Step 01 REFUND transaction and ledger entry`() = runBlocking {
        val refund = (refundRepository.createRefund(
            projectId = "PRJ-01",
            customerId = "CUST-001",
            amount = Money(BigDecimal("4000.00")),
            refundMethod = CustomerRefundMethod.BANK_TRANSFER,
            refundReference = "EFT-REFUND-99",
            reason = "Step 01 refund integration test",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        ) as DomainResult.Success).data

        val postRes = refundRepository.postRefund(refund.refundId, "BANK_ACCOUNT", "acct-2", UserRole.ACCOUNTS)
        assertTrue(postRes is DomainResult.Success)
        val postedRefund = (postRes as DomainResult.Success).data

        assertNotNull(postedRefund.financialTransactionId)

        val txnRes = transactionRepository.getTransactionById(postedRefund.financialTransactionId!!, UserRole.ACCOUNTS)
        assertTrue(txnRes is DomainResult.Success)
        val txn = (txnRes as DomainResult.Success).data

        assertEquals(FinancialTransactionType.REFUND, txn.transactionType)
        assertEquals(FinancialEntryType.DEBIT, txn.entryType)
        assertEquals(FinancialReferenceType.REFUND, txn.referenceType)
        assertEquals(refund.refundId, txn.referenceId)
        assertEquals(Money(BigDecimal("4000.00")), txn.amount)

        val ledgerRes = transactionRepository.getLedgerEntriesByTransaction(txn.transactionId, UserRole.ACCOUNTS)
        assertTrue(ledgerRes is DomainResult.Success)
        val entries = (ledgerRes as DomainResult.Success).data
        assertEquals(1, entries.size)
        assertEquals("BANK_ACCOUNT", entries[0].accountHead)
        assertEquals(FinancialEntryType.DEBIT, entries[0].entryType)
    }
}

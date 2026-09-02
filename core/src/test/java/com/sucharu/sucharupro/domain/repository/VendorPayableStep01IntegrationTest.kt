package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeFinancialTransactionDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPayableDataSource
import com.sucharu.sucharupro.data.repository.FinancialTransactionRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorPayableRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.FinancialEntryType
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorPayableStep01IntegrationTest {

    private lateinit var financialDataSource: FakeFinancialTransactionDataSource
    private lateinit var payableDataSource: FakeVendorPayableDataSource

    private lateinit var financialTransactionRepository: FinancialTransactionRepository
    private lateinit var payableRepository: VendorPayableRepository

    @Before
    fun setUp() {
        financialDataSource = FakeFinancialTransactionDataSource()
        payableDataSource = FakeVendorPayableDataSource()

        financialTransactionRepository = FinancialTransactionRepositoryImpl(financialDataSource)
        payableRepository = VendorPayableRepositoryImpl(payableDataSource)
    }

    @Test
    fun `payable links directly to Step 01 financial transaction and canonical ledger without secondary ledgers`() = runBlocking {
        val projectId = "PRJ-STEP01-INTEG"

        // 1. Create Step 01 Purchase Expense Transaction
        val txnRes = financialTransactionRepository.createTransaction(
            projectId = projectId,
            transactionType = FinancialTransactionType.EXPENSE,
            entryType = FinancialEntryType.DEBIT,
            amount = Money(BigDecimal("75000.00")),
            currency = "BDT",
            referenceType = FinancialReferenceType.PURCHASE_ORDER,
            referenceId = "PO-777",
            vendorId = "VEND-001",
            description = "Raw material purchase",
            notes = null,
            actorId = "staff-1",
            callerRole = UserRole.STAFF
        )
        assertTrue(txnRes is DomainResult.Success)
        val txnId = (txnRes as DomainResult.Success).data.transactionId

        financialTransactionRepository.submitTransaction(txnId, "staff-1", UserRole.STAFF)
        financialTransactionRepository.postTransaction(txnId, "RAW_MATERIALS", "acct-1", UserRole.ACCOUNTS)

        // 2. Create Step 04 Vendor Payable linking to txnId
        val payableRes = payableRepository.createPayable(
            projectId = projectId,
            vendorId = "VEND-001",
            referenceType = FinancialReferenceType.PURCHASE_ORDER,
            referenceId = "PO-777",
            financialTransactionId = txnId,
            supplierInvoiceNo = "BILL-777",
            originalAmount = Money(BigDecimal("75000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Raw material purchase obligation",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(payableRes is DomainResult.Success)
        val payable = (payableRes as DomainResult.Success).data

        assertEquals(txnId, payable.financialTransactionId)

        // 3. Verify Step 01 Transaction remains canonically posted
        val fetchedTxn = (financialTransactionRepository.getTransactionById(txnId, UserRole.ACCOUNTS) as DomainResult.Success).data
        assertEquals(FinancialTransactionStatus.POSTED, fetchedTxn.transactionStatus)

        // 4. Verify Ledger Entry from Step 01
        val ledgerRes = financialTransactionRepository.getLedgerEntriesByTransaction(txnId, UserRole.ACCOUNTS)
        assertTrue(ledgerRes is DomainResult.Success)
        val ledgerEntries = (ledgerRes as DomainResult.Success).data
        assertEquals(1, ledgerEntries.size)
        assertEquals("RAW_MATERIALS", ledgerEntries[0].accountHead)
    }
}

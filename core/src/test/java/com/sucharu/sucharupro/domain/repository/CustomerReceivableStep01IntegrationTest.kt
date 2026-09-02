package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeCustomerReceivableDataSource
import com.sucharu.sucharupro.data.datasource.FakeFinancialTransactionDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryMovementLedgerDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryReceivingDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryStockOutDataSource
import com.sucharu.sucharupro.data.repository.CustomerReceivableRepositoryImpl
import com.sucharu.sucharupro.data.repository.FinancialTransactionRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.FinancialEntryType
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerReceivableStep01IntegrationTest {

    private lateinit var stockOutDataSource: FakeInventoryStockOutDataSource
    private lateinit var receivingDataSource: FakeInventoryReceivingDataSource
    private lateinit var inventoryLedgerDataSource: FakeInventoryMovementLedgerDataSource
    private lateinit var financeTransactionDataSource: FakeFinancialTransactionDataSource
    private lateinit var receivableDataSource: FakeCustomerReceivableDataSource

    private lateinit var financialTransactionRepository: FinancialTransactionRepository
    private lateinit var receivableRepository: CustomerReceivableRepository

    @Before
    fun setUp() {
        stockOutDataSource = FakeInventoryStockOutDataSource()
        receivingDataSource = FakeInventoryReceivingDataSource()
        inventoryLedgerDataSource = FakeInventoryMovementLedgerDataSource()
        financeTransactionDataSource = FakeFinancialTransactionDataSource()
        receivableDataSource = FakeCustomerReceivableDataSource()

        financialTransactionRepository = FinancialTransactionRepositoryImpl(financeTransactionDataSource)
        receivableRepository = CustomerReceivableRepositoryImpl(receivableDataSource)
    }

    @Test
    fun `customer receivable integrates cleanly with Step 01 financial transaction without inventory mutations`() = runBlocking {
        val projectId = "PRJ-01"

        // Baseline inventory checks
        val initialStockOuts = stockOutDataSource.observeStockOutRecords().first().size
        val initialStockIns = receivingDataSource.observeStockInRecords().first().size
        val initialInventoryLedger = inventoryLedgerDataSource.getEntries(projectId).size

        // 1. Post Step 01 financial transaction
        val txnRes = financialTransactionRepository.createTransaction(
            projectId = projectId,
            transactionType = FinancialTransactionType.SALE,
            entryType = FinancialEntryType.DEBIT,
            amount = Money(BigDecimal("50000.00")),
            currency = "BDT",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-2026-STEP1",
            customerId = "CUST-001",
            description = "Commercial invoice financial entry",
            notes = null,
            actorId = "staff-1",
            callerRole = UserRole.STAFF
        )
        val txnId = (txnRes as DomainResult.Success).data.transactionId
        financialTransactionRepository.submitTransaction(txnId, "staff-1", UserRole.STAFF)
        financialTransactionRepository.postTransaction(txnId, "ACCOUNTS_RECEIVABLE", "acct-1", UserRole.ACCOUNTS)

        // 2. Create Step 02 Customer Receivable linking to Step 01 Transaction
        val recRes = receivableRepository.createReceivable(
            projectId = projectId,
            customerId = "CUST-001",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-2026-STEP1",
            financialTransactionId = txnId,
            originalAmount = Money(BigDecimal("50000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Customer receivable obligation for invoice",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(recRes is DomainResult.Success)
        val receivable = (recRes as DomainResult.Success).data
        assertEquals(txnId, receivable.financialTransactionId)
        assertEquals(Money(BigDecimal("50000.00")), receivable.outstandingAmount)

        // 3. Verify zero inventory mutations
        assertEquals(initialStockOuts, stockOutDataSource.observeStockOutRecords().first().size)
        assertEquals(initialStockIns, receivingDataSource.observeStockInRecords().first().size)
        assertEquals(initialInventoryLedger, inventoryLedgerDataSource.getEntries(projectId).size)
    }
}

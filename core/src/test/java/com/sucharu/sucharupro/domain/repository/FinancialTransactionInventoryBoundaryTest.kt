package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeFinancialTransactionDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryMovementLedgerDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryReceivingDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryStockOutDataSource
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

class FinancialTransactionInventoryBoundaryTest {

    private lateinit var stockOutDataSource: FakeInventoryStockOutDataSource
    private lateinit var receivingDataSource: FakeInventoryReceivingDataSource
    private lateinit var inventoryLedgerDataSource: FakeInventoryMovementLedgerDataSource
    private lateinit var financeDataSource: FakeFinancialTransactionDataSource
    private lateinit var repository: FinancialTransactionRepository

    @Before
    fun setUp() {
        stockOutDataSource = FakeInventoryStockOutDataSource()
        receivingDataSource = FakeInventoryReceivingDataSource()
        inventoryLedgerDataSource = FakeInventoryMovementLedgerDataSource()
        financeDataSource = FakeFinancialTransactionDataSource()
        repository = FinancialTransactionRepositoryImpl(financeDataSource)
    }

    @Test
    fun `financial transaction and ledger operations perform zero inventory mutations`() = runBlocking {
        val initialStockOuts = stockOutDataSource.observeStockOutRecords().first().size
        val initialStockIns = receivingDataSource.observeStockInRecords().first().size
        val initialInventoryLedger = inventoryLedgerDataSource.getEntries("PRJ-01").size

        val createRes = repository.createTransaction(
            projectId = "PRJ-01",
            transactionType = FinancialTransactionType.SALE,
            entryType = FinancialEntryType.DEBIT,
            amount = Money(BigDecimal("50000.00")),
            currency = "BDT",
            referenceType = FinancialReferenceType.ORDER,
            referenceId = "ORD-001",
            description = "Major corporate printing invoice",
            notes = null,
            actorId = "staff-1",
            callerRole = UserRole.STAFF
        )
        val txnId = (createRes as DomainResult.Success).data.transactionId

        repository.submitTransaction(txnId, "staff-1", UserRole.STAFF)
        repository.postTransaction(txnId, "ACCOUNTS_RECEIVABLE", "acct-1", UserRole.ACCOUNTS)

        val finalStockOuts = stockOutDataSource.observeStockOutRecords().first().size
        val finalStockIns = receivingDataSource.observeStockInRecords().first().size
        val finalInventoryLedger = inventoryLedgerDataSource.getEntries("PRJ-01").size

        assertEquals(initialStockOuts, finalStockOuts)
        assertEquals(initialStockIns, finalStockIns)
        assertEquals(initialInventoryLedger, finalInventoryLedger)
    }
}

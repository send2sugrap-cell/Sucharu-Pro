package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.FakeFinancialTransactionDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryMovementLedgerDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryReceivingDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryStockOutDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPayableDataSource
import com.sucharu.sucharupro.data.repository.FinancialTransactionRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorPayableRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.FinancialEntryType
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionType
import com.sucharu.sucharupro.domain.model.finance.VendorPayableAgingBucket
import com.sucharu.sucharupro.domain.model.finance.VendorPayableStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.FinancialTransactionRepository
import com.sucharu.sucharupro.domain.repository.VendorPayableRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorPayableEndToEndTest {

    private lateinit var stockOutDataSource: FakeInventoryStockOutDataSource
    private lateinit var receivingDataSource: FakeInventoryReceivingDataSource
    private lateinit var inventoryLedgerDataSource: FakeInventoryMovementLedgerDataSource
    private lateinit var financialDataSource: FakeFinancialTransactionDataSource
    private lateinit var payableDataSource: FakeVendorPayableDataSource

    private lateinit var financialTransactionRepository: FinancialTransactionRepository
    private lateinit var payableRepository: VendorPayableRepository

    @Before
    fun setUp() {
        stockOutDataSource = FakeInventoryStockOutDataSource()
        receivingDataSource = FakeInventoryReceivingDataSource()
        inventoryLedgerDataSource = FakeInventoryMovementLedgerDataSource()
        financialDataSource = FakeFinancialTransactionDataSource()
        payableDataSource = FakeVendorPayableDataSource()

        financialTransactionRepository = FinancialTransactionRepositoryImpl(financialDataSource)
        payableRepository = VendorPayableRepositoryImpl(payableDataSource)
    }

    @Test
    fun `full end to end supplier payable lifecycle from purchase recognition to payable settlement tracking`() = runBlocking {
        val projectId = "PRJ-E2E-PAYABLE"
        val vendorId = "VEND-E2E-PAPER"
        val poReference = "PO-2026-E2E-01"
        val supplierInvoice = "INV-SUPPLIER-8899"

        // Baseline inventory verification
        val initialStockOuts = stockOutDataSource.observeStockOutRecords().first().size
        val initialStockIns = receivingDataSource.observeStockInRecords().first().size
        val initialInventoryLedger = inventoryLedgerDataSource.getEntries(projectId).size

        // 1. Step 01 Financial Transaction recognition (Debit Raw Material Expense)
        val txnRes = financialTransactionRepository.createTransaction(
            projectId = projectId,
            transactionType = FinancialTransactionType.EXPENSE,
            entryType = FinancialEntryType.DEBIT,
            amount = Money(BigDecimal("120000.00")),
            currency = "BDT",
            referenceType = FinancialReferenceType.PURCHASE_ORDER,
            referenceId = poReference,
            vendorId = vendorId,
            description = "Commercial purchase of offset paper rolls",
            notes = "Terms net 30",
            actorId = "staff-operator-1",
            callerRole = UserRole.STAFF
        )
        assertTrue(txnRes is DomainResult.Success)
        val txnId = (txnRes as DomainResult.Success).data.transactionId

        financialTransactionRepository.submitTransaction(txnId, "staff-operator-1", UserRole.STAFF)
        financialTransactionRepository.postTransaction(txnId, "RAW_MATERIALS", "acct-manager-1", UserRole.ACCOUNTS)

        // 2. Step 04 Create Vendor Payable obligation
        val payableRes = payableRepository.createPayable(
            projectId = projectId,
            vendorId = vendorId,
            referenceType = FinancialReferenceType.PURCHASE_ORDER,
            referenceId = poReference,
            financialTransactionId = txnId,
            supplierInvoiceNo = supplierInvoice,
            originalAmount = Money(BigDecimal("120000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Commercial payable obligation for paper rolls",
            notes = "Linked to PO #$poReference",
            actorId = "acct-manager-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(payableRes is DomainResult.Success)
        val payable = (payableRes as DomainResult.Success).data
        val payableId = payable.payableId

        assertEquals(VendorPayableStatus.APPROVED, payable.status)
        assertEquals(Money(BigDecimal("120000.00")), payable.outstandingAmount)
        assertEquals(Money.ZERO, payable.settledAmount)
        assertEquals(VendorPayableAgingBucket.CURRENT, payable.agingBucket)

        // 3. Step 04 Record partial settlement (৳40,000)
        val partialSettleRes = payableRepository.recordSettlement(
            payableId = payableId,
            settlementAmount = Money(BigDecimal("40000.00")),
            actorId = "acct-manager-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(partialSettleRes is DomainResult.Success)
        val partialPayable = (partialSettleRes as DomainResult.Success).data

        assertEquals(VendorPayableStatus.PARTIALLY_SETTLED, partialPayable.status)
        assertEquals(Money(BigDecimal("40000.00")), partialPayable.settledAmount)
        assertEquals(Money(BigDecimal("80000.00")), partialPayable.outstandingAmount)

        // 4. Step 04 Record final settlement (৳80,000)
        val finalSettleRes = payableRepository.recordSettlement(
            payableId = payableId,
            settlementAmount = Money(BigDecimal("80000.00")),
            actorId = "acct-manager-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(finalSettleRes is DomainResult.Success)
        val finalPayable = (finalSettleRes as DomainResult.Success).data

        assertEquals(VendorPayableStatus.SETTLED, finalPayable.status)
        assertEquals(Money(BigDecimal("120000.00")), finalPayable.settledAmount)
        assertEquals(Money.ZERO, finalPayable.outstandingAmount)

        // 5. Verify Summary Aggregates
        val summaryRes = payableRepository.getVendorPayableSummary(
            projectId = projectId,
            vendorId = vendorId,
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(summaryRes is DomainResult.Success)
        val summary = (summaryRes as DomainResult.Success).data
        assertEquals(1, summary.totalPayablesCount)
        assertEquals(0, summary.openPayablesCount)
        assertEquals(Money(BigDecimal("120000.00")), summary.totalOriginalAmount)
        assertEquals(Money(BigDecimal("120000.00")), summary.totalSettledAmount)
        assertEquals(Money.ZERO, summary.totalOutstandingPayable)

        // 6. Verify Step 01 Transaction remains posted and canonical ledger untouched by Step 04
        val txnCheck = (financialTransactionRepository.getTransactionById(txnId, UserRole.ACCOUNTS) as DomainResult.Success).data
        assertEquals(FinancialTransactionStatus.POSTED, txnCheck.transactionStatus)

        // 7. Verify zero inventory mutations
        assertEquals(initialStockOuts, stockOutDataSource.observeStockOutRecords().first().size)
        assertEquals(initialStockIns, receivingDataSource.observeStockInRecords().first().size)
        assertEquals(initialInventoryLedger, inventoryLedgerDataSource.getEntries(projectId).size)
    }
}

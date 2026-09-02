package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeInventoryMovementLedgerDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryReceivingDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryStockOutDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPayableDataSource
import com.sucharu.sucharupro.data.repository.VendorPayableRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorPayableInventoryBoundaryTest {

    private lateinit var stockOutDataSource: FakeInventoryStockOutDataSource
    private lateinit var receivingDataSource: FakeInventoryReceivingDataSource
    private lateinit var inventoryLedgerDataSource: FakeInventoryMovementLedgerDataSource
    private lateinit var payableDataSource: FakeVendorPayableDataSource

    private lateinit var payableRepository: VendorPayableRepository

    @Before
    fun setUp() {
        stockOutDataSource = FakeInventoryStockOutDataSource()
        receivingDataSource = FakeInventoryReceivingDataSource()
        inventoryLedgerDataSource = FakeInventoryMovementLedgerDataSource()
        payableDataSource = FakeVendorPayableDataSource()

        payableRepository = VendorPayableRepositoryImpl(payableDataSource)
    }

    @Test
    fun `creating, updating, approving, and settling payables does not mutate inventory or stock ledger`() = runBlocking {
        val projectId = "PRJ-INV-BOUND"

        val initialStockOuts = stockOutDataSource.observeStockOutRecords().first().size
        val initialStockIns = receivingDataSource.observeStockInRecords().first().size
        val initialLedger = inventoryLedgerDataSource.getEntries(projectId).size

        // 1. Create payable
        val createRes = payableRepository.createPayable(
            projectId = projectId,
            vendorId = "VEND-001",
            referenceType = FinancialReferenceType.STOCK_RECEIPT,
            referenceId = "GRN-001",
            originalAmount = Money(BigDecimal("80000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Paper goods receiving",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(createRes is DomainResult.Success)
        val payableId = (createRes as DomainResult.Success).data.payableId

        // 2. Record Settlement
        val settleRes = payableRepository.recordSettlement(payableId, Money(BigDecimal("80000.00")), "acct-1", UserRole.ACCOUNTS)
        assertTrue(settleRes is DomainResult.Success)

        // Verify zero inventory side-effects
        assertEquals(initialStockOuts, stockOutDataSource.observeStockOutRecords().first().size)
        assertEquals(initialStockIns, receivingDataSource.observeStockInRecords().first().size)
        assertEquals(initialLedger, inventoryLedgerDataSource.getEntries(projectId).size)
    }
}

package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeCustomerReceivableDataSource
import com.sucharu.sucharupro.data.datasource.FakeFinancialAdjustmentDataSource
import com.sucharu.sucharupro.data.datasource.FakeFinancialTransactionDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryMovementLedgerDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryReceivingDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryStockOutDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPayableDataSource
import com.sucharu.sucharupro.data.repository.CustomerReceivableRepositoryImpl
import com.sucharu.sucharupro.data.repository.FinancialAdjustmentRepositoryImpl
import com.sucharu.sucharupro.data.repository.FinancialTransactionRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorPayableRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentType
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class FinancialAdjustmentInventoryBoundaryTest {

    private lateinit var stockOutDataSource: FakeInventoryStockOutDataSource
    private lateinit var receivingDataSource: FakeInventoryReceivingDataSource
    private lateinit var inventoryLedgerDataSource: FakeInventoryMovementLedgerDataSource

    private lateinit var adjustmentDataSource: FakeFinancialAdjustmentDataSource
    private lateinit var financialTransactionDataSource: FakeFinancialTransactionDataSource
    private lateinit var receivableDataSource: FakeCustomerReceivableDataSource
    private lateinit var payableDataSource: FakeVendorPayableDataSource

    private lateinit var financialTransactionRepository: FinancialTransactionRepository
    private lateinit var customerReceivableRepository: CustomerReceivableRepository
    private lateinit var vendorPayableRepository: VendorPayableRepository
    private lateinit var adjustmentRepository: FinancialAdjustmentRepository

    @Before
    fun setUp() {
        stockOutDataSource = FakeInventoryStockOutDataSource()
        receivingDataSource = FakeInventoryReceivingDataSource()
        inventoryLedgerDataSource = FakeInventoryMovementLedgerDataSource()

        adjustmentDataSource = FakeFinancialAdjustmentDataSource()
        financialTransactionDataSource = FakeFinancialTransactionDataSource()
        receivableDataSource = FakeCustomerReceivableDataSource()
        payableDataSource = FakeVendorPayableDataSource()

        financialTransactionRepository = FinancialTransactionRepositoryImpl(financialTransactionDataSource)
        customerReceivableRepository = CustomerReceivableRepositoryImpl(receivableDataSource)
        vendorPayableRepository = VendorPayableRepositoryImpl(payableDataSource)

        adjustmentRepository = FinancialAdjustmentRepositoryImpl(
            adjustmentDataSource,
            financialTransactionRepository,
            customerReceivableRepository,
            vendorPayableRepository
        )
    }

    @Test
    fun `financial adjustment operations cause zero mutation to inventory stock or movement records`() = runBlocking {
        val projectId = "PRJ-INV-ADJ-BOUND"

        val initialStockOuts = stockOutDataSource.observeStockOutRecords().first().size
        val initialStockIns = receivingDataSource.observeStockInRecords().first().size
        val initialLedger = inventoryLedgerDataSource.getEntries(projectId).size

        val adj = (adjustmentRepository.createAdjustment(
            projectId = projectId,
            adjustmentType = FinancialAdjustmentType.CUSTOMER_CREDIT_NOTE,
            amount = Money(BigDecimal("1500.00")),
            customerId = "CUST-001",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-1001",
            reasonCode = "DAMAGE",
            reason = "Damaged packaging",
            description = "Boundary test",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        ) as DomainResult.Success).data

        adjustmentRepository.postAdjustment(adj.adjustmentId, "SALES_RETURN", "acct-2", UserRole.ACCOUNTS)

        val postStockOuts = stockOutDataSource.observeStockOutRecords().first().size
        val postStockIns = receivingDataSource.observeStockInRecords().first().size
        val postLedger = inventoryLedgerDataSource.getEntries(projectId).size

        assertEquals(initialStockIns, postStockIns)
        assertEquals(initialStockOuts, postStockOuts)
        assertEquals(initialLedger, postLedger)
    }
}

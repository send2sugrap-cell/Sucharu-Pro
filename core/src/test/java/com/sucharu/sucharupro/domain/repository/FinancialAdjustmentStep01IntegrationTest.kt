package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeCustomerReceivableDataSource
import com.sucharu.sucharupro.data.datasource.FakeFinancialAdjustmentDataSource
import com.sucharu.sucharupro.data.datasource.FakeFinancialTransactionDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPayableDataSource
import com.sucharu.sucharupro.data.repository.CustomerReceivableRepositoryImpl
import com.sucharu.sucharupro.data.repository.FinancialAdjustmentRepositoryImpl
import com.sucharu.sucharupro.data.repository.FinancialTransactionRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorPayableRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentType
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

class FinancialAdjustmentStep01IntegrationTest {

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
    fun `posting adjustment automatically creates and posts canonical Step 01 FinancialTransaction and LedgerEntry`() = runBlocking {
        val adj = (adjustmentRepository.createAdjustment(
            projectId = "PRJ-01",
            adjustmentType = FinancialAdjustmentType.CUSTOMER_CREDIT_NOTE,
            amount = Money(BigDecimal("3500.00")),
            customerId = "CUST-001",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-1001",
            reasonCode = "DAMAGE",
            reason = "Damaged packaging",
            description = "Step 01 integration test",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        ) as DomainResult.Success).data

        val postRes = adjustmentRepository.postAdjustment(adj.adjustmentId, "SALES_RETURN", "acct-2", UserRole.ACCOUNTS)
        assertTrue(postRes is DomainResult.Success)
        val postedAdj = (postRes as DomainResult.Success).data

        assertNotNull(postedAdj.financialTransactionId)

        val txnRes = financialTransactionRepository.getTransactionById(postedAdj.financialTransactionId!!, UserRole.ACCOUNTS)
        assertTrue(txnRes is DomainResult.Success)
        val txn = (txnRes as DomainResult.Success).data

        assertEquals(FinancialTransactionType.CREDIT, txn.transactionType)
        assertEquals(FinancialEntryType.CREDIT, txn.entryType)
        assertEquals(FinancialReferenceType.ADJUSTMENT, txn.referenceType)
        assertEquals(adj.adjustmentId, txn.referenceId)
        assertEquals(Money(BigDecimal("3500.00")), txn.amount)

        val ledgerRes = financialTransactionRepository.getLedgerEntriesByTransaction(txn.transactionId, UserRole.ACCOUNTS)
        assertTrue(ledgerRes is DomainResult.Success)
        val entries = (ledgerRes as DomainResult.Success).data
        assertEquals(1, entries.size)
        assertEquals("SALES_RETURN", entries[0].accountHead)
        assertEquals(FinancialEntryType.CREDIT, entries[0].entryType)
    }
}

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
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorDebitNoteTest {

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
    fun `posting vendor debit note adjustment creates debit note record`() = runBlocking {
        val adj = (adjustmentRepository.createAdjustment(
            projectId = "PRJ-01",
            adjustmentType = FinancialAdjustmentType.VENDOR_DEBIT_NOTE,
            amount = Money(BigDecimal("4500.00")),
            vendorId = "VEND-001",
            referenceType = FinancialReferenceType.VENDOR_BILL,
            referenceId = "BILL-1001",
            reasonCode = "REBATE",
            reason = "Paper GSM discrepancy",
            description = "Debit note test",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        ) as DomainResult.Success).data

        val postRes = adjustmentRepository.postAdjustment(adj.adjustmentId, "PURCHASE_RETURN", "acct-2", UserRole.ACCOUNTS)
        assertTrue(postRes is DomainResult.Success)
        val postedAdj = (postRes as DomainResult.Success).data

        assertNotNull(postedAdj.debitNoteId)

        val dnRes = adjustmentRepository.getDebitNoteById(postedAdj.debitNoteId!!, UserRole.ACCOUNTS)
        assertTrue(dnRes is DomainResult.Success)
        val debitNote = (dnRes as DomainResult.Success).data

        assertEquals("VEND-001", debitNote.vendorId)
        assertEquals(Money(BigDecimal("4500.00")), debitNote.amount)
        assertEquals("PRJ-01", debitNote.projectId)
    }
}

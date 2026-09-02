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
import com.sucharu.sucharupro.domain.model.finance.CustomerReceivableStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentDirection
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentStatus
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

class FinancialAdjustmentEndToEndTest {

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
    fun `end to end workflow creation approval posting credit note issuance and receivable settlement`() = runBlocking {
        val projectId = "PRJ-E2E-ADJ"

        // 1. Create customer receivable for Invoice
        val recRes = customerReceivableRepository.createReceivable(
            projectId = projectId,
            customerId = "CUST-001",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-2026-99",
            originalAmount = Money(BigDecimal("50000.00")),
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "E2E Invoice Due",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        val receivableId = (recRes as DomainResult.Success).data.receivableId

        // 2. Draft financial adjustment for return
        val createAdjRes = adjustmentRepository.createAdjustment(
            projectId = projectId,
            adjustmentType = FinancialAdjustmentType.CUSTOMER_CREDIT_NOTE,
            direction = FinancialAdjustmentDirection.CREDIT,
            amount = Money(BigDecimal("10000.00")),
            customerId = "CUST-001",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-2026-99",
            reasonCode = "PRINT_DEFECT",
            reason = "Partial color misalignment defect",
            description = "Credit note for defective batch",
            relatedReceivableId = receivableId,
            actorId = "staff-1",
            callerRole = UserRole.STAFF
        )
        assertTrue(createAdjRes is DomainResult.Success)
        val adjId = (createAdjRes as DomainResult.Success).data.adjustmentId

        // 3. Staff submits
        val submitRes = adjustmentRepository.submitAdjustment(adjId, "staff-1", UserRole.STAFF)
        assertTrue(submitRes is DomainResult.Success)

        // 4. Manager approves
        val approveRes = adjustmentRepository.approveAdjustment(adjId, "mgr-1", UserRole.MANAGER)
        assertTrue(approveRes is DomainResult.Success)

        // 5. Accounts posts to ledger
        val postRes = adjustmentRepository.postAdjustment(adjId, "SALES_RETURN", "acct-2", UserRole.ACCOUNTS)
        assertTrue(postRes is DomainResult.Success)
        val postedAdj = (postRes as DomainResult.Success).data

        assertEquals(FinancialAdjustmentStatus.POSTED, postedAdj.status)
        assertNotNull(postedAdj.creditNoteId)
        assertNotNull(postedAdj.financialTransactionId)

        // 6. Verify issued credit note
        val cnRes = adjustmentRepository.getCreditNoteById(postedAdj.creditNoteId!!, UserRole.ACCOUNTS)
        assertTrue(cnRes is DomainResult.Success)
        val creditNote = (cnRes as DomainResult.Success).data
        assertEquals(Money(BigDecimal("10000.00")), creditNote.amount)
        assertEquals("CUST-001", creditNote.customerId)

        // 7. Verify receivable settlement
        val recUpdated = (customerReceivableRepository.getReceivableById(receivableId, UserRole.ACCOUNTS) as DomainResult.Success).data
        assertEquals(Money(BigDecimal("10000.00")), recUpdated.settledAmount)
        assertEquals(Money(BigDecimal("40000.00")), recUpdated.outstandingAmount)
        assertEquals(CustomerReceivableStatus.PARTIALLY_SETTLED, recUpdated.status)

        // 8. Verify ledger entries
        val ledgerRes = financialTransactionRepository.getLedgerEntriesByTransaction(postedAdj.financialTransactionId!!, UserRole.ACCOUNTS)
        assertTrue(ledgerRes is DomainResult.Success)
        assertEquals(1, (ledgerRes as DomainResult.Success).data.size)
    }
}

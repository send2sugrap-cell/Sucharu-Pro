package com.sucharu.sucharupro.businessledger

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.businessexpense.FakeBusinessExpenseDataSource
import com.sucharu.sucharupro.data.datasource.businessledger.FakeBusinessLedgerDataSource
import com.sucharu.sucharupro.data.datasource.vendorpayable.FakeVendorPayableDataSource
import com.sucharu.sucharupro.data.repository.businessexpense.BusinessExpenseRepositoryImpl
import com.sucharu.sucharupro.data.repository.businessledger.BusinessLedgerRepositoryImpl
import com.sucharu.sucharupro.data.repository.vendorpayable.VendorPayableRepositoryImpl
import com.sucharu.sucharupro.domain.model.businessexpense.*
import com.sucharu.sucharupro.domain.model.businessledger.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorpayable.*
import com.sucharu.sucharupro.domain.service.businessledger.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessLedgerServiceTest {

    private lateinit var ledgerDataSource: FakeBusinessLedgerDataSource
    private lateinit var ledgerRepository: BusinessLedgerRepositoryImpl
    private lateinit var expenseDataSource: FakeBusinessExpenseDataSource
    private lateinit var expenseRepository: BusinessExpenseRepositoryImpl
    private lateinit var payableDataSource: FakeVendorPayableDataSource
    private lateinit var payableRepository: VendorPayableRepositoryImpl
    private lateinit var service: BusinessLedgerServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"

    private val adminPrincipal = AuthenticatedPrincipal(
        userId = "USER-ADMIN-1",
        projectId = projectId,
        username = "admin1",
        role = UserRole.ADMIN
    )

    private val managerPrincipal = AuthenticatedPrincipal(
        userId = "USER-MGR-1",
        projectId = projectId,
        username = "mgr1",
        role = UserRole.MANAGER
    )

    @Before
    fun setup() {
        ledgerDataSource = FakeBusinessLedgerDataSource()
        ledgerRepository = BusinessLedgerRepositoryImpl(ledgerDataSource)
        expenseDataSource = FakeBusinessExpenseDataSource()
        expenseRepository = BusinessExpenseRepositoryImpl(expenseDataSource)
        payableDataSource = FakeVendorPayableDataSource()
        payableRepository = VendorPayableRepositoryImpl(payableDataSource)

        service = BusinessLedgerServiceImpl(
            repository = ledgerRepository,
            expenseRepository = expenseRepository,
            payableRepository = payableRepository,
            defaultTenantId = tenantId
        )
    }

    @Test
    fun testPostApprovedExpense() = runBlocking {
        // Create an approved expense
        val expense = BusinessExpense(
            expenseId = "EXP-101",
            tenantId = tenantId,
            projectId = projectId,
            expenseNumber = "EXP-2026-001",
            expenseCategoryId = "CAT-001",
            amount = BigDecimal("7500.0000"),
            currency = "BDT",
            status = BusinessExpenseStatus.APPROVED,
            description = "UV Offset Inks Batch",
            createdBy = "USER-STAFF-1"
        )
        expenseRepository.createExpense(expense)

        val cmd = PostApprovedExpenseCommand(
            expenseId = "EXP-101",
            accountCategory = BusinessLedgerAccountCategory.PRODUCTION_COST,
            jobId = "JOB-1025"
        )

        val postRes = service.postApprovedExpense(adminPrincipal, cmd)
        assertTrue(postRes is DomainResult.Success)
        val posting = (postRes as DomainResult.Success).data

        assertEquals(BusinessLedgerPostingType.EXPENSE_RECOGNITION, posting.postingType)
        assertEquals(BusinessLedgerSourceType.BUSINESS_EXPENSE, posting.sourceType)
        assertEquals("EXP-101", posting.sourceId)
        assertEquals(BigDecimal("7500.0000"), posting.debitAmount)
        assertEquals(BigDecimal("0.0000"), posting.creditAmount)
        assertEquals(BusinessLedgerAccountCategory.PRODUCTION_COST, posting.accountCategory)
        assertEquals("JOB-1025", posting.jobId)
    }

    @Test
    fun testDraftExpenseCannotBePosted() = runBlocking {
        val draftExpense = BusinessExpense(
            expenseId = "EXP-DRAFT-1",
            tenantId = tenantId,
            projectId = projectId,
            expenseNumber = "EXP-2026-DRAFT",
            expenseCategoryId = "CAT-001",
            amount = BigDecimal("3000.0000"),
            currency = "BDT",
            status = BusinessExpenseStatus.DRAFT,
            description = "Draft Printing Plates",
            createdBy = "USER-STAFF-1"
        )
        expenseRepository.createExpense(draftExpense)

        val postRes = service.postApprovedExpense(
            adminPrincipal,
            PostApprovedExpenseCommand(expenseId = "EXP-DRAFT-1")
        )
        assertTrue(postRes is DomainResult.Error)
        assertTrue((postRes as DomainResult.Error).message.contains("Only approved or postable"))
    }

    @Test
    fun testPostApprovedPayableAndVendorPayment() = runBlocking {
        val payable = VendorPayable(
            payableId = "PAY-101",
            tenantId = tenantId,
            projectId = projectId,
            payableNumber = "BILL-2026-001",
            vendorId = "VEND-001",
            description = "Lamination Foil Rolls",
            originalAmount = BigDecimal("20000.0000"),
            paidAmount = BigDecimal.ZERO,
            currency = "BDT",
            issueDate = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis() + 86400000L * 30,
            status = VendorPayableStatus.APPROVED,
            createdBy = "USER-STAFF-1"
        )
        payableRepository.createPayable(payable)

        // 1. Recognize Vendor Liability
        val payPostRes = service.postApprovedPayable(
            adminPrincipal,
            PostApprovedPayableCommand(payableId = "PAY-101")
        )
        assertTrue(payPostRes is DomainResult.Success)
        val liabilityPosting = (payPostRes as DomainResult.Success).data
        assertEquals(BusinessLedgerPostingType.VENDOR_LIABILITY_RECOGNITION, liabilityPosting.postingType)
        assertEquals(BigDecimal("20000.0000"), liabilityPosting.debitAmount)

        // 2. Post Vendor Payment Settlement (Credit movement)
        val pmtCmd = PostVendorPaymentCommand(
            payableId = "PAY-101",
            allocationId = "ALLOC-PAY-001",
            amount = BigDecimal("12000.0000"),
            currency = "BDT",
            accountCategory = BusinessLedgerAccountCategory.BANK,
            description = "Partial Wire Transfer for BILL-2026-001"
        )
        val pmtPostRes = service.postVendorPayment(adminPrincipal, pmtCmd)
        assertTrue(pmtPostRes is DomainResult.Success)
        val pmtPosting = (pmtPostRes as DomainResult.Success).data
        assertEquals(BusinessLedgerPostingType.VENDOR_PAYMENT, pmtPosting.postingType)
        assertEquals(BigDecimal("12000.0000"), pmtPosting.creditAmount)
        assertEquals(BigDecimal("0.0000"), pmtPosting.debitAmount)

        // 3. Verify Balance Calculation: 20,000 Debits - 12,000 Credits = 8,000 Net
        val balRes = service.getBalanceSummary(adminPrincipal)
        assertTrue(balRes is DomainResult.Success)
        val bal = (balRes as DomainResult.Success).data
        assertEquals(BigDecimal("20000.0000"), bal.totalDebit)
        assertEquals(BigDecimal("12000.0000"), bal.totalCredit)
        assertEquals(BigDecimal("8000.0000"), bal.netMovement)
        assertEquals(BigDecimal("8000.0000"), bal.closingBalance)
    }

    @Test
    fun testReversalPostingCreatesCompensatingEntry() = runBlocking {
        val expense = BusinessExpense(
            expenseId = "EXP-201",
            tenantId = tenantId,
            projectId = projectId,
            expenseNumber = "EXP-2026-002",
            expenseCategoryId = "CAT-001",
            amount = BigDecimal("4000.0000"),
            currency = "BDT",
            status = BusinessExpenseStatus.APPROVED,
            description = "Courier Charges",
            createdBy = "USER-STAFF-1"
        )
        expenseRepository.createExpense(expense)

        val postRes = service.postApprovedExpense(adminPrincipal, PostApprovedExpenseCommand(expenseId = "EXP-201"))
        val original = (postRes as DomainResult.Success).data

        // Execute Reversal
        val revRes = service.reversePosting(
            managerPrincipal,
            ReversePostingCommand(postingId = original.id, reason = "Incorrect courier billing amount")
        )
        assertTrue(revRes is DomainResult.Success)
        val reversal = (revRes as DomainResult.Success).data

        assertEquals(BusinessLedgerPostingType.REVERSAL, reversal.postingType)
        assertEquals(original.id, reversal.reversalOfPostingId)
        assertEquals(BigDecimal("0.0000"), reversal.debitAmount)
        assertEquals(BigDecimal("4000.0000"), reversal.creditAmount)

        // Verify original is marked reversed
        val updatedOriginal = (service.getPostingById(adminPrincipal, original.id) as DomainResult.Success).data
        assertTrue(updatedOriginal.isReversed)
        assertEquals("Incorrect courier billing amount", updatedOriginal.reversalReason)

        // Net movement for reversed expense should be 0: Debits (4000) - Credits (4000) = 0
        val balRes = service.getBalanceSummary(adminPrincipal)
        val bal = (balRes as DomainResult.Success).data
        assertEquals(BigDecimal("4000.0000"), bal.totalDebit)
        assertEquals(BigDecimal("4000.0000"), bal.totalCredit)
        assertEquals(BigDecimal("0.0000"), bal.netMovement)
    }
}

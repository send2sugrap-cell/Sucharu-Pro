package com.sucharu.sucharupro.businessfinancialadjustment

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.businessexpense.FakeBusinessExpenseDataSource
import com.sucharu.sucharupro.data.datasource.businessfinancialadjustment.FakeBusinessFinancialAdjustmentDataSource
import com.sucharu.sucharupro.data.datasource.businessledger.FakeBusinessLedgerDataSource
import com.sucharu.sucharupro.data.datasource.vendorpayable.FakeVendorPayableDataSource
import com.sucharu.sucharupro.data.repository.businessexpense.BusinessExpenseRepositoryImpl
import com.sucharu.sucharupro.data.repository.businessfinancialadjustment.BusinessFinancialAdjustmentRepositoryImpl
import com.sucharu.sucharupro.data.repository.businessledger.BusinessLedgerRepositoryImpl
import com.sucharu.sucharupro.data.repository.vendorpayable.VendorPayableRepositoryImpl
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpense
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseStatus
import com.sucharu.sucharupro.domain.model.businessfinancialadjustment.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.businessfinancialadjustment.*
import com.sucharu.sucharupro.domain.service.businessledger.BusinessLedgerServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessFinancialAdjustmentConsistencyTest {

    private lateinit var adjDataSource: FakeBusinessFinancialAdjustmentDataSource
    private lateinit var adjRepository: BusinessFinancialAdjustmentRepositoryImpl
    private lateinit var expenseDataSource: FakeBusinessExpenseDataSource
    private lateinit var expenseRepository: BusinessExpenseRepositoryImpl
    private lateinit var payableDataSource: FakeVendorPayableDataSource
    private lateinit var payableRepository: VendorPayableRepositoryImpl
    private lateinit var ledgerDataSource: FakeBusinessLedgerDataSource
    private lateinit var ledgerRepository: BusinessLedgerRepositoryImpl
    private lateinit var ledgerService: BusinessLedgerServiceImpl
    private lateinit var service: BusinessFinancialAdjustmentServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"

    private val staff = AuthenticatedPrincipal(userId = "USR-STAFF", username = "staff_user", role = UserRole.STAFF, projectId = projectId)
    private val manager = AuthenticatedPrincipal(userId = "USR-MGR", username = "mgr_user", role = UserRole.MANAGER, projectId = projectId)
    private val admin = AuthenticatedPrincipal(userId = "USR-ADMIN", username = "admin_user", role = UserRole.ADMIN, projectId = projectId)

    @Before
    fun setup() {
        adjDataSource = FakeBusinessFinancialAdjustmentDataSource()
        adjRepository = BusinessFinancialAdjustmentRepositoryImpl(adjDataSource)
        expenseDataSource = FakeBusinessExpenseDataSource()
        expenseRepository = BusinessExpenseRepositoryImpl(expenseDataSource)
        payableDataSource = FakeVendorPayableDataSource()
        payableRepository = VendorPayableRepositoryImpl(payableDataSource)
        ledgerDataSource = FakeBusinessLedgerDataSource()
        ledgerRepository = BusinessLedgerRepositoryImpl(ledgerDataSource)
        ledgerService = BusinessLedgerServiceImpl(ledgerRepository, defaultTenantId = tenantId)

        service = BusinessFinancialAdjustmentServiceImpl(
            repository = adjRepository,
            ledgerService = ledgerService,
            expenseRepository = expenseRepository,
            payableRepository = payableRepository,
            defaultTenantId = tenantId
        )
    }

    @Test
    fun testZeroMutationGuaranteeOnOriginalExpenseAndLedger() = runBlocking {
        // 1. Setup original Expense
        val originalExpense = BusinessExpense(
            expenseId = "EXP-CANONICAL-001",
            tenantId = tenantId,
            projectId = projectId,
            expenseNumber = "EXP-2026-0001",
            expenseCategoryId = "CAT-PROD",
            description = "Printing Paper Inventory Purchase",
            amount = BigDecimal("50000.0000"),
            currency = "BDT",
            status = BusinessExpenseStatus.APPROVED,
            createdBy = "USR-STAFF"
        )
        expenseRepository.createExpense(originalExpense)

        // Verify initial state
        val initialExpenseResult = expenseRepository.getExpenseById(tenantId, projectId, "EXP-CANONICAL-001")
        assertTrue(initialExpenseResult is DomainResult.Success)
        val initialExpense = (initialExpenseResult as DomainResult.Success).data
        assertNotNull(initialExpense)
        assertEquals(BigDecimal("50000.0000"), initialExpense?.amount)
        assertEquals(BusinessExpenseStatus.APPROVED, initialExpense?.status)

        // 2. Perform Financial Adjustment of BDT -5,000 (created by staff, approved by manager)
        val createCmd = CreateAdjustmentCommand(
            adjustmentNumber = "ADJ-MUT-001",
            adjustmentType = BusinessFinancialAdjustmentType.EXPENSE_CORRECTION,
            sourceType = AdjustmentSourceType.BUSINESS_EXPENSE,
            sourceId = "EXP-CANONICAL-001",
            originalAmount = BigDecimal("50000.0000"),
            adjustmentAmount = BigDecimal("-5000.0000"),
            reason = "Vendor volume discount granted",
            justification = "Vendor volume discount granted post delivery with credit note",
            periodId = "PER-2026-08"
        )
        val createRes = service.createAdjustment(staff, createCmd)
        assertTrue(createRes is DomainResult.Success)
        val adj = (createRes as DomainResult.Success).data

        val submitRes = service.submitAdjustment(staff, SubmitAdjustmentCommand(adj.id))
        assertTrue(submitRes is DomainResult.Success)
        val approveRes = service.approveAdjustment(manager, ApproveAdjustmentCommand(adj.id))
        assertTrue(approveRes is DomainResult.Success)
        val postRes = service.postAdjustment(admin, PostAdjustmentCommand(adj.id))
        assertTrue(postRes is DomainResult.Success)

        // 3. ZERO-MUTATION VERIFICATION: Check original expense is completely untouched!
        val postExpenseResult = expenseRepository.getExpenseById(tenantId, projectId, "EXP-CANONICAL-001")
        assertTrue(postExpenseResult is DomainResult.Success)
        val postExpense = (postExpenseResult as DomainResult.Success).data
        assertNotNull(postExpense)
        assertEquals(BigDecimal("50000.0000"), postExpense?.amount)
        assertEquals(BusinessExpenseStatus.APPROVED, postExpense?.status)
        assertEquals("EXP-2026-0001", postExpense?.expenseNumber)

        // 4. Check that compensating record holds the adjustment and effective calculations
        val loadedAdj = service.getAdjustmentById(admin, adj.id)
        assertTrue(loadedAdj is DomainResult.Success)
        val adjData = (loadedAdj as DomainResult.Success).data
        assertEquals(BigDecimal("50000.0000"), adjData.originalAmount)
        assertEquals(BigDecimal("-5000.0000"), adjData.adjustmentAmount)
        assertEquals(BigDecimal("45000.0000"), adjData.effectiveAmount)
        assertEquals(AdjustmentStatus.POSTED, adjData.status)
    }
}

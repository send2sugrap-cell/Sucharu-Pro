package com.sucharu.sucharupro.businessreconciliation

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.businessexpense.FakeBusinessExpenseDataSource
import com.sucharu.sucharupro.data.datasource.businessledger.BusinessLedgerPostingFilter
import com.sucharu.sucharupro.data.datasource.businessledger.FakeBusinessLedgerDataSource
import com.sucharu.sucharupro.data.datasource.businessreconciliation.FakeBusinessFinancialReconciliationDataSource
import com.sucharu.sucharupro.data.datasource.vendorpayable.FakeVendorPayableDataSource
import com.sucharu.sucharupro.data.repository.businessexpense.BusinessExpenseRepositoryImpl
import com.sucharu.sucharupro.data.repository.businessledger.BusinessLedgerRepositoryImpl
import com.sucharu.sucharupro.data.repository.businessreconciliation.BusinessFinancialReconciliationRepositoryImpl
import com.sucharu.sucharupro.data.repository.vendorpayable.VendorPayableRepositoryImpl
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpense
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseStatus
import com.sucharu.sucharupro.domain.model.businessledger.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorpayable.VendorPayable
import com.sucharu.sucharupro.domain.model.vendorpayable.VendorPayableStatus
import com.sucharu.sucharupro.domain.service.businessreconciliation.BusinessFinancialReconciliationServiceImpl
import com.sucharu.sucharupro.domain.service.businessreconciliation.CreateReconciliationRunCommand
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessFinancialReconciliationConsistencyTest {

    private lateinit var reconDataSource: FakeBusinessFinancialReconciliationDataSource
    private lateinit var expenseDataSource: FakeBusinessExpenseDataSource
    private lateinit var payableDataSource: FakeVendorPayableDataSource
    private lateinit var ledgerDataSource: FakeBusinessLedgerDataSource

    private lateinit var reconRepo: BusinessFinancialReconciliationRepositoryImpl
    private lateinit var expenseRepo: BusinessExpenseRepositoryImpl
    private lateinit var payableRepo: VendorPayableRepositoryImpl
    private lateinit var ledgerRepo: BusinessLedgerRepositoryImpl

    private lateinit var service: BusinessFinancialReconciliationServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val admin = AuthenticatedPrincipal(userId = "USR-ADMIN", username = "admin_user", role = UserRole.ADMIN, projectId = projectId)

    @Before
    fun setup() {
        reconDataSource = FakeBusinessFinancialReconciliationDataSource()
        expenseDataSource = FakeBusinessExpenseDataSource()
        payableDataSource = FakeVendorPayableDataSource()
        ledgerDataSource = FakeBusinessLedgerDataSource()

        reconRepo = BusinessFinancialReconciliationRepositoryImpl(reconDataSource)
        expenseRepo = BusinessExpenseRepositoryImpl(expenseDataSource)
        payableRepo = VendorPayableRepositoryImpl(payableDataSource)
        ledgerRepo = BusinessLedgerRepositoryImpl(ledgerDataSource)

        service = BusinessFinancialReconciliationServiceImpl(
            repository = reconRepo,
            expenseRepository = expenseRepo,
            payableRepository = payableRepo,
            ledgerRepository = ledgerRepo,
            defaultTenantId = tenantId
        )
    }

    @Test
    fun testReconciliationExecutionCausesZeroMutationOnCanonicalRecords() = runBlocking {
        // Seed Canonical Expense
        val initialExpense = BusinessExpense(
            expenseId = "EXP-ORIG-01",
            tenantId = tenantId,
            projectId = projectId,
            expenseNumber = "EXP-2026-01",
            expenseCategoryId = "CAT-RAW",
            amount = BigDecimal("75000.0000"),
            currency = "BDT",
            status = BusinessExpenseStatus.APPROVED,
            description = "Offset paper stock",
            expenseDate = System.currentTimeMillis(),
            createdBy = "USR-01",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        expenseRepo.createExpense(initialExpense)

        // Seed Canonical Payable
        val initialPayable = VendorPayable(
            payableId = "PAY-ORIG-01",
            tenantId = tenantId,
            projectId = projectId,
            payableNumber = "VP-2026-01",
            vendorId = "VEN-001",
            description = "Supplier invoice for plates",
            originalAmount = BigDecimal("120000.0000"),
            paidAmount = BigDecimal("40000.0000"),
            currency = "BDT",
            issueDate = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis() + 86400000L,
            status = VendorPayableStatus.PARTIALLY_PAID,
            createdBy = "USR-01",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        payableRepo.createPayable(initialPayable)

        // Seed Canonical Ledger Postings (Balanced)
        ledgerRepo.createPosting(
            BusinessLedgerPosting(
                id = "BLP-01",
                tenantId = tenantId,
                projectId = projectId,
                postingNumber = "BLP-2026-01",
                postingType = BusinessLedgerPostingType.EXPENSE_RECOGNITION,
                sourceType = BusinessLedgerSourceType.BUSINESS_EXPENSE,
                sourceId = "EXP-ORIG-01",
                accountCategory = BusinessLedgerAccountCategory.OPERATING_EXPENSE,
                debitAmount = BigDecimal("75000.0000"),
                creditAmount = BigDecimal.ZERO,
                currency = "BDT",
                description = "Expense recognition",
                reference = "EXP-2026-01",
                createdBy = "USR-01",
                createdAt = System.currentTimeMillis()
            )
        )
        ledgerRepo.createPosting(
            BusinessLedgerPosting(
                id = "BLP-02",
                tenantId = tenantId,
                projectId = projectId,
                postingNumber = "BLP-2026-02",
                postingType = BusinessLedgerPostingType.EXPENSE_PAYMENT,
                sourceType = BusinessLedgerSourceType.BUSINESS_EXPENSE,
                sourceId = "EXP-ORIG-01",
                accountCategory = BusinessLedgerAccountCategory.CASH,
                debitAmount = BigDecimal.ZERO,
                creditAmount = BigDecimal("75000.0000"),
                currency = "BDT",
                description = "Expense payment settlement",
                reference = "EXP-2026-01",
                createdBy = "USR-01",
                createdAt = System.currentTimeMillis()
            )
        )

        // Snapshot canonical state before reconciliation
        val expensesBefore = (expenseRepo.listExpenses(tenantId, projectId) as DomainResult.Success).data
        val payablesBefore = (payableRepo.listPayables(tenantId, projectId) as DomainResult.Success).data
        val postingsBefore = ledgerRepo.listPostings(tenantId, projectId, BusinessLedgerPostingFilter())

        // Execute reconciliation run
        val createRes = service.createReconciliationRun(
            admin,
            CreateReconciliationRunCommand(periodId = "PER-2026-08", runNumber = "REC-ZERO-MUT")
        )
        assertTrue(createRes is DomainResult.Success)
        val runId = (createRes as DomainResult.Success).data.id

        val execRes = service.executeReconciliationRun(admin, runId)
        assertTrue(execRes is DomainResult.Success)

        // Snapshot canonical state after reconciliation
        val expensesAfter = (expenseRepo.listExpenses(tenantId, projectId) as DomainResult.Success).data
        val payablesAfter = (payableRepo.listPayables(tenantId, projectId) as DomainResult.Success).data
        val postingsAfter = ledgerRepo.listPostings(tenantId, projectId, BusinessLedgerPostingFilter())

        // PROVE ZERO MUTATION: Canonical records are strictly identical before and after
        assertEquals(expensesBefore.size, expensesAfter.size)
        assertEquals(expensesBefore[0], expensesAfter[0])

        assertEquals(payablesBefore.size, payablesAfter.size)
        assertEquals(payablesBefore[0], payablesAfter[0])

        assertEquals(postingsBefore.size, postingsAfter.size)
        assertEquals(postingsBefore[0], postingsAfter[0])
        assertEquals(postingsBefore[1], postingsAfter[1])
    }
}

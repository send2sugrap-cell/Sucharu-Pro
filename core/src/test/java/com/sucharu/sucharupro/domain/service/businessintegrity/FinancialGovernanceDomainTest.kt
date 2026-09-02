package com.sucharu.sucharupro.domain.service.businessintegrity

import com.sucharu.sucharupro.data.datasource.businesscost.FakeBusinessCostManagementDataSource
import com.sucharu.sucharupro.data.datasource.businesscostcontrol.FakeBusinessCostControlDataSource
import com.sucharu.sucharupro.data.datasource.businessexpense.FakeBusinessExpenseDataSource
import com.sucharu.sucharupro.data.datasource.businessfinancialadjustment.FakeBusinessFinancialAdjustmentDataSource
import com.sucharu.sucharupro.data.datasource.businessfinancialgovernance.FakeBusinessFinancialGovernanceDataSource
import com.sucharu.sucharupro.data.datasource.businessintegrity.FakeBusinessFinancialIntegrityDataSource
import com.sucharu.sucharupro.data.datasource.businessledger.FakeBusinessLedgerDataSource
import com.sucharu.sucharupro.data.datasource.businessreconciliation.FakeBusinessFinancialReconciliationDataSource
import com.sucharu.sucharupro.data.datasource.vendorpayable.FakeVendorPayableDataSource
import com.sucharu.sucharupro.data.repository.businesscost.BusinessCostManagementRepositoryImpl
import com.sucharu.sucharupro.data.repository.businesscostcontrol.BusinessCostControlRepositoryImpl
import com.sucharu.sucharupro.data.repository.businessexpense.BusinessExpenseRepositoryImpl
import com.sucharu.sucharupro.data.repository.businessfinancialadjustment.BusinessFinancialAdjustmentRepositoryImpl
import com.sucharu.sucharupro.data.repository.businessfinancialgovernance.BusinessFinancialGovernanceRepositoryImpl
import com.sucharu.sucharupro.data.repository.businessintegrity.BusinessFinancialIntegrityRepositoryImpl
import com.sucharu.sucharupro.data.repository.businessledger.BusinessLedgerRepositoryImpl
import com.sucharu.sucharupro.data.repository.businessreconciliation.BusinessFinancialReconciliationRepositoryImpl
import com.sucharu.sucharupro.data.repository.vendorpayable.VendorPayableRepositoryImpl
import com.sucharu.sucharupro.domain.model.businesscostcontrol.BusinessFinancialPeriod
import com.sucharu.sucharupro.domain.model.businesscostcontrol.BusinessFinancialPeriodStatus
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpense
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpensePaymentMethod
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseStatus
import com.sucharu.sucharupro.domain.model.businessintegrity.*
import com.sucharu.sucharupro.domain.model.businessledger.BusinessLedgerAccountCategory
import com.sucharu.sucharupro.domain.model.businessledger.BusinessLedgerPosting
import com.sucharu.sucharupro.domain.model.businessledger.BusinessLedgerPostingType
import com.sucharu.sucharupro.domain.model.businessledger.BusinessLedgerSourceType
import com.sucharu.sucharupro.domain.model.common.DomainResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class FinancialGovernanceDomainTest {

    private lateinit var integrityDataSource: FakeBusinessFinancialIntegrityDataSource
    private lateinit var expenseDataSource: FakeBusinessExpenseDataSource
    private lateinit var payableDataSource: FakeVendorPayableDataSource
    private lateinit var ledgerDataSource: FakeBusinessLedgerDataSource
    private lateinit var costManagementDataSource: FakeBusinessCostManagementDataSource
    private lateinit var costControlDataSource: FakeBusinessCostControlDataSource
    private lateinit var reconciliationDataSource: FakeBusinessFinancialReconciliationDataSource
    private lateinit var adjustmentDataSource: FakeBusinessFinancialAdjustmentDataSource
    private lateinit var governanceDataSource: FakeBusinessFinancialGovernanceDataSource

    private lateinit var integrityService: BusinessFinancialIntegrityService

    private val tenantId = "TENANT-001"
    private val projectId = "PROJ-101"
    private val periodId = "PER-2026-M08"

    @Before
    fun setUp() = runBlocking {
        integrityDataSource = FakeBusinessFinancialIntegrityDataSource()
        expenseDataSource = FakeBusinessExpenseDataSource()
        payableDataSource = FakeVendorPayableDataSource()
        ledgerDataSource = FakeBusinessLedgerDataSource()
        costManagementDataSource = FakeBusinessCostManagementDataSource()
        costControlDataSource = FakeBusinessCostControlDataSource()
        reconciliationDataSource = FakeBusinessFinancialReconciliationDataSource()
        adjustmentDataSource = FakeBusinessFinancialAdjustmentDataSource()
        governanceDataSource = FakeBusinessFinancialGovernanceDataSource()

        val integrityRepo = BusinessFinancialIntegrityRepositoryImpl(integrityDataSource)
        val expenseRepo = BusinessExpenseRepositoryImpl(expenseDataSource)
        val payableRepo = VendorPayableRepositoryImpl(payableDataSource)
        val ledgerRepo = BusinessLedgerRepositoryImpl(ledgerDataSource)
        val costManagementRepo = BusinessCostManagementRepositoryImpl(costManagementDataSource)
        val costControlRepo = BusinessCostControlRepositoryImpl(costControlDataSource)
        val reconciliationRepo = BusinessFinancialReconciliationRepositoryImpl(reconciliationDataSource)
        val adjustmentRepo = BusinessFinancialAdjustmentRepositoryImpl(adjustmentDataSource)
        val governanceRepo = BusinessFinancialGovernanceRepositoryImpl(governanceDataSource)

        // Seed Open Financial Period
        costControlRepo.createFinancialPeriod(
            BusinessFinancialPeriod(
                id = periodId,
                tenantId = tenantId,
                projectId = projectId,
                periodCode = "2026-M08",
                periodName = "August 2026",
                startDate = 1754092800000L,
                endDate = 1756771199000L,
                status = BusinessFinancialPeriodStatus.OPEN,
                createdBy = "ADMIN-1"
            )
        )

        // Seed Balanced Ledger Postings
        ledgerRepo.createPosting(
            BusinessLedgerPosting(
                id = "POST-1",
                tenantId = tenantId,
                projectId = projectId,
                postingNumber = "PN-001",
                postingType = BusinessLedgerPostingType.EXPENSE_RECOGNITION,
                sourceType = BusinessLedgerSourceType.BUSINESS_EXPENSE,
                sourceId = "EXP-1",
                accountCategory = BusinessLedgerAccountCategory.OFFICE_EXPENSE,
                debitAmount = BigDecimal("5000.0000"),
                creditAmount = BigDecimal.ZERO,
                currency = "BDT",
                description = "Office Supplies",
                createdBy = "USER-1"
            )
        )
        ledgerRepo.createPosting(
            BusinessLedgerPosting(
                id = "POST-2",
                tenantId = tenantId,
                projectId = projectId,
                postingNumber = "PN-002",
                postingType = BusinessLedgerPostingType.EXPENSE_PAYMENT,
                sourceType = BusinessLedgerSourceType.BUSINESS_EXPENSE,
                sourceId = "EXP-1",
                accountCategory = BusinessLedgerAccountCategory.CASH,
                debitAmount = BigDecimal.ZERO,
                creditAmount = BigDecimal("5000.0000"),
                currency = "BDT",
                description = "Office Supplies Cash",
                createdBy = "USER-1"
            )
        )

        // Seed Posted Expense
        expenseRepo.createExpense(
            BusinessExpense(
                expenseId = "EXP-1",
                tenantId = tenantId,
                projectId = projectId,
                expenseNumber = "EXP-001",
                expenseCategoryId = "CAT-GEN",
                amount = BigDecimal("5000.0000"),
                currency = "BDT",
                paymentMethod = BusinessExpensePaymentMethod.CASH,
                status = BusinessExpenseStatus.POSTABLE,
                description = "Office Supplies",
                createdBy = "USER-1",
                approvedBy = "ADMIN-1"
            )
        )

        integrityService = BusinessFinancialIntegrityServiceImpl(
            integrityRepository = integrityRepo,
            expenseRepository = expenseRepo,
            payableRepository = payableRepo,
            ledgerRepository = ledgerRepo,
            costManagementRepository = costManagementRepo,
            costControlRepository = costControlRepo,
            reconciliationRepository = reconciliationRepo,
            adjustmentRepository = adjustmentRepo,
            governanceRepository = governanceRepo,
            defaultTenantId = tenantId
        )
    }

    @Test
    fun `executeIntegrityRun runs all 18 assertions and succeeds on balanced canonical data`() = runBlocking {
        val result = integrityService.executeIntegrityRun(
            tenantId = tenantId,
            projectId = projectId,
            periodId = periodId,
            actorId = "ADMIN-1",
            actorRole = "ADMIN",
            notes = "Scheduled period-end integrity verification"
        )

        assertTrue(result is DomainResult.Success)
        val run = (result as DomainResult.Success).data

        assertEquals(18, run.totalAssertionsCount)
        assertEquals(18, run.assertions.size)
        assertEquals(FinancialIntegrityStatus.PASSED, run.status)
        assertEquals(18, run.passedAssertionsCount)
        assertEquals(0, run.failedAssertionsCount)
        assertTrue(run.integrityChecksum.isNotBlank())

        val assert01 = run.assertions.find { it.assertionType == FinancialAssertionType.ASSERTION_01_LEDGER_BALANCE }
        assertNotNull(assert01)
        assertEquals(FinancialIntegrityStatus.PASSED, assert01!!.status)

        val assert02 = run.assertions.find { it.assertionType == FinancialAssertionType.ASSERTION_02_EXPENSE_POSTING }
        assertNotNull(assert02)
        assertEquals(FinancialIntegrityStatus.PASSED, assert02!!.status)
    }

    @Test
    fun `evaluatePeriodFinalizationReadiness returns READY when all assertions pass and ledger is balanced`() = runBlocking {
        integrityService.executeIntegrityRun(tenantId, projectId, periodId, "ADMIN-1", "ADMIN")

        val readinessRes = integrityService.evaluatePeriodFinalizationReadiness(tenantId, projectId, periodId)
        assertTrue(readinessRes is DomainResult.Success)
        val readiness = (readinessRes as DomainResult.Success).data

        assertTrue(readiness.isReadyForClose)
        assertEquals(PeriodClosureStatus.READY, readiness.status)
        assertTrue(readiness.blockingReasons.isEmpty())
    }

    @Test
    fun `finalizePeriodClose succeeds and generates tamper-evident certificate`() = runBlocking {
        integrityService.executeIntegrityRun(tenantId, projectId, periodId, "ADMIN-1", "ADMIN")

        val certRes = integrityService.finalizePeriodClose(
            tenantId = tenantId,
            projectId = projectId,
            periodId = periodId,
            actorId = "MANAGER-2",
            actorRole = "MANAGER",
            requesterId = "USER-1",
            notes = "August 2026 final close"
        )

        assertTrue(certRes is DomainResult.Success)
        val cert = (certRes as DomainResult.Success).data

        assertEquals("FINALIZED", cert.status)
        assertEquals("MANAGER-2", cert.closedBy)
        assertEquals(BigDecimal("5000.0000"), cert.totalRecognizedExpenses)
        assertEquals(BigDecimal("5000.0000"), cert.totalLedgerDebit)
        assertEquals(BigDecimal("5000.0000"), cert.totalLedgerCredit)
        assertTrue(cert.certificateChecksum.isNotBlank())
    }

    @Test
    fun `generateModule16HandoffContract produces complete verified handoff dataset`() = runBlocking {
        val handoffRes = integrityService.generateModule16HandoffContract(tenantId, projectId, periodId)
        assertTrue(handoffRes is DomainResult.Success)
        val handoff = (handoffRes as DomainResult.Success).data

        assertEquals(tenantId, handoff.tenantId)
        assertEquals(projectId, handoff.projectId)
        assertEquals(periodId, handoff.periodId)
        assertEquals(BigDecimal("5000.0000"), handoff.totalDirectExpenses)
        assertEquals(BigDecimal("5000.0000"), handoff.ledgerTotalDebit)
        assertEquals(BigDecimal("5000.0000"), handoff.ledgerTotalCredit)
        assertTrue(handoff.isLedgerBalanced)
    }
}

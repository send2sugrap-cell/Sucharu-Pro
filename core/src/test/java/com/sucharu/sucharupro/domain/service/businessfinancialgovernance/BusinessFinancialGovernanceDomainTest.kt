package com.sucharu.sucharupro.domain.service.businessfinancialgovernance

import com.sucharu.sucharupro.data.datasource.businessfinancialgovernance.FakeBusinessFinancialGovernanceDataSource
import com.sucharu.sucharupro.data.repository.businessfinancialgovernance.BusinessFinancialGovernanceRepositoryImpl
import com.sucharu.sucharupro.domain.model.businesscostcontrol.BusinessCostCommitment
import com.sucharu.sucharupro.domain.model.businesscostcontrol.BusinessCostCommitmentStatus
import com.sucharu.sucharupro.domain.model.businesscostcontrol.BusinessFinancialPeriod
import com.sucharu.sucharupro.domain.model.businesscostcontrol.BusinessFinancialPeriodStatus
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpense
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpensePaymentMethod
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseStatus
import com.sucharu.sucharupro.domain.model.businessfinancialgovernance.*
import com.sucharu.sucharupro.domain.repository.businessfinancialgovernance.GovernanceAlertFilter
import com.sucharu.sucharupro.domain.model.common.DomainResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessFinancialGovernanceDomainTest {

    private lateinit var fakeGovernanceDataSource: FakeBusinessFinancialGovernanceDataSource
    private lateinit var governanceRepository: BusinessFinancialGovernanceRepositoryImpl
    private lateinit var expenseRepository: TestExpenseRepository
    private lateinit var payableRepository: TestPayableRepository
    private lateinit var ledgerRepository: TestLedgerRepository
    private lateinit var costManagementRepository: TestCostManagementRepository
    private lateinit var costControlRepository: TestCostControlRepository
    private lateinit var reconciliationRepository: TestReconciliationRepository
    private lateinit var adjustmentRepository: TestAdjustmentRepository
    private lateinit var service: BusinessFinancialGovernanceServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PROJ-001"
    private val periodId = "PERIOD-2026-Q1"

    @Before
    fun setUp() {
        fakeGovernanceDataSource = FakeBusinessFinancialGovernanceDataSource()
        governanceRepository = BusinessFinancialGovernanceRepositoryImpl(fakeGovernanceDataSource)
        expenseRepository = TestExpenseRepository()
        payableRepository = TestPayableRepository()
        ledgerRepository = TestLedgerRepository()
        costManagementRepository = TestCostManagementRepository()
        costControlRepository = TestCostControlRepository()
        reconciliationRepository = TestReconciliationRepository()
        adjustmentRepository = TestAdjustmentRepository()

        service = BusinessFinancialGovernanceServiceImpl(
            governanceRepository = governanceRepository,
            expenseRepository = expenseRepository,
            payableRepository = payableRepository,
            ledgerRepository = ledgerRepository,
            costManagementRepository = costManagementRepository,
            costControlRepository = costControlRepository,
            reconciliationRepository = reconciliationRepository,
            adjustmentRepository = adjustmentRepository,
            defaultTenantId = tenantId
        )
    }

    @Test
    fun `test full budget lifecycle and SoD approval constraint`() = runBlocking {
        val budget = BusinessFinancialBudget(
            id = "BUD-001",
            tenantId = tenantId,
            projectId = projectId,
            budgetName = "Q1 Marketing Budget",
            periodId = periodId,
            dimensionType = BusinessFinancialBudgetDimensionType.COST_CATEGORY,
            dimensionId = "CAT-MKT-01",
            allocatedAmount = BigDecimal("50000.0000"),
            currency = "BDT",
            effectiveStartDate = 1767225600000L,
            effectiveEndDate = 1775088000000L,
            createdBy = "USER-CREATOR"
        )

        // 1. Create Budget
        val createRes = service.createBudget(budget, "USER-CREATOR", "MANAGER")
        assertTrue(createRes is DomainResult.Success)
        val created = (createRes as DomainResult.Success).data
        assertEquals(BusinessFinancialBudgetStatus.DRAFT, created.status)
        assertEquals(1L, created.version)

        // 2. Submit Budget
        val submitRes = service.submitBudget(tenantId, projectId, "BUD-001", "USER-CREATOR", "MANAGER")
        assertTrue(submitRes is DomainResult.Success)
        assertEquals(BusinessFinancialBudgetStatus.SUBMITTED, (submitRes as DomainResult.Success).data.status)

        // 3. Review Budget
        val reviewRes = service.reviewBudget(tenantId, projectId, "BUD-001", "USER-REVIEWER", "MANAGER")
        assertTrue(reviewRes is DomainResult.Success)
        assertEquals(BusinessFinancialBudgetStatus.REVIEWED, (reviewRes as DomainResult.Success).data.status)

        // 4. SoD Check: Creator cannot approve their own budget
        val invalidApproveRes = service.approveBudget(tenantId, projectId, "BUD-001", "USER-CREATOR", "ADMIN")
        assertTrue(invalidApproveRes is DomainResult.Error)
        assertTrue((invalidApproveRes as DomainResult.Error).message.contains("Separation of Duties violation"))

        // 5. Valid Approval by distinct Admin
        val approveRes = service.approveBudget(tenantId, projectId, "BUD-001", "USER-APPROVER", "ADMIN")
        assertTrue(approveRes is DomainResult.Success)
        assertEquals(BusinessFinancialBudgetStatus.APPROVED, (approveRes as DomainResult.Success).data.status)

        // 6. Activate Budget
        val activateRes = service.activateBudget(tenantId, projectId, "BUD-001", "USER-APPROVER", "ADMIN")
        assertTrue(activateRes is DomainResult.Success)
        assertEquals(BusinessFinancialBudgetStatus.ACTIVE, (activateRes as DomainResult.Success).data.status)

        // 7. Revise Budget
        val reviseRes = service.reviseBudget(
            tenantId = tenantId,
            projectId = projectId,
            budgetId = "BUD-001",
            newAllocatedAmount = BigDecimal("65000.0000"),
            revisionReason = "Scope expansion for campaign",
            actorId = "USER-APPROVER",
            actorRole = "ADMIN"
        )
        assertTrue(reviseRes is DomainResult.Success)
        val revised = (reviseRes as DomainResult.Success).data
        assertEquals(2L, revised.version)
        assertEquals(BigDecimal("65000.0000"), revised.allocatedAmount)

        // Verify revision audit snapshot
        val revisionsRes = service.listBudgetRevisions(tenantId, projectId, "BUD-001")
        assertTrue(revisionsRes is DomainResult.Success)
        val revisions = (revisionsRes as DomainResult.Success).data
        assertEquals(1, revisions.size)
        assertEquals(BigDecimal("50000.0000"), revisions[0].previousAllocatedAmount)
        assertEquals(BigDecimal("65000.0000"), revisions[0].newAllocatedAmount)
    }

    @Test
    fun `test budget vs actual comparison projecting from canonical repositories`() = runBlocking {
        val budget = BusinessFinancialBudget(
            id = "BUD-002",
            tenantId = tenantId,
            projectId = projectId,
            budgetName = "IT Infrastructure",
            periodId = periodId,
            dimensionType = BusinessFinancialBudgetDimensionType.COST_CATEGORY,
            dimensionId = "CAT-IT",
            allocatedAmount = BigDecimal("100000.0000"),
            currency = "BDT",
            effectiveStartDate = 1000L,
            effectiveEndDate = 2000L,
            status = BusinessFinancialBudgetStatus.ACTIVE,
            createdBy = "USER-A"
        )
        fakeGovernanceDataSource.saveBudget(budget)

        // Seed Canonical Expenses
        val expense = BusinessExpense(
            expenseId = "EXP-1",
            tenantId = tenantId,
            projectId = projectId,
            expenseNumber = "EXP-001",
            expenseCategoryId = "CAT-IT",
            amount = BigDecimal("45000.0000"),
            currency = "BDT",
            expenseDate = 1500L,
            paymentMethod = BusinessExpensePaymentMethod.BANK,
            status = BusinessExpenseStatus.APPROVED,
            description = "Server licensing",
            createdBy = "USER-A",
            createdAt = 1500L,
            updatedAt = 1500L
        )
        expenseRepository.createExpense(expense)

        // Seed Canonical Commitments
        val commitment = BusinessCostCommitment(
            id = "COM-1",
            tenantId = tenantId,
            projectId = projectId,
            commitmentNumber = "COM-001",
            costCategoryId = "CAT-IT",
            description = "Hardware Lease",
            committedAmount = BigDecimal("20000.0000"),
            consumedAmount = BigDecimal("5000.0000"),
            remainingAmount = BigDecimal("15000.0000"),
            currency = "BDT",
            status = BusinessCostCommitmentStatus.ACTIVE,
            sourceId = "SRC-1",
            commitmentDate = 1500L,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        costControlRepository.createCommitment(commitment)

        // Calculate Budget vs Actual
        val comparisonRes = service.calculateBudgetVsActual(tenantId, projectId, "BUD-002", "BDT")
        assertTrue(comparisonRes is DomainResult.Success)
        val comp = (comparisonRes as DomainResult.Success).data

        assertEquals(BigDecimal("100000.0000"), comp.allocatedBudget)
        assertEquals(BigDecimal("45000.0000"), comp.actualSpend)
        assertEquals(BigDecimal("15000.0000"), comp.committedExposure) // 15k remaining
        assertEquals(BigDecimal("60000.0000"), comp.totalProjectedExposure) // 45k + 15k
        assertEquals(BigDecimal("55000.0000"), comp.remainingBudget) // 100k - 45k
        assertEquals(BigDecimal("40000.0000"), comp.remainingProjectedBudget) // 100k - 60k
        assertEquals(BigDecimal("45.0000"), comp.utilizationPercentage)
        assertEquals(BigDecimal("60.0000"), comp.projectedUtilizationPercentage)
        assertEquals(BudgetVarianceStatus.ON_TRACK, comp.varianceStatus)
    }

    @Test
    fun `test deterministic run-rate forecasting and scenarios`() = runBlocking {
        val now = System.currentTimeMillis()
        val startDate = now - (15 * 86400000L) // 15 days elapsed
        val endDate = now + (15 * 86400000L) // 15 days remaining

        val period = BusinessFinancialPeriod(
            id = periodId,
            tenantId = tenantId,
            projectId = projectId,
            periodCode = "2026-03",
            periodName = "March 2026",
            startDate = startDate,
            endDate = endDate,
            status = BusinessFinancialPeriodStatus.OPEN,
            createdAt = startDate,
            updatedAt = startDate
        )
        costControlRepository.createFinancialPeriod(period)

        val budget = BusinessFinancialBudget(
            id = "BUD-003",
            tenantId = tenantId,
            projectId = projectId,
            budgetName = "Operations",
            periodId = periodId,
            dimensionType = BusinessFinancialBudgetDimensionType.COST_CATEGORY,
            dimensionId = "CAT-OPS",
            allocatedAmount = BigDecimal("100000.0000"),
            currency = "BDT",
            effectiveStartDate = startDate,
            effectiveEndDate = endDate,
            status = BusinessFinancialBudgetStatus.ACTIVE,
            createdBy = "USER-A"
        )
        fakeGovernanceDataSource.saveBudget(budget)

        val expense = BusinessExpense(
            expenseId = "EXP-2",
            tenantId = tenantId,
            projectId = projectId,
            expenseNumber = "EXP-002",
            expenseCategoryId = "CAT-OPS",
            amount = BigDecimal("60000.0000"),
            currency = "BDT",
            expenseDate = now - 1000L,
            paymentMethod = BusinessExpensePaymentMethod.CASH,
            status = BusinessExpenseStatus.APPROVED,
            description = "Operations daily expenses",
            createdBy = "USER-A",
            createdAt = now - 1000L,
            updatedAt = now - 1000L
        )
        expenseRepository.createExpense(expense)

        // Generate Forecast
        val forecastRes = service.generateDeterministicForecast(
            tenantId = tenantId,
            projectId = projectId,
            periodId = periodId,
            dimensionType = BusinessFinancialBudgetDimensionType.COST_CATEGORY,
            dimensionId = "CAT-OPS",
            currency = "BDT",
            actorId = "USER-A"
        )
        assertTrue(forecastRes is DomainResult.Success)
        val (forecast, scenarios) = (forecastRes as DomainResult.Success).data

        assertEquals(BigDecimal("60000.0000"), forecast.actualYtdAmount)
        assertEquals(3, scenarios.size)
        val baseline = scenarios.find { it.scenarioType == ForecastScenarioType.BASELINE }
        assertNotNull(baseline)
        assertEquals(forecast.forecastTotalAmount, baseline!!.projectedAmount)
    }

    @Test
    fun `test threshold evaluation and alert deduplication`() = runBlocking {
        val budget = BusinessFinancialBudget(
            id = "BUD-004",
            tenantId = tenantId,
            projectId = projectId,
            budgetName = "Overspend Project",
            periodId = periodId,
            dimensionType = BusinessFinancialBudgetDimensionType.JOB,
            dimensionId = "JOB-99",
            allocatedAmount = BigDecimal("50000.0000"),
            currency = "BDT",
            effectiveStartDate = 1000L,
            effectiveEndDate = 2000L,
            status = BusinessFinancialBudgetStatus.ACTIVE,
            createdBy = "USER-A"
        )
        fakeGovernanceDataSource.saveBudget(budget)

        val expense = BusinessExpense(
            expenseId = "EXP-3",
            tenantId = tenantId,
            projectId = projectId,
            expenseNumber = "EXP-003",
            jobId = "JOB-99",
            expenseCategoryId = "CAT-GEN",
            amount = BigDecimal("48000.0000"),
            currency = "BDT",
            expenseDate = 1500L,
            paymentMethod = BusinessExpensePaymentMethod.BANK,
            status = BusinessExpenseStatus.APPROVED,
            description = "Job execution costs",
            createdBy = "USER-A",
            createdAt = 1500L,
            updatedAt = 1500L
        )
        expenseRepository.createExpense(expense)

        // Evaluate Thresholds - First Run generates alert (96% utilization -> BUDGET_WARNING)
        val eval1 = service.evaluateGovernanceThresholdsAndGenerateAlerts(tenantId, projectId, periodId, "BDT", "SYSTEM")
        assertTrue(eval1 is DomainResult.Success)
        val alerts1 = (eval1 as DomainResult.Success).data
        assertTrue(alerts1.isNotEmpty())
        assertEquals(GovernanceAlertType.BUDGET_WARNING, alerts1[0].alertType)

        // Evaluate Thresholds - Second Run must NOT duplicate the open alert
        val eval2 = service.evaluateGovernanceThresholdsAndGenerateAlerts(tenantId, projectId, periodId, "BDT", "SYSTEM")
        assertTrue(eval2 is DomainResult.Success)
        val totalAlerts = fakeGovernanceDataSource.listAlerts(tenantId, projectId, GovernanceAlertFilter())
        assertEquals(1, totalAlerts.size)

        // Acknowledge Alert
        val ackRes = service.acknowledgeAlert(tenantId, projectId, totalAlerts[0].id, "Monitoring job completion", "USER-MGR", "MANAGER")
        assertTrue(ackRes is DomainResult.Success)
        assertEquals(GovernanceAlertStatus.ACKNOWLEDGED, (ackRes as DomainResult.Success).data.status)
    }
}

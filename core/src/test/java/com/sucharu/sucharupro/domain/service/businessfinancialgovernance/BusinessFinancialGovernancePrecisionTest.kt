package com.sucharu.sucharupro.domain.service.businessfinancialgovernance

import com.sucharu.sucharupro.data.datasource.businessfinancialgovernance.FakeBusinessFinancialGovernanceDataSource
import com.sucharu.sucharupro.data.repository.businessfinancialgovernance.BusinessFinancialGovernanceRepositoryImpl
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpense
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpensePaymentMethod
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseStatus
import com.sucharu.sucharupro.domain.model.businessfinancialgovernance.BusinessFinancialBudget
import com.sucharu.sucharupro.domain.model.businessfinancialgovernance.BusinessFinancialBudgetDimensionType
import com.sucharu.sucharupro.domain.model.businessfinancialgovernance.BusinessFinancialBudgetStatus
import com.sucharu.sucharupro.domain.model.common.DomainResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessFinancialGovernancePrecisionTest {

    private lateinit var fakeDataSource: FakeBusinessFinancialGovernanceDataSource
    private lateinit var governanceRepository: BusinessFinancialGovernanceRepositoryImpl
    private lateinit var expenseRepository: TestExpenseRepository
    private lateinit var costControlRepository: TestCostControlRepository
    private lateinit var service: BusinessFinancialGovernanceServiceImpl

    private val tenantId = "TENANT-PRECISION"
    private val projectId = "PROJ-PRECISION"
    private val periodId = "PERIOD-PRECISION"

    @Before
    fun setUp() {
        fakeDataSource = FakeBusinessFinancialGovernanceDataSource()
        governanceRepository = BusinessFinancialGovernanceRepositoryImpl(fakeDataSource)
        expenseRepository = TestExpenseRepository()
        costControlRepository = TestCostControlRepository()

        service = BusinessFinancialGovernanceServiceImpl(
            governanceRepository = governanceRepository,
            expenseRepository = expenseRepository,
            payableRepository = TestPayableRepository(),
            ledgerRepository = TestLedgerRepository(),
            costManagementRepository = TestCostManagementRepository(),
            costControlRepository = costControlRepository,
            reconciliationRepository = TestReconciliationRepository(),
            adjustmentRepository = TestAdjustmentRepository(),
            defaultTenantId = tenantId
        )
    }

    @Test
    fun `test BigDecimal arithmetic precision - no float inaccuracy in variance and utilization`() = runBlocking {
        // Budget = 33333.3333
        val budget = BusinessFinancialBudget(
            id = "BUD-PRECISION-1",
            tenantId = tenantId,
            projectId = projectId,
            budgetName = "Precision Test Budget",
            periodId = periodId,
            dimensionType = BusinessFinancialBudgetDimensionType.COST_CATEGORY,
            dimensionId = "CAT-1",
            allocatedAmount = BigDecimal("33333.3333"),
            currency = "BDT",
            effectiveStartDate = 1000L,
            effectiveEndDate = 2000L,
            status = BusinessFinancialBudgetStatus.ACTIVE,
            createdBy = "USER-1"
        )
        fakeDataSource.saveBudget(budget)

        // Expense = 11111.1111
        val expense = BusinessExpense(
            expenseId = "EXP-P1",
            tenantId = tenantId,
            projectId = projectId,
            expenseNumber = "EXP-P1",
            expenseCategoryId = "CAT-1",
            amount = BigDecimal("11111.1111"),
            currency = "BDT",
            expenseDate = 1500L,
            paymentMethod = BusinessExpensePaymentMethod.BANK,
            status = BusinessExpenseStatus.APPROVED,
            description = "Exact fractional expense",
            createdBy = "USER-1",
            createdAt = 1500L,
            updatedAt = 1500L
        )
        expenseRepository.createExpense(expense)

        val compRes = service.calculateBudgetVsActual(tenantId, projectId, "BUD-PRECISION-1", "BDT")
        assertTrue(compRes is DomainResult.Success)
        val comp = (compRes as DomainResult.Success).data

        // Remaining = 33333.3333 - 11111.1111 = 22222.2222
        assertEquals(BigDecimal("22222.2222"), comp.remainingBudget)
        // Utilization = 11111.1111 / 33333.3333 * 100 = 33.3333 %
        assertEquals(BigDecimal("33.3333"), comp.utilizationPercentage)
        assertEquals(4, comp.utilizationPercentage.scale())
    }
}

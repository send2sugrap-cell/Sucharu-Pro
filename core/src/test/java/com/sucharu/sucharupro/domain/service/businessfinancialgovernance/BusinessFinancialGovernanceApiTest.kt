package com.sucharu.sucharupro.domain.service.businessfinancialgovernance

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.api.model.businessfinancialgovernance.*
import com.sucharu.sucharupro.data.api.server.BackendUseCases
import com.sucharu.sucharupro.data.datasource.businessfinancialgovernance.FakeBusinessFinancialGovernanceDataSource
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.data.repository.businessfinancialgovernance.BusinessFinancialGovernanceRepositoryImpl
import com.sucharu.sucharupro.domain.model.businessfinancialgovernance.BusinessFinancialBudgetDimensionType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessFinancialGovernanceApiTest {

    private lateinit var fakeDataSource: FakeBusinessFinancialGovernanceDataSource
    private lateinit var governanceRepository: BusinessFinancialGovernanceRepositoryImpl
    private lateinit var service: BusinessFinancialGovernanceServiceImpl

    private val tenantId = "TENANT-API-01"
    private val projectId = "PROJ-API-01"

    @Before
    fun setUp() {
        fakeDataSource = FakeBusinessFinancialGovernanceDataSource()
        governanceRepository = BusinessFinancialGovernanceRepositoryImpl(fakeDataSource)
        service = BusinessFinancialGovernanceServiceImpl(
            governanceRepository = governanceRepository,
            expenseRepository = TestExpenseRepository(),
            payableRepository = TestPayableRepository(),
            ledgerRepository = TestLedgerRepository(),
            costManagementRepository = TestCostManagementRepository(),
            costControlRepository = TestCostControlRepository(),
            reconciliationRepository = TestReconciliationRepository(),
            adjustmentRepository = TestAdjustmentRepository(),
            defaultTenantId = tenantId
        )
    }

    @Test
    fun `test create budget and variance comparison API workflow`() = runBlocking {
        val adminPrincipal = AuthenticatedPrincipal(
            userId = "ADMIN-01",
            projectId = projectId,
            username = "admin_user",
            role = UserRole.ADMIN
        )

        // 1. Create Budget Request
        val createDto = CreateFinancialBudgetRequestDto(
            budgetName = "Executive Travel Budget",
            periodId = "2026-Q2",
            dimensionType = BusinessFinancialBudgetDimensionType.COST_CATEGORY,
            dimensionId = "CAT-TRAVEL",
            allocatedAmount = BigDecimal("80000.0000"),
            currency = "BDT",
            effectiveStartDate = 1000L,
            effectiveEndDate = 2000L,
            description = "Travel and board budget for executive team"
        )

        val budget = createDto.toDomain(tenantId, projectId, adminPrincipal.userId)
        val createRes = service.createBudget(budget, adminPrincipal.userId, adminPrincipal.role.name)
        assertTrue(createRes is com.sucharu.sucharupro.domain.model.common.DomainResult.Success)
        val created = (createRes as com.sucharu.sucharupro.domain.model.common.DomainResult.Success).data
        assertEquals("Executive Travel Budget", created.budgetName)

        // 2. Submit, Approve and Activate
        val submitRes = service.submitBudget(tenantId, projectId, created.id, adminPrincipal.userId, adminPrincipal.role.name)
        assertTrue(submitRes is com.sucharu.sucharupro.domain.model.common.DomainResult.Success)

        val approveRes = service.approveBudget(tenantId, projectId, created.id, "DISTINCT-APPROVER", "ADMIN")
        assertTrue(approveRes is com.sucharu.sucharupro.domain.model.common.DomainResult.Success)

        val activateRes = service.activateBudget(tenantId, projectId, created.id, "DISTINCT-APPROVER", "ADMIN")
        assertTrue(activateRes is com.sucharu.sucharupro.domain.model.common.DomainResult.Success)

        // 3. Query Budget vs Actual
        val compRes = service.calculateBudgetVsActual(tenantId, projectId, created.id, "BDT")
        assertTrue(compRes is com.sucharu.sucharupro.domain.model.common.DomainResult.Success)
        val compDto = BudgetVsActualComparisonDto.fromDomain((compRes as com.sucharu.sucharupro.domain.model.common.DomainResult.Success).data)
        assertEquals(BigDecimal("80000.0000"), compDto.allocatedBudget)
        assertEquals(BigDecimal("80000.0000"), compDto.remainingBudget)

        // 4. Executive Overview
        val overviewRes = service.getExecutiveGovernanceOverview(tenantId, projectId, "2026-Q2", "BDT")
        assertTrue(overviewRes is com.sucharu.sucharupro.domain.model.common.DomainResult.Success)
        val overviewDto = ExecutiveGovernanceOverviewDto.fromDomain((overviewRes as com.sucharu.sucharupro.domain.model.common.DomainResult.Success).data)
        assertEquals(1, overviewDto.totalActiveBudgetsCount)
        assertEquals(BigDecimal("80000.0000"), overviewDto.totalAllocatedBudgetAmount)
    }
}

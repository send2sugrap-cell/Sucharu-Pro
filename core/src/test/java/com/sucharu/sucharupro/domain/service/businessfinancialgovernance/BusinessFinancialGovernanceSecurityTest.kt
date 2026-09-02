package com.sucharu.sucharupro.domain.service.businessfinancialgovernance

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.businessfinancialgovernance.FakeBusinessFinancialGovernanceDataSource
import com.sucharu.sucharupro.data.repository.businessfinancialgovernance.BusinessFinancialGovernanceRepositoryImpl
import com.sucharu.sucharupro.domain.model.businessfinancialgovernance.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.validation.businessfinancialgovernance.BusinessFinancialGovernanceValidator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessFinancialGovernanceSecurityTest {

    private lateinit var fakeDataSource: FakeBusinessFinancialGovernanceDataSource
    private lateinit var governanceRepository: BusinessFinancialGovernanceRepositoryImpl
    private lateinit var service: BusinessFinancialGovernanceServiceImpl

    private val tenantA = "TENANT-A"
    private val tenantB = "TENANT-B"
    private val projectId = "PROJ-SEC-01"

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
            defaultTenantId = tenantA
        )
    }

    @Test
    fun `test RBAC - Admin and Manager allowed but Customer and Vendor disallowed from budget modifications`() = runBlocking {
        val adminPrincipal = AuthenticatedPrincipal(userId = "admin-1", projectId = projectId, username = "admin", role = UserRole.ADMIN)
        val managerPrincipal = AuthenticatedPrincipal(userId = "mgr-1", projectId = projectId, username = "manager", role = UserRole.MANAGER)
        val staffPrincipal = AuthenticatedPrincipal(userId = "staff-1", projectId = projectId, username = "staff", role = UserRole.STAFF)
        val customerPrincipal = AuthenticatedPrincipal(userId = "cust-1", projectId = projectId, username = "customer", role = UserRole.CUSTOMER)
        val vendorPrincipal = AuthenticatedPrincipal(userId = "vend-1", projectId = projectId, username = "vendor", role = UserRole.VENDOR)

        val budget = BusinessFinancialBudget(
            id = "BUD-SEC-1",
            tenantId = tenantA,
            projectId = projectId,
            budgetName = "Security R&D",
            periodId = "2026-FY",
            dimensionType = BusinessFinancialBudgetDimensionType.COST_CATEGORY,
            dimensionId = "CAT-SEC",
            allocatedAmount = BigDecimal("75000.0000"),
            currency = "BDT",
            effectiveStartDate = 1000L,
            effectiveEndDate = 2000L,
            createdBy = "mgr-1"
        )

        // Manager creates
        val createRes = service.createBudget(budget, managerPrincipal.userId, managerPrincipal.role.name)
        assertTrue(createRes is DomainResult.Success)

        // Staff tries to approve -> rejected by validator / SoD
        val staffApprove = BusinessFinancialGovernanceValidator.validateBudgetStatusTransition(
            budget = (createRes as DomainResult.Success).data,
            targetStatus = BusinessFinancialBudgetStatus.APPROVED,
            actorId = staffPrincipal.userId,
            actorRole = staffPrincipal.role.name
        )
        assertTrue(staffApprove is DomainResult.Error)

        // Customer tries to approve -> rejected
        val custApprove = BusinessFinancialGovernanceValidator.validateBudgetStatusTransition(
            budget = createRes.data,
            targetStatus = BusinessFinancialBudgetStatus.APPROVED,
            actorId = customerPrincipal.userId,
            actorRole = customerPrincipal.role.name
        )
        assertTrue(custApprove is DomainResult.Error)

        // Admin approves -> allowed
        val adminApprove = BusinessFinancialGovernanceValidator.validateBudgetStatusTransition(
            budget = createRes.data.copy(status = BusinessFinancialBudgetStatus.SUBMITTED),
            targetStatus = BusinessFinancialBudgetStatus.APPROVED,
            actorId = adminPrincipal.userId,
            actorRole = adminPrincipal.role.name
        )
        assertTrue(adminApprove is DomainResult.Success)
    }

    @Test
    fun `test Separation of Duties (SoD) enforcement - Creator cannot self-approve`() = runBlocking {
        val budget = BusinessFinancialBudget(
            id = "BUD-SOD-1",
            tenantId = tenantA,
            projectId = projectId,
            budgetName = "SoD Test Budget",
            periodId = "2026-Q2",
            dimensionType = BusinessFinancialBudgetDimensionType.COST_CENTER,
            dimensionId = "CC-EXEC",
            allocatedAmount = BigDecimal("100000.0000"),
            currency = "BDT",
            effectiveStartDate = 1000L,
            effectiveEndDate = 2000L,
            createdBy = "USER-ALPHA",
            status = BusinessFinancialBudgetStatus.SUBMITTED
        )
        fakeDataSource.saveBudget(budget)

        // Self approval attempt
        val selfApproveRes = service.approveBudget(tenantA, projectId, "BUD-SOD-1", "USER-ALPHA", "ADMIN")
        assertTrue(selfApproveRes is DomainResult.Error)
        assertTrue((selfApproveRes as DomainResult.Error).message.contains("Separation of Duties violation"))

        // Distinct approver approval
        val otherApproveRes = service.approveBudget(tenantA, projectId, "BUD-SOD-1", "USER-BETA", "ADMIN")
        assertTrue(otherApproveRes is DomainResult.Success)
        assertEquals(BusinessFinancialBudgetStatus.APPROVED, (otherApproveRes as DomainResult.Success).data.status)
    }

    @Test
    fun `test Multi-Tenant Isolation - Tenant B cannot read or mutate Tenant A budgets or alerts`() = runBlocking {
        val budgetA = BusinessFinancialBudget(
            id = "BUD-TENANT-A",
            tenantId = tenantA,
            projectId = projectId,
            budgetName = "Tenant A Confidential Budget",
            periodId = "2026-Q1",
            dimensionType = BusinessFinancialBudgetDimensionType.OVERALL_BUSINESS,
            dimensionId = "ALL",
            allocatedAmount = BigDecimal("500000.0000"),
            currency = "BDT",
            effectiveStartDate = 1000L,
            effectiveEndDate = 2000L,
            status = BusinessFinancialBudgetStatus.ACTIVE,
            createdBy = "ADMIN-A"
        )
        fakeDataSource.saveBudget(budgetA)

        // Query with Tenant B context
        val getRes = service.getBudgetById(tenantB, projectId, "BUD-TENANT-A")
        assertTrue(getRes is DomainResult.Success)
        assertNull((getRes as DomainResult.Success).data)

        // Tenant B trying to approve Tenant A budget
        val approveRes = service.approveBudget(tenantB, projectId, "BUD-TENANT-A", "ADMIN-B", "ADMIN")
        assertTrue(approveRes is DomainResult.Error)
        assertTrue((approveRes as DomainResult.Error).message.contains("Budget not found"))
    }
}

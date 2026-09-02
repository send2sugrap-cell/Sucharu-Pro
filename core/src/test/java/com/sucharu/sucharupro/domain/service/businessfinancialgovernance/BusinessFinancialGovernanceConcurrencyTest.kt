package com.sucharu.sucharupro.domain.service.businessfinancialgovernance

import com.sucharu.sucharupro.data.datasource.businessfinancialgovernance.FakeBusinessFinancialGovernanceDataSource
import com.sucharu.sucharupro.data.repository.businessfinancialgovernance.BusinessFinancialGovernanceRepositoryImpl
import com.sucharu.sucharupro.domain.model.businessfinancialgovernance.BusinessFinancialBudget
import com.sucharu.sucharupro.domain.model.businessfinancialgovernance.BusinessFinancialBudgetDimensionType
import com.sucharu.sucharupro.domain.model.businessfinancialgovernance.BusinessFinancialBudgetStatus
import com.sucharu.sucharupro.domain.repository.businessfinancialgovernance.BusinessFinancialBudgetFilter
import com.sucharu.sucharupro.domain.model.common.DomainResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessFinancialGovernanceConcurrencyTest {

    private lateinit var fakeDataSource: FakeBusinessFinancialGovernanceDataSource
    private lateinit var governanceRepository: BusinessFinancialGovernanceRepositoryImpl
    private lateinit var service: BusinessFinancialGovernanceServiceImpl

    private val tenantId = "TENANT-CONCURRENCY"
    private val projectId = "PROJ-CONCURRENCY"

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
    fun `test concurrent budget creation and revisions thread safety`() = runBlocking {
        val jobs = (1..50).map { i ->
            async {
                val budget = BusinessFinancialBudget(
                    id = "BUD-CONCUR-$i",
                    tenantId = tenantId,
                    projectId = projectId,
                    budgetName = "Concurrent Budget $i",
                    periodId = "2026-Q1",
                    dimensionType = BusinessFinancialBudgetDimensionType.COST_CENTER,
                    dimensionId = "CC-$i",
                    allocatedAmount = BigDecimal("${1000 * i}.0000"),
                    currency = "BDT",
                    effectiveStartDate = 1000L,
                    effectiveEndDate = 2000L,
                    createdBy = "USER-$i"
                )
                val createRes = service.createBudget(budget, "USER-$i", "MANAGER")
                assertTrue(createRes is DomainResult.Success)

                val submitRes = service.submitBudget(tenantId, projectId, "BUD-CONCUR-$i", "USER-$i", "MANAGER")
                assertTrue(submitRes is DomainResult.Success)

                val approveRes = service.approveBudget(tenantId, projectId, "BUD-CONCUR-$i", "ADMIN-$i", "ADMIN")
                assertTrue(approveRes is DomainResult.Success)

                val reviseRes = service.reviseBudget(
                    tenantId = tenantId,
                    projectId = projectId,
                    budgetId = "BUD-CONCUR-$i",
                    newAllocatedAmount = BigDecimal("${1500 * i}.0000"),
                    revisionReason = "Concurrent revision $i",
                    actorId = "ADMIN-$i",
                    actorRole = "ADMIN"
                )
                assertTrue(reviseRes is DomainResult.Success)
            }
        }

        jobs.awaitAll()

        val allBudgets = fakeDataSource.listBudgets(tenantId, projectId, BusinessFinancialBudgetFilter())
        assertEquals(50, allBudgets.size)
        for (b in allBudgets) {
            assertEquals(2L, b.version)
        }
    }
}

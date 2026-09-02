package com.sucharu.sucharupro.businessreconciliation

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.businessreconciliation.FakeBusinessFinancialReconciliationDataSource
import com.sucharu.sucharupro.data.repository.businessreconciliation.BusinessFinancialReconciliationRepositoryImpl
import com.sucharu.sucharupro.domain.model.businessreconciliation.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.businessreconciliation.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessFinancialReconciliationPeriodCloseTest {

    private lateinit var dataSource: FakeBusinessFinancialReconciliationDataSource
    private lateinit var repository: BusinessFinancialReconciliationRepositoryImpl
    private lateinit var service: BusinessFinancialReconciliationServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val staff = AuthenticatedPrincipal(userId = "USR-STAFF", username = "staff_user", role = UserRole.STAFF, projectId = projectId)
    private val manager = AuthenticatedPrincipal(userId = "USR-MGR", username = "mgr_user", role = UserRole.MANAGER, projectId = projectId)

    @Before
    fun setup() {
        dataSource = FakeBusinessFinancialReconciliationDataSource()
        repository = BusinessFinancialReconciliationRepositoryImpl(dataSource)
        service = BusinessFinancialReconciliationServiceImpl(
            repository = repository,
            defaultTenantId = tenantId
        )
    }

    @Test
    fun testPeriodCloseReadinessGateBlocksWhenUnapprovedRunsOrCriticalDiscrepanciesExist() = runBlocking {
        // 1. Initially without runs -> Not ready
        val initialReadiness = service.getPeriodCloseReadiness(manager, "PER-2026-08")
        assertTrue(initialReadiness is DomainResult.Success)
        val r1 = (initialReadiness as DomainResult.Success).data
        assertFalse(r1.isReady)
        assertTrue(r1.blockingIssues.any { it.contains("No financial reconciliation run") })

        // 2. Create Run and add Critical Discrepancy
        val run = repository.createRun(
            BusinessFinancialReconciliationRun(
                id = "RUN-GATE-01",
                tenantId = tenantId,
                projectId = projectId,
                periodId = "PER-2026-08",
                runNumber = "RUN-GATE-01",
                status = ReconciliationRunStatus.COMPLETED,
                createdBy = "USR-STAFF",
                checksum = "chk"
            )
        )

        val disc = repository.createDiscrepancy(
            BusinessFinancialReconciliationDiscrepancy(
                id = "DISC-CRIT-01",
                tenantId = tenantId,
                projectId = projectId,
                reconciliationRunId = run.id,
                periodId = "PER-2026-08",
                discrepancyType = FinancialDiscrepancyType.MISSING_ACCRUAL_REVERSAL,
                severity = DiscrepancySeverity.CRITICAL,
                sourceType = "ACCRUAL",
                sourceId = "ACC-01",
                expectedAmount = BigDecimal("50000.0000"),
                actualAmount = BigDecimal("0.0000"),
                differenceAmount = BigDecimal("50000.0000"),
                description = "Critical accrual reversal missing"
            )
        )

        val unapprovedReadiness = service.getPeriodCloseReadiness(manager, "PER-2026-08")
        val r2 = (unapprovedReadiness as DomainResult.Success).data
        assertFalse(r2.isReady)
        assertEquals(1, r2.unresolvedCriticalCount)
        assertFalse(r2.allRequiredRunsApproved)

        // 3. Resolve Critical Discrepancy
        service.resolveDiscrepancy(
            staff,
            ResolveDiscrepancyCommand(discrepancyId = disc.id, resolutionNote = "Accrual reversed via BCA-01")
        )

        // 4. Approve Run
        service.approveReconciliationRun(
            manager,
            ApproveReconciliationCommand(runId = run.id, notes = "Verified and approved")
        )

        // 5. Now Period Close Readiness evaluates to TRUE
        val finalReadiness = service.getPeriodCloseReadiness(manager, "PER-2026-08")
        val r3 = (finalReadiness as DomainResult.Success).data
        assertTrue(r3.isReady)
        assertEquals(0, r3.unresolvedCriticalCount)
        assertTrue(r3.allRequiredRunsApproved)
        assertTrue(r3.blockingIssues.isEmpty())
    }
}

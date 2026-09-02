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

class BusinessFinancialReconciliationServiceTest {

    private lateinit var dataSource: FakeBusinessFinancialReconciliationDataSource
    private lateinit var repository: BusinessFinancialReconciliationRepositoryImpl
    private lateinit var service: BusinessFinancialReconciliationServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"

    private val admin = AuthenticatedPrincipal(userId = "USR-ADMIN", username = "admin_user", role = UserRole.ADMIN, projectId = projectId)
    private val manager = AuthenticatedPrincipal(userId = "USR-MGR", username = "mgr_user", role = UserRole.MANAGER, projectId = projectId)
    private val staff = AuthenticatedPrincipal(userId = "USR-STAFF", username = "staff_user", role = UserRole.STAFF, projectId = projectId)

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
    fun testReconciliationRunLifecycle() = runBlocking {
        // 1. Create Run by Staff
        val createCmd = CreateReconciliationRunCommand(
            periodId = "PER-2026-08",
            runNumber = "RUN-2026-001",
            runType = ReconciliationRunType.FULL_PERIOD,
            notes = "August 2026 Month-End Reconciliation"
        )
        val createRes = service.createReconciliationRun(staff, createCmd)
        assertTrue(createRes is DomainResult.Success)
        val run = (createRes as DomainResult.Success).data
        assertEquals(ReconciliationRunStatus.CREATED, run.status)

        // 2. Execute Run
        val execRes = service.executeReconciliationRun(staff, run.id)
        assertTrue(execRes is DomainResult.Success)
        val executedRun = (execRes as DomainResult.Success).data
        assertEquals(ReconciliationRunStatus.COMPLETED, executedRun.status)

        // 3. Approve Run by Manager (SoD: creator is staff, approver is manager)
        val appCmd = ApproveReconciliationCommand(runId = run.id, notes = "Verified clean reconciliation")
        val appRes = service.approveReconciliationRun(manager, appCmd)
        assertTrue(appRes is DomainResult.Success)
        val approvedRun = (appRes as DomainResult.Success).data
        assertEquals(ReconciliationRunStatus.APPROVED, approvedRun.status)
        assertEquals("USR-MGR", approvedRun.approvedBy)
    }

    @Test
    fun testDiscrepancyAssignmentAndResolution() = runBlocking {
        val run = repository.createRun(
            BusinessFinancialReconciliationRun(
                id = "RUN-200",
                tenantId = tenantId,
                projectId = projectId,
                periodId = "PER-2026-08",
                runNumber = "RUN-200",
                createdBy = "USR-STAFF",
                checksum = "checksum"
            )
        )

        val disc = repository.createDiscrepancy(
            BusinessFinancialReconciliationDiscrepancy(
                id = "DISC-200",
                tenantId = tenantId,
                projectId = projectId,
                reconciliationRunId = run.id,
                periodId = "PER-2026-08",
                discrepancyType = FinancialDiscrepancyType.AMOUNT_MISMATCH,
                severity = DiscrepancySeverity.CRITICAL,
                sourceType = "EXPENSE",
                sourceId = "EXP-101",
                description = "Mismatch in ledger debit"
            )
        )

        // Assign
        val assignRes = service.assignDiscrepancy(staff, AssignDiscrepancyCommand(discrepancyId = disc.id, assignedTo = "USR-STAFF-02"))
        assertTrue(assignRes is DomainResult.Success)
        assertEquals(DiscrepancyStatus.INVESTIGATING, (assignRes as DomainResult.Success).data.status)
        assertEquals("USR-STAFF-02", (assignRes as DomainResult.Success).data.assignedTo)

        // Resolve
        val resolveRes = service.resolveDiscrepancy(
            staff,
            ResolveDiscrepancyCommand(discrepancyId = disc.id, resolutionNote = "Adjusted via canonical posting BLP-101")
        )
        assertTrue(resolveRes is DomainResult.Success)
        assertEquals(DiscrepancyStatus.RESOLVED, (resolveRes as DomainResult.Success).data.status)
    }

    @Test
    fun testDiscrepancyWaiverRequiresPrivilegeAndJustification() = runBlocking {
        val run = repository.createRun(
            BusinessFinancialReconciliationRun(
                id = "RUN-201",
                tenantId = tenantId,
                projectId = projectId,
                periodId = "PER-2026-08",
                runNumber = "RUN-201",
                createdBy = "USR-STAFF",
                checksum = "checksum"
            )
        )

        val disc = repository.createDiscrepancy(
            BusinessFinancialReconciliationDiscrepancy(
                id = "DISC-201",
                tenantId = tenantId,
                projectId = projectId,
                reconciliationRunId = run.id,
                periodId = "PER-2026-08",
                discrepancyType = FinancialDiscrepancyType.AMOUNT_MISMATCH,
                severity = DiscrepancySeverity.WARNING,
                sourceType = "EXPENSE",
                sourceId = "EXP-102",
                description = "Immaterial fractional cent variance"
            )
        )

        // Staff cannot waive
        val staffWaive = service.waiveDiscrepancy(
            staff,
            WaiveDiscrepancyCommand(discrepancyId = disc.id, waiverReason = "Fractional rounding variance approved by finance")
        )
        assertTrue(staffWaive is DomainResult.Error)

        // Short justification fails
        val shortJustify = service.waiveDiscrepancy(
            manager,
            WaiveDiscrepancyCommand(discrepancyId = disc.id, waiverReason = "Too short")
        )
        assertTrue(shortJustify is DomainResult.Error)

        // Manager with full justification succeeds
        val validWaive = service.waiveDiscrepancy(
            manager,
            WaiveDiscrepancyCommand(discrepancyId = disc.id, waiverReason = "Fractional rounding variance approved by finance governance")
        )
        assertTrue(validWaive is DomainResult.Success)
        assertEquals(DiscrepancyStatus.WAIVED, (validWaive as DomainResult.Success).data.status)
    }
}

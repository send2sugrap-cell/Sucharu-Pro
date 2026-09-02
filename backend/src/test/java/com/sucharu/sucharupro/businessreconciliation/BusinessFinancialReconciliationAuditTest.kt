package com.sucharu.sucharupro.businessreconciliation

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.businessreconciliation.FakeBusinessFinancialReconciliationDataSource
import com.sucharu.sucharupro.data.repository.businessreconciliation.BusinessFinancialReconciliationRepositoryImpl
import com.sucharu.sucharupro.domain.model.businessreconciliation.FinancialDiscrepancyType
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.businessreconciliation.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessFinancialReconciliationAuditTest {

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
    fun testReconciliationAuditLogIsAppendOnlyAndTracksActorEvents() = runBlocking {
        // 1. Create Run
        val createRes = service.createReconciliationRun(
            staff,
            CreateReconciliationRunCommand(periodId = "PER-2026-08", runNumber = "RUN-AUDIT-01")
        )
        val runId = (createRes as DomainResult.Success).data.id

        // 2. Execute Run
        service.executeReconciliationRun(staff, runId)

        // 3. Approve Run
        service.approveReconciliationRun(manager, ApproveReconciliationCommand(runId = runId, notes = "Audited and approved"))

        // Fetch Audit Trail
        val auditRes = service.listAuditEvents(manager, runId = runId)
        assertTrue(auditRes is DomainResult.Success)
        val events = (auditRes as DomainResult.Success).data

        assertEquals(3, events.size)
        assertTrue(events.any { it.eventType == "RUN_CREATED" && it.actorId == "USR-STAFF" })
        assertTrue(events.any { it.eventType == "RUN_COMPLETED" && it.actorId == "USR-STAFF" })
        assertTrue(events.any { it.eventType == "RECONCILIATION_APPROVED" && it.actorId == "USR-MGR" })
    }
}

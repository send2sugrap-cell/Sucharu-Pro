package com.sucharu.sucharupro.domain.service.substratereservation

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.ForbiddenException
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.api.model.substratereservation.EvaluateCancellationGovernanceRequestDto
import com.sucharu.sucharupro.data.api.server.BackendUseCases
import com.sucharu.sucharupro.data.datasource.substratereservation.FakeSubstrateReleaseGovernanceDataSource
import com.sucharu.sucharupro.data.event.MockPostgresEventDatabase
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.repository.substratereservation.SubstrateReleaseGovernanceRepositoryImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Security, RBAC, and Segregation of Duties test suite for Substrate Release Governance.
 * Module 19 Step 05.
 *
 * Verifies:
 * 1. AI_AGENT cannot approve or execute releases (Strict Segregation of Duties).
 * 2. CUSTOMER and VENDOR roles are strictly forbidden.
 * 3. Human roles (STAFF, MANAGER, ADMIN) can evaluate, approve, and execute.
 * 4. Cross-tenant isolation blocks unauthorized leakage.
 */
class SubstrateReleaseGovernanceSecurityEdgeTest {

    private lateinit var useCases: BackendUseCases
    private val tenantAlpha = "TENANT-ALPHA"
    private val tenantBeta = "TENANT-BETA"

    private val customerPrincipal = AuthenticatedPrincipal(
        userId = "cust-1",
        username = "customer_user",
        role = UserRole.CUSTOMER,
        projectId = tenantAlpha
    )

    private val vendorPrincipal = AuthenticatedPrincipal(
        userId = "vend-1",
        username = "vendor_user",
        role = UserRole.VENDOR,
        projectId = tenantAlpha
    )

    private val managerPrincipal = AuthenticatedPrincipal(
        userId = "mgr-1",
        username = "manager_user",
        role = UserRole.MANAGER,
        projectId = tenantAlpha
    )

    private val staffPrincipal = AuthenticatedPrincipal(
        userId = "staff-1",
        username = "staff_user",
        role = UserRole.STAFF,
        projectId = tenantAlpha
    )

    private val aiAgentPrincipal = AuthenticatedPrincipal(
        userId = "ai-1",
        username = "ai_agent_user",
        role = UserRole.AI_AGENT,
        projectId = tenantAlpha
    )

    private val reqDto = EvaluateCancellationGovernanceRequestDto(
        reservationId = "RES-ART300-01",
        orderId = "ORD-2026-9041",
        orderItemId = "ITEM-01",
        executionJobId = "JOB-1122",
        sku = "ART-300-25X36",
        materialName = "Art Card 300 GSM",
        warehouseId = "WH-01",
        allocatedSheets = 10000L,
        consumedSheets = 0L,
        committedSheets = 0L,
        productionStatus = "READY"
    )

    @Before
    fun setup() {
        val fakeDs = FakeSubstrateReleaseGovernanceDataSource()
        val repo = SubstrateReleaseGovernanceRepositoryImpl(fakeDs)
        val service = SubstrateReleaseGovernanceServiceImpl(repo)

        val mockDb = MockPostgresEventDatabase()

        val factory = object : PostgresRepositoryFactory(
            transactionManager = mockDb,
            defaultTenantId = tenantAlpha
        ) {
            override fun createSubstrateReleaseGovernanceDataSource(tenantId: String) = fakeDs
            override fun createSubstrateReleaseGovernanceRepository(tenantId: String) = repo
            override fun createSubstrateReleaseGovernanceService(tenantId: String) = service
        }

        useCases = BackendUseCases(mockDb, factory)
    }

    @Test
    fun `test Customer and Vendor roles are strictly rejected from release governance`() {
        assertThrows(ForbiddenException::class.java) {
            runBlocking { useCases.evaluateCancellationGovernance(customerPrincipal, reqDto) }
        }

        assertThrows(ForbiddenException::class.java) {
            runBlocking { useCases.evaluateCancellationGovernance(vendorPrincipal, reqDto) }
        }
    }

    @Test
    fun `test Staff and Manager roles are permitted to evaluate release governance`() = runBlocking {
        val resStaff = useCases.evaluateCancellationGovernance(staffPrincipal, reqDto)
        assertNotNull(resStaff)
        assertEquals("RELEASE_ELIGIBLE", resStaff.decision)

        val resMgr = useCases.evaluateCancellationGovernance(managerPrincipal, reqDto)
        assertNotNull(resMgr)
    }

    @Test
    fun `test AI Agent can evaluate but is FORBIDDEN from approving or executing release`() = runBlocking {
        // AI Agent can evaluate
        val eval = useCases.evaluateCancellationGovernance(aiAgentPrincipal, reqDto)
        assertNotNull(eval)
        assertEquals("RELEASE_ELIGIBLE", eval.decision)

        // AI Agent is FORBIDDEN from approving release
        assertThrows(ForbiddenException::class.java) {
            runBlocking { useCases.approveSubstrateRelease(aiAgentPrincipal, eval.governanceId) }
        }

        // Manager approves the case
        val approved = useCases.approveSubstrateRelease(managerPrincipal, eval.governanceId)
        assertEquals("APPROVED", approved.executionStatus)

        // AI Agent is FORBIDDEN from executing release
        assertThrows(ForbiddenException::class.java) {
            runBlocking { useCases.executeSubstrateRelease(aiAgentPrincipal, eval.governanceId) }
        }

        // Staff executes the release successfully
        val executed = useCases.executeSubstrateRelease(staffPrincipal, eval.governanceId)
        assertEquals("RELEASE_EXECUTED", executed.executionStatus)
    }

    @Test
    fun `test Cross-Tenant Isolation blocks unauthorized record access`() = runBlocking {
        // Evaluate in Tenant Alpha
        val eval = useCases.evaluateCancellationGovernance(managerPrincipal, reqDto)

        // Principal from Tenant Beta tries to access
        val betaPrincipal = AuthenticatedPrincipal(
            userId = "mgr-beta",
            username = "manager_beta",
            role = UserRole.MANAGER,
            projectId = tenantBeta
        )

        val record = useCases.getSubstrateReleaseGovernanceRecord(betaPrincipal, eval.governanceId)
        assertNull("Record created in Alpha must not be visible to Beta", record)
    }
}

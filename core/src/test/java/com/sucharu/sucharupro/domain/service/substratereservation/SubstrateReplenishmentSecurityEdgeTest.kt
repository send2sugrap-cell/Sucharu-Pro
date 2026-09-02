package com.sucharu.sucharupro.domain.service.substratereservation

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.ForbiddenException
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.api.model.substratereservation.EvaluateSubstrateReplenishmentRequestDto
import com.sucharu.sucharupro.data.api.server.BackendUseCases
import com.sucharu.sucharupro.data.datasource.substratereservation.FakeSubstrateReplenishmentDataSource
import com.sucharu.sucharupro.data.event.MockPostgresEventDatabase
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.repository.substratereservation.SubstrateReplenishmentRepositoryImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class SubstrateReplenishmentSecurityEdgeTest {

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

    private val reqDto = EvaluateSubstrateReplenishmentRequestDto(
        productId = "PROD-01",
        sku = "ART-300-25X36",
        materialName = "Art Card 300 GSM",
        stockType = "ART_CARD",
        gsm = BigDecimal("300.0000"),
        sheetWidthMm = BigDecimal("635.0000"),
        sheetHeightMm = BigDecimal("914.4000"),
        warehouseId = "WH-01",
        warehouseName = "Main Warehouse",
        onHandPhysicalSheets = 8000L,
        activeReservedSheets = 3000L,
        minimumStockSheets = 2000L,
        safetyStockSheets = 4000L,
        reorderPointSheets = 10000L,
        targetStockSheets = 30000L,
        minimumOrderQuantitySheets = 5000L
    )

    @Before
    fun setup() {
        val fakeDs = FakeSubstrateReplenishmentDataSource()
        val repo = SubstrateReplenishmentRepositoryImpl(fakeDs)
        val service = SubstrateReplenishmentServiceImpl(repo)

        val mockDb = MockPostgresEventDatabase()

        val factory = object : PostgresRepositoryFactory(
            transactionManager = mockDb,
            defaultTenantId = tenantAlpha
        ) {
            override fun createSubstrateReplenishmentDataSource(tenantId: String) = fakeDs
            override fun createSubstrateReplenishmentRepository(tenantId: String) = repo
            override fun createSubstrateReplenishmentService(tenantId: String) = service
        }

        useCases = BackendUseCases(mockDb, factory)
    }

    @Test
    fun `test Customer and Vendor roles are strictly rejected from evaluating substrate replenishment`() {
        // Customer rejected (403 ForbiddenException)
        assertThrows(ForbiddenException::class.java) {
            runBlocking {
                useCases.evaluateSubstrateReplenishment(customerPrincipal, reqDto)
            }
        }

        // Vendor rejected (403 ForbiddenException)
        assertThrows(ForbiddenException::class.java) {
            runBlocking {
                useCases.evaluateSubstrateReplenishment(vendorPrincipal, reqDto)
            }
        }
    }

    @Test
    fun `test Manager and Staff roles are authorized to evaluate substrate replenishment`() = runBlocking {
        val resMgr = useCases.evaluateSubstrateReplenishment(managerPrincipal, reqDto)
        assertNotNull(resMgr)
        assertEquals("ART-300-25X36", resMgr.sku)

        val resStaff = useCases.evaluateSubstrateReplenishment(staffPrincipal, reqDto)
        assertNotNull(resStaff)
        assertEquals("ART-300-25X36", resStaff.sku)
    }

    @Test
    fun `test AI Agent can evaluate but is strictly forbidden from dispatching supplier alerts or updating status`() {
        runBlocking {
            // AI Agent can evaluate
            val eval = useCases.evaluateSubstrateReplenishment(aiAgentPrincipal, reqDto)
            assertNotNull(eval)

            // AI Agent forbidden from triggering supplier alerts (requires human manager/staff)
            assertThrows(ForbiddenException::class.java) {
                runBlocking {
                    useCases.triggerSupplierReorderAlert(aiAgentPrincipal, eval.evaluationId)
                }
            }

            // AI Agent forbidden from updating replenishment status
            assertThrows(ForbiddenException::class.java) {
                runBlocking {
                    useCases.updateSubstrateReplenishmentStatus(aiAgentPrincipal, eval.evaluationId, "CANCELLED", "Reason")
                }
            }
        }
    }

    @Test
    fun `test Tenant Alpha cannot access evaluations in Tenant Beta`() {
        runBlocking {
            val eval = useCases.evaluateSubstrateReplenishment(managerPrincipal, reqDto)

            val tenantBetaPrincipal = AuthenticatedPrincipal(
                userId = "mgr-beta",
                username = "beta_mgr",
                role = UserRole.MANAGER,
                projectId = tenantBeta
            )

            val evalFromBeta = useCases.getSubstrateReplenishmentEvaluation(tenantBetaPrincipal, eval.evaluationId)
            assertNull(evalFromBeta)
        }
    }
}

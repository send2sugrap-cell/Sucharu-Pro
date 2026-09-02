package com.sucharu.sucharupro.businesscostcontrol

import com.sucharu.sucharupro.backend.integration.MockIntegrationDb
import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.model.businesscostcontrol.*
import com.sucharu.sucharupro.data.api.server.BackendRouter
import com.sucharu.sucharupro.data.api.server.BackendSecurityContext
import com.sucharu.sucharupro.data.api.server.BackendUseCases
import com.sucharu.sucharupro.data.api.server.HttpRequest
import com.sucharu.sucharupro.data.auth.security.AuthConfig
import com.sucharu.sucharupro.data.auth.security.JwtTokenProvider
import com.sucharu.sucharupro.data.datasource.businesscostcontrol.FakeBusinessCostControlDataSource
import com.sucharu.sucharupro.data.datasource.businessledger.FakeBusinessLedgerDataSource
import com.sucharu.sucharupro.data.persistence.postgres.DatabaseHealthChecker
import com.sucharu.sucharupro.data.persistence.postgres.DefaultPostgresTransactionManager
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.repository.businesscostcontrol.BusinessCostControlRepositoryImpl
import com.sucharu.sucharupro.data.repository.businessledger.BusinessLedgerRepositoryImpl
import com.sucharu.sucharupro.domain.service.businesscostcontrol.BusinessCostControlServiceImpl
import com.sucharu.sucharupro.domain.service.businessledger.BusinessLedgerServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class BusinessCostControlApiTest {

    private lateinit var router: BackendRouter
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var adminToken: String
    private lateinit var customerToken: String
    private lateinit var costControlRepo: BusinessCostControlRepositoryImpl

    private val projectId = "PRJ-API-01"
    private val tenantId = "TENANT-001"

    @Before
    fun setup() {
        runBlocking {
            val costDs = FakeBusinessCostControlDataSource()
            costControlRepo = BusinessCostControlRepositoryImpl(costDs)
            val ledgerDs = FakeBusinessLedgerDataSource()
            val ledgerRepo = BusinessLedgerRepositoryImpl(ledgerDs)
            val ledgerService = BusinessLedgerServiceImpl(ledgerRepo, defaultTenantId = tenantId)

            val costControlService = BusinessCostControlServiceImpl(
                repository = costControlRepo,
                ledgerService = ledgerService,
                defaultTenantId = tenantId
            )

            val mockDb = MockIntegrationDb()
            val txManager = DefaultPostgresTransactionManager(mockDb)

            val customFactory = object : PostgresRepositoryFactory(txManager) {
                override fun createBusinessCostControlRepository(tenantId: String) = costControlRepo
                override fun createBusinessCostControlService(tenantId: String) = costControlService
                override fun createBusinessLedgerRepository(tenantId: String) = ledgerRepo
                override fun createBusinessLedgerService(tenantId: String) = ledgerService
            }

            val useCases = BackendUseCases(txManager, customFactory)

            val authConfig = AuthConfig(
                jwtSigningSecret = "test_signing_secret_for_cost_control_api_test_2026",
                jwtIssuer = "sucharu-test",
                jwtAudience = "sucharu-api"
            )
            jwtTokenProvider = JwtTokenProvider(authConfig)

            adminToken = jwtTokenProvider.generateAccessToken(
                AuthenticatedPrincipal(
                    userId = "ADMIN-API",
                    projectId = projectId,
                    username = "admin_api",
                    role = UserRole.ADMIN,
                    permissions = emptySet()
                )
            )

            customerToken = jwtTokenProvider.generateAccessToken(
                AuthenticatedPrincipal(
                    userId = "CUST-API",
                    projectId = projectId,
                    username = "cust_api",
                    role = UserRole.CUSTOMER,
                    permissions = emptySet()
                )
            )

            val securityContext = BackendSecurityContext(jwtTokenProvider)
            val healthChecker = DatabaseHealthChecker(mockDb)
            router = BackendRouter(securityContext, useCases, healthChecker)
        }
    }

    @Test
    fun testFinancialPeriodRestEndpoints() = runBlocking {
        // 1. Create Period
        val createReq = HttpRequest(
            method = "POST",
            path = "/api/v1/business-financial-periods",
            headers = mapOf("Authorization" to "Bearer $adminToken"),
            body = CreateFinancialPeriodRequest(
                periodCode = "2026-08",
                periodName = "August 2026",
                startDate = 1754092800000L,
                endDate = 1756684799000L
            )
        )
        val createResp = router.handleRequest(createReq)
        assertEquals(201, createResp.statusCode)
        val pData = (createResp.body as ApiSuccessResponse<*>).data as BusinessFinancialPeriodResponse
        assertEquals("2026-08", pData.periodCode)

        // 2. List Periods
        val listReq = HttpRequest(
            method = "GET",
            path = "/api/v1/business-financial-periods",
            headers = mapOf("Authorization" to "Bearer $adminToken")
        )
        val listResp = router.handleRequest(listReq)
        assertEquals(200, listResp.statusCode)
        val periods = (listResp.body as ApiSuccessResponse<*>).data as List<*>
        assertEquals(1, periods.size)

        // 3. Get Period By ID
        val getReq = HttpRequest(
            method = "GET",
            path = "/api/v1/business-financial-periods/${pData.id}",
            headers = mapOf("Authorization" to "Bearer $adminToken")
        )
        val getResp = router.handleRequest(getReq)
        assertEquals(200, getResp.statusCode)
    }

    @Test
    fun testCostCommitmentRestEndpoints() = runBlocking {
        // 1. Create Commitment
        val createReq = HttpRequest(
            method = "POST",
            path = "/api/v1/business-cost-commitments",
            headers = mapOf("Authorization" to "Bearer $adminToken"),
            body = CreateCostCommitmentRequest(
                costCategoryId = "CAT-PAPER",
                description = "Paper PO via REST API",
                committedAmount = "60000.0000"
            )
        )
        val createResp = router.handleRequest(createReq)
        assertEquals(201, createResp.statusCode)
        val cData = (createResp.body as ApiSuccessResponse<*>).data as BusinessCostCommitmentResponse
        assertEquals("DRAFT", cData.status)

        // 2. Submit
        val submitReq = HttpRequest(
            method = "POST",
            path = "/api/v1/business-cost-commitments/${cData.id}/submit",
            headers = mapOf("Authorization" to "Bearer $adminToken")
        )
        val subResp = router.handleRequest(submitReq)
        assertEquals(200, subResp.statusCode)

        // 3. Approve
        val approveReq = HttpRequest(
            method = "POST",
            path = "/api/v1/business-cost-commitments/${cData.id}/approve",
            headers = mapOf("Authorization" to "Bearer $adminToken")
        )
        val appResp = router.handleRequest(approveReq)
        assertEquals(200, appResp.statusCode)

        // 4. Activate
        val actReq = HttpRequest(
            method = "POST",
            path = "/api/v1/business-cost-commitments/${cData.id}/activate",
            headers = mapOf("Authorization" to "Bearer $adminToken")
        )
        val actResp = router.handleRequest(actReq)
        assertEquals(200, actResp.statusCode)

        // 5. Consume
        val conReq = HttpRequest(
            method = "POST",
            path = "/api/v1/business-cost-commitments/${cData.id}/consume",
            headers = mapOf("Authorization" to "Bearer $adminToken"),
            body = ConsumeCostCommitmentRequest(
                amount = "25000.0000",
                sourceId = "INV-REST-01"
            )
        )
        val conResp = router.handleRequest(conReq)
        assertEquals(200, conResp.statusCode)

        // 6. List Consumptions
        val listConReq = HttpRequest(
            method = "GET",
            path = "/api/v1/business-cost-commitments/${cData.id}/consumptions",
            headers = mapOf("Authorization" to "Bearer $adminToken")
        )
        val listConResp = router.handleRequest(listConReq)
        assertEquals(200, listConResp.statusCode)
        val conList = (listConResp.body as ApiSuccessResponse<*>).data as List<*>
        assertEquals(1, conList.size)
    }

    @Test
    fun testCostControlDashboardAndReconciliationEndpoints() = runBlocking {
        val dashReq = HttpRequest(
            method = "GET",
            path = "/api/v1/business-cost-controls/dashboard",
            headers = mapOf("Authorization" to "Bearer $adminToken")
        )
        val dashResp = router.handleRequest(dashReq)
        assertEquals(200, dashResp.statusCode)

        val reconReq = HttpRequest(
            method = "GET",
            path = "/api/v1/business-cost-controls/reconciliation",
            headers = mapOf("Authorization" to "Bearer $adminToken")
        )
        val reconResp = router.handleRequest(reconReq)
        assertEquals(200, reconResp.statusCode)
    }
}

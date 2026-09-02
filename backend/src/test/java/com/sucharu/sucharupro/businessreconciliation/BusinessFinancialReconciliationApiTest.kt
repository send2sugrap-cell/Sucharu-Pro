package com.sucharu.sucharupro.businessreconciliation

import com.sucharu.sucharupro.backend.integration.MockIntegrationDb
import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.model.businessreconciliation.*
import com.sucharu.sucharupro.data.api.server.BackendRouter
import com.sucharu.sucharupro.data.api.server.BackendSecurityContext
import com.sucharu.sucharupro.data.api.server.BackendUseCases
import com.sucharu.sucharupro.data.api.server.HttpRequest
import com.sucharu.sucharupro.data.auth.security.AuthConfig
import com.sucharu.sucharupro.data.auth.security.JwtTokenProvider
import com.sucharu.sucharupro.data.datasource.businessreconciliation.FakeBusinessFinancialReconciliationDataSource
import com.sucharu.sucharupro.data.persistence.postgres.DatabaseHealthChecker
import com.sucharu.sucharupro.data.persistence.postgres.DefaultPostgresTransactionManager
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.repository.businessreconciliation.BusinessFinancialReconciliationRepositoryImpl
import com.sucharu.sucharupro.domain.model.businessreconciliation.*
import com.sucharu.sucharupro.domain.service.businessreconciliation.BusinessFinancialReconciliationServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessFinancialReconciliationApiTest {

    private lateinit var router: BackendRouter
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var adminToken: String
    private lateinit var staffToken: String
    private lateinit var customerToken: String
    private lateinit var reconRepo: BusinessFinancialReconciliationRepositoryImpl
    private lateinit var reconService: BusinessFinancialReconciliationServiceImpl

    private val projectId = "PRJ-API-01"
    private val tenantId = "TENANT-001"

    @Before
    fun setup() {
        runBlocking {
            val reconDs = FakeBusinessFinancialReconciliationDataSource()
            reconRepo = BusinessFinancialReconciliationRepositoryImpl(reconDs)

            reconService = BusinessFinancialReconciliationServiceImpl(
                repository = reconRepo,
                defaultTenantId = tenantId
            )

            val mockDb = MockIntegrationDb()
            val txManager = DefaultPostgresTransactionManager(mockDb)

            val customFactory = object : PostgresRepositoryFactory(txManager) {
                override fun createBusinessFinancialReconciliationRepository(tenantId: String) = reconRepo
                override fun createBusinessFinancialReconciliationService(tenantId: String) = reconService
            }

            val useCases = BackendUseCases(txManager, customFactory)
            val authConfig = AuthConfig(
                jwtSigningSecret = "test_signing_secret_for_reconciliation_api_test_2026",
                jwtIssuer = "sucharu-test",
                jwtAudience = "sucharu-api"
            )
            jwtTokenProvider = JwtTokenProvider(authConfig)
            val secContext = BackendSecurityContext(jwtTokenProvider)
            val healthChecker = DatabaseHealthChecker(mockDb)

            router = BackendRouter(secContext, useCases, healthChecker)

            adminToken = "Bearer " + jwtTokenProvider.generateAccessToken(
                AuthenticatedPrincipal(
                    userId = "USR-ADMIN",
                    username = "admin_user",
                    role = UserRole.ADMIN,
                    projectId = projectId
                )
            )

            staffToken = "Bearer " + jwtTokenProvider.generateAccessToken(
                AuthenticatedPrincipal(
                    userId = "USR-STAFF",
                    username = "staff_user",
                    role = UserRole.STAFF,
                    projectId = projectId
                )
            )

            customerToken = "Bearer " + jwtTokenProvider.generateAccessToken(
                AuthenticatedPrincipal(
                    userId = "USR-CUST",
                    username = "cust_user",
                    role = UserRole.CUSTOMER,
                    projectId = projectId
                )
            )
        }
    }

    @Test
    fun testReconciliationRunEndpoints() = runBlocking {
        // 1. POST /api/v1/business-financial-reconciliation/runs (Staff creates run)
        val createReq = HttpRequest(
            method = "POST",
            path = "/api/v1/business-financial-reconciliation/runs",
            headers = mapOf("Authorization" to staffToken),
            body = CreateReconciliationRunRequest(
                periodId = "PER-2026-08",
                runNumber = "RUN-API-01",
                runType = "FULL_PERIOD",
                notes = "API Test Run"
            )
        )
        val createRes = router.handleRequest(createReq)
        assertEquals(201, createRes.statusCode)
        val createdRun = (createRes.body as ApiSuccessResponse<BusinessFinancialReconciliationRunResponse>).data
        assertEquals("RUN-API-01", createdRun.runNumber)

        // 2. POST /api/v1/business-financial-reconciliation/runs/{id}/execute
        val execReq = HttpRequest(
            method = "POST",
            path = "/api/v1/business-financial-reconciliation/runs/${createdRun.id}/execute",
            headers = mapOf("Authorization" to staffToken)
        )
        val execRes = router.handleRequest(execReq)
        assertEquals(200, execRes.statusCode)
        val executedRun = (execRes.body as ApiSuccessResponse<BusinessFinancialReconciliationRunResponse>).data
        assertEquals("COMPLETED", executedRun.status)

        // 3. GET /api/v1/business-financial-reconciliation/runs/{id}
        val getReq = HttpRequest(
            method = "GET",
            path = "/api/v1/business-financial-reconciliation/runs/${createdRun.id}",
            headers = mapOf("Authorization" to staffToken)
        )
        val getRes = router.handleRequest(getReq)
        assertEquals(200, getRes.statusCode)

        // 4. POST /api/v1/business-financial-reconciliation/runs/{id}/approve (Admin approves)
        val appReq = HttpRequest(
            method = "POST",
            path = "/api/v1/business-financial-reconciliation/runs/${createdRun.id}/approve",
            headers = mapOf("Authorization" to adminToken),
            body = ApproveReconciliationRequest(notes = "Admin approved")
        )
        val appRes = router.handleRequest(appReq)
        assertEquals(200, appRes.statusCode)
        val approvedRun = (appRes.body as ApiSuccessResponse<BusinessFinancialReconciliationRunResponse>).data
        assertEquals("APPROVED", approvedRun.status)

        // 5. GET /api/v1/business-financial-reconciliation/runs
        val listReq = HttpRequest(
            method = "GET",
            path = "/api/v1/business-financial-reconciliation/runs?periodId=PER-2026-08",
            headers = mapOf("Authorization" to staffToken)
        )
        val listRes = router.handleRequest(listReq)
        assertEquals(200, listRes.statusCode)
        val list = (listRes.body as ApiSuccessResponse<List<BusinessFinancialReconciliationRunResponse>>).data
        assertEquals(1, list.size)
    }

    @Test
    fun testDashboardAndPeriodReadinessEndpoints() = runBlocking {
        // Dashboard
        val dashReq = HttpRequest(
            method = "GET",
            path = "/api/v1/business-financial-reconciliation/dashboard",
            headers = mapOf("Authorization" to staffToken)
        )
        val dashRes = router.handleRequest(dashReq)
        assertEquals(200, dashRes.statusCode)

        // Period Readiness
        val readReq = HttpRequest(
            method = "GET",
            path = "/api/v1/business-financial-reconciliation/period-readiness?periodId=PER-2026-08",
            headers = mapOf("Authorization" to staffToken)
        )
        val readRes = router.handleRequest(readReq)
        assertEquals(200, readRes.statusCode)
    }

    @Test
    fun testCustomerRoleIsForbidden() = runBlocking {
        val req = HttpRequest(
            method = "GET",
            path = "/api/v1/business-financial-reconciliation/runs",
            headers = mapOf("Authorization" to customerToken)
        )
        val res = router.handleRequest(req)
        assertEquals(403, res.statusCode)
    }
}

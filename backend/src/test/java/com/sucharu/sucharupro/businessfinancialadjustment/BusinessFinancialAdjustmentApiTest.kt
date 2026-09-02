package com.sucharu.sucharupro.businessfinancialadjustment

import com.sucharu.sucharupro.backend.integration.MockIntegrationDb
import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.model.businessfinancialadjustment.*
import com.sucharu.sucharupro.data.api.server.BackendRouter
import com.sucharu.sucharupro.data.api.server.BackendSecurityContext
import com.sucharu.sucharupro.data.api.server.BackendUseCases
import com.sucharu.sucharupro.data.api.server.HttpRequest
import com.sucharu.sucharupro.data.auth.security.AuthConfig
import com.sucharu.sucharupro.data.auth.security.JwtTokenProvider
import com.sucharu.sucharupro.data.datasource.businessfinancialadjustment.FakeBusinessFinancialAdjustmentDataSource
import com.sucharu.sucharupro.data.datasource.businessledger.FakeBusinessLedgerDataSource
import com.sucharu.sucharupro.data.persistence.postgres.DatabaseHealthChecker
import com.sucharu.sucharupro.data.persistence.postgres.DefaultPostgresTransactionManager
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.repository.businessfinancialadjustment.BusinessFinancialAdjustmentRepositoryImpl
import com.sucharu.sucharupro.data.repository.businessledger.BusinessLedgerRepositoryImpl
import com.sucharu.sucharupro.domain.model.businessfinancialadjustment.*
import com.sucharu.sucharupro.domain.service.businessfinancialadjustment.BusinessFinancialAdjustmentServiceImpl
import com.sucharu.sucharupro.domain.service.businessledger.BusinessLedgerServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessFinancialAdjustmentApiTest {

    private lateinit var router: BackendRouter
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var adminToken: String
    private lateinit var customerToken: String
    private lateinit var adjRepo: BusinessFinancialAdjustmentRepositoryImpl
    private lateinit var adjService: BusinessFinancialAdjustmentServiceImpl

    private val projectId = "PRJ-API-01"
    private val tenantId = "TENANT-001"

    @Before
    fun setup() {
        runBlocking {
            val adjDs = FakeBusinessFinancialAdjustmentDataSource()
            adjRepo = BusinessFinancialAdjustmentRepositoryImpl(adjDs)

            val ledgerDs = FakeBusinessLedgerDataSource()
            val ledgerRepo = BusinessLedgerRepositoryImpl(ledgerDs)
            val ledgerService = BusinessLedgerServiceImpl(ledgerRepo, defaultTenantId = tenantId)

            adjService = BusinessFinancialAdjustmentServiceImpl(
                repository = adjRepo,
                ledgerService = ledgerService,
                defaultTenantId = tenantId
            )

            val mockDb = MockIntegrationDb()
            val txManager = DefaultPostgresTransactionManager(mockDb)

            val customFactory = object : PostgresRepositoryFactory(txManager) {
                override fun createBusinessFinancialAdjustmentRepository(tenantId: String) = adjRepo
                override fun createBusinessFinancialAdjustmentService(tenantId: String) = adjService
            }

            val useCases = BackendUseCases(txManager, customFactory)
            val authConfig = AuthConfig(
                jwtSigningSecret = "test_signing_secret_for_adjustment_api_test_2026",
                jwtIssuer = "sucharu-test",
                jwtAudience = "sucharu-api"
            )
            jwtTokenProvider = JwtTokenProvider(authConfig)
            val securityContext = BackendSecurityContext(jwtTokenProvider)

            router = BackendRouter(
                securityContext = securityContext,
                useCases = useCases,
                healthChecker = DatabaseHealthChecker(mockDb)
            )

            adminToken = "Bearer " + jwtTokenProvider.generateAccessToken(
                AuthenticatedPrincipal(
                    userId = "USR-ADMIN",
                    username = "admin",
                    role = UserRole.ADMIN,
                    projectId = projectId
                )
            )

            customerToken = "Bearer " + jwtTokenProvider.generateAccessToken(
                AuthenticatedPrincipal(
                    userId = "CUST-001",
                    username = "customer",
                    role = UserRole.CUSTOMER,
                    projectId = projectId
                )
            )
        }
    }

    @Test
    fun testRestEndpointsForAdjustment() = runBlocking {
        // 1. POST /api/v1/business-financial-adjustments
        val createReq = CreateAdjustmentRequest(
            adjustmentNumber = "ADJ-REST-001",
            adjustmentType = "EXPENSE_CORRECTION",
            sourceType = "BUSINESS_EXPENSE",
            sourceId = "EXP-999",
            originalAmount = "10000.0000",
            adjustmentAmount = "-1000.0000",
            currency = "BDT",
            reason = "Bulk discount correction",
            justification = "Vendor agreed on 10% retrospective price discount",
            periodId = "PER-2026-08"
        )
        val httpReq = HttpRequest(
            method = "POST",
            path = "/api/v1/business-financial-adjustments",
            headers = mapOf("Authorization" to adminToken),
            body = createReq
        )
        val resp = router.handleRequest(httpReq)
        assertEquals(201, resp.statusCode)

        val apiSuccess = resp.body as ApiSuccessResponse<*>
        val adjResp = apiSuccess.data as FinancialAdjustmentResponse
        assertEquals("ADJ-REST-001", adjResp.adjustmentNumber)
        assertEquals("9000.0000", adjResp.effectiveAmount)

        // 2. GET /api/v1/business-financial-adjustments/{id}
        val getReq = HttpRequest(
            method = "GET",
            path = "/api/v1/business-financial-adjustments/${adjResp.id}",
            headers = mapOf("Authorization" to adminToken)
        )
        val getResp = router.handleRequest(getReq)
        assertEquals(200, getResp.statusCode)

        // 3. GET /api/v1/business-financial-adjustments/summary
        val sumReq = HttpRequest(
            method = "GET",
            path = "/api/v1/business-financial-adjustments/summary",
            headers = mapOf("Authorization" to adminToken)
        )
        val sumResp = router.handleRequest(sumReq)
        assertEquals(200, sumResp.statusCode)
    }

    @Test
    fun testCustomerDirectApiInvocationBlocked() = runBlocking {
        val createReq = CreateAdjustmentRequest(
            adjustmentNumber = "ADJ-REST-002",
            adjustmentType = "EXPENSE_CORRECTION",
            sourceType = "BUSINESS_EXPENSE",
            sourceId = "EXP-999",
            originalAmount = "10000.0000",
            adjustmentAmount = "-1000.0000",
            currency = "BDT",
            reason = "Unauthorized Attempt",
            justification = "Customer attempting direct API adjustment",
            periodId = "PER-2026-08"
        )
        val httpReq = HttpRequest(
            method = "POST",
            path = "/api/v1/business-financial-adjustments",
            headers = mapOf("Authorization" to customerToken),
            body = createReq
        )
        val resp = router.handleRequest(httpReq)
        // Access denied returns 400 with message or 403
        assertTrue(resp.statusCode in setOf(400, 403))
    }
}

package com.sucharu.sucharupro.backend.inventory

import com.sucharu.sucharupro.backend.integration.MockIntegrationDb
import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.server.*
import com.sucharu.sucharupro.data.auth.security.AuthConfig
import com.sucharu.sucharupro.data.auth.security.JwtTokenProvider
import com.sucharu.sucharupro.data.datasource.FakeFinishedProductInventoryDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryProductDataSource
import com.sucharu.sucharupro.data.datasource.finalqc.FakeFinalQcPackagingDataSource
import com.sucharu.sucharupro.data.persistence.postgres.DatabaseHealthChecker
import com.sucharu.sucharupro.data.persistence.postgres.DefaultPostgresTransactionManager
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.domain.model.finalqc.*
import com.sucharu.sucharupro.domain.service.inventory.ProductionInventoryIntegrationServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class ProductionInventoryApiTest {

    private lateinit var router: BackendRouter
    private lateinit var securityContext: BackendSecurityContext
    private lateinit var useCases: BackendUseCases
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var adminToken: String
    private lateinit var finalQcDs: FakeFinalQcPackagingDataSource
    private val tenantId = "TENANT-001"
    private val executionJobId = "JOB-API-01"

    @Before
    fun setup() {
        runBlocking {
            finalQcDs = FakeFinalQcPackagingDataSource()
            val inventoryDs = FakeFinishedProductInventoryDataSource()
            val productDs = FakeInventoryProductDataSource()

            val inventoryService = ProductionInventoryIntegrationServiceImpl(
                finishedProductInventoryDataSource = inventoryDs,
                finalQcPackagingDataSource = finalQcDs,
                inventoryProductDataSource = productDs
            )

            val mockDb = MockIntegrationDb()
            val txManager = DefaultPostgresTransactionManager(mockDb)

            val customFactory = object : PostgresRepositoryFactory(txManager, tenantId) {
                override fun createFinalQcPackagingDataSource(tenantId: String) = finalQcDs
                override fun createFinishedProductInventoryDataSource(tenantId: String) = inventoryDs
                override fun createInventoryProductDataSource(tenantId: String) = productDs
                override fun createProductionInventoryIntegrationService(tenantId: String) = inventoryService
            }

            useCases = BackendUseCases(txManager, customFactory)

            val authConfig = AuthConfig(
                jwtSigningSecret = "test_signing_secret_for_inventory_api_test_2026",
                jwtIssuer = "sucharu-test",
                jwtAudience = "sucharu-api"
            )
            jwtTokenProvider = JwtTokenProvider(authConfig)
            securityContext = BackendSecurityContext(jwtTokenProvider)

            adminToken = jwtTokenProvider.generateAccessToken(
                principal = AuthenticatedPrincipal(
                    userId = "USR-ADMIN-01",
                    projectId = tenantId,
                    username = "admin_user",
                    role = UserRole.ADMIN
                )
            )

            val healthChecker = DatabaseHealthChecker(mockDb)
            router = BackendRouter(securityContext, useCases, healthChecker)
        }
    }

    @Test
    fun testUnauthenticatedRequest_ReturnsError() = runBlocking {
        val req = HttpRequest(
            method = "GET",
            path = "/api/v1/inventory/finished-goods/jobs/$executionJobId/eligibility"
        )
        try {
            securityContext.authenticate(req.authorizationHeader)
            fail("Expected UnauthenticatedException")
        } catch (e: UnauthenticatedException) {
            assertNotNull(e.message)
        }
    }

    @Test
    fun testEligibleJob_CanEvaluateAndReceiveViaRouter() = runBlocking {
        // Setup QC inspection and release
        val inspection = ProductionFinalQcInspection(
            inspectionId = "INSP-API-01",
            tenantId = tenantId,
            executionJobId = executionJobId,
            orderId = "ORD-API-01",
            samplePlanType = InspectionSamplePlanType.FULL_100_PERCENT,
            totalLotQuantity = BigDecimal("500"),
            sampleSize = BigDecimal("500"),
            acceptedQuantity = BigDecimal("500"),
            rejectedQuantity = BigDecimal.ZERO,
            reworkQuantity = BigDecimal.ZERO,
            status = FinalQcInspectionStatus.ACCEPTED,
            inspectorId = "INSP-01",
            inspectorName = "Inspector Alpha"
        )
        finalQcDs.saveInspection(tenantId, inspection)

        val release = FinishedGoodsReleaseRecord(
            releaseId = "REL-API-01",
            tenantId = tenantId,
            executionJobId = executionJobId,
            orderId = "ORD-API-01",
            inspectionId = "INSP-API-01",
            packagingId = "PKG-API-01",
            releasedQuantity = BigDecimal("500"),
            destination = "WAREHOUSE_FINISHED_GOODS",
            status = FinishedGoodsReleaseStatus.RELEASE_APPROVED,
            authorizedBy = "ADMIN-01",
            integrityHash = "hash-xyz"
        )
        finalQcDs.saveReleaseRecord(tenantId, release)

        val headers = mapOf("Authorization" to "Bearer $adminToken")

        // 1. Eligibility Check
        val eligReq = HttpRequest(
            method = "GET",
            path = "/api/v1/inventory/finished-goods/jobs/$executionJobId/eligibility",
            headers = headers
        )
        val eligRes = handleInventoryRouterRequest(securityContext, useCases, eligReq)
        assertNotNull(eligRes)
        assertEquals(200, eligRes!!.statusCode)

        // 2. Receive Finished Goods
        val receiveBodyJson = """{"warehouseId":"WH-MAIN","binId":"BIN-A1","notes":"Received via API Test"}"""
        val receiveReq = HttpRequest(
            method = "POST",
            path = "/api/v1/inventory/finished-goods/jobs/$executionJobId/receive",
            headers = headers,
            body = receiveBodyJson
        )
        val receiveRes = handleInventoryRouterRequest(securityContext, useCases, receiveReq)
        assertNotNull(receiveRes)
        assertEquals(201, receiveRes!!.statusCode)

        // 3. Query Receipt
        val receiptReq = HttpRequest(
            method = "GET",
            path = "/api/v1/inventory/finished-goods/jobs/$executionJobId/receipt",
            headers = headers
        )
        val receiptRes = handleInventoryRouterRequest(securityContext, useCases, receiptReq)
        assertNotNull(receiptRes)
        assertEquals(200, receiptRes!!.statusCode)
    }
}

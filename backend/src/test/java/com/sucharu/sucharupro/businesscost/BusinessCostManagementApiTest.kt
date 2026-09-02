package com.sucharu.sucharupro.businesscost

import com.sucharu.sucharupro.backend.integration.MockIntegrationDb
import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.server.BackendRouter
import com.sucharu.sucharupro.data.api.server.BackendSecurityContext
import com.sucharu.sucharupro.data.api.server.BackendUseCases
import com.sucharu.sucharupro.data.api.server.HttpRequest
import com.sucharu.sucharupro.data.auth.security.AuthConfig
import com.sucharu.sucharupro.data.auth.security.JwtTokenProvider
import com.sucharu.sucharupro.data.datasource.businesscost.FakeBusinessCostManagementDataSource
import com.sucharu.sucharupro.data.datasource.businessexpense.FakeBusinessExpenseDataSource
import com.sucharu.sucharupro.data.datasource.businessledger.FakeBusinessLedgerDataSource
import com.sucharu.sucharupro.data.datasource.vendorpayable.FakeVendorPayableDataSource
import com.sucharu.sucharupro.data.persistence.postgres.DatabaseHealthChecker
import com.sucharu.sucharupro.data.persistence.postgres.DefaultPostgresTransactionManager
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.repository.businesscost.BusinessCostManagementRepositoryImpl
import com.sucharu.sucharupro.data.repository.businessexpense.BusinessExpenseRepositoryImpl
import com.sucharu.sucharupro.data.repository.businessledger.BusinessLedgerRepositoryImpl
import com.sucharu.sucharupro.data.repository.vendorpayable.VendorPayableRepositoryImpl
import com.sucharu.sucharupro.domain.model.businesscost.*
import com.sucharu.sucharupro.domain.service.businesscost.BusinessCostManagementServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessCostManagementApiTest {

    private lateinit var router: BackendRouter
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var adminToken: String
    private lateinit var customerToken: String
    private lateinit var costRepo: BusinessCostManagementRepositoryImpl
    private val projectId = "PRJ-API-01"
    private val tenantId = "TENANT-001"

    @Before
    fun setup() {
        runBlocking {
            val costDs = FakeBusinessCostManagementDataSource()
            costRepo = BusinessCostManagementRepositoryImpl(costDs)
            val expenseDs = FakeBusinessExpenseDataSource()
            val expenseRepo = BusinessExpenseRepositoryImpl(expenseDs)
            val payableDs = FakeVendorPayableDataSource()
            val payableRepo = VendorPayableRepositoryImpl(payableDs)
            val ledgerDs = FakeBusinessLedgerDataSource()
            val ledgerRepo = BusinessLedgerRepositoryImpl(ledgerDs)

            val costService = BusinessCostManagementServiceImpl(
                repository = costRepo,
                expenseRepository = expenseRepo,
                payableRepository = payableRepo,
                ledgerRepository = ledgerRepo,
                defaultTenantId = tenantId
            )

            val mockDb = MockIntegrationDb()
            val txManager = DefaultPostgresTransactionManager(mockDb)

            val customFactory = object : PostgresRepositoryFactory(txManager) {
                override fun createBusinessCostManagementRepository(tenantId: String) = costRepo
                override fun createBusinessCostManagementService(tenantId: String) = costService
                override fun createBusinessExpenseRepository(tenantId: String) = expenseRepo
                override fun createVendorPayableRepository(tenantId: String) = payableRepo
                override fun createBusinessLedgerRepository(tenantId: String) = ledgerRepo
            }

            val useCases = BackendUseCases(txManager, customFactory)

            val authConfig = AuthConfig(
                jwtSigningSecret = "test_signing_secret_for_cost_management_api_test_2026",
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

            // Seed default cost center & category
            costRepo.createCostCenter(
                BusinessCostCenter(
                    id = "CC-PRINT",
                    code = "CC-PRINT",
                    name = "Offset Printing",
                    description = null,
                    tenantId = tenantId,
                    projectId = projectId
                )
            )
            costRepo.createCostCategory(
                BusinessCostCategory(
                    id = "CAT-PAPER",
                    code = "CAT-PAPER",
                    name = "Paper Stock",
                    description = null,
                    tenantId = tenantId,
                    projectId = projectId
                )
            )
        }
    }

    @Test
    fun testListCostCentersEndpoint() = runBlocking {
        val request = HttpRequest(
            method = "GET",
            path = "/api/v1/business-cost-centers",
            headers = mapOf("Authorization" to "Bearer $adminToken"),
            body = null
        )
        val response = router.handleRequest(request)
        assertEquals(200, response.statusCode)
        val data = (response.body as ApiSuccessResponse<*>).data as List<*>
        assertEquals(1, data.size)
    }

    @Test
    fun testTrackCostEndpoint() = runBlocking {
        val request = HttpRequest(
            method = "POST",
            path = "/api/v1/business-cost-tracking",
            headers = mapOf("Authorization" to "Bearer $adminToken"),
            body = TrackOperationalCostRequest(
                sourceType = "MANUAL_OPERATIONAL_REFERENCE",
                sourceId = "MAN-API-01",
                costCenterId = "CC-PRINT",
                costCategoryId = "CAT-PAPER",
                jobId = "JOB-API-101",
                amount = "3500.0000",
                currency = "BDT",
                notes = "Manual API test"
            )
        )
        val response = router.handleRequest(request)
        assertEquals(201, response.statusCode)
        val tracking = (response.body as ApiSuccessResponse<*>).data as BusinessCostTrackingResponse
        assertEquals("MAN-API-01", tracking.sourceId)
        assertEquals("3500.0000", tracking.amount)
        assertEquals("FULLY_ALLOCATED", tracking.allocationStatus)
    }

    @Test
    fun testCustomerRoleIsForbidden() = runBlocking {
        val request = HttpRequest(
            method = "GET",
            path = "/api/v1/business-cost-centers",
            headers = mapOf("Authorization" to "Bearer $customerToken"),
            body = null
        )
        val response = router.handleRequest(request)
        assertEquals(400, response.statusCode)
    }
}

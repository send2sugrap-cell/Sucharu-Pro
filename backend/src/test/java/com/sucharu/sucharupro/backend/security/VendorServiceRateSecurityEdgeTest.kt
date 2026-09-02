package com.sucharu.sucharupro.backend.security

import com.sucharu.sucharupro.backend.BackendRuntime
import com.sucharu.sucharupro.backend.config.BackendConfig
import com.sucharu.sucharupro.backend.config.BackendEnvironment
import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.server.*
import com.sucharu.sucharupro.data.auth.security.JwtTokenProvider
import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.persistence.postgres.DatabaseHealthChecker
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.repository.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.repository.*
import com.sucharu.sucharupro.domain.service.vendor.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URI

class VendorServiceRateSecurityEdgeTest {

    private var testPort: Int = 0
    private var runtime: BackendRuntime? = null
    private var jwtProvider: JwtTokenProvider? = null

    @Before
    fun setUp() {
        testPort = findFreePort()
        val config = BackendConfig(
            serverPort = testPort,
            serverHost = "127.0.0.1",
            environment = BackendEnvironment.TEST,
            jwtSigningSecret = "sucharu_backend_integration_secret_test_2026_secure"
        )
        runtime = BackendRuntime(config)
        jwtProvider = runtime!!.composition.jwtTokenProvider
        runtime!!.start()
    }

    @After
    fun tearDown() {
        try {
            runtime?.stop()
        } catch (_: Exception) {}
    }

    private fun findFreePort(): Int {
        ServerSocket(0).use { socket ->
            return socket.localPort
        }
    }

    private fun generateToken(
        userId: String = "test_user",
        projectId: String = "test_project",
        role: UserRole = UserRole.ADMIN,
        permissions: Set<UserPermission> = setOf(UserPermission.ADMIN_ALL, UserPermission.MANAGE_VENDORS, UserPermission.MANAGE_VENDOR_RATES)
    ): String {
        val principal = AuthenticatedPrincipal(
            userId = userId,
            projectId = projectId,
            username = "user_$userId",
            role = role,
            permissions = permissions
        )
        return jwtProvider!!.generateAccessToken(principal)
    }

    private fun executeHttp(
        method: String,
        path: String,
        body: String? = null,
        token: String? = null
    ): Pair<Int, String> {
        val url = URI("http://127.0.0.1:$testPort$path").toURL()
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        if (token != null) {
            conn.setRequestProperty("Authorization", "Bearer $token")
        }
        if (body != null) {
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { os ->
                os.write(body.toByteArray(Charsets.UTF_8))
            }
        }
        val statusCode = conn.responseCode
        val responseBody = try {
            val stream = if (statusCode in 200..299) conn.inputStream else conn.errorStream
            stream?.bufferedReader()?.use { it.readText() } ?: ""
        } catch (e: Exception) {
            ""
        }
        return Pair(statusCode, responseBody)
    }

    @Test
    fun `unauthenticated request to vendor rate endpoints returns 401`() {
        val (status1, _) = executeHttp("GET", "/api/v1/vendors/vnd_01/rates")
        assertEquals(401, status1)

        val (status2, _) = executeHttp("POST", "/api/v1/vendors/vnd_01/rates", "{}")
        assertEquals(401, status2)

        val (status3, _) = executeHttp("POST", "/api/v1/vendors/vnd_01/rates/resolve", "{}")
        assertEquals(401, status3)

        val (status4, _) = executeHttp("GET", "/api/v1/vendors/vnd_01/rates/rate_01")
        assertEquals(401, status4)
    }

    @Test
    fun `customer or affiliate role is forbidden with 403 on vendor rate endpoints`() {
        val custToken = generateToken(userId = "cust_1", role = UserRole.CUSTOMER, permissions = setOf(UserPermission.READ_OWN_PROFILE))
        val affToken = generateToken(userId = "aff_1", role = UserRole.AFFILIATE, permissions = setOf(UserPermission.READ_OWN_AFFILIATE))

        val (status1, _) = executeHttp("GET", "/api/v1/vendors/vnd_01/rates", token = custToken)
        assertEquals(403, status1)

        val (status2, _) = executeHttp("POST", "/api/v1/vendors/vnd_01/rates", "{}", token = affToken)
        assertEquals(403, status2)
    }

    @Test
    fun `router direct verification of vendor service rate CRUD, resolution, estimation and RBAC`() = runBlocking {
        val fakeVendorDs = FakeVendorDataSource()
        val fakeCapDs = FakeVendorCapabilityDataSource()
        val fakeRateDs = FakeVendorServiceRateDataSource()

        val fakeVendorRepo = VendorRepositoryImpl(fakeVendorDs)
        val fakeCapRepo = VendorCapabilityRepositoryImpl(fakeCapDs)
        val fakeRateRepo = VendorServiceRateRepositoryImpl(fakeRateDs)

        val fakeVendorService = VendorServiceImpl(fakeVendorRepo)
        val fakeCapService = VendorCapabilityServiceImpl(fakeVendorRepo, fakeCapRepo)
        val fakeRateService = VendorServiceRateServiceImpl(fakeVendorRepo, fakeCapRepo, fakeRateRepo)

        val customRepoFactory = object : PostgresRepositoryFactory(
            transactionManager = runtime!!.composition.transactionManager
        ) {
            override fun createVendorRepository(tenantId: String): VendorRepository = fakeVendorRepo
            override fun createVendorService(tenantId: String): VendorService = fakeVendorService
            override fun createVendorCapabilityRepository(tenantId: String): VendorCapabilityRepository = fakeCapRepo
            override fun createVendorCapabilityService(tenantId: String): VendorCapabilityService = fakeCapService
            override fun createVendorServiceRateRepository(tenantId: String): VendorServiceRateRepository = fakeRateRepo
            override fun createVendorServiceRateService(tenantId: String): VendorServiceRateService = fakeRateService
        }

        val testUseCases = BackendUseCases(
            transactionManager = runtime!!.composition.transactionManager,
            repositoryFactory = customRepoFactory
        )

        val testRouter = BackendRouter(
            securityContext = runtime!!.composition.securityContext,
            useCases = testUseCases,
            healthChecker = DatabaseHealthChecker(runtime!!.composition.poolProvider)
        )

        val adminPrincipal = AuthenticatedPrincipal(
            userId = "admin_01",
            projectId = "p_sec_01",
            username = "admin",
            role = UserRole.ADMIN,
            permissions = setOf(UserPermission.ADMIN_ALL, UserPermission.MANAGE_VENDORS, UserPermission.MANAGE_VENDOR_RATES)
        )
        val adminToken = jwtProvider!!.generateAccessToken(adminPrincipal)

        // 1. Seed vendor & capability
        fakeVendorRepo.createVendor(
            Vendor(
                vendorId = "vnd_test",
                projectId = "p_sec_01",
                vendorCode = "V-TEST",
                vendorName = "Testing Press",
                vendorType = VendorType.SERVICE_PROVIDER,
                vendorCategory = VendorCategory.PRINTING,
                status = VendorStatus.ACTIVE
            )
        )
        fakeCapRepo.createCapability(
            VendorCapability(
                capabilityId = "cap_ctp_test",
                vendorId = "vnd_test",
                projectId = "p_sec_01",
                capabilityType = CapabilityType.CTP,
                displayName = "CTP Output",
                status = CapabilityStatus.ACTIVE
            )
        )

        // 2. Create Rate via Router
        val createRateReq = HttpRequest(
            method = "POST",
            path = "/api/v1/vendors/vnd_test/rates",
            headers = mapOf("Authorization" to "Bearer $adminToken", "Content-Type" to "application/json"),
            body = CreateVendorServiceRateRequestDto(
                capabilityType = "CTP",
                serviceName = "CTP Metal Plate",
                pricingMethod = "PER_UNIT",
                unitOfMeasure = "PLATE",
                rateAmount = 800.00,
                minimumQuantity = 1.0,
                effectiveFrom = 1000L,
                effectiveTo = 9000L
            )
        )
        val createRes = testRouter.handleRequest(createRateReq)
        assertEquals(201, createRes.statusCode)
        val createdRate = (createRes.body as ApiSuccessResponse<*>).data as VendorServiceRateDto
        assertEquals("CTP Metal Plate", createdRate.serviceName)
        assertEquals(800.00, createdRate.rateAmount, 0.001)

        // 3. Resolve Rate via Router
        val resolveReq = HttpRequest(
            method = "POST",
            path = "/api/v1/vendors/vnd_test/rates/resolve",
            headers = mapOf("Authorization" to "Bearer $adminToken", "Content-Type" to "application/json"),
            body = ResolveVendorRateRequestDto(
                capabilityType = "CTP",
                effectiveDate = 5000L
            )
        )
        val resolveRes = testRouter.handleRequest(resolveReq)
        assertEquals(200, resolveRes.statusCode)
        val resolved = (resolveRes.body as ApiSuccessResponse<*>).data as VendorServiceRateDto
        assertEquals(createdRate.rateId, resolved.rateId)

        // 4. Estimate Cost via Router
        val estimateReq = HttpRequest(
            method = "POST",
            path = "/api/v1/vendors/vnd_test/rates/${createdRate.rateId}/estimate",
            headers = mapOf("Authorization" to "Bearer $adminToken", "Content-Type" to "application/json"),
            body = EstimateVendorCostRequestDto(quantity = 5.0)
        )
        val estimateRes = testRouter.handleRequest(estimateReq)
        assertEquals(200, estimateRes.statusCode)
        val costResp = (estimateRes.body as ApiSuccessResponse<*>).data as EstimateVendorCostResponseDto
        assertEquals(4000.00, costResp.estimatedCost, 0.001)

        // 5. Update Status via Router
        val statusReq = HttpRequest(
            method = "PATCH",
            path = "/api/v1/vendors/vnd_test/rates/${createdRate.rateId}/status",
            headers = mapOf("Authorization" to "Bearer $adminToken", "Content-Type" to "application/json"),
            body = UpdateVendorServiceRateStatusRequestDto(status = "SUSPENDED")
        )
        val statusRes = testRouter.handleRequest(statusReq)
        assertEquals(200, statusRes.statusCode)
        val updatedRate = (statusRes.body as ApiSuccessResponse<*>).data as VendorServiceRateDto
        assertEquals("SUSPENDED", updatedRate.status)
    }
}

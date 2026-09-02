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

class VendorWorkOrderSecurityEdgeTest {

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
        permissions: Set<UserPermission> = setOf(
            UserPermission.ADMIN_ALL,
            UserPermission.MANAGE_VENDORS,
            UserPermission.MANAGE_VENDOR_RATES,
            UserPermission.READ_VENDOR_WORK_ORDERS,
            UserPermission.MANAGE_VENDOR_WORK_ORDERS,
            UserPermission.RELEASE_VENDOR_WORK_ORDERS
        )
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
    fun `unauthenticated request to vendor work order endpoints returns 401`() {
        val (codeList, _) = executeHttp("GET", "/api/v1/vendor-work-orders")
        assertEquals(401, codeList)

        val (codePost, _) = executeHttp("POST", "/api/v1/vendor-work-orders", body = "{}")
        assertEquals(401, codePost)

        val (codeRelease, _) = executeHttp("POST", "/api/v1/vendor-work-orders/vwo_123/release", body = "{}")
        assertEquals(401, codeRelease)
    }

    @Test
    fun `customer or affiliate role is forbidden with 403 on vendor work order endpoints`() {
        val customerToken = generateToken(userId = "cust_1", role = UserRole.CUSTOMER, permissions = setOf(UserPermission.READ_OWN_PROFILE))
        val affiliateToken = generateToken(userId = "aff_1", role = UserRole.AFFILIATE, permissions = setOf(UserPermission.READ_OWN_AFFILIATE))

        val (custCode, _) = executeHttp("GET", "/api/v1/vendor-work-orders", token = customerToken)
        assertEquals(403, custCode)

        val (affCode, _) = executeHttp("POST", "/api/v1/vendor-work-orders", token = affiliateToken, body = "{}")
        assertEquals(403, affCode)
    }

    @Test
    fun `router direct verification of vendor work order CRUD, assignment, release, lifecycle and RBAC`() = runBlocking {
        val fakeVendorDs = FakeVendorDataSource()
        val fakeCapDs = FakeVendorCapabilityDataSource()
        val fakeRateDs = FakeVendorServiceRateDataSource()
        val fakeWorkOrderDs = FakeVendorWorkOrderDataSource()

        val fakeVendorRepo = VendorRepositoryImpl(fakeVendorDs)
        val fakeVendorService = VendorServiceImpl(fakeVendorRepo)
        val fakeCapRepo = VendorCapabilityRepositoryImpl(fakeCapDs)
        val fakeCapService = VendorCapabilityServiceImpl(fakeVendorRepo, fakeCapRepo)
        val fakeRateRepo = VendorServiceRateRepositoryImpl(fakeRateDs)
        val fakeRateService = VendorServiceRateServiceImpl(fakeVendorRepo, fakeCapRepo, fakeRateRepo)
        val fakeWorkOrderRepo = VendorWorkOrderRepositoryImpl(fakeWorkOrderDs)
        val fakeWorkOrderService = VendorWorkOrderServiceImpl(fakeVendorRepo, fakeCapRepo, fakeRateService, fakeWorkOrderRepo)

        val customRepoFactory = object : PostgresRepositoryFactory(
            transactionManager = runtime!!.composition.transactionManager
        ) {
            override fun createVendorRepository(tenantId: String): VendorRepository = fakeVendorRepo
            override fun createVendorService(tenantId: String): VendorService = fakeVendorService
            override fun createVendorCapabilityRepository(tenantId: String): VendorCapabilityRepository = fakeCapRepo
            override fun createVendorCapabilityService(tenantId: String): VendorCapabilityService = fakeCapService
            override fun createVendorServiceRateRepository(tenantId: String): VendorServiceRateRepository = fakeRateRepo
            override fun createVendorServiceRateService(tenantId: String): VendorServiceRateService = fakeRateService
            override fun createVendorWorkOrderRepository(tenantId: String): VendorWorkOrderRepository = fakeWorkOrderRepo
            override fun createVendorWorkOrderService(tenantId: String): VendorWorkOrderService = fakeWorkOrderService
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
            permissions = setOf(
                UserPermission.ADMIN_ALL,
                UserPermission.MANAGE_VENDORS,
                UserPermission.MANAGE_VENDOR_RATES,
                UserPermission.MANAGE_VENDOR_WORK_ORDERS,
                UserPermission.RELEASE_VENDOR_WORK_ORDERS
            )
        )
        val adminToken = jwtProvider!!.generateAccessToken(adminPrincipal)

        // 1. Seed active vendor, capability and rate
        fakeVendorRepo.createVendor(
            Vendor(
                vendorId = "vnd_press",
                projectId = "p_sec_01",
                vendorCode = "V-PR",
                vendorName = "City Press",
                status = VendorStatus.ACTIVE
            )
        )
        fakeCapRepo.createCapability(
            VendorCapability(
                capabilityId = "cap_lam",
                vendorId = "vnd_press",
                projectId = "p_sec_01",
                capabilityType = CapabilityType.LAMINATION,
                displayName = "Thermal Lamination",
                status = CapabilityStatus.ACTIVE
            )
        )
        fakeRateService.createRate(
            projectId = "p_sec_01",
            vendorId = "vnd_press",
            capabilityType = CapabilityType.LAMINATION,
            serviceName = "Thermal Matt Lamination",
            pricingMethod = PricingMethod.PER_UNIT,
            unitOfMeasure = UnitOfMeasure.SHEET,
            rateAmount = Money(BigDecimal("3.00")),
            currency = "BDT"
        )

        // 2. Create work order via Router
        val createReq = HttpRequest(
            method = "POST",
            path = "/api/v1/vendor-work-orders",
            headers = mapOf("Authorization" to "Bearer $adminToken", "Content-Type" to "application/json"),
            body = CreateVendorWorkOrderRequestDto(
                vendorId = "vnd_press",
                capabilityType = "LAMINATION",
                title = "Catalog Covers Lamination",
                quantity = 500.0,
                unitOfMeasure = "SHEET",
                pricingMethod = "PER_UNIT"
            )
        )
        val createRes = testRouter.handleRequest(createReq)
        assertEquals(201, createRes.statusCode)
        val createdOrder = (createRes.body as ApiSuccessResponse<*>).data as VendorWorkOrderDto
        assertEquals("vnd_press", createdOrder.vendorId)
        assertEquals(1500.0, createdOrder.estimatedAmount, 0.01)
        assertEquals("ASSIGNED", createdOrder.status)

        val workOrderId = createdOrder.workOrderId

        // 3. Update work order title & quantity
        val updateReq = HttpRequest(
            method = "PUT",
            path = "/api/v1/vendor-work-orders/$workOrderId",
            headers = mapOf("Authorization" to "Bearer $adminToken", "Content-Type" to "application/json"),
            body = UpdateVendorWorkOrderRequestDto(
                title = "Catalog Covers Lamination Revised",
                quantity = 600.0
            )
        )
        val updateRes = testRouter.handleRequest(updateReq)
        assertEquals(200, updateRes.statusCode)
        val updatedOrder = (updateRes.body as ApiSuccessResponse<*>).data as VendorWorkOrderDto
        assertEquals("Catalog Covers Lamination Revised", updatedOrder.title)
        assertEquals(1800.0, updatedOrder.estimatedAmount, 0.01)

        // 4. Release work order
        val releaseReq = HttpRequest(
            method = "POST",
            path = "/api/v1/vendor-work-orders/$workOrderId/release",
            headers = mapOf("Authorization" to "Bearer $adminToken", "Content-Type" to "application/json"),
            body = ""
        )
        val releaseRes = testRouter.handleRequest(releaseReq)
        assertEquals(200, releaseRes.statusCode)
        val releasedOrder = (releaseRes.body as ApiSuccessResponse<*>).data as VendorWorkOrderDto
        assertEquals("RELEASED", releasedOrder.status)

        // 5. Start work order
        val startReq = HttpRequest(
            method = "POST",
            path = "/api/v1/vendor-work-orders/$workOrderId/start",
            headers = mapOf("Authorization" to "Bearer $adminToken", "Content-Type" to "application/json"),
            body = ""
        )
        val startRes = testRouter.handleRequest(startReq)
        assertEquals(200, startRes.statusCode)
        assertEquals("IN_PROGRESS", ((startRes.body as ApiSuccessResponse<*>).data as VendorWorkOrderDto).status)

        // 6. Hold and resume
        val holdReq = HttpRequest(
            method = "POST",
            path = "/api/v1/vendor-work-orders/$workOrderId/hold",
            headers = mapOf("Authorization" to "Bearer $adminToken", "Content-Type" to "application/json"),
            body = ""
        )
        val holdRes = testRouter.handleRequest(holdReq)
        assertEquals(200, holdRes.statusCode)
        assertEquals("ON_HOLD", ((holdRes.body as ApiSuccessResponse<*>).data as VendorWorkOrderDto).status)

        val resumeReq = HttpRequest(
            method = "POST",
            path = "/api/v1/vendor-work-orders/$workOrderId/resume",
            headers = mapOf("Authorization" to "Bearer $adminToken", "Content-Type" to "application/json"),
            body = ""
        )
        val resumeRes = testRouter.handleRequest(resumeReq)
        assertEquals(200, resumeRes.statusCode)
        assertEquals("IN_PROGRESS", ((resumeRes.body as ApiSuccessResponse<*>).data as VendorWorkOrderDto).status)

        // 7. Complete work order
        val completeReq = HttpRequest(
            method = "POST",
            path = "/api/v1/vendor-work-orders/$workOrderId/complete",
            headers = mapOf("Authorization" to "Bearer $adminToken", "Content-Type" to "application/json"),
            body = ""
        )
        val completeRes = testRouter.handleRequest(completeReq)
        assertEquals(200, completeRes.statusCode)
        assertEquals("COMPLETED", ((completeRes.body as ApiSuccessResponse<*>).data as VendorWorkOrderDto).status)

        // 8. View Audit Trail
        val auditReq = HttpRequest(
            method = "GET",
            path = "/api/v1/vendor-work-orders/$workOrderId/audit",
            headers = mapOf("Authorization" to "Bearer $adminToken")
        )
        val auditRes = testRouter.handleRequest(auditReq)
        assertEquals(200, auditRes.statusCode)
        val audits = (auditRes.body as ApiSuccessResponse<*>).data as List<*>
        assertTrue(audits.isNotEmpty())
    }
}

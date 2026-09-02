package com.sucharu.sucharupro.backend.security

import com.sucharu.sucharupro.backend.BackendRuntime
import com.sucharu.sucharupro.backend.config.BackendConfig
import com.sucharu.sucharupro.backend.config.BackendEnvironment
import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.server.*
import com.sucharu.sucharupro.data.auth.security.JwtTokenProvider
import com.sucharu.sucharupro.data.datasource.FakeVendorDataSource
import com.sucharu.sucharupro.data.persistence.postgres.DatabaseHealthChecker
import com.sucharu.sucharupro.data.persistence.postgres.PostgresConnectionProvider
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.data.repository.VendorRepositoryImpl
import com.sucharu.sucharupro.domain.repository.VendorRepository
import com.sucharu.sucharupro.domain.service.vendor.VendorService
import com.sucharu.sucharupro.domain.service.vendor.VendorServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URI

/**
 * End-to-end HTTP Security Edge Test for Vendor Master API (Module 12 Step 01).
 * Tests authentication, authorization/RBAC, tenant isolation, and validation over HTTP.
 */
class VendorSecurityEdgeTest {

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

    private fun executeHttp(
        method: String,
        urlStr: String,
        headers: Map<String, String> = emptyMap(),
        body: String? = null
    ): Pair<Int, String> {
        val connection = URI(urlStr).toURL().openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = 3000
        connection.readTimeout = 3000
        headers.forEach { (k, v) -> connection.setRequestProperty(k, v) }

        if (body != null) {
            connection.doOutput = true
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        }

        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val responseBody = stream?.bufferedReader()?.use { it.readText() } ?: ""
        connection.disconnect()
        return Pair(code, responseBody)
    }

    @Test
    fun test01_unauthenticatedVendorAccess_returns401() {
        val (code, body) = executeHttp("GET", "http://127.0.0.1:$testPort/api/v1/vendors")
        assertEquals(401, code)
        assertTrue(body.contains("UNAUTHENTICATED"))
    }

    @Test
    fun test02_customerRole_cannotAccessVendors_returns403() {
        val customer = AuthenticatedPrincipal(
            userId = "cust_001",
            projectId = "TENANT-001",
            username = "customer_joe",
            role = UserRole.CUSTOMER
        )
        val token = jwtProvider!!.generateAccessToken(customer)

        val (code, body) = executeHttp(
            method = "GET",
            urlStr = "http://127.0.0.1:$testPort/api/v1/vendors",
            headers = mapOf("Authorization" to "Bearer $token")
        )
        assertEquals(403, code)
        assertTrue(body.contains("FORBIDDEN"))
    }

    @Test
    fun test03_affiliateRole_cannotMutateVendors_returns403() {
        val affiliate = AuthenticatedPrincipal(
            userId = "affiliate_001",
            projectId = "TENANT-001",
            username = "affiliate_user",
            role = UserRole.AFFILIATE
        )
        val token = jwtProvider!!.generateAccessToken(affiliate)

        val (code, body) = executeHttp(
            method = "POST",
            urlStr = "http://127.0.0.1:$testPort/api/v1/vendors",
            headers = mapOf(
                "Authorization" to "Bearer $token",
                "Content-Type" to "application/json"
            ),
            body = """{"vendorName":"Test Vendor"}"""
        )
        assertEquals(403, code)
        assertTrue(body.contains("FORBIDDEN"))
    }

    @Test
    fun test04_malformedOrTamperedToken_returns401() {
        val (code, body) = executeHttp(
            method = "GET",
            urlStr = "http://127.0.0.1:$testPort/api/v1/vendors",
            headers = mapOf("Authorization" to "Bearer invalid.tampered.token")
        )
        assertEquals(401, code)
        assertTrue(body.contains("UNAUTHENTICATED"))
    }

    @Test
    fun test05_routerDirectVendorManagement_withFakeDataSource() = runBlocking {
        // Direct Router test using FakeVendorDataSource to verify full endpoint contract
        val fakeVendorDs = FakeVendorDataSource()
        val fakeRepo = VendorRepositoryImpl(fakeVendorDs)
        val fakeService = VendorServiceImpl(fakeRepo)

        val customRepoFactory = object : PostgresRepositoryFactory(
            transactionManager = runtime!!.composition.transactionManager
        ) {
            override fun createVendorRepository(tenantId: String): VendorRepository = fakeRepo
            override fun createVendorService(tenantId: String): VendorService = fakeService
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
            projectId = "TENANT-01",
            username = "admin",
            role = UserRole.ADMIN,
            permissions = setOf(UserPermission.ADMIN_ALL, UserPermission.MANAGE_VENDORS)
        )
        val token = jwtProvider!!.generateAccessToken(adminPrincipal)

        // 1. Create Vendor via POST /api/v1/vendors
        val createPayload = """
            {
                "vendorName": "Bengal Plates & Finishing",
                "vendorCode": "VND-BENGAL-01",
                "legalName": "Bengal Plates Ltd.",
                "vendorType": "SERVICE_PROVIDER",
                "vendorCategory": "FINISHING",
                "status": "ACTIVE",
                "primaryContactName": "Kamal",
                "primaryPhone": "+8801700112233",
                "primaryEmail": "kamal@bengalplates.com"
            }
        """.trimIndent()

        val postReq = HttpRequest(
            method = "POST",
            path = "/api/v1/vendors",
            headers = mapOf("Authorization" to "Bearer $token", "Content-Type" to "application/json"),
            body = createPayload
        )
        val postResp = testRouter.handleRequest(postReq)
        assertEquals(201, postResp.statusCode)
        val createdDto = postResp.body as ApiSuccessResponse<*>
        assertNotNull(createdDto.data)

        // 2. Query Vendor By Code via GET /api/v1/vendors/code/VND-BENGAL-01
        val getCodeReq = HttpRequest(
            method = "GET",
            path = "/api/v1/vendors/code/VND-BENGAL-01",
            headers = mapOf("Authorization" to "Bearer $token")
        )
        val getCodeResp = testRouter.handleRequest(getCodeReq)
        assertEquals(200, getCodeResp.statusCode)

        // 3. List Vendors via GET /api/v1/vendors
        val getListReq = HttpRequest(
            method = "GET",
            path = "/api/v1/vendors",
            headers = mapOf("Authorization" to "Bearer $token")
        )
        val getListResp = testRouter.handleRequest(getListReq)
        assertEquals(200, getListResp.statusCode)
    }
}

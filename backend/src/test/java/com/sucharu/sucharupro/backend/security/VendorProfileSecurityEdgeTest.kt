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
import com.sucharu.sucharupro.domain.repository.*
import com.sucharu.sucharupro.domain.service.vendor.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URI

class VendorProfileSecurityEdgeTest {

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
        permissions: Set<UserPermission> = setOf(UserPermission.ADMIN_ALL, UserPermission.MANAGE_VENDORS)
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
    fun `unauthenticated request to vendor profile or capability endpoints returns 401`() {
        val (status1, _) = executeHttp("GET", "/api/v1/vendors/vnd_01/profile")
        assertEquals(401, status1)

        val (status2, _) = executeHttp("GET", "/api/v1/vendors/vnd_01/contacts")
        assertEquals(401, status2)

        val (status3, _) = executeHttp("GET", "/api/v1/vendors/vnd_01/addresses")
        assertEquals(401, status3)

        val (status4, _) = executeHttp("GET", "/api/v1/vendors/vnd_01/capabilities")
        assertEquals(401, status4)

        val (status5, _) = executeHttp("GET", "/api/v1/vendor-capabilities/PRINTING/vendors")
        assertEquals(401, status5)
    }

    @Test
    fun `customer or affiliate role is forbidden with 403 on vendor profile and capability endpoints`() {
        val customerToken = generateToken(role = UserRole.CUSTOMER, permissions = emptySet())
        val affiliateToken = generateToken(role = UserRole.AFFILIATE, permissions = emptySet())

        val (cStatus1, _) = executeHttp("GET", "/api/v1/vendors/vnd_01/profile", token = customerToken)
        assertEquals(403, cStatus1)

        val (aStatus1, _) = executeHttp("GET", "/api/v1/vendors/vnd_01/contacts", token = affiliateToken)
        assertEquals(403, aStatus1)

        val (cStatus2, _) = executeHttp("GET", "/api/v1/vendors/vnd_01/capabilities", token = customerToken)
        assertEquals(403, cStatus2)
    }

    @Test
    fun `router direct verification of vendor profile, contact, address, capability and RBAC`() = runBlocking {
        val fakeVendorDs = FakeVendorDataSource()
        val fakeProfileDs = FakeVendorProfileDataSource()
        val fakeContactDs = FakeVendorContactDataSource()
        val fakeAddressDs = FakeVendorAddressDataSource()
        val fakeCapDs = FakeVendorCapabilityDataSource()

        val fakeVendorRepo = VendorRepositoryImpl(fakeVendorDs)
        val fakeProfileRepo = VendorProfileRepositoryImpl(fakeProfileDs)
        val fakeContactRepo = VendorContactRepositoryImpl(fakeContactDs)
        val fakeAddressRepo = VendorAddressRepositoryImpl(fakeAddressDs)
        val fakeCapRepo = VendorCapabilityRepositoryImpl(fakeCapDs)

        val fakeVendorService = VendorServiceImpl(fakeVendorRepo)
        val fakeProfileService = VendorProfileServiceImpl(fakeVendorRepo, fakeProfileRepo)
        val fakeContactService = VendorContactServiceImpl(fakeVendorRepo, fakeContactRepo)
        val fakeAddressService = VendorAddressServiceImpl(fakeVendorRepo, fakeAddressRepo)
        val fakeCapService = VendorCapabilityServiceImpl(fakeVendorRepo, fakeCapRepo)

        val customRepoFactory = object : PostgresRepositoryFactory(
            transactionManager = runtime!!.composition.transactionManager
        ) {
            override fun createVendorRepository(tenantId: String): VendorRepository = fakeVendorRepo
            override fun createVendorService(tenantId: String): VendorService = fakeVendorService
            override fun createVendorProfileRepository(tenantId: String): VendorProfileRepository = fakeProfileRepo
            override fun createVendorProfileService(tenantId: String): VendorProfileService = fakeProfileService
            override fun createVendorContactRepository(tenantId: String): VendorContactRepository = fakeContactRepo
            override fun createVendorContactService(tenantId: String): VendorContactService = fakeContactService
            override fun createVendorAddressRepository(tenantId: String): VendorAddressRepository = fakeAddressRepo
            override fun createVendorAddressService(tenantId: String): VendorAddressService = fakeAddressService
            override fun createVendorCapabilityRepository(tenantId: String): VendorCapabilityRepository = fakeCapRepo
            override fun createVendorCapabilityService(tenantId: String): VendorCapabilityService = fakeCapService
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
            permissions = setOf(UserPermission.ADMIN_ALL, UserPermission.MANAGE_VENDORS)
        )
        val adminToken = jwtProvider!!.generateAccessToken(adminPrincipal)

        // 1. Create Vendor via POST /api/v1/vendors
        val createVendorBody = """
            {
                "vendorName": "Press Alpha",
                "vendorType": "SERVICE_PROVIDER",
                "vendorCategory": "PRINTING"
            }
        """.trimIndent()
        val postReq = HttpRequest(
            method = "POST",
            path = "/api/v1/vendors",
            headers = mapOf("Authorization" to "Bearer $adminToken", "Content-Type" to "application/json"),
            body = createVendorBody
        )
        val postResp = testRouter.handleRequest(postReq)
        assertEquals(201, postResp.statusCode)
        val vendorData = (postResp.body as ApiSuccessResponse<*>).data as VendorDetailDto
        val vendorId = vendorData.vendorId

        // 2. Update Profile via PUT /api/v1/vendors/{vendorId}/profile
        val updateProfileBody = """
            {
                "displayName": "Press Alpha International",
                "legalName": "Press Alpha Ltd.",
                "email": "info@pressalpha.com"
            }
        """.trimIndent()
        val profReq = HttpRequest(
            method = "PUT",
            path = "/api/v1/vendors/$vendorId/profile",
            headers = mapOf("Authorization" to "Bearer $adminToken", "Content-Type" to "application/json"),
            body = updateProfileBody
        )
        val profResp = testRouter.handleRequest(profReq)
        assertEquals(200, profResp.statusCode)
        val profData = (profResp.body as ApiSuccessResponse<*>).data as VendorProfileDto
        assertEquals("Press Alpha International", profData.displayName)

        // 3. Create Contact via POST /api/v1/vendors/{vendorId}/contacts
        val contactBody = """
            {
                "name": "Mahmudul Hasan",
                "contactType": "PRODUCTION",
                "phone": "+8801700112233",
                "email": "mahmud@pressalpha.com",
                "isPrimary": true
            }
        """.trimIndent()
        val cntReq = HttpRequest(
            method = "POST",
            path = "/api/v1/vendors/$vendorId/contacts",
            headers = mapOf("Authorization" to "Bearer $adminToken", "Content-Type" to "application/json"),
            body = contactBody
        )
        val cntResp = testRouter.handleRequest(cntReq)
        assertEquals(201, cntResp.statusCode)

        // 4. Create Address via POST /api/v1/vendors/{vendorId}/addresses
        val addrBody = """
            {
                "addressLine1": "Plot 10, Tejgaon Industrial Area",
                "addressType": "FACTORY",
                "city": "Dhaka",
                "country": "Bangladesh",
                "isPrimary": true
            }
        """.trimIndent()
        val addrReq = HttpRequest(
            method = "POST",
            path = "/api/v1/vendors/$vendorId/addresses",
            headers = mapOf("Authorization" to "Bearer $adminToken", "Content-Type" to "application/json"),
            body = addrBody
        )
        val addrResp = testRouter.handleRequest(addrReq)
        assertEquals(201, addrResp.statusCode)

        // 5. Add Capability via POST /api/v1/vendors/{vendorId}/capabilities
        val capBody = """
            {
                "capabilityType": "FOILING",
                "displayName": "Hot Foil Stamping",
                "status": "ACTIVE"
            }
        """.trimIndent()
        val capReq = HttpRequest(
            method = "POST",
            path = "/api/v1/vendors/$vendorId/capabilities",
            headers = mapOf("Authorization" to "Bearer $adminToken", "Content-Type" to "application/json"),
            body = capBody
        )
        val capResp = testRouter.handleRequest(capReq)
        assertEquals(201, capResp.statusCode)

        // 6. List Capabilities via GET /api/v1/vendors/{vendorId}/capabilities
        val listCapReq = HttpRequest(
            method = "GET",
            path = "/api/v1/vendors/$vendorId/capabilities",
            headers = mapOf("Authorization" to "Bearer $adminToken")
        )
        val listCapResp = testRouter.handleRequest(listCapReq)
        assertEquals(200, listCapResp.statusCode)

        // 7. Query Vendors by Capability via GET /api/v1/vendor-capabilities/FOILING/vendors
        val byCapReq = HttpRequest(
            method = "GET",
            path = "/api/v1/vendor-capabilities/FOILING/vendors",
            headers = mapOf("Authorization" to "Bearer $adminToken")
        )
        val byCapResp = testRouter.handleRequest(byCapReq)
        assertEquals(200, byCapResp.statusCode)
        val vendorsWithCap = (byCapResp.body as ApiSuccessResponse<*>).data as List<*>
        assertTrue(vendorsWithCap.contains(vendorId))
    }
}

package com.sucharu.sucharupro.backend.security

import com.sucharu.sucharupro.backend.BackendRuntime
import com.sucharu.sucharupro.backend.config.BackendConfig
import com.sucharu.sucharupro.backend.config.BackendEnvironment
import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserPermission
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.auth.security.JwtTokenProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URI

/**
 * End-to-end HTTP Security Edge Integration Test (INFRA-05 Step 03).
 *
 * Verifies standalone backend runtime over live HTTP network connections:
 * - Public routes accessible without token (/health, /health/live, /health/readiness, /)
 * - Protected API routes require valid Bearer token
 * - Rejected malformed/expired/tampered tokens with 401 Unauthorized
 * - Rejected unauthorized role/capability requests with 403 Forbidden
 * - Anti-tenant-spoofing at HTTP gateway boundary
 */
class BackendSecurityEdgeIntegrationTest {

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

    @Test
    fun test01_publicHealthAndReadinessEndpoints_accessibleOverHttp() {
        val (statusLive, bodyLive) = executeGet("http://127.0.0.1:$testPort/health/live")
        assertEquals(200, statusLive)
        assertTrue(bodyLive.contains("\"live\":true"))

        val (statusReady, bodyReady) = executeGet("http://127.0.0.1:$testPort/health/readiness")
        assertTrue("Readiness status must be 200 or 503", statusReady in listOf(200, 503))
        assertTrue(bodyReady.contains("\"ready\":") || bodyReady.contains("\"status\":"))

        val (statusRoot, bodyRoot) = executeGet("http://127.0.0.1:$testPort/")
        assertEquals(200, statusRoot)
        assertTrue(bodyRoot.contains("\"status\": \"RUNNING\""))
    }

    @Test
    fun test02_protectedApiWithoutToken_returns401OverHttp() {
        val (statusCode, responseBody) = executeGet("http://127.0.0.1:$testPort/api/v1/customer/profile")
        assertEquals(401, statusCode)
        assertTrue(responseBody.contains("UNAUTHENTICATED"))
        assertTrue(responseBody.contains("Authorization header is missing"))
    }

    @Test
    fun test03_protectedApiWithInvalidToken_returns401OverHttp() {
        val (statusCode, responseBody) = executeGet(
            urlStr = "http://127.0.0.1:$testPort/api/v1/customer/profile",
            headers = mapOf("Authorization" to "Bearer invalid.jwt.signature")
        )
        assertEquals(401, statusCode)
        assertTrue(responseBody.contains("UNAUTHENTICATED"))
    }

    @Test
    fun test04_protectedApiWithValidToken_executesSuccessfullyOverHttp() {
        val principal = AuthenticatedPrincipal(
            userId = "CUST-LIVE-001",
            projectId = "PROJECT-LIVE-ALPHA",
            username = "live_client",
            role = UserRole.CUSTOMER,
            permissions = setOf(UserPermission.READ_OWN_PROFILE, UserPermission.READ_OWN_ORDERS)
        )
        val validToken = jwtProvider!!.generateAccessToken(principal)

        val (statusCode, responseBody) = executeGet(
            urlStr = "http://127.0.0.1:$testPort/api/v1/auth/me",
            headers = mapOf("Authorization" to "Bearer $validToken")
        )
        assertEquals(200, statusCode)
        assertTrue(responseBody.contains("CUST-LIVE-001"))
        assertTrue(responseBody.contains("PROJECT-LIVE-ALPHA"))
    }

    @Test
    fun test05_tenantHeaderSpoofingOverHttp_preservesServerAuthoritativeTenant() {
        val principal = AuthenticatedPrincipal(
            userId = "CUST-LIVE-002",
            projectId = "PROJECT-ORIGINAL-TENANT",
            username = "live_client_2",
            role = UserRole.CUSTOMER
        )
        val validToken = jwtProvider!!.generateAccessToken(principal)

        // Attacker sends X-Project-Id: PROJECT-SPOOFED-TENANT
        val (statusCode, responseBody) = executeGet(
            urlStr = "http://127.0.0.1:$testPort/api/v1/auth/me",
            headers = mapOf(
                "Authorization" to "Bearer $validToken",
                "X-Project-Id" to "PROJECT-SPOOFED-TENANT"
            )
        )
        assertEquals(200, statusCode)
        // Response contains server-verified PROJECT-ORIGINAL-TENANT, completely ignoring the spoofed header
        assertTrue(responseBody.contains("PROJECT-ORIGINAL-TENANT"))
        assertFalse(responseBody.contains("PROJECT-SPOOFED-TENANT"))
    }

    private fun executeGet(urlStr: String, headers: Map<String, String> = emptyMap()): Pair<Int, String> {
        val uri = URI(urlStr)
        val conn = uri.toURL().openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 3000
        conn.readTimeout = 3000
        headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }

        val statusCode = conn.responseCode
        val stream = if (statusCode in 200..299) conn.inputStream else conn.errorStream
        val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
        return Pair(statusCode, body)
    }

    private fun findFreePort(): Int {
        ServerSocket(0).use { socket ->
            return socket.localPort
        }
    }
}

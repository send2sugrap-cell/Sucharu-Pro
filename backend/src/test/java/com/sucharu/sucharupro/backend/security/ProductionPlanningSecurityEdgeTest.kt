package com.sucharu.sucharupro.backend.security

import com.sucharu.sucharupro.backend.BackendRuntime
import com.sucharu.sucharupro.backend.config.BackendConfig
import com.sucharu.sucharupro.backend.config.BackendEnvironment
import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.auth.security.JwtTokenProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URI

/**
 * End-to-end HTTP Security Edge Test for Module 17 Step 04 Order-to-Production Planning API.
 * Verifies authentication, RBAC authorization, and API route security.
 */
class ProductionPlanningSecurityEdgeTest {

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

    private fun createToken(role: UserRole, tenantId: String = "tenant_test_001", userId: String = "user_test_001"): String {
        val principal = AuthenticatedPrincipal(
            userId = userId,
            projectId = tenantId,
            username = "test_user",
            role = role
        )
        return jwtProvider!!.generateAccessToken(principal)
    }

    private fun makeRequest(
        path: String,
        method: String = "GET",
        token: String? = null,
        bodyJson: String? = null
    ): Pair<Int, String> {
        val url = URI("http://127.0.0.1:$testPort$path").toURL()
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        if (token != null) {
            conn.setRequestProperty("Authorization", "Bearer $token")
        }
        if (bodyJson != null) {
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { os ->
                os.write(bodyJson.toByteArray())
            }
        }
        val status = conn.responseCode
        val stream = if (status in 200..299) conn.inputStream else conn.errorStream
        val responseText = stream?.bufferedReader()?.use { it.readText() } ?: ""
        return status to responseText
    }

    @Test
    fun `unauthenticated request to evaluate readiness returns 401`() {
        val (status, _) = makeRequest(
            path = "/api/v1/production-planning/orders/ORD-001/readiness",
            method = "GET"
        )
        assertEquals(401, status)
    }

    @Test
    fun `unauthorized customer role request to create plan returns 403`() {
        val customerToken = createToken(UserRole.CUSTOMER)
        val (status, _) = makeRequest(
            path = "/api/v1/production-planning/orders/ORD-001/plan",
            method = "POST",
            token = customerToken,
            bodyJson = """{"orderItemId":"ITEM-001"}"""
        )
        assertEquals(403, status)
    }

    @Test
    fun `unauthenticated request to get plan snapshot returns 401`() {
        val (status, _) = makeRequest(
            path = "/api/v1/production-planning/plans/PLAN-001",
            method = "GET"
        )
        assertEquals(401, status)
    }
}

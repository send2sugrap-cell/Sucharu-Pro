package com.sucharu.sucharupro.backend.security

import com.sucharu.sucharupro.backend.BackendRuntime
import com.sucharu.sucharupro.backend.config.BackendConfig
import com.sucharu.sucharupro.backend.config.BackendEnvironment
import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.auth.security.JwtTokenProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URI

class VendorPerformanceSecurityEdgeTest {

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
            UserPermission.READ_VENDOR_PERFORMANCE,
            UserPermission.MANAGE_VENDOR_PERFORMANCE,
            UserPermission.EVALUATE_VENDOR,
            UserPermission.APPROVE_VENDOR_EVALUATION,
            UserPermission.READ_VENDOR_COMPLIANCE,
            UserPermission.MANAGE_VENDOR_COMPLIANCE,
            UserPermission.MANAGE_VENDOR_CORRECTIVE_ACTION
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
        return statusCode to responseBody
    }

    @Test
    fun testUnauthenticatedRequestReturns401() {
        val (codeKpi, _) = executeHttp("GET", "/api/v1/vendor-performance-kpis")
        assertEquals(401, codeKpi)

        val (codeReq, _) = executeHttp("GET", "/api/v1/vendor-compliance-requirements")
        assertEquals(401, codeReq)

        val (codeEval, _) = executeHttp("GET", "/api/v1/vendors/VND-01/evaluations")
        assertEquals(401, codeEval)

        val (codePost, _) = executeHttp("POST", "/api/v1/vendor-performance-kpis", body = "{}")
        assertEquals(401, codePost)
    }

    @Test
    fun testCustomerOrAffiliateRoleIsForbiddenWith403() {
        val customerToken = generateToken(userId = "cust_01", role = UserRole.CUSTOMER, permissions = emptySet())
        val affiliateToken = generateToken(userId = "aff_01", role = UserRole.AFFILIATE, permissions = emptySet())

        val (custCode, _) = executeHttp("GET", "/api/v1/vendor-performance-kpis", token = customerToken)
        assertEquals(403, custCode)

        val (affCode, _) = executeHttp("POST", "/api/v1/vendor-performance-kpis", body = "{}", token = affiliateToken)
        assertEquals(403, affCode)

        val (custEval, _) = executeHttp("GET", "/api/v1/vendors/VND-01/evaluations", token = customerToken)
        assertEquals(403, custEval)
    }

    @Test
    fun testStaffCannotApproveEvaluationWithoutApprovalPermission() {
        val staffToken = generateToken(
            userId = "staff_01",
            role = UserRole.STAFF,
            permissions = setOf(UserPermission.READ_VENDOR_PERFORMANCE, UserPermission.EVALUATE_VENDOR)
        )

        val (codeApprove, _) = executeHttp(
            "POST",
            "/api/v1/vendors/VND-01/evaluations/eval_123/approve",
            body = "{\"decision\":\"APPROVED\"}",
            token = staffToken
        )
        assertEquals(403, codeApprove)
    }

    @Test
    fun testTamperedTokenReturns401() {
        val validToken = generateToken()
        val tamperedToken = validToken.substring(0, validToken.length - 6) + "XXXXXX"

        val (code, _) = executeHttp("GET", "/api/v1/vendor-performance-kpis", token = tamperedToken)
        assertEquals(401, code)
    }
}

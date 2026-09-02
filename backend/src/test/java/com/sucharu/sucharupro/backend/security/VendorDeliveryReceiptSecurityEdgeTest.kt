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

class VendorDeliveryReceiptSecurityEdgeTest {

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
            UserPermission.READ_VENDOR_RECEIPTS,
            UserPermission.MANAGE_VENDOR_RECEIPTS,
            UserPermission.INSPECT_VENDOR_RECEIPTS
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
    fun testUnauthenticatedRequestReturns401() {
        val (codeList, _) = executeHttp("GET", "/api/v1/vendor-delivery-receipts")
        assertEquals(401, codeList)

        val (codePost, _) = executeHttp("POST", "/api/v1/vendor-delivery-receipts", body = "{}")
        assertEquals(401, codePost)

        val (codeAccept, _) = executeHttp("POST", "/api/v1/vendor-delivery-receipts/vdr_123/accept", body = "{}")
        assertEquals(401, codeAccept)
    }

    @Test
    fun testCustomerOrAffiliateRoleIsForbiddenWith403() {
        val customerToken = generateToken(userId = "cust_1", role = UserRole.CUSTOMER, permissions = setOf(UserPermission.READ_OWN_PROFILE))
        val affiliateToken = generateToken(userId = "aff_1", role = UserRole.AFFILIATE, permissions = setOf(UserPermission.READ_OWN_AFFILIATE))

        val (codeCustList, _) = executeHttp("GET", "/api/v1/vendor-delivery-receipts", token = customerToken)
        assertEquals(403, codeCustList)

        val (codeAffList, _) = executeHttp("GET", "/api/v1/vendor-delivery-receipts", token = affiliateToken)
        assertEquals(403, codeAffList)

        val (codeCustPost, _) = executeHttp("POST", "/api/v1/vendor-delivery-receipts", body = "{}", token = customerToken)
        assertEquals(403, codeCustPost)
    }

    @Test
    fun testStaffRoleCannotAcceptReceiptsRequiringAdminOrManagerRole() {
        val staffToken = generateToken(userId = "staff_1", role = UserRole.STAFF, permissions = setOf(UserPermission.READ_VENDOR_RECEIPTS, UserPermission.MANAGE_VENDOR_RECEIPTS))

        val (codeAccept, _) = executeHttp("POST", "/api/v1/vendor-delivery-receipts/vdr_123/accept", body = "{}", token = staffToken)
        assertEquals(403, codeAccept)
    }

    @Test
    fun testTamperedTokenReturns401() {
        val validToken = generateToken()
        val tamperedToken = validToken.substring(0, validToken.length - 5) + "abcde"

        val (statusCode, _) = executeHttp("GET", "/api/v1/vendor-delivery-receipts", token = tamperedToken)
        assertEquals(401, statusCode)
    }
}

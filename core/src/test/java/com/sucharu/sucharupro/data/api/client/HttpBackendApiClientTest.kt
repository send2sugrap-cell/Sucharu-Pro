package com.sucharu.sucharupro.data.api.client

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.auth.model.*
import com.sucharu.sucharupro.data.composition.AppRuntimeMode
import com.sucharu.sucharupro.data.composition.ProductionRuntimeComposition
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.util.concurrent.Executors

/**
 * Targeted Unit and End-to-End Live HTTP Test Suite for [HttpBackendApiClient] (INFRA-05 Step 02).
 */
class HttpBackendApiClientTest {

    private var serverPort: Int = 0
    private var httpServer: HttpServer? = null
    private val executor = Executors.newFixedThreadPool(2)
    private val tokenStorage: AuthTokenStorage = InMemoryAuthTokenStorage()
    private lateinit var client: HttpBackendApiClient

    private var lastReceivedAuthHeader: String? = null
    private var lastReceivedCorrelationId: String? = null

    @Before
    fun setUp() {
        val socket = ServerSocket(0)
        serverPort = socket.localPort
        socket.close()

        val server = HttpServer.create(InetSocketAddress("127.0.0.1", serverPort), 0)

        server.createContext("/health/live", object : HttpHandler {
            override fun handle(exchange: HttpExchange) {
                val json = """{"status":"UP","live":true}"""
                sendJsonResponse(exchange, 200, json)
            }
        })

        server.createContext("/health/ready", object : HttpHandler {
            override fun handle(exchange: HttpExchange) {
                val json = """{"isLive":true,"isReady":true,"databaseName":"sucharu_prod"}"""
                sendJsonResponse(exchange, 200, json)
            }
        })

        server.createContext("/api/v1/public/company", object : HttpHandler {
            override fun handle(exchange: HttpExchange) {
                val json = """{"success":true,"data":{"companyName":"Sucharu Graphics Pro","supportEmail":"support@sucharu.pro"},"correlationId":"corr-company-1"}"""
                sendJsonResponse(exchange, 200, json)
            }
        })

        server.createContext("/api/v1/auth/login", object : HttpHandler {
            override fun handle(exchange: HttpExchange) {
                val json = """{"success":true,"data":{"accessToken":"test_jwt_access_token_123","refreshToken":"test_jwt_refresh_token_456","expiresInSeconds":3600},"correlationId":"corr-login-1"}"""
                sendJsonResponse(exchange, 200, json)
            }
        })

        server.createContext("/api/v1/auth/me", object : HttpHandler {
            override fun handle(exchange: HttpExchange) {
                lastReceivedAuthHeader = exchange.requestHeaders.getFirst("Authorization")
                lastReceivedCorrelationId = exchange.requestHeaders.getFirst("X-Correlation-ID")
                if (lastReceivedAuthHeader == "Bearer test_jwt_access_token_123") {
                    val json = """{"success":true,"data":{"userId":"USER-001","projectId":"PROJECT-001","username":"testuser","role":"CUSTOMER","accountStatus":"ACTIVE"},"correlationId":"corr-me-1"}"""
                    sendJsonResponse(exchange, 200, json)
                } else {
                    val json = """{"success":false,"errorCode":"UNAUTHENTICATED","message":"Missing or invalid bearer token","correlationId":"corr-me-err"}"""
                    sendJsonResponse(exchange, 401, json)
                }
            }
        })

        server.createContext("/api/v1/auth/register", object : HttpHandler {
            override fun handle(exchange: HttpExchange) {
                val json = """{"success":false,"errorCode":"VALIDATION_ERROR","message":"Invalid payload format"}"""
                sendJsonResponse(exchange, 400, json)
            }
        })

        server.createContext("/api/v1/customer/profile", object : HttpHandler {
            override fun handle(exchange: HttpExchange) {
                val json = """{"success":false,"errorCode":"FORBIDDEN","message":"Access denied"}"""
                sendJsonResponse(exchange, 403, json)
            }
        })

        server.createContext("/api/v1/customer/orders/NON_EXISTENT", object : HttpHandler {
            override fun handle(exchange: HttpExchange) {
                val json = """{"success":false,"errorCode":"NOT_FOUND","message":"Resource not found"}"""
                sendJsonResponse(exchange, 404, json)
            }
        })

        server.createContext("/api/v1/affiliate/profile", object : HttpHandler {
            override fun handle(exchange: HttpExchange) {
                val json = """{"success":false,"errorCode":"INTERNAL_ERROR","message":"Server error occurred"}"""
                sendJsonResponse(exchange, 500, json)
            }
        })

        server.executor = executor
        server.start()
        httpServer = server

        client = HttpBackendApiClient(
            baseUrl = "http://127.0.0.1:$serverPort",
            tokenStorage = tokenStorage
        )
    }

    @After
    fun tearDown() {
        httpServer?.stop(0)
        executor.shutdown()
    }

    private fun sendJsonResponse(exchange: HttpExchange, statusCode: Int, json: String) {
        val bytes = json.toByteArray(Charsets.UTF_8)
        exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
        exchange.sendResponseHeaders(statusCode, bytes.size.toLong())
        val os: OutputStream = exchange.responseBody
        os.write(bytes)
        os.close()
    }

    @Test
    fun testCheckHealthLive_returnsSuccess() = runBlocking {
        val result = client.checkHealthLive()
        assertTrue("Health live check must be success", result.isSuccess)
        val data = result.getOrNull()
        assertNotNull(data)
        assertEquals("UP", data!!["status"])
    }

    @Test
    fun testCheckHealthReady_returnsSuccess() = runBlocking {
        val result = client.checkHealthReady()
        assertTrue("Health ready check must be success", result.isSuccess)
        val status = result.getOrNull()
        assertNotNull(status)
        assertTrue(status!!.isReady)
    }

    @Test
    fun testGetPublicCompanyInfo_returnsSuccess() = runBlocking {
        val result = client.getPublicCompanyInfo()
        assertTrue("Public company info check must be success", result.isSuccess)
        val info = result.getOrNull()
        assertNotNull(info)
        assertEquals("Sucharu Graphics Pro", info!!.companyName)
    }

    @Test
    fun testLoginAndAuthenticatedProfile_attachesBearerToken() = runBlocking {
        val loginResult = client.login(LoginRequestDto(identifier = "user@sucharu.pro", password = "password123"))
        assertTrue("Login must be successful", loginResult.isSuccess)
        val authData = loginResult.getOrNull()
        assertNotNull(authData)
        assertEquals("test_jwt_access_token_123", authData!!.accessToken)
        assertEquals("test_jwt_access_token_123", tokenStorage.getToken())

        val meResult = client.getMyProfile()
        assertTrue("Get my profile with auth token must succeed", meResult.isSuccess)
        val profile = meResult.getOrNull()
        assertNotNull(profile)
        assertEquals("USER-001", profile!!.userId)
        assertEquals("Bearer test_jwt_access_token_123", lastReceivedAuthHeader)
        assertNotNull(lastReceivedCorrelationId)
    }

    @Test
    fun testUnauthenticatedRequest_returns401Error() = runBlocking {
        tokenStorage.clearToken()
        val meResult = client.getMyProfile()
        assertTrue("Request without token must fail with error", meResult.isError)
        val err = (meResult as ApiResult.Error).errorResponse
        assertEquals(ErrorCode.UNAUTHENTICATED, err.errorCode)
    }

    @Test
    fun testErrorMapping_handles400_403_404_500() = runBlocking {
        val clientBadPort = HttpBackendApiClient(baseUrl = "http://127.0.0.1:$serverPort")
        
        // 400 test
        val res400 = clientBadPort.register(RegisterRequestDto(displayName = "Test User", email = "test@domain.com", password = "p"))
        assertTrue(res400.isError)
        assertEquals(ErrorCode.VALIDATION_ERROR, (res400 as ApiResult.Error).errorResponse.errorCode)

        // 403 test
        val res403 = clientBadPort.getCustomerProfile()
        assertTrue(res403.isError)
        assertEquals(ErrorCode.FORBIDDEN, (res403 as ApiResult.Error).errorResponse.errorCode)

        // 404 test
        val res404 = clientBadPort.getCustomerOrderDetail("NON_EXISTENT")
        assertTrue(res404.isError)
        assertEquals(ErrorCode.NOT_FOUND, (res404 as ApiResult.Error).errorResponse.errorCode)

        // 500 test
        val res500 = clientBadPort.getAffiliateProfile()
        assertTrue(res500.isError)
        assertEquals(ErrorCode.INTERNAL_ERROR, (res500 as ApiResult.Error).errorResponse.errorCode)
    }

    @Test
    fun testNetworkTimeoutOrConnectionFailure_returnsStructuredError() = runBlocking {
        val deadPort = 65432
        val deadClient = HttpBackendApiClient(
            baseUrl = "http://127.0.0.1:$deadPort",
            connectTimeoutMs = 100,
            readTimeoutMs = 100
        )
        val result = deadClient.checkHealthLive()
        assertTrue("Dead connection must return error result", result.isError)
        val err = (result as ApiResult.Error).errorResponse
        assertEquals(ErrorCode.DATABASE_UNAVAILABLE, err.errorCode)
        assertTrue(err.message.contains("Network Transport Error"))
    }

    @Test
    fun testProductionRuntimeComposition_createsRealHttpClient() {
        val prodComposition = ProductionRuntimeComposition(apiGatewayUrl = "http://127.0.0.1:$serverPort")
        assertEquals(AppRuntimeMode.PRODUCTION, prodComposition.mode)

        val sessionManager = prodComposition.createSessionManager()
        assertNotNull(sessionManager)
    }
}

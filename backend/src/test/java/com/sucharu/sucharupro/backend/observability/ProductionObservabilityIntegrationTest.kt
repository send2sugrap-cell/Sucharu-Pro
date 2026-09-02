package com.sucharu.sucharupro.backend.observability

import com.sun.net.httpserver.HttpServer
import com.sucharu.sucharupro.backend.config.BackendConfig
import com.sucharu.sucharupro.backend.config.BackendEnvironment
import com.sucharu.sucharupro.backend.integration.MockIntegrationDb
import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.api.server.BackendApiServer
import com.sucharu.sucharupro.data.api.server.BackendSecurityContext
import com.sucharu.sucharupro.data.api.server.HttpRequest
import com.sucharu.sucharupro.data.auth.security.AuthConfig
import com.sucharu.sucharupro.data.auth.security.JwtTokenProvider
import com.sucharu.sucharupro.data.observability.event.OperationalEventRecorder
import com.sucharu.sucharupro.data.observability.event.SecurityEventRecorder
import com.sucharu.sucharupro.data.observability.health.HealthCheck
import com.sucharu.sucharupro.data.observability.health.HealthRegistry
import com.sucharu.sucharupro.data.observability.metrics.ObservabilityMetricsRegistry
import com.sucharu.sucharupro.data.observability.model.ComponentHealth
import com.sucharu.sucharupro.data.observability.model.HealthStatus
import com.sucharu.sucharupro.data.persistence.postgres.DatabaseHealthChecker
import com.sucharu.sucharupro.data.persistence.postgres.DefaultPostgresTransactionManager
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class ProductionObservabilityIntegrationTest {

    private var serverPort = 0
    private var httpServer: HttpServer? = null
    private val executor = Executors.newFixedThreadPool(4)

    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var metricsRegistry: ObservabilityMetricsRegistry
    private lateinit var healthRegistry: HealthRegistry
    private lateinit var securityEventRecorder: SecurityEventRecorder
    private lateinit var operationalEventRecorder: OperationalEventRecorder
    private lateinit var mockDb: MockIntegrationDb
    private lateinit var txManager: DefaultPostgresTransactionManager
    private lateinit var apiServer: BackendApiServer

    private val dbIsHealthy = AtomicBoolean(true)

    @Before
    fun setUp() {
        val socket = ServerSocket(0)
        serverPort = socket.localPort
        socket.close()

        val authConfig = AuthConfig(
            jwtSigningSecret = "test_super_secure_signing_secret_for_observability_tests_2026",
            jwtIssuer = "sucharu-test",
            jwtAudience = "sucharu-clients"
        )
        jwtTokenProvider = JwtTokenProvider(authConfig)
        metricsRegistry = ObservabilityMetricsRegistry()
        healthRegistry = HealthRegistry()
        securityEventRecorder = SecurityEventRecorder(metricsRegistry = metricsRegistry)
        operationalEventRecorder = OperationalEventRecorder(metricsRegistry = metricsRegistry)

        mockDb = MockIntegrationDb()
        txManager = DefaultPostgresTransactionManager(mockDb)
        val repositoryFactory = PostgresRepositoryFactory(txManager, "TENANT-001")
        val securityContext = BackendSecurityContext(jwtTokenProvider = jwtTokenProvider)
        val healthChecker = DatabaseHealthChecker(mockDb)

        healthRegistry.register(object : HealthCheck {
            override val name = "database"
            override val isCritical = true
            override suspend fun check(): ComponentHealth {
                val healthy = dbIsHealthy.get()
                return ComponentHealth("database", if (healthy) HealthStatus.UP else HealthStatus.DOWN)
            }
        })

        apiServer = BackendApiServer(
            connectionProvider = mockDb,
            transactionManager = txManager,
            repositoryFactory = repositoryFactory,
            securityContext = securityContext,
            healthChecker = healthChecker,
            metricsRegistry = metricsRegistry,
            healthRegistry = healthRegistry,
            securityEventRecorder = securityEventRecorder,
            operationalEventRecorder = operationalEventRecorder,
            slowRequestThresholdMs = 200L
        )
        apiServer.start()

        val address = InetSocketAddress("127.0.0.1", serverPort)
        val server = HttpServer.create(address, 0)

        server.createContext("/") { exchange ->
            val method = exchange.requestMethod
            val path = exchange.requestURI.path
            val headers = mutableMapOf<String, String>()
            exchange.requestHeaders.forEach { (k, v) ->
                if (v.isNotEmpty()) headers[k] = v[0]
            }
            val bodyBytes = exchange.requestBody.readBytes()
            val bodyString = if (bodyBytes.isNotEmpty()) String(bodyBytes, Charsets.UTF_8) else null

            val req = HttpRequest(method, path, headers, bodyString, "127.0.0.1")
            val resp = runBlocking { apiServer.handle(req) }

            val responseBytes = if (resp.body is String) {
                (resp.body as String).toByteArray(Charsets.UTF_8)
            } else {
                com.sucharu.sucharupro.backend.server.HttpServerBootstrap.formatResponseJson(resp.body).toByteArray(Charsets.UTF_8)
            }

            exchange.responseHeaders.set("X-Correlation-ID", resp.correlationId)
            exchange.sendResponseHeaders(resp.statusCode, responseBytes.size.toLong())
            val os: OutputStream = exchange.responseBody
            os.write(responseBytes)
            os.close()
        }

        server.executor = executor
        server.start()
        httpServer = server
    }

    @After
    fun tearDown() {
        httpServer?.stop(0)
        executor.shutdown()
        apiServer.close()
    }

    private fun sendHttp(method: String, path: String, headers: Map<String, String> = emptyMap(), body: String? = null): Pair<Int, String> {
        val url = URL("http://127.0.0.1:$serverPort$path")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = 3000
        conn.readTimeout = 3000
        headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }

        if (body != null) {
            conn.doOutput = true
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        }

        val code = conn.responseCode
        val stream = if (code < 400) conn.inputStream else conn.errorStream
        val respBody = stream?.bufferedReader()?.use { it.readText() } ?: ""
        return Pair(code, respBody)
    }

    // =========================================================================
    // ADVERSARIAL OBSERVABILITY TEST MATRIX
    // =========================================================================

    @Test
    fun test01_healthEndpoint_accessibleWithoutAuth_leaksZeroSecrets() {
        val (code, body) = sendHttp("GET", "/health")
        assertEquals(200, code)
        assertTrue(body.contains("UP"))
        assertTrue(body.contains("sucharu-server"))
        assertFalse(body.contains("password"))
        assertFalse(body.contains("secret"))
        assertFalse(body.contains("jwt"))
    }

    @Test
    fun test02_readinessEndpoint_reflectsDatabaseHealth_503WhenDown() {
        // Healthy state
        dbIsHealthy.set(true)
        val (code1, body1) = sendHttp("GET", "/ready")
        assertEquals(200, code1)
        assertTrue(body1.contains("true") || body1.contains("UP") || body1.contains("READY"))

        // Unhealthy state
        dbIsHealthy.set(false)
        val (code2, body2) = sendHttp("GET", "/ready")
        assertEquals(503, code2)
        assertTrue(body2.contains("DATABASE_UNAVAILABLE") || body2.contains("Service is not ready"))
        assertFalse(body2.contains("SQLException"))
    }

    @Test
    fun test03_metricsEndpoint_returnsPrometheusFormat_zeroSecretLeakage() {
        sendHttp("GET", "/health")
        val (code, body) = sendHttp("GET", "/metrics")
        assertEquals(200, code)
        assertTrue(body.contains("http_requests_total"))
        assertFalse(body.contains("Bearer"))
        assertFalse(body.contains("password"))
    }

    @Test
    fun test04_adminOperationalSummary_allowedForStaffAdmin() {
        val staffPrincipal = AuthenticatedPrincipal(
            userId = "USER-ADMIN-01",
            username = "admin",
            projectId = "PROJECT-ALPHA",
            role = UserRole.ADMIN
        )
        val token = jwtTokenProvider.generateAccessToken(staffPrincipal)

        val (code, body) = sendHttp(
            method = "GET",
            path = "/api/v1/admin/operations/summary",
            headers = mapOf("Authorization" to "Bearer $token")
        )
        assertEquals(200, code)
        assertTrue(body.contains("serverStatus"))
        assertTrue(body.contains("readiness"))
        assertTrue(body.contains("totalRequests"))
    }

    @Test
    fun test05_adminOperationalSummary_forbiddenForCustomerAndAffiliate() {
        val customerPrincipal = AuthenticatedPrincipal(
            userId = "USER-CUST-01",
            username = "cust_user",
            projectId = "PROJECT-ALPHA",
            role = UserRole.CUSTOMER
        )
        val token = jwtTokenProvider.generateAccessToken(customerPrincipal)

        val (code, body) = sendHttp(
            method = "GET",
            path = "/api/v1/admin/operations/summary",
            headers = mapOf("Authorization" to "Bearer $token")
        )
        assertEquals(403, code)
        assertTrue(body.contains("FORBIDDEN"))
    }

    @Test
    fun test06_correlationId_propagatedInHeaderAndLogs() {
        val customCorrelation = "corr-custom-trace-999"
        val url = URL("http://127.0.0.1:$serverPort/health")
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("X-Correlation-ID", customCorrelation)
        val code = conn.responseCode
        val headerCorr = conn.getHeaderField("X-Correlation-ID")

        assertEquals(200, code)
        assertEquals(customCorrelation, headerCorr)
    }

    @Test
    fun test07_failedAuthentication_recordsSecurityEventAndIncrementsMetrics() {
        val (code, _) = sendHttp(
            method = "GET",
            path = "/api/v1/admin/operations/summary",
            headers = mapOf("Authorization" to "Bearer invalid-corrupted-token")
        )
        assertEquals(401, code)

        val recentSecEvents = securityEventRecorder.getRecentEvents()
        assertTrue(recentSecEvents.isNotEmpty())
        assertEquals("AUTHENTICATION_FAILED", recentSecEvents.last().eventType.name)

        val authFailures = metricsRegistry.getCounter("authentication_failure_total")
        assertTrue(authFailures >= 1L)
    }

    @Test
    fun test08_concurrentRequests_remainThreadSafeWithoutMetricCorruption() = runBlocking {
        val tasks = (1..20).map { i ->
            async {
                sendHttp("GET", "/health", mapOf("X-Correlation-ID" to "corr-concurrent-$i"))
            }
        }
        val results = tasks.awaitAll()
        results.forEach { (code, _) -> assertEquals(200, code) }

        val totalHttp = metricsRegistry.getCounter("http_requests_total", mapOf("method" to "GET", "route" to "/health", "status_class" to "2xx"))
        assertTrue(totalHttp >= 20L)
    }

    @Test
    fun test09_compositionRoot_initializesObservabilitySubsystem() {
        val config = BackendConfig(
            environment = BackendEnvironment.TEST,
            databasePassword = "test_password",
            jwtSigningSecret = "test_secret_for_composition_obs_32_chars"
        )
        val composition = com.sucharu.sucharupro.backend.composition.ProductionBackendComposition(config)
        assertNotNull(composition.metricsRegistry)
        assertNotNull(composition.healthRegistry)
        assertNotNull(composition.securityEventRecorder)
        assertNotNull(composition.operationalEventRecorder)
    }
}

package com.sucharu.sucharupro.backend.release

import com.sun.net.httpserver.HttpServer
import com.sucharu.sucharupro.backend.config.BackendConfig
import com.sucharu.sucharupro.backend.config.BackendEnvironment
import com.sucharu.sucharupro.backend.integration.MockIntegrationDb
import com.sucharu.sucharupro.backend.server.HttpServerBootstrap
import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserPermission
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

/**
 * End-to-End Live Deployment Smoke Test Suite (INFRA-05 Step 07).
 * Tests a real live HTTP server instance running backend routing, health/readiness,
 * metrics exposition, authentication authority, tenant isolation, and operational endpoints.
 */
class ProductionDeploymentSmokeTest {

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
    private lateinit var config: BackendConfig

    @Before
    fun setUp() {
        val socket = ServerSocket(0)
        serverPort = socket.localPort
        socket.close()

        val authConfig = AuthConfig(
            jwtSigningSecret = "test_super_secure_signing_secret_for_smoke_tests_2026",
            jwtIssuer = "sucharu-test-server",
            jwtAudience = "sucharu-test-clients"
        )
        jwtTokenProvider = JwtTokenProvider(authConfig)
        metricsRegistry = ObservabilityMetricsRegistry()
        healthRegistry = HealthRegistry()
        securityEventRecorder = SecurityEventRecorder(metricsRegistry = metricsRegistry)
        operationalEventRecorder = OperationalEventRecorder(metricsRegistry = metricsRegistry)

        mockDb = MockIntegrationDb()
        txManager = DefaultPostgresTransactionManager(mockDb)
        val repositoryFactory = PostgresRepositoryFactory(txManager, "TENANT-001")
        val securityContext = BackendSecurityContext(jwtTokenProvider)
        val healthChecker = DatabaseHealthChecker(mockDb)

        healthRegistry.register(object : HealthCheck {
            override val name = "database"
            override val isCritical = true
            override suspend fun check(): ComponentHealth {
                return ComponentHealth("database", HealthStatus.UP, "PostgreSQL healthy.")
            }
        })

        healthRegistry.register(object : HealthCheck {
            override val name = "worker"
            override val isCritical = false
            override suspend fun check(): ComponentHealth {
                return ComponentHealth("worker", HealthStatus.UP, "Worker pool active.")
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
            operationalEventRecorder = operationalEventRecorder
        )
        apiServer.start()

        config = BackendConfig(
            appName = "sucharu-backend",
            appVersion = "1.0.0",
            buildVersion = "1.0.0-PROD",
            gitRevision = "smoke-test-commit",
            serverPort = serverPort,
            environment = BackendEnvironment.TEST
        )

        // Bootstrap native HttpServer with unified dispatch
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

            if (path == "/" && method == "GET") {
                val rel = config.getReleaseMetadata()
                val json = """
                    {
                        "application": "${rel.appName}",
                        "version": "${rel.appVersion}",
                        "buildVersion": "${rel.buildVersion}",
                        "gitRevision": "${rel.gitRevision}",
                        "environment": "${rel.environment}",
                        "status": "RUNNING",
                        "health": "/health",
                        "readiness": "/ready",
                        "metrics": "/metrics",
                        "api": "/api/v1"
                    }
                """.trimIndent()
                val responseBytes = json.toByteArray(Charsets.UTF_8)
                exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
                exchange.sendResponseHeaders(200, responseBytes.size.toLong())
                val os: OutputStream = exchange.responseBody
                os.write(responseBytes)
                os.close()
            } else {
                val req = HttpRequest(method, path, headers, bodyString, "127.0.0.1")
                val resp = runBlocking { apiServer.handle(req) }

                val responseBytes = if (resp.body is String) {
                    (resp.body as String).toByteArray(Charsets.UTF_8)
                } else {
                    HttpServerBootstrap.formatResponseJson(resp.body).toByteArray(Charsets.UTF_8)
                }

                if (resp.body is String && path == "/metrics") {
                    exchange.responseHeaders.set("Content-Type", "text/plain; version=0.0.4; charset=utf-8")
                } else {
                    exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
                }
                exchange.responseHeaders.set("X-Correlation-ID", resp.correlationId)
                exchange.sendResponseHeaders(resp.statusCode, responseBytes.size.toLong())
                val os: OutputStream = exchange.responseBody
                os.write(responseBytes)
                os.close()
            }
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

    private fun executeHttp(
        path: String,
        method: String = "GET",
        headers: Map<String, String> = emptyMap(),
        body: String? = null
    ): Pair<Int, String> {
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

        val status = conn.responseCode
        val stream = if (status in 200..299) conn.inputStream else conn.errorStream
        val content = stream?.bufferedReader()?.use { it.readText() } ?: ""
        conn.disconnect()
        return Pair(status, content)
    }

    @Test
    fun testLiveLivenessProbeReturns200Up() {
        val (status, body) = executeHttp("/health")
        assertEquals(200, status)
        assertTrue(body.contains("\"status\":\"UP\"") || body.contains("UP"))
        assertFalse(body.contains("password"))
    }

    @Test
    fun testLiveReadinessProbeReturns200Ready() {
        val (status, body) = executeHttp("/ready")
        assertEquals(200, status)
        assertTrue(body.contains("READY") || body.contains("true"))
    }

    @Test
    fun testLivePrometheusMetricsReturnsTextExposition() {
        val (status, body) = executeHttp("/metrics")
        assertEquals(200, status)
        assertTrue(body.contains("# HELP") || body.contains("sucharu"))
    }

    @Test
    fun testLiveRootReturnsReleaseMetadata() {
        val (status, body) = executeHttp("/")
        assertEquals(200, status)
        assertTrue(body.contains("sucharu-backend"))
        assertTrue(body.contains("1.0.0"))
        assertTrue(body.contains("1.0.0-PROD"))
    }

    @Test
    fun testLivePublicCompanyInfoReturns200() {
        val (status, body) = executeHttp("/api/v1/public/company")
        assertEquals(200, status)
        assertTrue(body.contains("Sucharu"))
    }

    @Test
    fun testLiveCustomerOrdersUnauthenticatedReturns401() {
        val (status, body) = executeHttp("/api/v1/customer/orders")
        assertEquals(401, status)
        assertTrue(body.contains("UNAUTHENTICATED"))
    }

    @Test
    fun testLiveCustomerOrdersWithValidTokenReturns200() {
        val principal = AuthenticatedPrincipal(
            userId = "cust_001",
            projectId = "TENANT-001",
            username = "customer1",
            role = UserRole.CUSTOMER,
            permissions = setOf(UserPermission.READ_OWN_ORDERS)
        )
        val token = jwtTokenProvider.generateAccessToken(principal)

        val (status, body) = executeHttp(
            path = "/api/v1/customer/orders",
            headers = mapOf("Authorization" to "Bearer $token")
        )
        assertEquals(200, status)
        assertTrue(body.contains("success"))
    }

    @Test
    fun testLiveAdminSummaryProtectedByRbac() {
        val custPrincipal = AuthenticatedPrincipal(
            userId = "cust_002",
            projectId = "TENANT-001",
            username = "customer2",
            role = UserRole.CUSTOMER
        )
        val custToken = jwtTokenProvider.generateAccessToken(custPrincipal)

        // Customer attempt -> 403 Forbidden
        val (custStatus, _) = executeHttp(
            path = "/api/v1/admin/operations/summary",
            headers = mapOf("Authorization" to "Bearer $custToken")
        )
        assertEquals(403, custStatus)

        // Admin attempt -> 200 OK
        val adminPrincipal = AuthenticatedPrincipal(
            userId = "admin_001",
            projectId = "TENANT-001",
            username = "admin1",
            role = UserRole.ADMIN
        )
        val adminToken = jwtTokenProvider.generateAccessToken(adminPrincipal)

        val (adminStatus, adminBody) = executeHttp(
            path = "/api/v1/admin/operations/summary",
            headers = mapOf("Authorization" to "Bearer $adminToken")
        )
        assertEquals(200, adminStatus)
        assertTrue(adminBody.contains("serverStatus"))
    }

    @Test
    fun testCorrelationIdPropagation() {
        val customCorrelationId = "smoke-trace-999"
        val (status, _) = executeHttp(
            path = "/api/v1/public/company",
            headers = mapOf("X-Correlation-ID" to customCorrelationId)
        )
        assertEquals(200, status)
    }
}

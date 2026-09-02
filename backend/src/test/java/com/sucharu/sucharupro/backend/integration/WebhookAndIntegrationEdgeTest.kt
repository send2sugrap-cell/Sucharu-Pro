package com.sucharu.sucharupro.backend.integration

import com.sun.net.httpserver.HttpServer
import com.sucharu.sucharupro.backend.composition.ProductionBackendComposition
import com.sucharu.sucharupro.backend.config.BackendConfig
import com.sucharu.sucharupro.backend.config.BackendEnvironment
import com.sucharu.sucharupro.data.api.model.ApiErrorResponse
import com.sucharu.sucharupro.data.api.model.ApiSuccessResponse
import com.sucharu.sucharupro.data.api.server.BackendApiServer
import com.sucharu.sucharupro.data.api.server.HttpRequest
import com.sucharu.sucharupro.data.integration.client.DefaultIntegrationHttpClient
import com.sucharu.sucharupro.data.integration.model.ExternalIntegration
import com.sucharu.sucharupro.data.integration.model.IntegrationRequest
import com.sucharu.sucharupro.data.integration.model.IntegrationStatus
import com.sucharu.sucharupro.data.integration.postgres.PostgresIntegrationAuditRepository
import com.sucharu.sucharupro.data.integration.postgres.PostgresIntegrationRepository
import com.sucharu.sucharupro.data.integration.postgres.PostgresWebhookRepository
import com.sucharu.sucharupro.data.integration.resilience.IntegrationCircuitBreaker
import com.sucharu.sucharupro.data.integration.resilience.IntegrationRateLimiter
import com.sucharu.sucharupro.data.integration.security.DefaultIntegrationSecretProvider
import com.sucharu.sucharupro.data.integration.security.HmacSha256SignatureVerifier
import com.sucharu.sucharupro.data.integration.security.SsrfProtectionValidator
import com.sucharu.sucharupro.data.integration.service.WebhookIngressService
import com.sucharu.sucharupro.data.job.postgres.PostgresJobRepository
import com.sucharu.sucharupro.data.persistence.postgres.DefaultPostgresTransactionManager
import com.sucharu.sucharupro.data.persistence.postgres.PostgresConnectionProvider
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.event.consumer.orchestration.IntegrationType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.lang.reflect.Proxy
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.URI
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Timestamp
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class WebhookAndIntegrationEdgeTest {

    private lateinit var mockDb: MockIntegrationDb
    private lateinit var txManager: DefaultPostgresTransactionManager
    private lateinit var integrationRepo: PostgresIntegrationRepository
    private lateinit var webhookRepo: PostgresWebhookRepository
    private lateinit var auditRepo: PostgresIntegrationAuditRepository
    private lateinit var jobRepo: PostgresJobRepository
    private lateinit var secretProvider: DefaultIntegrationSecretProvider
    private lateinit var signatureVerifier: HmacSha256SignatureVerifier
    private lateinit var webhookService: WebhookIngressService
    private lateinit var apiServer: BackendApiServer
    private lateinit var httpServer: HttpServer
    private var serverPort: Int = 0

    private val tenantAlpha = TenantContext("PROJECT-ALPHA")
    private val tenantBeta = TenantContext("PROJECT-BETA")
    private val signingSecret = "whsec_test_signing_key_456"

    @Before
    fun setUp() {
        mockDb = MockIntegrationDb()
        txManager = DefaultPostgresTransactionManager(mockDb)

        integrationRepo = PostgresIntegrationRepository(txManager)
        webhookRepo = PostgresWebhookRepository(txManager)
        auditRepo = PostgresIntegrationAuditRepository(txManager)
        jobRepo = PostgresJobRepository(txManager)

        secretProvider = DefaultIntegrationSecretProvider(mapOf("integration.n8n.signing_key" to signingSecret))
        signatureVerifier = HmacSha256SignatureVerifier()

        webhookService = WebhookIngressService(
            integrationRepository = integrationRepo,
            webhookRepository = webhookRepo,
            auditRepository = auditRepo,
            secretProvider = secretProvider,
            signatureVerifier = signatureVerifier,
            jobRepository = jobRepo
        )

        // Seed integration for PROJECT-ALPHA
        runBlocking {
            integrationRepo.saveIntegration(
                ExternalIntegration(
                    integrationId = "INT-N8N-01",
                    projectId = "PROJECT-ALPHA",
                    provider = "n8n",
                    integrationType = IntegrationType.N8N,
                    status = IntegrationStatus.ACTIVE,
                    baseUrl = "https://automation.sucharu.internal",
                    configurationReference = "integration.n8n.signing_key"
                ),
                tenantAlpha
            )
        }

        apiServer = BackendApiServer(
            connectionProvider = mockDb,
            transactionManager = txManager,
            repositoryFactory = PostgresRepositoryFactory(txManager),
            webhookIngressService = webhookService
        )
        apiServer.start()

        serverPort = findFreePort()
        httpServer = HttpServer.create(InetSocketAddress("127.0.0.1", serverPort), 0)
        httpServer.createContext("/api") { exchange ->
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

            val respJson = when (val b = resp.body) {
                is ApiSuccessResponse<*> -> {
                    val map = b.data as? Map<*, *>
                    if (map != null) {
                        """{"status":"${map["status"]}","eventId":"${map["eventId"]}"}"""
                    } else {
                        """{"data":"${b.data}"}"""
                    }
                }
                is ApiErrorResponse -> """{"errorCode":"${b.errorCode}","message":"${b.message}"}"""
                else -> b.toString()
            }

            val respBytes = respJson.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
            exchange.responseHeaders.set("X-Correlation-ID", resp.correlationId)
            exchange.sendResponseHeaders(resp.statusCode, respBytes.size.toLong())
            exchange.responseBody.use { it.write(respBytes) }
        }
        httpServer.start()
    }

    @After
    fun tearDown() {
        try {
            httpServer.stop(0)
        } catch (_: Exception) {}
        try {
            apiServer.close()
        } catch (_: Exception) {}
    }

    private fun findFreePort(): Int {
        ServerSocket(0).use { return it.localPort }
    }

    private fun computeHmacSignature(payload: String, secret: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(payload.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    private fun sendHttp(
        path: String,
        method: String = "POST",
        body: String? = null,
        headers: Map<String, String> = emptyMap()
    ): Pair<Int, String> {
        val url = URI("http://127.0.0.1:$serverPort$path").toURL()
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        conn.useCaches = false
        headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }

        if (body != null) {
            conn.doOutput = true
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }
        }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else (conn.errorStream ?: conn.inputStream)
        val respBody = stream?.let { BufferedReader(InputStreamReader(it, Charsets.UTF_8)).readText() } ?: ""
        conn.disconnect()
        return Pair(code, respBody)
    }

    @Test
    fun test01_validWebhook_executesSuccessfullyOverHttp() {
        val payload = """{"event":"order.placed","orderId":"ORD-101","total":5000}"""
        val sig = computeHmacSignature(payload, signingSecret)

        val (code, body) = sendHttp(
            path = "/api/v1/webhooks/n8n/INT-N8N-01",
            method = "POST",
            body = payload,
            headers = mapOf(
                "Content-Type" to "application/json",
                "X-Webhook-Signature" to "sha256=$sig",
                "X-Event-ID" to "EVT-N8N-101"
            )
        )

        assertEquals(200, code)
        assertTrue(body.contains("ACCEPTED"))

        // Verify background job was enqueued with authoritative tenant
        val job = mockDb.backgroundJobs.find { it["job_type"] == "webhook.process" }
        assertNotNull(job)
        assertEquals("PROJECT-ALPHA", job?.get("project_id"))
    }

    @Test
    fun test02_attack1_webhookTenantSpoofing_boundStrictlyToAuthoritativeIntegrationTenant() {
        // Attacker injects projectId="PROJECT-BETA" into payload
        val maliciousPayload = """{"event":"order.placed","projectId":"PROJECT-BETA","orderId":"ORD-HACK"}"""
        val sig = computeHmacSignature(maliciousPayload, signingSecret)

        val (code, _) = sendHttp(
            path = "/api/v1/webhooks/n8n/INT-N8N-01",
            method = "POST",
            body = maliciousPayload,
            headers = mapOf(
                "Content-Type" to "application/json",
                "X-Webhook-Signature" to "sha256=$sig",
                "X-Event-ID" to "EVT-SPOOF-01"
            )
        )

        assertEquals(200, code)

        // The background job and webhook event are bound to PROJECT-ALPHA from integration record, NOT payload PROJECT-BETA
        val job = mockDb.backgroundJobs.find { it["idempotency_key"] == "webhook-EVT-SPOOF-01" }
        assertNotNull(job)
        assertEquals("PROJECT-ALPHA", job?.get("project_id"))
        assertNotEquals("PROJECT-BETA", job?.get("project_id"))
    }

    @Test
    fun test03_attack2_invalidWebhookSignature_returns401() {
        val payload = """{"event":"order.placed"}"""
        val invalidSig = "sha256=0000000000000000000000000000000000000000000000000000000000000000"

        val (code, body) = sendHttp(
            path = "/api/v1/webhooks/n8n/INT-N8N-01",
            method = "POST",
            body = payload,
            headers = mapOf(
                "Content-Type" to "application/json",
                "X-Webhook-Signature" to invalidSig
            )
        )

        assertEquals(401, code)
        assertTrue(body.contains("Invalid webhook signature"))
    }

    @Test
    fun test04_attack3_webhookReplayDuplicateDelivery_acknowledgedWithoutDoubleEnqueue() {
        val payload = """{"event":"order.placed","orderId":"ORD-REPLAY"}"""
        val sig = computeHmacSignature(payload, signingSecret)
        val headers = mapOf(
            "Content-Type" to "application/json",
            "X-Webhook-Signature" to "sha256=$sig",
            "X-Event-ID" to "EVT-REPLAY-99"
        )

        // 1st delivery
        val (code1, body1) = sendHttp("/api/v1/webhooks/n8n/INT-N8N-01", "POST", payload, headers)
        assertEquals(200, code1)
        assertTrue(body1.contains("ACCEPTED"))
        val countAfterFirst = mockDb.backgroundJobs.size

        // 2nd duplicate delivery
        val (code2, body2) = sendHttp("/api/v1/webhooks/n8n/INT-N8N-01", "POST", payload, headers)
        assertEquals(200, code2)
        assertTrue(body2.contains("DUPLICATE_IGNORED"))
        assertEquals(countAfterFirst, mockDb.backgroundJobs.size) // No second job enqueued!
    }

    @Test
    fun test05_attack4_crossTenantIntegrationAccess_isolated() {
        runBlocking {
            // Tenant Beta cannot access Tenant Alpha's integration record
            val betaFetch = integrationRepo.getIntegrationById("INT-N8N-01", tenantBeta)
            assertNull(betaFetch)
        }
    }

    @Test
    fun test06_attack5And6And7_outboundSsrfProtection() {
        val httpClient = DefaultIntegrationHttpClient(
            ssrfValidator = SsrfProtectionValidator()
        )

        runBlocking {
            // Attack 5: Loopback
            val resp1 = httpClient.execute(
                IntegrationRequest(
                    integrationId = "INT-01",
                    projectId = "PROJECT-ALPHA",
                    provider = "GENERIC",
                    url = "http://127.0.0.1/admin"
                )
            )
            assertFalse(resp1.isSuccess)
            assertTrue(resp1.sanitizedError?.contains("SSRF") == true)

            // Attack 7: Cloud metadata
            val resp2 = httpClient.execute(
                IntegrationRequest(
                    integrationId = "INT-01",
                    projectId = "PROJECT-ALPHA",
                    provider = "GENERIC",
                    url = "http://169.254.169.254/latest/meta-data"
                )
            )
            assertFalse(resp2.isSuccess)
            assertTrue(resp2.sanitizedError?.contains("SSRF") == true)
        }
    }

    @Test
    fun test07_attack8_zeroSecretLeakage_inAuditAndMetadata() {
        val payload = """{"event":"test"}"""
        val sig = computeHmacSignature(payload, signingSecret)

        sendHttp(
            path = "/api/v1/webhooks/n8n/INT-N8N-01",
            method = "POST",
            body = payload,
            headers = mapOf(
                "Content-Type" to "application/json",
                "X-Webhook-Signature" to "sha256=$sig",
                "X-Event-ID" to "EVT-AUDIT-01"
            )
        )

        runBlocking {
            val logs = auditRepo.listAuditLogs("INT-N8N-01", 10, tenantAlpha)
            assertFalse(logs.isEmpty())
            logs.forEach { log ->
                assertFalse("Audit log must not contain raw signing secret", log.sanitizedError?.contains(signingSecret) == true)
            }
        }
    }

    @Test
    fun test08_attack9_circuitBreaker_activatesOnProviderFailures() {
        val breaker = IntegrationCircuitBreaker(failureThreshold = 2, resetTimeoutMs = 5000L)
        assertTrue(breaker.allowRequest())

        breaker.recordFailure()
        breaker.recordFailure()

        assertFalse("Circuit must be OPEN after 2 failures", breaker.allowRequest())
    }

    @Test
    fun test09_attack10_rateLimiter_appliesBackoff() {
        val limiter = IntegrationRateLimiter(maxRequestsPerSecond = 5, burstCapacity = 5)
        limiter.applyRetryAfter(10L)
        assertFalse("Rate limiter must reject requests while under 429 backoff", limiter.tryAcquire())
    }

    @Test
    fun test10_productionComposition_wiresExternalIntegrationSubsystem() {
        val config = BackendConfig(
            serverPort = 9999,
            serverHost = "127.0.0.1",
            environment = BackendEnvironment.TEST,
            jwtSigningSecret = "sucharu_backend_integration_secret_test_2026_secure"
        )
        val composition = ProductionBackendComposition(config)

        assertNotNull(composition.integrationRepository)
        assertNotNull(composition.webhookRepository)
        assertNotNull(composition.integrationAuditRepository)
        assertNotNull(composition.integrationHttpClient)
        assertNotNull(composition.webhookSignatureVerifier)
        assertNotNull(composition.webhookIngressService)
        assertNotNull(composition.circuitBreakerRegistry)
        assertNotNull(composition.rateLimiterRegistry)
    }
}

// -------------------------------------------------------------
// In-Memory Transactional Mock Database for Integrations
// -------------------------------------------------------------

class MockIntegrationDb : PostgresConnectionProvider {
    val externalIntegrations = CopyOnWriteArrayList<MutableMap<String, Any?>>()
    val webhookEvents = CopyOnWriteArrayList<MutableMap<String, Any?>>()
    val integrationAuditLog = CopyOnWriteArrayList<MutableMap<String, Any?>>()
    val backgroundJobs = CopyOnWriteArrayList<MutableMap<String, Any?>>()
    val idempotencyKeys = ConcurrentHashMap<String, Boolean>()
    var currentSessionProjectId: String = ""
    private var isClosed = false

    override suspend fun acquireConnection(): Connection {
        return createMockConnection()
    }

    override suspend fun releaseConnection(connection: Connection) {
        currentSessionProjectId = ""
    }

    override fun getActiveConnectionCount(): Int = 0
    override fun getIdleConnectionCount(): Int = 1
    override fun getTotalAcquisitions(): Long = 1L
    override fun getAcquisitionFailureCount(): Long = 0L
    override suspend fun shutdownGracefully(drainTimeoutMs: Long) { isClosed = true }
    override fun close() { isClosed = true }

    private fun createMockConnection(): Connection {
        return Proxy.newProxyInstance(
            Connection::class.java.classLoader,
            arrayOf(Connection::class.java)
        ) { _, method, args ->
            val mArgs = args ?: emptyArray()
            when (method.name) {
                "prepareStatement" -> {
                    val sql = mArgs[0] as String
                    createMockPreparedStatement(sql)
                }
                "createStatement" -> createMockStatement()
                "setAutoCommit", "commit", "rollback", "close" -> null
                "isClosed" -> false
                "isValid" -> true
                else -> null
            }
        } as Connection
    }

    private fun createMockStatement(): Statement {
        return Proxy.newProxyInstance(
            Statement::class.java.classLoader,
            arrayOf(Statement::class.java)
        ) { _, method, args ->
            val mArgs = args ?: emptyArray()
            when (method.name) {
                "execute" -> {
                    val sql = mArgs[0] as String
                    if (sql.contains("set_config")) {
                        // handled
                    }
                    true
                }
                "close" -> null
                else -> null
            }
        } as Statement
    }

    private fun createMockPreparedStatement(sql: String): PreparedStatement {
        val params = mutableListOf<Any?>()

        return Proxy.newProxyInstance(
            PreparedStatement::class.java.classLoader,
            arrayOf(PreparedStatement::class.java)
        ) { _, method, args ->
            val mArgs = args ?: emptyArray()
            when (method.name) {
                "setString", "setInt", "setLong", "setBigDecimal", "setBoolean", "setTimestamp", "setObject" -> {
                    val idx = mArgs[0] as Int
                    val v = mArgs.getOrNull(1)
                    while (params.size < idx) params.add(null)
                    params[idx - 1] = v
                    null
                }
                "setNull" -> {
                    val idx = mArgs[0] as Int
                    while (params.size < idx) params.add(null)
                    params[idx - 1] = null
                    null
                }
                "execute" -> {
                    if (sql.contains("set_config")) {
                        currentSessionProjectId = params.getOrNull(0) as? String ?: ""
                    }
                    true
                }
                "executeUpdate" -> executeMockUpdate(sql, params)
                "executeQuery" -> createMockResultSet(sql, params)
                "close" -> null
                else -> null
            }
        } as PreparedStatement
    }

    private fun executeMockUpdate(sql: String, params: List<Any?>): Int {
        val upperSql = sql.uppercase()

        if (upperSql.contains("INSERT INTO EXTERNAL_INTEGRATIONS")) {
            val record = mutableMapOf<String, Any?>(
                "integration_id" to params.getOrNull(0),
                "project_id" to params.getOrNull(1),
                "provider" to params.getOrNull(2),
                "integration_type" to params.getOrNull(3),
                "status" to params.getOrNull(4),
                "base_url" to params.getOrNull(5),
                "configuration_reference" to params.getOrNull(6),
                "allowed_event_types" to params.getOrNull(7),
                "version" to params.getOrNull(8),
                "created_at" to params.getOrNull(9),
                "updated_at" to params.getOrNull(10),
                "last_successful_at" to params.getOrNull(11),
                "last_failure_at" to params.getOrNull(12)
            )
            externalIntegrations.removeIf { it["project_id"] == params.getOrNull(1) && it["integration_id"] == params.getOrNull(0) }
            externalIntegrations.add(record)
            return 1
        }

        if (upperSql.contains("INSERT INTO WEBHOOK_EVENTS")) {
            val eventId = params.getOrNull(0) as? String ?: ""
            val projectId = params.getOrNull(1) as? String ?: ""
            val provider = params.getOrNull(2) as? String ?: ""
            val integrationId = params.getOrNull(3) as? String ?: ""
            val externalEventId = params.getOrNull(4) as? String

            if (externalEventId != null) {
                val uniqueKey = "$projectId:$provider:$externalEventId"
                if (idempotencyKeys.putIfAbsent(uniqueKey, true) != null) {
                    return 0 // ON CONFLICT DO NOTHING
                }
            }

            val record = mutableMapOf<String, Any?>(
                "event_id" to eventId,
                "project_id" to projectId,
                "provider" to provider,
                "integration_id" to integrationId,
                "external_event_id" to externalEventId,
                "event_type" to params.getOrNull(5),
                "payload" to params.getOrNull(6),
                "payload_hash" to params.getOrNull(7),
                "headers" to params.getOrNull(8),
                "received_at" to params.getOrNull(9),
                "verified_at" to params.getOrNull(10),
                "status" to params.getOrNull(11),
                "attempt_count" to params.getOrNull(12),
                "correlation_id" to params.getOrNull(13),
                "causation_id" to params.getOrNull(14),
                "created_at" to params.getOrNull(15)
            )
            webhookEvents.add(record)
            return 1
        }

        if (upperSql.contains("INSERT INTO INTEGRATION_AUDIT_LOG")) {
            val record = mutableMapOf<String, Any?>(
                "audit_id" to params.getOrNull(0),
                "project_id" to params.getOrNull(1),
                "integration_id" to params.getOrNull(2),
                "provider" to params.getOrNull(3),
                "operation_type" to params.getOrNull(4),
                "direction" to params.getOrNull(5),
                "status" to params.getOrNull(6),
                "sanitized_error" to params.getOrNull(7),
                "duration_ms" to params.getOrNull(8),
                "correlation_id" to params.getOrNull(9),
                "job_id" to params.getOrNull(10),
                "created_at" to params.getOrNull(11)
            )
            integrationAuditLog.add(record)
            return 1
        }

        if (upperSql.contains("INSERT INTO BACKGROUND_JOBS")) {
            val jobId = params.getOrNull(0) as? String ?: ""
            val projectId = params.getOrNull(1) as? String ?: ""
            val jobType = params.getOrNull(2) as? String ?: ""
            val jobVersion = params.getOrNull(3) as? String ?: "v1"
            val triggerType = params.getOrNull(4) as? String ?: "EVENT"
            val priority = params.getOrNull(5) as? Int ?: 5
            val status = params.getOrNull(6) as? String ?: "QUEUED"
            val idempotencyKey = params.getOrNull(29) as? String

            if (idempotencyKey != null) {
                val uniqueKey = "$projectId:$idempotencyKey"
                if (idempotencyKeys.putIfAbsent(uniqueKey, true) != null) {
                    return 0 // ON CONFLICT DO NOTHING
                }
            }

            val record = mutableMapOf<String, Any?>(
                "job_id" to jobId,
                "project_id" to projectId,
                "job_type" to jobType,
                "job_version" to jobVersion,
                "trigger_type" to triggerType,
                "priority" to priority,
                "status" to status,
                "idempotency_key" to idempotencyKey
            )
            backgroundJobs.add(record)
            return 1
        }

        if (upperSql.contains("UPDATE WEBHOOK_EVENTS SET STATUS = ?")) {
            val status = params.getOrNull(0) as? String ?: "ENQUEUED"
            val projectId = params.getOrNull(1) as? String ?: ""
            val eventId = params.getOrNull(2) as? String ?: ""
            webhookEvents.find { it["project_id"] == projectId && it["event_id"] == eventId }?.let {
                it["status"] = status
            }
            return 1
        }

        return 1
    }

    private fun createMockResultSet(sql: String, params: List<Any?>): ResultSet {
        val upperSql = sql.uppercase()
        val rows = mutableListOf<Map<String, Any?>>()

        if (upperSql.contains("FROM EXTERNAL_INTEGRATIONS") && upperSql.contains("INTEGRATION_ID = ?")) {
            val projectId = params.getOrNull(0) as? String ?: ""
            val integrationId = params.getOrNull(1) as? String ?: ""
            val found = externalIntegrations.find { it["project_id"] == projectId && it["integration_id"] == integrationId }
            if (found != null) rows.add(found)
        } else if (upperSql.contains("FROM WEBHOOK_EVENTS") && upperSql.contains("EVENT_ID = ?")) {
            val projectId = params.getOrNull(0) as? String ?: ""
            val eventId = params.getOrNull(1) as? String ?: ""
            val found = webhookEvents.find { it["project_id"] == projectId && it["event_id"] == eventId }
            if (found != null) rows.add(found)
        } else if (upperSql.contains("FROM WEBHOOK_EVENTS") && upperSql.contains("EXTERNAL_EVENT_ID = ?")) {
            val projectId = params.getOrNull(0) as? String ?: ""
            val provider = params.getOrNull(1) as? String ?: ""
            val externalEventId = params.getOrNull(2) as? String ?: ""
            val found = webhookEvents.find { it["project_id"] == projectId && it["provider"] == provider && it["external_event_id"] == externalEventId }
            if (found != null) rows.add(found)
        } else if (upperSql.contains("FROM INTEGRATION_AUDIT_LOG")) {
            val projectId = params.getOrNull(0) as? String ?: ""
            val integrationId = params.getOrNull(1) as? String ?: ""
            val limit = (params.getOrNull(2) as? Number)?.toInt() ?: 50
            val matched = integrationAuditLog.filter { it["project_id"] == projectId && it["integration_id"] == integrationId }.take(limit)
            rows.addAll(matched)
        }

        var cursor = -1
        return Proxy.newProxyInstance(
            ResultSet::class.java.classLoader,
            arrayOf(ResultSet::class.java)
        ) { _, method, args ->
            val mArgs = args ?: emptyArray()
            when (method.name) {
                "next" -> {
                    cursor++
                    cursor < rows.size
                }
                "getString" -> {
                    val col = mArgs[0] as String
                    rows.getOrNull(cursor)?.get(col)?.toString()
                }
                "getInt" -> {
                    val col = mArgs[0] as String
                    (rows.getOrNull(cursor)?.get(col) as? Number)?.toInt() ?: 0
                }
                "getLong" -> {
                    val col = mArgs[0] as String
                    (rows.getOrNull(cursor)?.get(col) as? Number)?.toLong() ?: 0L
                }
                "getTimestamp" -> {
                    val col = mArgs[0] as String
                    rows.getOrNull(cursor)?.get(col) as? Timestamp
                }
                "close" -> null
                else -> null
            }
        } as ResultSet
    }
}

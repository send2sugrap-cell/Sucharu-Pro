package com.sucharu.sucharupro.data.observability

import com.sucharu.sucharupro.data.observability.correlation.CorrelationContext
import com.sucharu.sucharupro.data.observability.correlation.CorrelationIdGenerator
import com.sucharu.sucharupro.data.observability.event.OperationalEventRecorder
import com.sucharu.sucharupro.data.observability.event.SecurityEventRecorder
import com.sucharu.sucharupro.data.observability.health.HealthCheck
import com.sucharu.sucharupro.data.observability.health.HealthRegistry
import com.sucharu.sucharupro.data.observability.logging.LogSanitizer
import com.sucharu.sucharupro.data.observability.metrics.ObservabilityMetricsRegistry
import com.sucharu.sucharupro.data.observability.model.ComponentHealth
import com.sucharu.sucharupro.data.observability.model.HealthStatus
import com.sucharu.sucharupro.data.observability.model.OperationalEventType
import com.sucharu.sucharupro.data.observability.model.ReadinessStatus
import com.sucharu.sucharupro.data.observability.model.SecurityEventType
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ObservabilityRuntimeTest {

    @Test
    fun test01_logSanitizer_masksBearerTokensAndJwts() {
        val raw = "User authenticated with Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.doNotLeak"
        val sanitized = LogSanitizer.sanitize(raw)

        assertFalse(sanitized.contains("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"))
        assertTrue(sanitized.contains("Bearer [MASKED]"))
    }

    @Test
    fun test02_logSanitizer_masksPasswordsAndSecrets() {
        val raw = """{"username": "admin", "password": "superSecretPassword123", "apiKey": "live_key_9999"}"""
        val sanitized = LogSanitizer.sanitize(raw)

        assertFalse(sanitized.contains("superSecretPassword123"))
        assertFalse(sanitized.contains("live_key_9999"))
        assertTrue(sanitized.contains("[MASKED]"))
    }

    @Test
    fun test03_logSanitizer_maskSecretHelper() {
        assertEquals("sec_****1234", LogSanitizer.maskSecret("myLongSecretKey1234"))
        assertEquals("sec_****", LogSanitizer.maskSecret("short"))
        assertEquals("empty", LogSanitizer.maskSecret(""))
    }

    @Test
    fun test04_correlationIdGenerator_generatesSafeAndBoundedIds() {
        val id1 = CorrelationIdGenerator.generate("req")
        assertTrue(id1.startsWith("req-"))
        assertTrue(id1.length <= 64)

        val normalized = CorrelationIdGenerator.normalizeOrGenerate("custom-valid-id-123")
        assertEquals("custom-valid-id-123", normalized)

        // Oversized ID is regenerated
        val oversized = "a".repeat(100)
        val generated = CorrelationIdGenerator.normalizeOrGenerate(oversized)
        assertTrue(generated.startsWith("req-"))
        assertNotEquals(oversized, generated)

        // Malformed characters rejected
        val malformed = "req; DROP TABLE users;--"
        val safe = CorrelationIdGenerator.normalizeOrGenerate(malformed)
        assertTrue(safe.startsWith("req-"))
    }

    @Test
    fun test05_metricsRegistry_enforcesStrictLabelCardinality() {
        val registry = ObservabilityMetricsRegistry()

        // Forbidden high-cardinality tags (userId, orderId, tenantId) must be filtered out
        val tagsWithForbidden = mapOf(
            "method" to "GET",
            "route" to "/api/v1/orders",
            "user_id" to "USER-12345",
            "order_id" to "ORD-99999"
        )
        registry.increment("test_requests_total", 1, tagsWithForbidden)

        // Counter must be incremented under normalized key containing only allowed tags
        val allowedTags = mapOf("method" to "GET", "route" to "/api/v1/orders")
        val count = registry.getCounter("test_requests_total", allowedTags)
        assertEquals(1L, count)

        // Prometheus format verification
        val prometheus = registry.formatPrometheus()
        assertTrue(prometheus.contains("test_requests_total"))
        assertFalse(prometheus.contains("USER-12345"))
        assertFalse(prometheus.contains("ORD-99999"))
    }

    @Test
    fun test06_metricsRegistry_recordsHttpAndWorkerMetrics() {
        val registry = ObservabilityMetricsRegistry()

        registry.recordHttpRequest("GET", "/api/v1/customers/123e4567-e89b-12d3-a456-426614174000", 200, 45L)
        registry.recordHttpRequest("POST", "/api/v1/invoices", 500, 120L)
        registry.recordJobEnqueued("email.dispatch")
        registry.recordJobSucceeded("email.dispatch", 250L)

        val totalHttp = registry.getCounter("http_requests_total", mapOf("method" to "GET", "route" to "/api/v1/customers/:id", "status_class" to "2xx"))
        assertEquals(1L, totalHttp)

        val errors = registry.getCounter("http_errors_total", mapOf("method" to "POST", "route" to "/api/v1/invoices", "status_class" to "5xx"))
        assertEquals(1L, errors)

        val jobsEnqueued = registry.getCounter("jobs_enqueued_total", mapOf("job_type" to "email.dispatch"))
        assertEquals(1L, jobsEnqueued)
    }

    @Test
    fun test07_healthRegistry_evaluatesComponentHealthAndReadiness() = runBlocking {
        val registry = HealthRegistry(defaultTimeoutMs = 1000L)

        registry.register(object : HealthCheck {
            override val name = "database"
            override val isCritical = true
            override suspend fun check() = ComponentHealth("database", HealthStatus.UP)
        })

        registry.register(object : HealthCheck {
            override val name = "worker"
            override val isCritical = false
            override suspend fun check() = ComponentHealth("worker", HealthStatus.DEGRADED, "Slow worker")
        })

        assertEquals(ReadinessStatus.DEGRADED, registry.evaluateReadiness())

        val report = registry.getFullReport()
        assertEquals("DEGRADED", report["status"])
        assertTrue(report.containsKey("components"))
    }

    @Test
    fun test08_healthRegistry_timesOutGracefullyWithoutDeadlock() = runBlocking {
        val registry = HealthRegistry(defaultTimeoutMs = 100L)

        registry.register(object : HealthCheck {
            override val name = "hanging_service"
            override val isCritical = true
            override suspend fun check(): ComponentHealth {
                delay(500L)
                return ComponentHealth("hanging_service", HealthStatus.UP)
            }
        })

        val health = registry.checkComponent("hanging_service")
        assertEquals(HealthStatus.DEGRADED, health.status)
        assertTrue(health.message?.contains("timed out") == true)
    }

    @Test
    fun test09_securityAndOperationalEventRecorders_memoryBounded() {
        val metrics = ObservabilityMetricsRegistry()
        val secRecorder = SecurityEventRecorder(metricsRegistry = metrics, maxRetainedEvents = 10)
        val opRecorder = OperationalEventRecorder(metricsRegistry = metrics, maxRetainedEvents = 10)

        for (i in 1..25) {
            secRecorder.recordEvent(
                eventType = SecurityEventType.AUTHENTICATION_FAILED,
                correlationId = "corr-$i",
                component = "AuthService",
                reasonCode = "invalid_password"
            )
            opRecorder.recordEvent(
                eventType = OperationalEventType.SERVER_STARTED,
                correlationId = "corr-$i",
                component = "Server",
                summary = "Start event $i"
            )
        }

        // Memory bounded to maxRetainedEvents = 10
        assertEquals(10, secRecorder.getRecentEvents().size)
        assertEquals(10, opRecorder.getRecentEvents().size)

        // Metrics mirrored
        val authFailures = metrics.getCounter("authentication_failure_total", mapOf("reason" to "invalid_password"))
        assertEquals(25L, authFailures)
    }
}

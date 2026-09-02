package com.sucharu.sucharupro.backend.release

import com.sucharu.sucharupro.backend.config.BackendConfig
import com.sucharu.sucharupro.backend.config.BackendEnvironment
import com.sucharu.sucharupro.data.integration.resilience.IntegrationCircuitBreaker
import com.sucharu.sucharupro.data.integration.resilience.IntegrationRateLimiter
import com.sucharu.sucharupro.data.integration.security.HmacSha256SignatureVerifier
import com.sucharu.sucharupro.data.integration.security.SsrfProtectionValidator
import com.sucharu.sucharupro.data.observability.logging.LogSanitizer
import com.sucharu.sucharupro.data.observability.metrics.ObservabilityMetricsRegistry
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Authoritative Release Gate Enforcement Test (INFRA-05 Step 07).
 * Validates that all production release gates, security constraints, migration definitions,
 * resilience patterns, and observability components meet strict production readiness criteria.
 */
class ReleaseGateEnforcementTest {

    @Test
    fun testReleaseGate1_ProductionConfigurationValidation() {
        val invalidConfig = BackendConfig(
            environment = BackendEnvironment.PRODUCTION,
            databasePassword = "",
            jwtSigningSecret = "short"
        )
        val errors = invalidConfig.validate()
        assertTrue("Production must reject blank password and weak JWT secret", errors.size >= 2)

        val validConfig = BackendConfig(
            environment = BackendEnvironment.PRODUCTION,
            databasePassword = "prod_password_12345",
            jwtSigningSecret = "a_very_secure_and_long_jwt_signing_secret_for_production_2026",
            databaseUrl = "jdbc:postgresql://postgres-db:5432/sucharu_pro_db"
        )
        assertTrue("Valid production config must have zero errors", validConfig.validate().isEmpty())
    }

    @Test
    fun testReleaseGate2_SecretSanitizationAndMasking() {
        val raw = "User authenticated with Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.doNotLeak"
        val sanitized = LogSanitizer.sanitize(raw)
        assertFalse(sanitized.contains("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"))
        assertTrue(sanitized.contains("Bearer [MASKED]"))

        val passwordPayload = """{"username":"admin","password":"SuperSecretPassword999"}"""
        val sanitizedJson = LogSanitizer.sanitize(passwordPayload)
        assertFalse(sanitizedJson.contains("SuperSecretPassword999"))
        assertTrue(sanitizedJson.contains("[MASKED]"))

        val maskedSecret = LogSanitizer.maskSecret("my_database_password_1234")
        assertEquals("sec_****1234", maskedSecret)
    }

    @Test
    fun testReleaseGate3_DatabaseMigrationsIntegrity() {
        val migrationDir = File("../core/src/main/resources/db/migration")
        val fallbackDir = File("core/src/main/resources/db/migration")
        val dir = if (migrationDir.exists()) migrationDir else fallbackDir

        assertTrue("Migration directory must exist", dir.exists())
        val files = dir.listFiles()?.filter { it.name.endsWith(".sql") } ?: emptyList()

        assertTrue("Must have all 13 canonical migrations present", files.size >= 13)
        val filenames = files.map { it.name }

        // Critical RLS, auth, jobs, and webhook migrations
        assertTrue(filenames.contains("V1__canonical_postgresql_schema.sql"))
        assertTrue(filenames.contains("V20260830__create_auth_and_session_tables.sql"))
        assertTrue(filenames.contains("V20260907__create_background_job_execution_tables.sql"))
        assertTrue(filenames.contains("V20260913__force_row_level_security.sql"))
        assertTrue(filenames.contains("V20260914__create_integrations_and_webhooks.sql"))
    }

    @Test
    fun testReleaseGate4_PostgresRlsPoliciesPresentInMigrations() {
        val migrationDir = File("../core/src/main/resources/db/migration")
        val fallbackDir = File("core/src/main/resources/db/migration")
        val dir = if (migrationDir.exists()) migrationDir else fallbackDir

        val rlsFile = File(dir, "V20260913__force_row_level_security.sql")
        assertTrue("V20260913 force RLS migration must exist", rlsFile.exists())
        val content = rlsFile.readText()
        assertTrue("Must force row level security", content.contains("FORCE ROW LEVEL SECURITY"))

        val webhookMigration = File(dir, "V20260914__create_integrations_and_webhooks.sql")
        assertTrue("V20260914 webhook migration must exist", webhookMigration.exists())
        val webhookContent = webhookMigration.readText()
        assertTrue("Must enable RLS on external integrations", webhookContent.contains("ENABLE ROW LEVEL SECURITY"))
    }

    @Test
    fun testReleaseGate5_ResilienceCircuitBreakerAndRateLimiter() {
        val cb = IntegrationCircuitBreaker(failureThreshold = 2, resetTimeoutMs = 1000L)
        assertTrue(cb.allowRequest())
        cb.recordFailure()
        cb.recordFailure()
        assertFalse("Circuit breaker must trip to OPEN after failure threshold", cb.allowRequest())

        val limiter = IntegrationRateLimiter(maxRequestsPerSecond = 10, burstCapacity = 2)
        assertTrue(limiter.tryAcquire())
        assertTrue(limiter.tryAcquire())
        assertFalse("Rate limiter must reject tokens exceeding capacity", limiter.tryAcquire())
    }

    @Test
    fun testReleaseGate6_WebhookSignatureAndReplayValidation() {
        val verifier = HmacSha256SignatureVerifier()
        val payload = """{"event":"payment_success","amount":5000}"""
        val secret = "webhook_secret_key_123"

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val signature = mac.doFinal(payload.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

        val validHeaders = mapOf("X-Webhook-Signature" to signature)
        assertTrue("Valid signature must be accepted", verifier.verify(payload, validHeaders, secret))

        val tamperedHeaders = mapOf("X-Webhook-Signature" to "forged_invalid_hex_signature")
        assertFalse("Tampered signature must be rejected", verifier.verify(payload, tamperedHeaders, secret))
    }

    @Test
    fun testReleaseGate7_SsrfProtection() {
        val validator = SsrfProtectionValidator()

        var caught = false
        try {
            validator.validateUrl("http://localhost:8080/admin")
        } catch (_: SecurityException) {
            caught = true
        }
        assertTrue("SSRF validator must block localhost", caught)

        caught = false
        try {
            validator.validateUrl("http://127.0.0.1:8080")
        } catch (_: SecurityException) {
            caught = true
        }
        assertTrue("SSRF validator must block 127.0.0.1", caught)

        caught = false
        try {
            validator.validateUrl("http://169.254.169.254/latest/meta-data")
        } catch (_: SecurityException) {
            caught = true
        }
        assertTrue("SSRF validator must block metadata IP", caught)
    }

    @Test
    fun testReleaseGate8_ObservabilityRegistryCardinalityProtection() {
        val registry = ObservabilityMetricsRegistry()
        registry.recordHttpRequest("GET", "/api/v1/customers/123e4567-e89b-12d3-a456-426614174000", 200, 45L)
        val prometheus = registry.formatPrometheus()
        assertTrue("Prometheus exposition must contain http_requests_total", prometheus.contains("http_requests_total"))
        assertFalse("Dynamic UUIDs must be normalized to prevent unbounded cardinality", prometheus.contains("123e4567-e89b-12d3-a456-426614174000"))
    }
}

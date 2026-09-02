package com.sucharu.sucharupro.data.integration

import com.sucharu.sucharupro.data.integration.resilience.CircuitState
import com.sucharu.sucharupro.data.integration.resilience.IntegrationCircuitBreaker
import com.sucharu.sucharupro.data.integration.resilience.IntegrationRateLimiter
import com.sucharu.sucharupro.data.integration.security.DefaultIntegrationSecretProvider
import com.sucharu.sucharupro.data.integration.security.HmacSha256SignatureVerifier
import com.sucharu.sucharupro.data.integration.security.SsrfProtectionValidator
import org.junit.Assert.*
import org.junit.Test
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class ExternalIntegrationRuntimeTest {

    @Test
    fun test01_ssrfValidator_blocksLocalhostAndLoopback() {
        val validator = SsrfProtectionValidator()

        assertThrows(SecurityException::class.java) {
            validator.validateUrl("http://127.0.0.1/api/data")
        }

        assertThrows(SecurityException::class.java) {
            validator.validateUrl("https://localhost:8080/webhook")
        }

        assertThrows(SecurityException::class.java) {
            validator.validateUrl("http://127.0.0.2:9000/internal")
        }
    }

    @Test
    fun test02_ssrfValidator_blocksCloudMetadataEndpoints() {
        val validator = SsrfProtectionValidator()

        assertThrows(SecurityException::class.java) {
            validator.validateUrl("http://169.254.169.254/latest/meta-data")
        }

        assertThrows(SecurityException::class.java) {
            validator.validateUrl("http://metadata.google.internal/computeMetadata/v1/")
        }

        assertThrows(SecurityException::class.java) {
            validator.validateUrl("http://instance-data/latest/meta-data")
        }
    }

    @Test
    fun test03_ssrfValidator_blocksEmbeddedCredentialsAndMalformedUrls() {
        val validator = SsrfProtectionValidator()

        assertThrows(SecurityException::class.java) {
            validator.validateUrl("https://admin:secretPass@api.partner.com/webhook")
        }

        assertThrows(SecurityException::class.java) {
            validator.validateUrl("ftp://api.partner.com/file")
        }

        assertThrows(IllegalArgumentException::class.java) {
            validator.validateUrl("   ")
        }
    }

    @Test
    fun test04_secretProvider_maskingProtectsPlaintext() {
        val provider = DefaultIntegrationSecretProvider()

        assertEquals("[NONE]", provider.maskSecret(null))
        assertEquals("***", provider.maskSecret("short"))
        assertEquals("sec_****1234", provider.maskSecret("sec_very_secret_token_1234"))
    }

    @Test
    fun test05_webhookSignatureVerifier_verifiesValidSignature() {
        val secret = "whsec_super_secret_signing_key_999"
        val payload = """{"event":"order.completed","orderId":"ORD-100"}"""
        val verifier = HmacSha256SignatureVerifier()

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val signature = mac.doFinal(payload.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

        val headers = mapOf("X-Webhook-Signature" to "sha256=$signature")
        val isValid = verifier.verify(payload, headers, secret)
        assertTrue("Signature must be authentic", isValid)
    }

    @Test
    fun test06_webhookSignatureVerifier_rejectsTamperedPayload() {
        val secret = "whsec_super_secret_signing_key_999"
        val originalPayload = """{"event":"order.completed","orderId":"ORD-100"}"""
        val tamperedPayload = """{"event":"order.completed","orderId":"ORD-999"}"""
        val verifier = HmacSha256SignatureVerifier()

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val signature = mac.doFinal(originalPayload.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

        val headers = mapOf("X-Webhook-Signature" to "sha256=$signature")
        val isValid = verifier.verify(tamperedPayload, headers, secret)
        assertFalse("Tampered payload must be rejected", isValid)
    }

    @Test
    fun test07_webhookSignatureVerifier_rejectsExpiredTimestamp() {
        val secret = "whsec_super_secret_signing_key_999"
        val payload = """{"event":"order.completed"}"""
        val verifier = HmacSha256SignatureVerifier(maxTimestampDriftSeconds = 300L)

        val expiredTimestamp = (System.currentTimeMillis() / 1000L) - 600L // 10 minutes ago
        val payloadToSign = "$expiredTimestamp.$payload"

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val signature = mac.doFinal(payloadToSign.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

        val headers = mapOf(
            "X-Webhook-Signature" to signature,
            "X-Webhook-Timestamp" to expiredTimestamp.toString()
        )

        val isValid = verifier.verify(payload, headers, secret)
        assertFalse("Expired timestamp must fail verification for replay protection", isValid)
    }

    @Test
    fun test08_circuitBreaker_tripsAndRecovers() {
        val breaker = IntegrationCircuitBreaker(
            failureThreshold = 3,
            resetTimeoutMs = 100L,
            halfOpenProbeLimit = 2
        )

        assertTrue(breaker.allowRequest())
        assertEquals(CircuitState.CLOSED, breaker.getState())

        // 3 failures -> trips to OPEN
        breaker.recordFailure()
        breaker.recordFailure()
        breaker.recordFailure()

        assertEquals(CircuitState.OPEN, breaker.getState())
        assertFalse(breaker.allowRequest())

        // Wait for reset timeout
        Thread.sleep(150L)

        // Enters HALF_OPEN on next check
        assertEquals(CircuitState.HALF_OPEN, breaker.getState())
        assertTrue(breaker.allowRequest())

        // Probe 1 success
        breaker.recordSuccess()
        assertEquals(CircuitState.HALF_OPEN, breaker.getState())

        // Probe 2 success -> resets to CLOSED
        breaker.recordSuccess()
        assertEquals(CircuitState.CLOSED, breaker.getState())
        assertTrue(breaker.allowRequest())
    }

    @Test
    fun test09_rateLimiter_tokenCapacityAndRetryAfterBackoff() {
        val limiter = IntegrationRateLimiter(maxRequestsPerSecond = 2, burstCapacity = 3)

        // Burst 3 allowed
        assertTrue(limiter.tryAcquire())
        assertTrue(limiter.tryAcquire())
        assertTrue(limiter.tryAcquire())

        // 4th is rate limited
        assertFalse(limiter.tryAcquire())

        // Apply 429 Retry-After backoff of 2 seconds
        limiter.applyRetryAfter(2L)
        assertFalse("Must be blocked during backoff period", limiter.tryAcquire())
    }
}

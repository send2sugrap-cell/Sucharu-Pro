package com.sucharu.sucharupro.data.observability

import com.sucharu.sucharupro.data.observability.logging.StructuredObservabilityLogger
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Structured logging auto-redaction and secret protection test suite (INFRA-04 Step 09).
 */
class ObservabilityRedactionTest {

    private lateinit var logger: StructuredObservabilityLogger

    @Before
    fun setUp() {
        logger = StructuredObservabilityLogger()
    }

    @Test
    fun test01_jwtRedactedInLogMessage() {
        val jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
        val entry = logger.log(
            projectId = "p-001",
            subsystem = "AUTH",
            operation = "LOGIN",
            message = "User authenticated with token $jwt"
        )
        assertFalse("JWT must not appear in logged message", entry.message.contains(jwt))
        assertTrue("Logged message must contain [REDACTED_JWT]", entry.message.contains("[REDACTED_JWT]"))
    }

    @Test
    fun test02_apiKeyRedactedInLogMessage() {
        val entry = logger.log(
            projectId = "p-001",
            subsystem = "NOTIFICATION",
            operation = "PROVIDER_CALL",
            message = "Calling Twilio with key api_key=secret-twilio-token-12345"
        )
        assertFalse("API key must be redacted", entry.message.contains("secret-twilio-token-12345"))
    }

    @Test
    fun test03_sensitiveMetadataKeys_areStripped() {
        val metadata = mapOf(
            "orderId" to "ORD-123",
            "password" to "mypassword123",
            "apiKey" to "sk-secret-12345"
        )
        val entry = logger.log(
            projectId = "p-001",
            subsystem = "ORDER",
            operation = "CREATE",
            message = "Order processed",
            metadata = metadata
        )
        assertFalse("password metadata key must be stripped", entry.sanitizedMetadata.containsKey("password"))
        assertFalse("apiKey metadata key must be stripped", entry.sanitizedMetadata.containsKey("apiKey"))
        assertTrue("Safe orderId key must be retained", entry.sanitizedMetadata.containsKey("orderId"))
    }
}

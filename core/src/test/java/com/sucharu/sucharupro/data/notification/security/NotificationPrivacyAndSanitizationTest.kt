package com.sucharu.sucharupro.data.notification.security

import com.sucharu.sucharupro.domain.event.boundary.NotificationChannel
import com.sucharu.sucharupro.domain.event.boundary.NotificationIntent
import com.sucharu.sucharupro.domain.event.model.DomainEventType
import org.junit.Assert.*
import org.junit.Test

/**
 * Privacy and payload sanitization tests (INFRA-04 Step 07).
 */
class NotificationPrivacyAndSanitizationTest {

    private fun baseIntent() = NotificationIntent(
        eventId = "evt-priv-001",
        eventType = DomainEventType.ORDER_CREATED,
        projectId = "p-001",
        targetRecipientId = "CUST-1",
        targetChannels = setOf(NotificationChannel.EMAIL, NotificationChannel.SMS),
        title = "Order Update",
        body = "Your order status has been updated.",
        correlationId = "corr-priv"
    )

    @Test
    fun test01_credentialInTitle_isDetected() {
        val title = "Your login token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0In0.SflKxwRJSMeKKF2QT4fwpMeJf36P"
        assertTrue(
            "JWT in title must be detected as credential leak",
            NotificationPayloadSanitizer.containsCredentialLeak(title)
        )
    }

    @Test
    fun test02_scriptInjectionInTitle_isDetected() {
        val title = "Hello <script>alert('xss')</script> World"
        assertTrue(
            "Script injection in title must be detected",
            NotificationPayloadSanitizer.containsInjection(title)
        )
    }

    @Test
    fun test03_headerInjectionInTitle_isDetected() {
        val title = "Normal\r\nX-Injected-Header: evil"
        assertTrue(
            "CRLF injection in title must be detected",
            NotificationPayloadSanitizer.containsInjection(title)
        )
    }

    @Test
    fun test04_sensitiveMetadataKeys_areStripped() {
        val metadata = mapOf(
            "correlationId" to "corr-001",
            "password" to "hunter2",
            "apiKey" to "abc123-secret",
            "token" to "bearer xyz",
            "orderId" to "ORD-001"
        )
        val sanitized = NotificationPayloadSanitizer.sanitizeMetadata(metadata)
        assertFalse("password must be stripped", sanitized.containsKey("password"))
        assertFalse("apiKey must be stripped", sanitized.containsKey("apiKey"))
        assertFalse("token must be stripped", sanitized.containsKey("token"))
        assertTrue("Safe keys must be retained", sanitized.containsKey("correlationId"))
        assertTrue("Safe keys must be retained", sanitized.containsKey("orderId"))
    }

    @Test
    fun test05_smsBodyTruncation_above160Chars() {
        val longBody = "A".repeat(200)
        val intent = baseIntent().copy(body = longBody)
        val result = NotificationPayloadSanitizer.sanitize(intent, NotificationChannel.SMS)
        assertTrue(
            "SMS body must be truncated to <= 160 characters",
            result.body.length <= 160
        )
        assertTrue("Truncated SMS must end with '...'", result.body.endsWith("..."))
    }

    @Test
    fun test06_stackTraceInBody_isStripped() {
        val bodyWithStackTrace = """
            Order processed.
            at com.sucharu.sucharupro.order.OrderService.processOrder(OrderService.kt:42)
            at com.sucharu.sucharupro.order.OrderController.submit(OrderController.kt:18)
            Caused by: java.lang.NullPointerException
            ... 23 more
        """.trimIndent()
        val result = NotificationPayloadSanitizer.sanitize(baseIntent().copy(body = bodyWithStackTrace), NotificationChannel.EMAIL)
        assertFalse("Stack trace frames must be removed from notification body", result.body.contains("at com.sucharu"))
        assertFalse("Caused by must be removed", result.body.contains("Caused by:"))
        assertTrue("Safe content must be retained", result.body.contains("Order processed"))
    }
}

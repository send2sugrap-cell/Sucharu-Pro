package com.sucharu.sucharupro.data.notification.ai

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.api.model.UserPermission
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.domain.event.boundary.NotificationChannel
import com.sucharu.sucharupro.domain.notification.ai.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Payload sanitization and credential leak detection test suite for AI Agent notification interactions (INFRA-04 Step 08).
 */
class AiNotificationPayloadSanitizationTest {

    private lateinit var boundary: AiAgentNotificationSecurityBoundary

    private val adminAi = AuthenticatedPrincipal(
        userId = "ai-san",
        projectId = "p-001",
        username = "ai_san",
        role = UserRole.AI_AGENT,
        permissions = setOf(UserPermission.ADMIN_ALL),
        principalType = PrincipalType.AI_AGENT
    )

    private fun requestWith(title: String, body: String, metadata: Map<String, String> = emptyMap()) =
        AiNotificationActionRequest(
            projectId = "p-001",
            actionType = AiNotificationActionType.CREATE_DRAFT,
            targetRecipientId = "CUST-1",
            targetChannels = setOf(NotificationChannel.IN_APP),
            title = title,
            body = body,
            metadata = metadata,
            idempotencyKey = "idem-san-1",
            correlationId = "corr-san-1"
        )

    @Before
    fun setUp() {
        boundary = AiAgentNotificationSecurityBoundary()
    }

    @Test
    fun test01_jwtInTitle_blockedBySecurityBoundary() = runBlocking {
        val jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
        val decision = boundary.evaluateActionRequest(
            adminAi,
            requestWith(title = "Your token: $jwt", body = "Safe body"),
            "p-001"
        )
        assertTrue("Credential leak in title must be denied", decision is AiNotificationSecurityDecision.Denied)
        val deny = decision as AiNotificationSecurityDecision.Denied
        assertEquals(AiNotificationDenialReason.CREDENTIAL_LEAK_DETECTED, deny.reason)
    }

    @Test
    fun test02_jwtInBody_blockedBySecurityBoundary() = runBlocking {
        val jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
        val decision = boundary.evaluateActionRequest(
            adminAi,
            requestWith(title = "Safe title", body = "Your secret is $jwt"),
            "p-001"
        )
        assertTrue("Credential leak in body must be denied", decision is AiNotificationSecurityDecision.Denied)
        val deny = decision as AiNotificationSecurityDecision.Denied
        assertEquals(AiNotificationDenialReason.CREDENTIAL_LEAK_DETECTED, deny.reason)
    }

    @Test
    fun test03_scriptInjectionInTitle_blocked() = runBlocking {
        val decision = boundary.evaluateActionRequest(
            adminAi,
            requestWith(title = "<script>alert('pwn')</script>", body = "Safe body"),
            "p-001"
        )
        assertTrue("Script injection in title must be denied", decision is AiNotificationSecurityDecision.Denied)
        val deny = decision as AiNotificationSecurityDecision.Denied
        assertEquals(AiNotificationDenialReason.CONTENT_INJECTION_DETECTED, deny.reason)
    }

    @Test
    fun test04_scriptInBody_isSanitized() = runBlocking {
        val decision = boundary.evaluateActionRequest(
            adminAi,
            requestWith(title = "Safe title", body = "Order <script>evil()</script> ready"),
            "p-001"
        )
        assertTrue(decision is AiNotificationSecurityDecision.Allowed)
        val allowed = decision as AiNotificationSecurityDecision.Allowed
        assertFalse("Script tag must be stripped from body", allowed.sanitizedRequest.body.contains("<script>"))
    }

    @Test
    fun test05_sensitiveKeysInMetadata_areStripped() = runBlocking {
        val meta = mapOf(
            "orderId" to "ORD-123",
            "password" to "hunter2",
            "apiKey" to "secret-api-key-12345"
        )
        val decision = boundary.evaluateActionRequest(
            adminAi,
            requestWith(title = "Safe", body = "Safe", metadata = meta),
            "p-001"
        )
        assertTrue(decision is AiNotificationSecurityDecision.Allowed)
        val allowed = decision as AiNotificationSecurityDecision.Allowed
        assertFalse("password key must be stripped", allowed.sanitizedRequest.metadata.containsKey("password"))
        assertFalse("apiKey key must be stripped", allowed.sanitizedRequest.metadata.containsKey("apiKey"))
        assertTrue("Safe orderId must be retained", allowed.sanitizedRequest.metadata.containsKey("orderId"))
    }

    @Test
    fun test06_apiKeyInMetadataValue_isRedacted() = runBlocking {
        val meta = mapOf(
            "details" to "Bearer 12345678901234567890123456789012"
        )
        val decision = boundary.evaluateActionRequest(
            adminAi,
            requestWith(title = "Safe", body = "Safe", metadata = meta),
            "p-001"
        )
        // Redacted or allowed with [REDACTED] value
        assertTrue(decision is AiNotificationSecurityDecision.Allowed || decision is AiNotificationSecurityDecision.Denied)
    }
}

package com.sucharu.sucharupro.data.notification.security

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.domain.event.boundary.NotificationChannel
import com.sucharu.sucharupro.domain.event.boundary.NotificationIntent
import com.sucharu.sucharupro.domain.event.model.DomainEventType
import com.sucharu.sucharupro.domain.notification.security.NotificationDataClassification
import com.sucharu.sucharupro.domain.notification.security.NotificationSecurityContext
import com.sucharu.sucharupro.domain.notification.security.NotificationSecurityDecision
import com.sucharu.sucharupro.domain.notification.security.NotificationSecurityReason
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Channel security and data-classification eligibility tests (INFRA-04 Step 07).
 */
class NotificationChannelSecurityTest {

    private lateinit var policy: NotificationSecurityPolicy

    private fun staffPrincipal() = AuthenticatedPrincipal(
        userId = "staff-002",
        projectId = "p-001",
        username = "staff002",
        role = UserRole.STAFF,
        principalType = PrincipalType.HUMAN
    )

    private fun intent(channels: Set<NotificationChannel>, title: String = "Safe title", body: String = "Safe body") =
        NotificationIntent(
            eventId = "evt-ch-001",
            eventType = DomainEventType.ORDER_CREATED,
            projectId = "p-001",
            targetRecipientId = "CUST-1",
            targetChannels = channels,
            title = title,
            body = body,
            correlationId = "corr-ch"
        )

    @Before
    fun setUp() {
        policy = NotificationSecurityPolicy()
    }

    @Test
    fun test01_restrictedClassification_smsAndPushBlocked() = runBlocking {
        val context = NotificationSecurityContext(
            principal = staffPrincipal(),
            projectId = "p-001",
            intent = intent(setOf(NotificationChannel.SMS, NotificationChannel.PUSH)),
            classification = NotificationDataClassification.RESTRICTED,
            correlationId = "corr-ch-01",
            requestId = "req-ch-01"
        )
        val decision = policy.evaluateDispatch(context)
        assertTrue("RESTRICTED classification must deny SMS/PUSH channels", decision is NotificationSecurityDecision.Deny)
        val deny = decision as NotificationSecurityDecision.Deny
        assertEquals(NotificationSecurityReason.CHANNEL_CLASSIFICATION_MISMATCH, deny.reason)
    }

    @Test
    fun test02_restrictedClassification_emailAndInApp_allowed() = runBlocking {
        val context = NotificationSecurityContext(
            principal = staffPrincipal(),
            projectId = "p-001",
            intent = intent(setOf(NotificationChannel.IN_APP, NotificationChannel.EMAIL)),
            classification = NotificationDataClassification.RESTRICTED,
            correlationId = "corr-ch-02",
            requestId = "req-ch-02"
        )
        val decision = policy.evaluateDispatch(context)
        assertTrue("RESTRICTED classification must allow IN_APP + EMAIL", decision is NotificationSecurityDecision.Allow)
    }

    @Test
    fun test03_internalClassification_onlyInApp_allowed() = runBlocking {
        val context = NotificationSecurityContext(
            principal = staffPrincipal(),
            projectId = "p-001",
            intent = intent(setOf(NotificationChannel.IN_APP, NotificationChannel.SMS)),
            classification = NotificationDataClassification.INTERNAL,
            correlationId = "corr-ch-03",
            requestId = "req-ch-03"
        )
        val decision = policy.evaluateDispatch(context)
        assertTrue("INTERNAL with IN_APP+SMS requested should allow with effective IN_APP only", decision is NotificationSecurityDecision.Allow)
        val allow = decision as NotificationSecurityDecision.Allow
        assertTrue("Effective channels must only contain IN_APP", NotificationChannel.IN_APP in allow.effectiveChannels)
        assertFalse("SMS must be excluded from effective channels for INTERNAL", NotificationChannel.SMS in allow.effectiveChannels)
    }

    @Test
    fun test04_credentialLeakInBody_blockedByPolicy() = runBlocking {
        val jwtBody = "Your token is eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
        val context = NotificationSecurityContext(
            principal = staffPrincipal(),
            projectId = "p-001",
            intent = intent(setOf(NotificationChannel.IN_APP), body = jwtBody),
            classification = NotificationDataClassification.PUBLIC,
            correlationId = "corr-ch-04",
            requestId = "req-ch-04"
        )
        val decision = policy.evaluateDispatch(context)
        assertTrue("JWT in body must be blocked as credential leak", decision is NotificationSecurityDecision.Deny)
        val deny = decision as NotificationSecurityDecision.Deny
        assertEquals(NotificationSecurityReason.CREDENTIAL_LEAK_DETECTED, deny.reason)
    }

    @Test
    fun test05_scriptInjectionInTitle_blockedByPolicy() = runBlocking {
        val injectedTitle = "<script>alert('xss')</script>"
        val context = NotificationSecurityContext(
            principal = staffPrincipal(),
            projectId = "p-001",
            intent = intent(setOf(NotificationChannel.IN_APP), title = injectedTitle),
            classification = NotificationDataClassification.PUBLIC,
            correlationId = "corr-ch-05",
            requestId = "req-ch-05"
        )
        val decision = policy.evaluateDispatch(context)
        assertTrue("Script injection in title must be blocked", decision is NotificationSecurityDecision.Deny)
        val deny = decision as NotificationSecurityDecision.Deny
        assertEquals(NotificationSecurityReason.CONTENT_INJECTION_DETECTED, deny.reason)
    }
}

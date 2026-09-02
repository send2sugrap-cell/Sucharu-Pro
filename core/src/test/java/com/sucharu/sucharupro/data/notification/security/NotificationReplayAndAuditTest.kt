package com.sucharu.sucharupro.data.notification.security

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.event.boundary.NotificationChannel
import com.sucharu.sucharupro.domain.event.boundary.NotificationIntent
import com.sucharu.sucharupro.domain.event.model.DomainEventType
import com.sucharu.sucharupro.domain.notification.security.NotificationDataClassification
import com.sucharu.sucharupro.domain.notification.security.NotificationSecurityAuditEvent
import com.sucharu.sucharupro.domain.notification.security.NotificationSecurityContext
import com.sucharu.sucharupro.domain.notification.security.NotificationSecurityDecision
import com.sucharu.sucharupro.domain.notification.security.NotificationSecurityOperation
import com.sucharu.sucharupro.domain.notification.security.NotificationSecurityReason
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Replay authorization, provider callback security, and audit immutability tests (INFRA-04 Step 07).
 */
class NotificationReplayAndAuditTest {

    private lateinit var auditRepo: InMemoryNotificationAuditRepository
    private lateinit var auditService: NotificationAuditService
    private lateinit var policy: NotificationSecurityPolicy
    private val tenantCtx = TenantContext("p-001")

    private fun managerPrincipal() = AuthenticatedPrincipal(
        userId = "mgr-001",
        projectId = "p-001",
        username = "manager001",
        role = UserRole.MANAGER,
        principalType = PrincipalType.HUMAN
    )

    private fun staffPrincipal() = AuthenticatedPrincipal(
        userId = "staff-004",
        projectId = "p-001",
        username = "staff004",
        role = UserRole.STAFF,
        principalType = PrincipalType.HUMAN
    )

    private fun aiPrincipal() = AuthenticatedPrincipal(
        userId = "ai-agent-001",
        projectId = "p-001",
        username = "ai_agent_001",
        role = UserRole.AI_AGENT,
        principalType = PrincipalType.AI_AGENT
    )

    private fun intent() = NotificationIntent(
        eventId = "evt-replay-001",
        eventType = DomainEventType.ORDER_CREATED,
        projectId = "p-001",
        targetRecipientId = "CUST-5",
        targetChannels = setOf(NotificationChannel.IN_APP),
        title = "Replay Test",
        body = "This is a replay.",
        correlationId = "corr-replay"
    )

    @Before
    fun setUp() {
        auditRepo = InMemoryNotificationAuditRepository()
        auditService = NotificationAuditService(auditRepo)
        policy = NotificationSecurityPolicy(auditService = auditService)
        ProviderCallbackSecurity.clearSeenCallbacks()
    }

    @Test
    fun test01_managerCanAuthorizeReplay() {
        val svc = NotificationAuthorizationService()
        val result = svc.authorizeReplay(managerPrincipal(), "p-001")
        assertTrue("Manager must be authorized for replay", result.authorized)
    }

    @Test
    fun test02_staffCannotAuthorizeReplay() {
        val svc = NotificationAuthorizationService()
        val result = svc.authorizeReplay(staffPrincipal(), "p-001")
        assertFalse("STAFF lacks NOTIFICATION_REPLAY capability", result.authorized)
        assertEquals(NotificationSecurityReason.REPLAY_UNAUTHORIZED, result.reason)
    }

    @Test
    fun test03_auditService_appendsDecision() = runBlocking {
        val context = NotificationSecurityContext(
            principal = staffPrincipal(),
            projectId = "p-001",
            intent = intent(),
            classification = NotificationDataClassification.PUBLIC,
            correlationId = "corr-audit-03",
            requestId = "req-audit-03"
        )
        val decision = NotificationSecurityDecision.Deny(
            reason = NotificationSecurityReason.AI_AGENT_DENIED,
            message = "Test audit"
        )
        auditService.recordDecision(context, decision)
        assertEquals(1, auditRepo.count())
        val record = auditRepo.allRecords().first()
        assertEquals("p-001", record.projectId)
        assertEquals("DENY", record.decision)
    }

    @Test
    fun test04_auditRecords_areAppendOnly() = runBlocking {
        val context = NotificationSecurityContext(
            principal = staffPrincipal(),
            projectId = "p-001",
            intent = intent(),
            classification = NotificationDataClassification.PUBLIC,
            correlationId = "corr-audit-04",
            requestId = "req-audit-04"
        )
        auditService.recordDecision(context, NotificationSecurityDecision.Allow(
            sanitizedIntent = intent(),
            effectiveChannels = setOf(NotificationChannel.IN_APP)
        ))
        auditService.recordDecision(context, NotificationSecurityDecision.Deny(
            reason = NotificationSecurityReason.RATE_LIMITED,
            message = "second record"
        ))
        assertEquals("Audit must be append-only: 2 records expected", 2, auditRepo.count())
    }

    @Test
    fun test05_callbackSecurity_validSignatureAndTimestamp() {
        val secret = "super-secret-key"
        val payload = "callback-payload".toByteArray()
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        mac.init(javax.crypto.spec.SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
        val sig = mac.doFinal(payload).joinToString("") { "%02x".format(it) }

        val result = ProviderCallbackSecurity.validateCallback(
            payload = payload,
            signature = sig,
            secret = secret,
            timestampMs = System.currentTimeMillis(),
            idempotencyKey = "idem-${UUID.randomUUID()}"
        )
        assertTrue("Valid callback must pass all checks", result.isValid)
        assertTrue(result.signatureValid)
        assertTrue(result.timestampValid)
        assertFalse(result.isReplay)
    }

    @Test
    fun test06_callbackSecurity_replayIsDetected() {
        val secret = "secret-replay"
        val payload = "replay-payload".toByteArray()
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        mac.init(javax.crypto.spec.SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
        val sig = mac.doFinal(payload).joinToString("") { "%02x".format(it) }
        val idempotencyKey = "replay-key-fixed"

        val first = ProviderCallbackSecurity.validateCallback(payload, sig, secret, System.currentTimeMillis(), idempotencyKey)
        assertTrue("First callback must be valid", first.isValid)
        assertFalse("First callback must not be classified as replay", first.isReplay)

        val second = ProviderCallbackSecurity.validateCallback(payload, sig, secret, System.currentTimeMillis(), idempotencyKey)
        assertFalse("Replayed callback must be invalid", second.isValid)
        assertTrue("Second identical callback must be a replay", second.isReplay)
    }
}

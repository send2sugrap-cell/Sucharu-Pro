package com.sucharu.sucharupro.data.notification.security

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.event.boundary.NotificationChannel
import com.sucharu.sucharupro.domain.event.boundary.NotificationIntent
import com.sucharu.sucharupro.domain.event.model.DomainEventType
import com.sucharu.sucharupro.domain.notification.security.NotificationDataClassification
import com.sucharu.sucharupro.domain.notification.security.NotificationSecurityContext
import com.sucharu.sucharupro.domain.notification.security.NotificationSecurityDecision
import com.sucharu.sucharupro.domain.notification.security.NotificationSuppression
import com.sucharu.sucharupro.domain.notification.security.RateLimitPolicy
import com.sucharu.sucharupro.domain.notification.security.SuppressionReason
import com.sucharu.sucharupro.domain.notification.security.SuppressionType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Rate limiting and suppression tests (INFRA-04 Step 07).
 */
class NotificationRateLimitAndSuppressionTest {

    private lateinit var suppressionRepo: InMemoryNotificationSuppressionRepository
    private lateinit var rateLimiter: NotificationRateLimiter
    private lateinit var policy: NotificationSecurityPolicy

    private val tenantCtx = TenantContext("p-001")

    private fun staffPrincipal() = AuthenticatedPrincipal(
        userId = "staff-003",
        projectId = "p-001",
        username = "staff003",
        role = UserRole.STAFF,
        principalType = PrincipalType.HUMAN
    )

    private fun intent() = NotificationIntent(
        eventId = "evt-rl-001",
        eventType = DomainEventType.ORDER_CREATED,
        projectId = "p-001",
        targetRecipientId = "CUST-2",
        targetChannels = setOf(NotificationChannel.IN_APP),
        title = "Rate limit test",
        body = "Test body",
        correlationId = "corr-rl"
    )

    private fun context() = NotificationSecurityContext(
        principal = staffPrincipal(),
        projectId = "p-001",
        intent = intent(),
        classification = NotificationDataClassification.PUBLIC,
        correlationId = "corr-rl",
        requestId = UUID.randomUUID().toString()
    )

    @Before
    fun setUp() {
        suppressionRepo = InMemoryNotificationSuppressionRepository()
        rateLimiter = NotificationRateLimiter()
        policy = NotificationSecurityPolicy(
            suppressionRepository = suppressionRepo,
            rateLimiter = rateLimiter
        )
    }

    @Test
    fun test01_rateLimiter_allowsUpToMaxCount() {
        val policy = RateLimitPolicy("key-a", windowSeconds = 60, maxCount = 3)
        for (i in 1..3) {
            val result = rateLimiter.evaluateAndRecord("key-a", policy)
            assertTrue("Request $i should be allowed", result.allowed)
        }
    }

    @Test
    fun test02_rateLimiter_blocksAfterMaxCount() {
        val policy = RateLimitPolicy("key-b", windowSeconds = 60, maxCount = 3)
        repeat(3) { rateLimiter.record("key-b", policy) }
        val result = rateLimiter.evaluate("key-b", policy)
        assertFalse("Request beyond max count must be denied", result.allowed)
        assertTrue("Retry-after must be positive", result.retryAfterMs > 0)
    }

    @Test
    fun test03_suppressedRecipient_isSkipped() = runBlocking {
        suppressionRepo.createSuppression(
            NotificationSuppression(
                suppressionId = UUID.randomUUID().toString(),
                projectId = "p-001",
                recipientId = "CUST-2",
                channel = NotificationChannel.IN_APP,
                reason = SuppressionReason.USER_REQUESTED,
                suppressionType = SuppressionType.RECIPIENT,
                createdBy = "admin-001"
            ),
            tenantCtx
        )

        val decision = policy.evaluateDispatch(context())
        assertTrue("Suppressed recipient must produce Suppress decision", decision is NotificationSecurityDecision.Suppress)
    }

    @Test
    fun test04_suppressionIsIdempotent_noDuplicates() = runBlocking {
        val suppression = NotificationSuppression(
            suppressionId = UUID.randomUUID().toString(),
            projectId = "p-001",
            recipientId = "CUST-3",
            channel = NotificationChannel.SMS,
            reason = SuppressionReason.BOUNCE,
            suppressionType = SuppressionType.RECIPIENT,
            createdBy = "system"
        )
        suppressionRepo.createSuppression(suppression, tenantCtx)
        suppressionRepo.createSuppression(suppression.copy(suppressionId = UUID.randomUUID().toString()), tenantCtx)

        val list = suppressionRepo.listSuppressions("p-001", tenantCtx)
        assertEquals("Idempotent suppression must not create duplicates", 1, list.size)
    }

    @Test
    fun test05_suppressionRemoval_requiresExplicitCall() = runBlocking {
        suppressionRepo.createSuppression(
            NotificationSuppression(
                suppressionId = UUID.randomUUID().toString(),
                projectId = "p-001",
                recipientId = "CUST-4",
                channel = NotificationChannel.EMAIL,
                reason = SuppressionReason.ADMIN_BLOCK,
                suppressionType = SuppressionType.RECIPIENT,
                createdBy = "admin-001"
            ),
            tenantCtx
        )
        assertTrue(suppressionRepo.isSuppressed("p-001", "CUST-4", NotificationChannel.EMAIL, tenantCtx))

        val removed = suppressionRepo.removeSuppression("p-001", "CUST-4", NotificationChannel.EMAIL, "admin-001", tenantCtx)
        assertTrue("Removal must succeed", removed)
        assertFalse("After removal, recipient must no longer be suppressed", suppressionRepo.isSuppressed("p-001", "CUST-4", NotificationChannel.EMAIL, tenantCtx))
    }

    @Test
    fun test06_policyRateLimitDecision_fromSecurityPolicy() = runBlocking {
        val key = rateLimiter.buildRecipientChannelKey("p-001", "CUST-2", "IN_APP")
        val limitPolicy = RateLimitPolicy(key, windowSeconds = 60, maxCount = 10)
        repeat(10) { rateLimiter.record(key, limitPolicy) }

        val overLimitPolicy = NotificationSecurityPolicy(
            suppressionRepository = suppressionRepo,
            rateLimiter = rateLimiter
        )

        val ctx = context()
        val decision = overLimitPolicy.evaluateDispatch(ctx)
        assertTrue("Rate limit exhaustion must produce RateLimit decision", decision is NotificationSecurityDecision.RateLimit)
    }
}

package com.sucharu.sucharupro.data.notification.ai

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.api.model.UserPermission
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.notification.security.NotificationRateLimiter
import com.sucharu.sucharupro.domain.event.boundary.NotificationChannel
import com.sucharu.sucharupro.domain.notification.ai.*
import com.sucharu.sucharupro.domain.notification.security.RateLimitPolicy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Rate limit and anti-abuse test suite for AI Agent notification interactions (INFRA-04 Step 08).
 */
class AiNotificationRateLimitTest {

    private lateinit var rateLimiter: NotificationRateLimiter
    private lateinit var boundary: AiAgentNotificationSecurityBoundary

    private val adminAi = AuthenticatedPrincipal(
        userId = "ai-rate",
        projectId = "p-001",
        username = "ai_rate",
        role = UserRole.AI_AGENT,
        permissions = setOf(UserPermission.ADMIN_ALL),
        principalType = PrincipalType.AI_AGENT
    )

    private fun request(i: Int) = AiNotificationActionRequest(
        projectId = "p-001",
        actionType = AiNotificationActionType.CREATE_DRAFT,
        targetRecipientId = "CUST-1",
        targetChannels = setOf(NotificationChannel.IN_APP),
        title = "Title $i",
        body = "Body $i",
        idempotencyKey = "idem-rl-$i",
        correlationId = "corr-rl-$i"
    )

    @Before
    fun setUp() {
        rateLimiter = NotificationRateLimiter()
        boundary = AiAgentNotificationSecurityBoundary(rateLimiter = rateLimiter)
    }

    @Test
    fun test01_rateLimit_allowsWithinLimit() = runBlocking {
        for (i in 1..5) {
            val decision = boundary.evaluateActionRequest(adminAi, request(i), "p-001")
            assertTrue("Request $i should be allowed within rate limit", decision is AiNotificationSecurityDecision.Allowed)
        }
    }

    @Test
    fun test02_rateLimit_deniesExceedingLimit() = runBlocking {
        val key = "ai_agent:p-001:ai-rate:CREATE_DRAFT"
        val policy = RateLimitPolicy(key, windowSeconds = 60, maxCount = 20)
        repeat(20) { rateLimiter.record(key, policy) }

        val decision = boundary.evaluateActionRequest(adminAi, request(21), "p-001")
        assertTrue("Request exceeding rate limit must be denied", decision is AiNotificationSecurityDecision.Denied)
        val deny = decision as AiNotificationSecurityDecision.Denied
        assertEquals(AiNotificationDenialReason.RATE_LIMITED, deny.reason)
    }

    @Test
    fun test03_rateLimit_isTenantIsolated() = runBlocking {
        // Exhaust limit for tenant-1
        val key1 = "ai_agent:p-001:ai-rate:CREATE_DRAFT"
        val policy1 = RateLimitPolicy(key1, windowSeconds = 60, maxCount = 20)
        repeat(20) { rateLimiter.record(key1, policy1) }

        // Same agent in tenant-2 should still be allowed
        val adminAiTenant2 = adminAi.copy(projectId = "p-002")
        val reqTenant2 = request(1).copy(projectId = "p-002")
        val decision = boundary.evaluateActionRequest(adminAiTenant2, reqTenant2, "p-002")
        assertTrue("Rate limit on tenant-1 must not affect tenant-2", decision is AiNotificationSecurityDecision.Allowed)
    }
}

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
 * RBAC authorization tests for notification dispatch (INFRA-04 Step 07).
 */
class NotificationAuthorizationTest {

    private lateinit var policy: NotificationSecurityPolicy

    private fun principal(role: UserRole, projectId: String = "p-001", isAiAgent: Boolean = false) =
        AuthenticatedPrincipal(
            userId = "user-${role.name}",
            projectId = projectId,
            username = "user_${role.name.lowercase()}",
            role = role,
            principalType = if (isAiAgent) PrincipalType.AI_AGENT else PrincipalType.HUMAN,
            customerId = if (role == UserRole.CUSTOMER) "CUST-1" else null
        )

    private fun intent(recipientId: String = "CUST-1", projectId: String = "p-001") = NotificationIntent(
        eventId = "evt-auth-001",
        eventType = DomainEventType.ORDER_CREATED,
        projectId = projectId,
        targetRecipientId = recipientId,
        targetChannels = setOf(NotificationChannel.IN_APP, NotificationChannel.EMAIL),
        title = "Test notification",
        body = "Test body",
        correlationId = "corr-auth"
    )

    private fun context(principal: AuthenticatedPrincipal, intent: NotificationIntent) =
        NotificationSecurityContext(
            principal = principal,
            projectId = "p-001",
            intent = intent,
            classification = NotificationDataClassification.PUBLIC,
            correlationId = "corr-auth",
            requestId = "req-auth"
        )

    @Before
    fun setUp() {
        policy = NotificationSecurityPolicy()
    }

    @Test
    fun test01_aiAgent_isDeniedByDefault() = runBlocking {
        val ai = principal(UserRole.AI_AGENT, isAiAgent = true)
        val decision = policy.evaluateDispatch(context(ai, intent()))
        assertTrue("AI agent must be denied for notification dispatch", decision is NotificationSecurityDecision.Deny)
        val deny = decision as NotificationSecurityDecision.Deny
        assertEquals(NotificationSecurityReason.AI_AGENT_DENIED, deny.reason)
    }

    @Test
    fun test02_staff_isAllowedToSendNotifications() = runBlocking {
        val staff = principal(UserRole.STAFF)
        val decision = policy.evaluateDispatch(context(staff, intent()))
        assertTrue("STAFF must be allowed to dispatch notifications", decision is NotificationSecurityDecision.Allow)
    }

    @Test
    fun test03_manager_isAllowedToSendNotifications() = runBlocking {
        val manager = principal(UserRole.MANAGER)
        val decision = policy.evaluateDispatch(context(manager, intent()))
        assertTrue("MANAGER must be allowed to dispatch notifications", decision is NotificationSecurityDecision.Allow)
    }

    @Test
    fun test04_customer_canReceiveOwnNotifications() = runBlocking {
        val customer = principal(UserRole.CUSTOMER)
        val decision = policy.evaluateDispatch(context(customer, intent(recipientId = "CUST-1")))
        assertTrue("CUSTOMER should not be able to dispatch notifications to others", decision is NotificationSecurityDecision.Deny)
        val deny = decision as NotificationSecurityDecision.Deny
        assertEquals(NotificationSecurityReason.MISSING_CAPABILITY, deny.reason)
    }

    @Test
    fun test05_admin_isAllowed() = runBlocking {
        val admin = principal(UserRole.ADMIN)
        val decision = policy.evaluateDispatch(context(admin, intent()))
        assertTrue("ADMIN must be allowed to dispatch notifications", decision is NotificationSecurityDecision.Allow)
    }

    @Test
    fun test06_notificationAuthService_replayDeniedForAiAgent() {
        val svc = NotificationAuthorizationService()
        val ai = principal(UserRole.AI_AGENT, isAiAgent = true)
        val result = svc.authorizeReplay(ai, "p-001")
        assertFalse("AI agent must not replay notifications", result.authorized)
        assertEquals(NotificationSecurityReason.AI_AGENT_DENIED, result.reason)
    }
}

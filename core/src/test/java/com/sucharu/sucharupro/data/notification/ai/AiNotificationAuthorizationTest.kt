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
 * Authorization and machine principal test suite for AI Agent notification interactions (INFRA-04 Step 08).
 */
class AiNotificationAuthorizationTest {

    private lateinit var boundary: AiAgentNotificationSecurityBoundary

    private fun request(actionType: AiNotificationActionType = AiNotificationActionType.CREATE_DRAFT) =
        AiNotificationActionRequest(
            projectId = "p-001",
            actionType = actionType,
            targetRecipientId = "CUST-1",
            targetChannels = setOf(NotificationChannel.IN_APP),
            title = "Order status",
            body = "Order confirmed",
            idempotencyKey = "idem-auth-1",
            correlationId = "corr-auth-1"
        )

    @Before
    fun setUp() {
        boundary = AiAgentNotificationSecurityBoundary()
    }

    @Test
    fun test01_unauthenticatedPrincipal_isDenied() = runBlocking {
        val decision = boundary.evaluateActionRequest(
            principal = null,
            request = request(),
            serverProjectId = "p-001"
        )
        assertTrue("Unauthenticated principal must be denied", decision is AiNotificationSecurityDecision.Denied)
        val deny = decision as AiNotificationSecurityDecision.Denied
        assertEquals(AiNotificationDenialReason.UNAUTHENTICATED, deny.reason)
    }

    @Test
    fun test02_humanPrincipal_deniedFromAiEndpoint() = runBlocking {
        val humanStaff = AuthenticatedPrincipal(
            userId = "staff-01",
            projectId = "p-001",
            username = "staff01",
            role = UserRole.STAFF,
            principalType = PrincipalType.HUMAN
        )
        val decision = boundary.evaluateActionRequest(
            principal = humanStaff,
            request = request(),
            serverProjectId = "p-001"
        )
        assertTrue("Non-AI principal must be denied at AI boundary", decision is AiNotificationSecurityDecision.Denied)
        val deny = decision as AiNotificationSecurityDecision.Denied
        assertEquals(AiNotificationDenialReason.NOT_AN_AI_AGENT, deny.reason)
    }

    @Test
    fun test03_aiPrincipalWithAdminPermission_isAllowed() = runBlocking {
        val adminAi = AuthenticatedPrincipal(
            userId = "ai-admin",
            projectId = "p-001",
            username = "ai_admin",
            role = UserRole.AI_AGENT,
            permissions = setOf(UserPermission.ADMIN_ALL),
            principalType = PrincipalType.AI_AGENT
        )
        val decision = boundary.evaluateActionRequest(
            principal = adminAi,
            request = request(),
            serverProjectId = "p-001"
        )
        assertTrue("AI Agent with ADMIN_ALL permission must be allowed", decision is AiNotificationSecurityDecision.Allowed)
    }

    @Test
    fun test04_customerPrincipal_deniedFromAiEndpoint() = runBlocking {
        val customer = AuthenticatedPrincipal(
            userId = "cust-01",
            projectId = "p-001",
            username = "cust01",
            role = UserRole.CUSTOMER,
            principalType = PrincipalType.HUMAN
        )
        val decision = boundary.evaluateActionRequest(
            principal = customer,
            request = request(),
            serverProjectId = "p-001"
        )
        assertTrue("Customer principal must be denied at AI boundary", decision is AiNotificationSecurityDecision.Denied)
        val deny = decision as AiNotificationSecurityDecision.Denied
        assertEquals(AiNotificationDenialReason.NOT_AN_AI_AGENT, deny.reason)
    }

    @Test
    fun test05_managerPrincipal_deniedFromAiEndpoint() = runBlocking {
        val manager = AuthenticatedPrincipal(
            userId = "mgr-01",
            projectId = "p-001",
            username = "mgr01",
            role = UserRole.MANAGER,
            principalType = PrincipalType.HUMAN
        )
        val decision = boundary.evaluateActionRequest(
            principal = manager,
            request = request(),
            serverProjectId = "p-001"
        )
        assertTrue("Manager principal must be denied at AI boundary", decision is AiNotificationSecurityDecision.Denied)
        val deny = decision as AiNotificationSecurityDecision.Denied
        assertEquals(AiNotificationDenialReason.NOT_AN_AI_AGENT, deny.reason)
    }
}

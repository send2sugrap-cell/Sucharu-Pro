package com.sucharu.sucharupro.data.notification.ai

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.auth.authorization.AuthorizationCapability
import com.sucharu.sucharupro.domain.event.boundary.NotificationChannel
import com.sucharu.sucharupro.domain.notification.ai.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tenant isolation test suite for AI Agent notification interactions (INFRA-04 Step 08).
 */
class AiNotificationTenantIsolationTest {

    private lateinit var boundary: AiAgentNotificationSecurityBoundary

    private fun aiPrincipal(projectId: String) = AuthenticatedPrincipal(
        userId = "agent-001",
        projectId = projectId,
        username = "ai_agent_1",
        role = UserRole.AI_AGENT,
        principalType = PrincipalType.AI_AGENT
    )

    private fun request(projectId: String) = AiNotificationActionRequest(
        projectId = projectId,
        actionType = AiNotificationActionType.CREATE_DRAFT,
        targetRecipientId = "CUST-1",
        targetChannels = setOf(NotificationChannel.IN_APP),
        title = "Order update",
        body = "Safe body content",
        idempotencyKey = "idem-iso-1",
        correlationId = "corr-iso-1"
    )

    @Before
    fun setUp() {
        boundary = AiAgentNotificationSecurityBoundary()
    }

    @Test
    fun test01_crossTenantPrincipal_isDenied() = runBlocking {
        // Principal in Tenant-A trying to submit action against Tenant-B
        val decision = boundary.evaluateActionRequest(
            principal = aiPrincipal("tenant-A"),
            request = request("tenant-B"),
            serverProjectId = "tenant-B"
        )
        assertTrue("Cross-tenant principal must be denied", decision is AiNotificationSecurityDecision.Denied)
        val deny = decision as AiNotificationSecurityDecision.Denied
        assertEquals(AiNotificationDenialReason.TENANT_MISMATCH, deny.reason)
    }

    @Test
    fun test02_crossTenantRequestPayload_isDenied() = runBlocking {
        // Request payload specifies Tenant-B but server context is Tenant-A
        val decision = boundary.evaluateActionRequest(
            principal = aiPrincipal("tenant-A"),
            request = request("tenant-B"),
            serverProjectId = "tenant-A"
        )
        assertTrue("Mismatched request payload tenant must be denied", decision is AiNotificationSecurityDecision.Denied)
        val deny = decision as AiNotificationSecurityDecision.Denied
        assertEquals(AiNotificationDenialReason.TENANT_MISMATCH, deny.reason)
    }

    @Test
    fun test03_sameTenant_isAllowed() = runBlocking {
        val decision = boundary.evaluateActionRequest(
            principal = aiPrincipal("tenant-A"),
            request = request("tenant-A"),
            serverProjectId = "tenant-A"
        )
        // CREATE_DRAFT requires capability
        // AI_AGENT role has no default CREATE_DRAFT unless granted or admin
        // If not granted -> CAPABILITY_MISSING, still tenant check passed
        assertTrue(decision is AiNotificationSecurityDecision.Denied || decision is AiNotificationSecurityDecision.Allowed)
        if (decision is AiNotificationSecurityDecision.Denied) {
            assertNotEquals(AiNotificationDenialReason.TENANT_MISMATCH, decision.reason)
        }
    }

    @Test
    fun test04_readService_crossTenant_isDenied() = runBlocking {
        val readService = AiNotificationReadService()
        val result = readService.getNotificationStatus(
            notificationId = "notif-001",
            principal = aiPrincipal("tenant-A"),
            serverProjectId = "tenant-B"
        )
        assertTrue("Read service must deny cross-tenant access", result is AiReadResult.Denied)
        val denied = result as AiReadResult.Denied
        assertEquals("TENANT_MISMATCH", denied.code)
    }

    @Test
    fun test05_confirmation_crossTenant_isDenied() = runBlocking {
        val repo = InMemoryAiNotificationConfirmationRepository()
        val service = AiNotificationConfirmationService(repo)

        val conf = service.createConfirmationRequest(
            request("tenant-A"),
            aiPrincipal("tenant-A")
        )

        val humanMgrTenantB = AuthenticatedPrincipal(
            userId = "mgr-b",
            projectId = "tenant-B",
            username = "manager_b",
            role = UserRole.MANAGER,
            principalType = PrincipalType.HUMAN
        )

        val result = service.approveConfirmation(conf.confirmationId, humanMgrTenantB)
        assertTrue("Cross-tenant human confirmation must be denied", result is ConfirmationValidationResult.Invalid)
        val invalid = result as ConfirmationValidationResult.Invalid
        assertTrue(invalid.code == "TENANT_MISMATCH" || invalid.code == "CONFIRMATION_NOT_FOUND")
    }
}

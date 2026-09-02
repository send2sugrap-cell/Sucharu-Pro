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
 * Capability test suite verifying explicit capability enforcement (INFRA-04 Step 08).
 */
class AiNotificationCapabilityTest {

    private val grantedCapabilities = mutableSetOf<AuthorizationCapability>()

    private val boundary = AiAgentNotificationSecurityBoundary(
        explicitCapabilityChecker = { _, cap -> grantedCapabilities.contains(cap) }
    )

    private fun aiPrincipal() = AuthenticatedPrincipal(
        userId = "agent-cap",
        projectId = "p-001",
        username = "ai_cap",
        role = UserRole.AI_AGENT,
        principalType = PrincipalType.AI_AGENT
    )

    private fun request(actionType: AiNotificationActionType) = AiNotificationActionRequest(
        projectId = "p-001",
        actionType = actionType,
        targetRecipientId = "CUST-1",
        targetChannels = setOf(NotificationChannel.IN_APP),
        title = "Status",
        body = "Body",
        idempotencyKey = "idem-cap-${actionType.name}",
        correlationId = "corr-cap-1"
    )

    @Before
    fun setUp() {
        grantedCapabilities.clear()
    }

    @Test
    fun test01_missingDraftCapability_isDenied() = runBlocking {
        val decision = boundary.evaluateActionRequest(aiPrincipal(), request(AiNotificationActionType.CREATE_DRAFT), "p-001")
        assertTrue("Missing capability must be denied", decision is AiNotificationSecurityDecision.Denied)
        val deny = decision as AiNotificationSecurityDecision.Denied
        assertEquals(AiNotificationDenialReason.CAPABILITY_MISSING, deny.reason)
    }

    @Test
    fun test02_withDraftCapability_createDraftIsAllowed() = runBlocking {
        grantedCapabilities.add(AuthorizationCapability.AI_CREATE_NOTIFICATION_DRAFT)
        val decision = boundary.evaluateActionRequest(aiPrincipal(), request(AiNotificationActionType.CREATE_DRAFT), "p-001")
        assertTrue("With capability, draft creation must be allowed", decision is AiNotificationSecurityDecision.Allowed)
    }

    @Test
    fun test03_sendRequiresSpecificCapability() = runBlocking {
        // Has draft capability, but attempting send
        grantedCapabilities.add(AuthorizationCapability.AI_CREATE_NOTIFICATION_DRAFT)
        val decision = boundary.evaluateActionRequest(aiPrincipal(), request(AiNotificationActionType.REQUEST_SEND), "p-001")
        assertTrue("Lacking send capability must be denied", decision is AiNotificationSecurityDecision.Denied)
        val deny = decision as AiNotificationSecurityDecision.Denied
        assertEquals(AiNotificationDenialReason.CAPABILITY_MISSING, deny.reason)
    }

    @Test
    fun test04_withSendCapability_requestSendIsAllowed() = runBlocking {
        grantedCapabilities.add(AuthorizationCapability.AI_REQUEST_NOTIFICATION_SEND)
        val decision = boundary.evaluateActionRequest(aiPrincipal(), request(AiNotificationActionType.REQUEST_SEND), "p-001")
        assertTrue("With send capability, request send must pass security boundary", decision is AiNotificationSecurityDecision.Allowed)
    }

    @Test
    fun test05_replayRequiresSpecificCapability() = runBlocking {
        grantedCapabilities.add(AuthorizationCapability.AI_REQUEST_NOTIFICATION_SEND)
        val decision = boundary.evaluateActionRequest(aiPrincipal(), request(AiNotificationActionType.REQUEST_REPLAY), "p-001")
        assertTrue("Replay without explicit capability must be denied", decision is AiNotificationSecurityDecision.Denied)
        val deny = decision as AiNotificationSecurityDecision.Denied
        assertEquals(AiNotificationDenialReason.CAPABILITY_MISSING, deny.reason)
    }

    @Test
    fun test06_withReplayCapability_replayIsAllowed() = runBlocking {
        grantedCapabilities.add(AuthorizationCapability.AI_REQUEST_NOTIFICATION_REPLAY)
        val decision = boundary.evaluateActionRequest(aiPrincipal(), request(AiNotificationActionType.REQUEST_REPLAY), "p-001")
        assertTrue("With replay capability, replay request passes boundary", decision is AiNotificationSecurityDecision.Allowed)
    }

    @Test
    fun test07_withSuppressionCapability_suppressionIsAllowed() = runBlocking {
        grantedCapabilities.add(AuthorizationCapability.AI_REQUEST_NOTIFICATION_SUPPRESSION)
        val decision = boundary.evaluateActionRequest(aiPrincipal(), request(AiNotificationActionType.REQUEST_SUPPRESSION), "p-001")
        assertTrue("With suppression capability, suppression request passes boundary", decision is AiNotificationSecurityDecision.Allowed)
    }
}

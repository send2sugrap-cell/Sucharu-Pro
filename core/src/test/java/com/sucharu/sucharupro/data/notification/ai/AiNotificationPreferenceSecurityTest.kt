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
 * Preference boundary and mandatory security alerts immutability tests (INFRA-04 Step 08).
 */
class AiNotificationPreferenceSecurityTest {

    private lateinit var boundary: AiAgentNotificationSecurityBoundary

    private val adminAi = AuthenticatedPrincipal(
        userId = "ai-pref",
        projectId = "p-001",
        username = "ai_pref",
        role = UserRole.AI_AGENT,
        permissions = setOf(UserPermission.ADMIN_ALL),
        principalType = PrincipalType.AI_AGENT
    )

    @Before
    fun setUp() {
        boundary = AiAgentNotificationSecurityBoundary()
    }

    @Test
    fun test01_disablingSecurityNotifications_isBlocked() = runBlocking {
        val req = AiNotificationActionRequest(
            projectId = "p-001",
            actionType = AiNotificationActionType.REQUEST_PREFERENCE_UPDATE,
            targetRecipientId = "CUST-1",
            targetChannels = setOf(NotificationChannel.EMAIL),
            title = "Update Preferences",
            body = "Disable alerts",
            metadata = mapOf("disableSecurityAlerts" to "true"),
            idempotencyKey = "idem-pref-sec-1",
            correlationId = "corr-pref-1"
        )
        val decision = boundary.evaluateActionRequest(adminAi, req, "p-001")
        assertTrue("Attempt to disable mandatory security alerts must be denied", decision is AiNotificationSecurityDecision.Denied)
        val deny = decision as AiNotificationSecurityDecision.Denied
        assertEquals(AiNotificationDenialReason.MANDATORY_SECURITY_NOTIFICATION_IMMUTABLE, deny.reason)
    }

    @Test
    fun test02_safePreferenceUpdate_isAllowed() = runBlocking {
        val req = AiNotificationActionRequest(
            projectId = "p-001",
            actionType = AiNotificationActionType.REQUEST_PREFERENCE_UPDATE,
            targetRecipientId = "CUST-1",
            targetChannels = setOf(NotificationChannel.EMAIL),
            title = "Update Preferences",
            body = "Opt into email promotions",
            metadata = mapOf("promotions" to "true"),
            idempotencyKey = "idem-pref-safe-1",
            correlationId = "corr-pref-2"
        )
        val decision = boundary.evaluateActionRequest(adminAi, req, "p-001")
        assertTrue("Safe preference update proposal must pass security boundary", decision is AiNotificationSecurityDecision.Allowed)
    }
}

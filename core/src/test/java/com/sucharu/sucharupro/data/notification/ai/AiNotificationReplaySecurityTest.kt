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
 * Replay security, confirmation requirement, and provider isolation tests (INFRA-04 Step 08).
 */
class AiNotificationReplaySecurityTest {

    private lateinit var confirmationRepo: InMemoryAiNotificationConfirmationRepository
    private lateinit var confirmationService: AiNotificationConfirmationService
    private lateinit var gateway: AiNotificationActionGateway

    private val adminAi = AuthenticatedPrincipal(
        userId = "ai-rep",
        projectId = "p-001",
        username = "ai_rep",
        role = UserRole.AI_AGENT,
        permissions = setOf(UserPermission.ADMIN_ALL),
        principalType = PrincipalType.AI_AGENT
    )

    private val humanManager = AuthenticatedPrincipal(
        userId = "mgr-rep",
        projectId = "p-001",
        username = "human_mgr_rep",
        role = UserRole.MANAGER,
        principalType = PrincipalType.HUMAN
    )

    @Before
    fun setUp() {
        confirmationRepo = InMemoryAiNotificationConfirmationRepository()
        confirmationService = AiNotificationConfirmationService(confirmationRepo)
        gateway = AiNotificationActionGateway(
            confirmationService = confirmationService
        )
    }

    @Test
    fun test01_replayWithoutConfirmation_requiresConfirmation() = runBlocking {
        val req = AiNotificationActionRequest(
            projectId = "p-001",
            actionType = AiNotificationActionType.REQUEST_REPLAY,
            targetRecipientId = "CUST-1",
            targetChannels = setOf(NotificationChannel.EMAIL),
            title = "Replay Notification",
            body = "Order replayed",
            idempotencyKey = "idem-replay-1",
            correlationId = "corr-rep-1"
        )
        val result = gateway.processActionRequest(adminAi, req, "p-001")
        assertTrue("Replay must require human confirmation", result is AiNotificationActionResult.RequiresConfirmation)
    }

    @Test
    fun test02_replayWithHumanConfirmation_isScheduled() = runBlocking {
        val req = AiNotificationActionRequest(
            projectId = "p-001",
            actionType = AiNotificationActionType.REQUEST_REPLAY,
            targetRecipientId = "CUST-1",
            targetChannels = setOf(NotificationChannel.EMAIL),
            title = "Replay Notification",
            body = "Order replayed",
            idempotencyKey = "idem-replay-2",
            correlationId = "corr-rep-2"
        )
        val reqResult = gateway.processActionRequest(adminAi, req, "p-001")
        val confId = (reqResult as AiNotificationActionResult.RequiresConfirmation).confirmationId

        // Human Manager approves
        confirmationService.approveConfirmation(confId, humanManager)

        val confirmedReq = req.copy(
            confirmationId = confId,
            idempotencyKey = "idem-replay-exec-2"
        )
        val execResult = gateway.processActionRequest(adminAi, confirmedReq, "p-001")
        assertTrue("Confirmed replay must be scheduled", execResult is AiNotificationActionResult.ExecutionSubmitted)
        val submitted = execResult as AiNotificationActionResult.ExecutionSubmitted
        assertEquals("REPLAY_SUBMITTED", submitted.status)
    }
}

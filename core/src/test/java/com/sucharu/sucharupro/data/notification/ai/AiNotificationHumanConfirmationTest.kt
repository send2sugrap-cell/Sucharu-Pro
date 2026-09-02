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
 * Human confirmation lifecycle, separation of duties, and authorization tests (INFRA-04 Step 08).
 */
class AiNotificationHumanConfirmationTest {

    private lateinit var confirmationRepo: InMemoryAiNotificationConfirmationRepository
    private lateinit var confirmationService: AiNotificationConfirmationService

    private val aiAgent = AuthenticatedPrincipal(
        userId = "agent-req",
        projectId = "p-001",
        username = "ai_requester",
        role = UserRole.AI_AGENT,
        permissions = setOf(UserPermission.ADMIN_ALL),
        principalType = PrincipalType.AI_AGENT
    )

    private val humanManager = AuthenticatedPrincipal(
        userId = "mgr-001",
        projectId = "p-001",
        username = "human_mgr",
        role = UserRole.MANAGER,
        principalType = PrincipalType.HUMAN
    )

    private val humanStaff = AuthenticatedPrincipal(
        userId = "staff-001",
        projectId = "p-001",
        username = "human_staff",
        role = UserRole.STAFF,
        principalType = PrincipalType.HUMAN
    )

    private fun request(actionType: AiNotificationActionType = AiNotificationActionType.REQUEST_SEND, recipientId: String = "CUST-1") =
        AiNotificationActionRequest(
            projectId = "p-001",
            actionType = actionType,
            targetRecipientId = recipientId,
            targetChannels = setOf(NotificationChannel.IN_APP),
            title = "Order Ready",
            body = "Your printing order is complete",
            idempotencyKey = "idem-conf-1",
            correlationId = "corr-conf-1"
        )

    @Before
    fun setUp() {
        confirmationRepo = InMemoryAiNotificationConfirmationRepository()
        confirmationService = AiNotificationConfirmationService(confirmationRepo)
    }

    @Test
    fun test01_createConfirmation_isPending() = runBlocking {
        val conf = confirmationService.createConfirmationRequest(request(), aiAgent)
        assertEquals(AiConfirmationStatus.PENDING, conf.status)
        assertEquals("p-001", conf.projectId)
        assertEquals("agent-req", conf.requestedByAgentId)
    }

    @Test
    fun test02_humanManagerCanApprove() = runBlocking {
        val conf = confirmationService.createConfirmationRequest(request(), aiAgent)
        val result = confirmationService.approveConfirmation(conf.confirmationId, humanManager)
        assertTrue("Human manager must be able to approve confirmation", result is ConfirmationValidationResult.Valid)
        val valid = result as ConfirmationValidationResult.Valid
        assertEquals(AiConfirmationStatus.APPROVED, valid.confirmation.status)
        assertEquals("mgr-001", valid.confirmation.approvedByHumanId)
    }

    @Test
    fun test03_aiAgentCannotApproveConfirmation() = runBlocking {
        val conf = confirmationService.createConfirmationRequest(request(), aiAgent)
        val result = confirmationService.approveConfirmation(conf.confirmationId, aiAgent)
        assertTrue("AI Agent must be denied from approving confirmations", result is ConfirmationValidationResult.Invalid)
        val invalid = result as ConfirmationValidationResult.Invalid
        assertEquals("CONFIRMATION_AI_APPROVAL_DENIED", invalid.code)
    }

    @Test
    fun test04_staffLacksRoleToApprove() = runBlocking {
        val conf = confirmationService.createConfirmationRequest(request(), aiAgent)
        val result = confirmationService.approveConfirmation(conf.confirmationId, humanStaff)
        assertTrue("STAFF lacks required role (requires MANAGER or ADMIN)", result is ConfirmationValidationResult.Invalid)
        val invalid = result as ConfirmationValidationResult.Invalid
        assertEquals("INSUFFICIENT_APPROVER_ROLE", invalid.code)
    }

    @Test
    fun test05_humanManagerCanReject() = runBlocking {
        val conf = confirmationService.createConfirmationRequest(request(), aiAgent)
        val result = confirmationService.rejectConfirmation(conf.confirmationId, humanManager, "Not authorized by customer")
        assertTrue("Human manager can reject confirmation", result is ConfirmationValidationResult.Valid)
        val valid = result as ConfirmationValidationResult.Valid
        assertEquals(AiConfirmationStatus.REJECTED, valid.confirmation.status)
        assertEquals("Not authorized by customer", valid.confirmation.rejectionReason)
    }

    @Test
    fun test06_validateForExecution_mismatchedAction_isDenied() = runBlocking {
        val conf = confirmationService.createConfirmationRequest(request(AiNotificationActionType.REQUEST_SEND), aiAgent)
        confirmationService.approveConfirmation(conf.confirmationId, humanManager)

        // Attempting to execute REPLAY using SEND confirmation
        val replayReq = request(AiNotificationActionType.REQUEST_REPLAY).copy(confirmationId = conf.confirmationId)
        val result = confirmationService.validateForExecution(conf.confirmationId, replayReq)
        assertTrue("Execution with wrong actionType confirmation must be denied", result is ConfirmationValidationResult.Invalid)
        val invalid = result as ConfirmationValidationResult.Invalid
        assertEquals("CONFIRMATION_WRONG_ACTION", invalid.code)
    }

    @Test
    fun test07_selfApproval_isForbidden() = runBlocking {
        // If human user tried to approve a confirmation requested by themselves
        val sameUserAgent = AuthenticatedPrincipal(
            userId = "mgr-self",
            projectId = "p-001",
            username = "mgr_self",
            role = UserRole.MANAGER,
            principalType = PrincipalType.HUMAN
        )
        val conf = confirmationService.createConfirmationRequest(request(), sameUserAgent)
        val result = confirmationService.approveConfirmation(conf.confirmationId, sameUserAgent)
        assertTrue("Self-approval must be forbidden", result is ConfirmationValidationResult.Invalid)
        val invalid = result as ConfirmationValidationResult.Invalid
        assertEquals("CONFIRMATION_SELF_APPROVED", invalid.code)
    }
}

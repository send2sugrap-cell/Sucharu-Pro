package com.sucharu.sucharupro.data.notification.ai

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.api.model.UserPermission
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.event.integration.notification.NotificationDeliveryResult
import com.sucharu.sucharupro.data.event.integration.notification.NotificationDispatchService
import com.sucharu.sucharupro.data.event.integration.notification.NotificationProvider
import com.sucharu.sucharupro.data.event.integration.notification.NotificationRecipient
import com.sucharu.sucharupro.domain.event.boundary.NotificationChannel
import com.sucharu.sucharupro.domain.notification.ai.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * End-to-end pipeline and Draft vs Execution tests for AiNotificationActionGateway (INFRA-04 Step 08).
 */
class AiNotificationActionGatewayTest {

    private lateinit var confirmationRepo: InMemoryAiNotificationConfirmationRepository
    private lateinit var confirmationService: AiNotificationConfirmationService
    private lateinit var actionRecordRepo: InMemoryAiNotificationActionRecordRepository
    private lateinit var dispatchService: NotificationDispatchService
    private lateinit var gateway: AiNotificationActionGateway

    private val adminAi = AuthenticatedPrincipal(
        userId = "ai-gateway",
        projectId = "p-001",
        username = "ai_gateway",
        role = UserRole.AI_AGENT,
        permissions = setOf(UserPermission.ADMIN_ALL),
        principalType = PrincipalType.AI_AGENT
    )

    private val humanManager = AuthenticatedPrincipal(
        userId = "mgr-gw",
        projectId = "p-001",
        username = "human_mgr_gw",
        role = UserRole.MANAGER,
        principalType = PrincipalType.HUMAN
    )

    @Before
    fun setUp() {
        confirmationRepo = InMemoryAiNotificationConfirmationRepository()
        confirmationService = AiNotificationConfirmationService(confirmationRepo)
        actionRecordRepo = InMemoryAiNotificationActionRecordRepository()
        dispatchService = NotificationDispatchService()

        // Register dummy provider for IN_APP
        dispatchService.registerProvider(object : NotificationProvider {
            override val channel: NotificationChannel get() = NotificationChannel.IN_APP
            override suspend fun deliver(
                recipient: NotificationRecipient,
                title: String,
                body: String,
                metadata: Map<String, String>,
                idempotencyKey: String
            ): NotificationDeliveryResult = NotificationDeliveryResult(
                channel = NotificationChannel.IN_APP,
                isSuccess = true,
                providerRef = "mock-msg-123"
            )
        })

        gateway = AiNotificationActionGateway(
            confirmationService = confirmationService,
            actionRecordRepository = actionRecordRepo,
            dispatchService = dispatchService
        )
    }

    @Test
    fun test01_draftCreation_doesNotTriggerSend() = runBlocking {
        val req = AiNotificationActionRequest(
            projectId = "p-001",
            actionType = AiNotificationActionType.CREATE_DRAFT,
            targetRecipientId = "CUST-1",
            targetChannels = setOf(NotificationChannel.IN_APP),
            title = "Proposed Order Update",
            body = "Your order is in processing",
            idempotencyKey = "idem-draft-1",
            correlationId = "corr-draft-1"
        )
        val result = gateway.processActionRequest(adminAi, req, "p-001")
        assertTrue("CREATE_DRAFT must produce DraftCreated", result is AiNotificationActionResult.DraftCreated)
        val draft = result as AiNotificationActionResult.DraftCreated
        assertTrue("Draft must contain draft ID", draft.draftId.startsWith("draft-"))
    }

    @Test
    fun test02_sendWithoutConfirmation_returnsRequiresConfirmation() = runBlocking {
        val req = AiNotificationActionRequest(
            projectId = "p-001",
            actionType = AiNotificationActionType.REQUEST_SEND,
            targetRecipientId = "CUST-1",
            targetChannels = setOf(NotificationChannel.IN_APP),
            title = "Urgent Notification",
            body = "Order ready for pickup",
            idempotencyKey = "idem-send-unconf-1",
            correlationId = "corr-send-unconf-1"
        )
        val result = gateway.processActionRequest(adminAi, req, "p-001")
        assertTrue("Send without confirmation must require confirmation", result is AiNotificationActionResult.RequiresConfirmation)
        val reqConf = result as AiNotificationActionResult.RequiresConfirmation
        assertTrue("Confirmation ID must be populated", reqConf.confirmationId.startsWith("conf-"))
    }

    @Test
    fun test03_sendWithApprovedConfirmation_executesSuccessfully() = runBlocking {
        val sendReq = AiNotificationActionRequest(
            projectId = "p-001",
            actionType = AiNotificationActionType.REQUEST_SEND,
            targetRecipientId = "CUST-1",
            targetChannels = setOf(NotificationChannel.IN_APP),
            title = "Confirmed Order Ready",
            body = "Order ready for pickup",
            idempotencyKey = "idem-send-conf-1",
            correlationId = "corr-send-conf-1"
        )

        // 1. Create confirmation requirement
        val reqResult = gateway.processActionRequest(adminAi, sendReq, "p-001")
        val confId = (reqResult as AiNotificationActionResult.RequiresConfirmation).confirmationId

        // 2. Human Manager approves confirmation
        val approveResult = confirmationService.approveConfirmation(confId, humanManager)
        assertTrue("Approval must succeed", approveResult is ConfirmationValidationResult.Valid)

        // 3. AI Agent submits request WITH approved confirmationId
        val confirmedReq = sendReq.copy(
            confirmationId = confId,
            idempotencyKey = "idem-send-conf-exec-1"
        )
        val execResult = gateway.processActionRequest(adminAi, confirmedReq, "p-001")
        assertTrue("Confirmed request must execute", execResult is AiNotificationActionResult.ExecutionSubmitted)
        val submitted = execResult as AiNotificationActionResult.ExecutionSubmitted
        assertEquals("DELIVERED", submitted.status)
    }

    @Test
    fun test04_sendWithRejectedConfirmation_isDenied() = runBlocking {
        val sendReq = AiNotificationActionRequest(
            projectId = "p-001",
            actionType = AiNotificationActionType.REQUEST_SEND,
            targetRecipientId = "CUST-1",
            targetChannels = setOf(NotificationChannel.IN_APP),
            title = "Rejected Notification",
            body = "This will be rejected",
            idempotencyKey = "idem-send-rej-1",
            correlationId = "corr-send-rej-1"
        )

        val reqResult = gateway.processActionRequest(adminAi, sendReq, "p-001")
        val confId = (reqResult as AiNotificationActionResult.RequiresConfirmation).confirmationId

        // Human Manager rejects confirmation
        confirmationService.rejectConfirmation(confId, humanManager, "Spam content")

        // AI Agent tries to execute with rejected confirmation
        val confirmedReq = sendReq.copy(
            confirmationId = confId,
            idempotencyKey = "idem-send-rej-exec-1"
        )
        val execResult = gateway.processActionRequest(adminAi, confirmedReq, "p-001")
        assertTrue("Execution with rejected confirmation must be denied", execResult is AiNotificationActionResult.Denied)
        val denied = execResult as AiNotificationActionResult.Denied
        assertEquals("CONFIRMATION_NOT_APPROVED", denied.reasonCode)
    }
}

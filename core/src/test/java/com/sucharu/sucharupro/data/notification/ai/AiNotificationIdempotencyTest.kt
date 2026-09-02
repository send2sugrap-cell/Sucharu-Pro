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
 * Idempotency test suite for AI notification actions (INFRA-04 Step 08).
 */
class AiNotificationIdempotencyTest {

    private lateinit var actionRecordRepo: InMemoryAiNotificationActionRecordRepository
    private lateinit var confirmationRepo: InMemoryAiNotificationConfirmationRepository
    private lateinit var confirmationService: AiNotificationConfirmationService
    private lateinit var gateway: AiNotificationActionGateway

    private val adminAi = AuthenticatedPrincipal(
        userId = "ai-idem",
        projectId = "p-001",
        username = "ai_idem",
        role = UserRole.AI_AGENT,
        permissions = setOf(UserPermission.ADMIN_ALL),
        principalType = PrincipalType.AI_AGENT
    )

    private fun draftRequest(key: String) = AiNotificationActionRequest(
        projectId = "p-001",
        actionType = AiNotificationActionType.CREATE_DRAFT,
        targetRecipientId = "CUST-1",
        targetChannels = setOf(NotificationChannel.IN_APP),
        title = "Draft title",
        body = "Draft body",
        idempotencyKey = key,
        correlationId = "corr-idem-1"
    )

    @Before
    fun setUp() {
        actionRecordRepo = InMemoryAiNotificationActionRecordRepository()
        confirmationRepo = InMemoryAiNotificationConfirmationRepository()
        confirmationService = AiNotificationConfirmationService(confirmationRepo)
        gateway = AiNotificationActionGateway(
            confirmationService = confirmationService,
            actionRecordRepository = actionRecordRepo
        )
    }

    @Test
    fun test01_firstActionExecution_createsRecord() = runBlocking {
        val req = draftRequest("idem-key-1")
        val result = gateway.processActionRequest(adminAi, req, "p-001")
        assertTrue("First action must create draft", result is AiNotificationActionResult.DraftCreated)
    }

    @Test
    fun test02_duplicateActionWithSameKey_returnsCachedResponse() = runBlocking {
        val req = draftRequest("idem-key-2")
        val first = gateway.processActionRequest(adminAi, req, "p-001")
        assertTrue(first is AiNotificationActionResult.DraftCreated)

        // Second call with same idempotency key
        val second = gateway.processActionRequest(adminAi, req, "p-001")
        assertTrue("Duplicate request must return cached execution result", second is AiNotificationActionResult.ExecutionSubmitted)
        val submitted = second as AiNotificationActionResult.ExecutionSubmitted
        assertTrue("Response message must indicate idempotency replay", submitted.message.contains("Idempotent response"))
    }

    @Test
    fun test03_differentKey_executesSeparately() = runBlocking {
        val req1 = draftRequest("idem-key-3A")
        val req2 = draftRequest("idem-key-3B")
        val res1 = gateway.processActionRequest(adminAi, req1, "p-001")
        val res2 = gateway.processActionRequest(adminAi, req2, "p-001")
        assertTrue(res1 is AiNotificationActionResult.DraftCreated)
        assertTrue(res2 is AiNotificationActionResult.DraftCreated)
        val draft1 = (res1 as AiNotificationActionResult.DraftCreated).draftId
        val draft2 = (res2 as AiNotificationActionResult.DraftCreated).draftId
        assertNotEquals("Different idempotency keys must produce distinct drafts", draft1, draft2)
    }
}

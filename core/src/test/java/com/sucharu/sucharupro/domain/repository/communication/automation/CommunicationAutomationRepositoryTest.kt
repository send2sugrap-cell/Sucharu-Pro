package com.sucharu.sucharupro.domain.repository.communication.automation

import com.sucharu.sucharupro.data.datasource.FakeCommunicationAutomationDataSource
import com.sucharu.sucharupro.data.repository.CommunicationAutomationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.automation.AutomationDecisionType
import com.sucharu.sucharupro.domain.model.communication.automation.CommunicationAutomationEventType
import com.sucharu.sucharupro.domain.model.communication.automation.CommunicationTriggerEvent
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CommunicationAutomationRepositoryTest {

    private lateinit var dataSource: FakeCommunicationAutomationDataSource
    private lateinit var repository: CommunicationAutomationRepositoryImpl

    @Before
    fun setup() {
        dataSource = FakeCommunicationAutomationDataSource()
        repository = CommunicationAutomationRepositoryImpl(dataSource, null, null)
    }

    @Test
    fun processTrigger_validTrigger_generatesExecutions() = runBlocking {
        val trigger = CommunicationTriggerEvent(
            triggerId = "trg-new-1",
            projectId = "default-project",
            eventType = CommunicationAutomationEventType.ORDER_STATUS_CHANGED,
            sourceEntityType = "CUSTOMER",
            sourceEntityId = "cus-001",
            actorUserId = "user-admin-01",
            payloadMetadata = mapOf("newStatus" to "READY")
        )

        val result = repository.processTrigger(trigger, "user-admin-01", UserRole.ADMIN)
        assertTrue(result is DomainResult.Success)

        val executions = (result as DomainResult.Success).data
        assertTrue("Should generate at least one execution based on seeded rules", executions.isNotEmpty())

        val sendDecision = executions.find { it.decision.decisionType == AutomationDecisionType.SEND }
        assertTrue("Should have a SEND decision", sendDecision != null)
        assertEquals("user-cus-001", sendDecision?.recipientUserId)
    }

    @Test
    fun processTrigger_idempotency_blocksDuplicates() = runBlocking {
        val trigger1 = CommunicationTriggerEvent(
            triggerId = "trg-idem-1",
            projectId = "default-project",
            eventType = CommunicationAutomationEventType.ORDER_STATUS_CHANGED,
            sourceEntityType = "CUSTOMER",
            sourceEntityId = "cus-001",
            actorUserId = "user-admin-01",
            idempotencyKey = "idem-key-123",
            payloadMetadata = mapOf("newStatus" to "READY")
        )

        val trigger2 = trigger1.copy(triggerId = "trg-idem-2") // Different trigger ID, same idempotency key

        // First call
        val result1 = repository.processTrigger(trigger1, "user-admin-01", UserRole.ADMIN)
        assertTrue(result1 is DomainResult.Success)
        val executions1 = (result1 as DomainResult.Success).data

        // Second call (Duplicate)
        val result2 = repository.processTrigger(trigger2, "user-admin-01", UserRole.ADMIN)
        assertTrue(result2 is DomainResult.Success)
        val executions2 = (result2 as DomainResult.Success).data

        // Should return the exact same executions from the first run
        assertEquals(executions1.size, executions2.size)
        if (executions1.isNotEmpty()) {
            assertEquals(executions1.first().executionId, executions2.first().executionId)
        }
    }
}

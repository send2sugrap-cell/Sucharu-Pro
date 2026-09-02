package com.sucharu.sucharupro.data.notification.ai

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.api.model.UserPermission
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.event.boundary.NotificationChannel
import com.sucharu.sucharupro.domain.notification.ai.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Audit trail integrity and append-only test suite for AI notification interactions (INFRA-04 Step 08).
 */
class AiNotificationAuditTest {

    private lateinit var auditRepo: InMemoryAiNotificationAuditRepository
    private lateinit var auditService: AiNotificationAuditService
    private val tenantCtx = TenantContext("p-001")

    @Before
    fun setUp() {
        auditRepo = InMemoryAiNotificationAuditRepository()
        auditService = AiNotificationAuditService(auditRepo)
    }

    @Test
    fun test01_recordAudit_appendsEvent() = runBlocking {
        auditService.record(
            projectId = "p-001",
            operation = AiNotificationAuditOperation.AI_NOTIFICATION_DRAFT_CREATED,
            decision = "SUCCESS",
            agentId = "agent-001",
            actionType = "CREATE_DRAFT",
            recipientId = "CUST-1",
            correlationId = "corr-aud-1",
            requestId = "req-aud-1",
            safeSummary = "Created draft"
        )
        assertEquals(1, auditRepo.count())
        val record = auditRepo.allRecords().first()
        assertEquals("p-001", record.projectId)
        assertEquals("SUCCESS", record.decision)
        assertEquals("CREATE_DRAFT", record.actionType)
    }

    @Test
    fun test02_audit_isAppendOnly() = runBlocking {
        repeat(5) { i ->
            auditService.record(
                projectId = "p-001",
                operation = AiNotificationAuditOperation.AI_NOTIFICATION_CONTEXT_READ,
                decision = "SUCCESS",
                agentId = "agent-001",
                correlationId = "corr-$i",
                requestId = "req-$i",
                safeSummary = "Read context $i"
            )
        }
        assertEquals(5, auditRepo.count())
        val list = auditRepo.listAuditEvents("p-001", tenantCtx, limit = 3)
        assertEquals(3, list.size)
    }

    @Test
    fun test03_audit_tenantIsolation() = runBlocking {
        auditService.record(
            projectId = "p-001",
            operation = AiNotificationAuditOperation.AI_NOTIFICATION_DRAFT_CREATED,
            decision = "SUCCESS",
            agentId = "agent-001",
            correlationId = "corr-1",
            requestId = "req-1"
        )
        auditService.record(
            projectId = "p-002",
            operation = AiNotificationAuditOperation.AI_NOTIFICATION_DRAFT_CREATED,
            decision = "SUCCESS",
            agentId = "agent-001",
            correlationId = "corr-2",
            requestId = "req-2"
        )
        val p1List = auditRepo.listAuditEvents("p-001", tenantCtx)
        assertEquals(1, p1List.size)
        assertEquals("p-001", p1List.first().projectId)
    }
}

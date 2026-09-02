package com.sucharu.sucharupro.data.workflow.governance

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.event.MockPostgresEventDatabase
import com.sucharu.sucharupro.data.workflow.control.WorkflowControlPlaneService
import com.sucharu.sucharupro.data.workflow.control.WorkflowRealTimeBridge
import com.sucharu.sucharupro.data.workflow.postgres.*
import com.sucharu.sucharupro.domain.event.boundary.RealTimeEventFrame
import com.sucharu.sucharupro.domain.workflow.governance.CreateWorkflowDefinitionRequest
import com.sucharu.sucharupro.domain.workflow.governance.TimelineEventType
import com.sucharu.sucharupro.domain.workflow.model.WorkflowInstance
import com.sucharu.sucharupro.domain.workflow.model.WorkflowStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

class WorkflowRealTimeAndAuditTest {

    private lateinit var bridge: WorkflowRealTimeBridge
    private lateinit var controlPlaneService: WorkflowControlPlaneService

    private val adminPrincipal = AuthenticatedPrincipal(
        userId = "usr_admin_1",
        projectId = "PRJ-TENANT-A",
        username = "admin_user",
        role = UserRole.ADMIN,
        principalType = PrincipalType.HUMAN,
        permissions = emptySet()
    )

    @Before
    fun setup() {
        bridge = WorkflowRealTimeBridge()
        val mockDb = MockPostgresEventDatabase()

        controlPlaneService = WorkflowControlPlaneService(
            definitionRepository = PostgresWorkflowDefinitionRepository(mockDb),
            instanceRepository = PostgresWorkflowInstanceRepository(mockDb),
            stepExecutionRepository = PostgresWorkflowStepExecutionRepository(mockDb),
            compensationRepository = PostgresWorkflowCompensationRepository(mockDb),
            approvalRepository = PostgresWorkflowApprovalRepository(mockDb),
            idempotencyStore = PostgresWorkflowIdempotencyStore(mockDb),
            realTimeBridge = bridge
        )
    }

    @Test
    fun testRealTimeStreamingWithCredentialRedaction() {
        val receivedFrames = mutableListOf<RealTimeEventFrame>()
        val unsubscribe = bridge.subscribe("PRJ-TENANT-A") { frame ->
            receivedFrames.add(frame)
        }

        val instance = WorkflowInstance(
            workflowId = "wf-rt-01",
            projectId = "PRJ-TENANT-A",
            definitionId = "def-test",
            versionId = "v1",
            executionId = UUID.randomUUID().toString(),
            actorId = "usr_admin_1",
            status = WorkflowStatus.RUNNING
        )

        bridge.publishWorkflowEvent(
            instance = instance,
            eventType = TimelineEventType.STEP_COMPLETED,
            title = "Plate Exposure Completed",
            details = mapOf(
                "step" to "laser-plate",
                "secret_key" to "super-secret-password-123",
                "auth_token" to "jwt-raw-token"
            )
        )

        assertEquals(1, receivedFrames.size)
        val frame = receivedFrames.first()
        assertEquals("PRJ-TENANT-A", frame.projectId)
        assertEquals("wf-rt-01", frame.aggregateId)
        assertEquals("[REDACTED]", frame.payloadSummary["secret_key"])
        assertEquals("[REDACTED]", frame.payloadSummary["auth_token"])

        unsubscribe()
    }

    @Test
    fun testRealTimeTenantIsolation() {
        val tenantAFrames = mutableListOf<RealTimeEventFrame>()
        val tenantBFrames = mutableListOf<RealTimeEventFrame>()

        bridge.subscribe("PRJ-TENANT-A") { tenantAFrames.add(it) }
        bridge.subscribe("PRJ-TENANT-B") { tenantBFrames.add(it) }

        val instanceA = WorkflowInstance(
            workflowId = "wf-a-01",
            projectId = "PRJ-TENANT-A",
            definitionId = "def-a",
            versionId = "v1",
            executionId = UUID.randomUUID().toString(),
            actorId = "usr_admin_1",
            status = WorkflowStatus.RUNNING
        )

        bridge.publishWorkflowEvent(instanceA, TimelineEventType.WORKFLOW_STARTED, "Started A")

        assertEquals(1, tenantAFrames.size)
        assertEquals(0, tenantBFrames.size)
    }

    @Test
    fun testAuditTrailLogging() = runBlocking {
        controlPlaneService.createDefinition(
            CreateWorkflowDefinitionRequest(name = "Audit Test", description = "Test", category = "PRINT"),
            adminPrincipal
        )

        val logs = controlPlaneService.getAuditLogs(limit = 10, principal = adminPrincipal)
        assertTrue(logs.isNotEmpty())
        val first = logs.first()
        assertEquals(adminPrincipal.userId, first.actorId)
        assertEquals("CREATE_WORKFLOW_DEFINITION", first.operation)
    }
}

package com.sucharu.sucharupro.data.workflow.governance

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.event.MockPostgresEventDatabase
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.workflow.control.WorkflowControlPlaneService
import com.sucharu.sucharupro.data.workflow.postgres.*
import com.sucharu.sucharupro.domain.workflow.governance.*
import com.sucharu.sucharupro.domain.workflow.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

class WorkflowControlOperationsTest {

    private lateinit var mockDb: MockPostgresEventDatabase
    private lateinit var controlPlaneService: WorkflowControlPlaneService

    private val adminPrincipal = AuthenticatedPrincipal(
        userId = "usr_admin_1",
        projectId = "PRJ-TEST",
        username = "admin_user",
        role = UserRole.ADMIN,
        principalType = PrincipalType.HUMAN,
        permissions = emptySet()
    )

    private val managerPrincipal = AuthenticatedPrincipal(
        userId = "usr_mgr_1",
        projectId = "PRJ-TEST",
        username = "mgr_user",
        role = UserRole.MANAGER,
        principalType = PrincipalType.HUMAN,
        permissions = emptySet()
    )

    @Before
    fun setup() {
        mockDb = MockPostgresEventDatabase()
        controlPlaneService = WorkflowControlPlaneService(
            definitionRepository = PostgresWorkflowDefinitionRepository(mockDb),
            instanceRepository = PostgresWorkflowInstanceRepository(mockDb),
            stepExecutionRepository = PostgresWorkflowStepExecutionRepository(mockDb),
            compensationRepository = PostgresWorkflowCompensationRepository(mockDb),
            approvalRepository = PostgresWorkflowApprovalRepository(mockDb),
            idempotencyStore = PostgresWorkflowIdempotencyStore(mockDb)
        )
    }

    @Test
    fun testPauseAndResumeWorkflowLifecycle() = runBlocking {
        val def = controlPlaneService.createDefinition(
            CreateWorkflowDefinitionRequest(name = "Brochure Workflow", description = "Test", category = "PRINT"),
            managerPrincipal
        )

        val instance = WorkflowInstance(
            workflowId = "wf-inst-test-1",
            projectId = managerPrincipal.projectId,
            definitionId = def.definitionId,
            versionId = "v1",
            executionId = UUID.randomUUID().toString(),
            actorId = managerPrincipal.userId,
            status = WorkflowStatus.RUNNING,
            currentStepId = "init-step"
        )
        val instanceRepo = PostgresWorkflowInstanceRepository(mockDb)
        instanceRepo.createInstance(instance, TenantContext(managerPrincipal.projectId))

        // 1. Pause
        val paused = controlPlaneService.pauseWorkflow("wf-inst-test-1", WorkflowPauseRequest("Maintenance"), managerPrincipal)
        assertEquals(WorkflowStatus.PAUSED, paused.status)

        // 2. Resume
        val resumed = controlPlaneService.resumeWorkflow("wf-inst-test-1", WorkflowResumeRequest(mapOf("resumed" to "true")), managerPrincipal)
        assertEquals(WorkflowStatus.RUNNING, resumed.status)
        assertEquals("true", resumed.context["resumed"])
    }

    @Test
    fun testCancelWorkflowSetsTerminalCancelled() = runBlocking {
        val def = controlPlaneService.createDefinition(
            CreateWorkflowDefinitionRequest(name = "Print Job", description = "Test", category = "PRINT"),
            managerPrincipal
        )

        val instance = WorkflowInstance(
            workflowId = "wf-inst-test-2",
            projectId = managerPrincipal.projectId,
            definitionId = def.definitionId,
            versionId = "v1",
            executionId = UUID.randomUUID().toString(),
            actorId = managerPrincipal.userId,
            status = WorkflowStatus.RUNNING
        )
        PostgresWorkflowInstanceRepository(mockDb).createInstance(instance, TenantContext(managerPrincipal.projectId))

        val cancelled = controlPlaneService.cancelWorkflow("wf-inst-test-2", WorkflowCancelRequest("Customer requested stop"), managerPrincipal)
        assertEquals(WorkflowStatus.CANCELLED, cancelled.status)
    }

    @Test
    fun testAdminReplayDeadLetterWorkflow() = runBlocking {
        val def = controlPlaneService.createDefinition(
            CreateWorkflowDefinitionRequest(name = "Magazine Print", description = "Test", category = "PRINT"),
            adminPrincipal
        )

        val instance = WorkflowInstance(
            workflowId = "wf-inst-dl-1",
            projectId = adminPrincipal.projectId,
            definitionId = def.definitionId,
            versionId = "v1",
            executionId = UUID.randomUUID().toString(),
            actorId = adminPrincipal.userId,
            status = WorkflowStatus.DEAD_LETTER
        )
        PostgresWorkflowInstanceRepository(mockDb).createInstance(instance, TenantContext(adminPrincipal.projectId))

        val replayed = controlPlaneService.replayDeadLetter("wf-inst-dl-1", WorkflowReplayRequest(checkpointStepId = "init-step"), adminPrincipal)
        assertEquals(WorkflowStatus.RUNNING, replayed.status)
        assertEquals("init-step", replayed.currentStepId)
    }

    @Test
    fun testManualCompensationRollback() = runBlocking {
        val def = controlPlaneService.createDefinition(
            CreateWorkflowDefinitionRequest(name = "Offset Print", description = "Test", category = "PRINT"),
            adminPrincipal
        )

        val instance = WorkflowInstance(
            workflowId = "wf-inst-comp-1",
            projectId = adminPrincipal.projectId,
            definitionId = def.definitionId,
            versionId = "v1",
            executionId = UUID.randomUUID().toString(),
            actorId = adminPrincipal.userId,
            status = WorkflowStatus.RUNNING
        )
        PostgresWorkflowInstanceRepository(mockDb).createInstance(instance, TenantContext(adminPrincipal.projectId))

        val compensated = controlPlaneService.compensateWorkflow("wf-inst-comp-1", WorkflowCompensationRequest("Manual operator refund"), adminPrincipal)
        assertEquals(WorkflowStatus.FAILED, compensated.status)
    }
}

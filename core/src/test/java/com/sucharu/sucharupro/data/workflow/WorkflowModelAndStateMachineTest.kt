package com.sucharu.sucharupro.data.workflow

import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.domain.workflow.engine.WorkflowStateMachine
import com.sucharu.sucharupro.domain.workflow.model.StepRetryPolicy
import com.sucharu.sucharupro.domain.workflow.model.WorkflowDefinition
import com.sucharu.sucharupro.domain.workflow.model.WorkflowInstance
import com.sucharu.sucharupro.domain.workflow.model.WorkflowStatus
import com.sucharu.sucharupro.domain.workflow.model.WorkflowStepDefinition
import com.sucharu.sucharupro.domain.workflow.model.WorkflowStepType
import com.sucharu.sucharupro.domain.workflow.model.WorkflowVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class WorkflowModelAndStateMachineTest {

    @Test
    fun testWorkflowDefinitionInvariants() {
        val def = WorkflowDefinition(
            definitionId = "def-order-1",
            projectId = "tenant_alpha",
            workflowName = "Order Processing Workflow",
            description = "Handles standard printing order workflow",
            createdBy = "admin-1"
        )
        assertEquals("def-order-1", def.definitionId)
        assertEquals("tenant_alpha", def.projectId)
        assertTrue(def.isActive)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testWorkflowDefinitionRejectsBlankName() {
        WorkflowDefinition(
            definitionId = "def-1",
            projectId = "tenant_alpha",
            workflowName = "   ",
            createdBy = "admin-1"
        )
    }

    @Test
    fun testWorkflowVersionAndStepDefinitions() {
        val step1 = WorkflowStepDefinition(
            stepId = "step-1",
            definitionId = "def-1",
            versionId = "v1",
            projectId = "tenant_alpha",
            stepName = "Validate Order",
            stepType = WorkflowStepType.ACTION,
            sequenceOrder = 1,
            retryPolicy = StepRetryPolicy(maxAttempts = 3)
        )
        val step2 = WorkflowStepDefinition(
            stepId = "step-2",
            definitionId = "def-1",
            versionId = "v1",
            projectId = "tenant_alpha",
            stepName = "Approve High Value",
            stepType = WorkflowStepType.APPROVAL,
            sequenceOrder = 2
        )

        val version = WorkflowVersion(
            definitionId = "def-1",
            projectId = "tenant_alpha",
            versionId = "v1",
            steps = listOf(step1, step2),
            publishedBy = "admin-1"
        )

        assertEquals(2, version.steps.size)
        assertEquals(WorkflowStepType.ACTION, version.steps[0].stepType)
        assertEquals(WorkflowStepType.APPROVAL, version.steps[1].stepType)
    }

    @Test
    fun testWorkflowStateMachineLegalTransitions() {
        val instance = WorkflowInstance(
            workflowId = "wf-101",
            projectId = "tenant_alpha",
            definitionId = "def-1",
            versionId = "v1",
            status = WorkflowStatus.DRAFT,
            actorId = "user-1",
            actorType = PrincipalType.HUMAN
        )

        // DRAFT -> ACTIVE
        val (active, t1) = WorkflowStateMachine.transition(instance, WorkflowStatus.ACTIVE, "PUBLISH")
        assertEquals(WorkflowStatus.ACTIVE, active.status)
        assertEquals(WorkflowStatus.DRAFT, t1.fromStatus)
        assertEquals(WorkflowStatus.ACTIVE, t1.toStatus)

        // ACTIVE -> RUNNING
        val (running, _) = WorkflowStateMachine.transition(active, WorkflowStatus.RUNNING, "START")
        assertEquals(WorkflowStatus.RUNNING, running.status)

        // RUNNING -> WAITING_APPROVAL
        val (waitingApproval, _) = WorkflowStateMachine.transition(running, WorkflowStatus.WAITING_APPROVAL, "STEP_APPROVAL")
        assertEquals(WorkflowStatus.WAITING_APPROVAL, waitingApproval.status)

        // WAITING_APPROVAL -> RUNNING
        val (resumed, _) = WorkflowStateMachine.transition(waitingApproval, WorkflowStatus.RUNNING, "APPROVED")
        assertEquals(WorkflowStatus.RUNNING, resumed.status)

        // RUNNING -> COMPLETED
        val (completed, _) = WorkflowStateMachine.transition(resumed, WorkflowStatus.COMPLETED, "SUCCESS")
        assertEquals(WorkflowStatus.COMPLETED, completed.status)
        assertNotNull(completed.completedAt)
        assertTrue(completed.status.isTerminal)
    }

    @Test(expected = IllegalStateException::class)
    fun testWorkflowStateMachineRejectsIllegalTransition() {
        val instance = WorkflowInstance(
            workflowId = "wf-102",
            projectId = "tenant_alpha",
            definitionId = "def-1",
            versionId = "v1",
            status = WorkflowStatus.COMPLETED,
            actorId = "user-1"
        )
        // COMPLETED cannot transition to RUNNING
        WorkflowStateMachine.transition(instance, WorkflowStatus.RUNNING, "ILLEGAL_MOVE")
    }

    @Test
    fun testDeadLetterCanBeReplayedByAdmin() {
        val instance = WorkflowInstance(
            workflowId = "wf-103",
            projectId = "tenant_alpha",
            definitionId = "def-1",
            versionId = "v1",
            status = WorkflowStatus.DEAD_LETTER,
            actorId = "admin-1"
        )
        assertTrue(WorkflowStateMachine.canTransition(WorkflowStatus.DEAD_LETTER, WorkflowStatus.RUNNING))

        val (replayed, t) = WorkflowStateMachine.transition(instance, WorkflowStatus.RUNNING, "ADMIN_REPLAY")
        assertEquals(WorkflowStatus.RUNNING, replayed.status)
        assertEquals(WorkflowStatus.DEAD_LETTER, t.fromStatus)
    }
}

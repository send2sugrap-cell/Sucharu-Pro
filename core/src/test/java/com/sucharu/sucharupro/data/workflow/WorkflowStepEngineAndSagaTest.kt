package com.sucharu.sucharupro.data.workflow

import com.sucharu.sucharupro.domain.workflow.engine.SagaCompensationEngine
import com.sucharu.sucharupro.domain.workflow.engine.StepEngineResult
import com.sucharu.sucharupro.domain.workflow.engine.WorkflowStepEngine
import com.sucharu.sucharupro.domain.workflow.model.CompensationStatus
import com.sucharu.sucharupro.domain.workflow.model.StepExecutionStatus
import com.sucharu.sucharupro.domain.workflow.model.WorkflowInstance
import com.sucharu.sucharupro.domain.workflow.model.WorkflowStepDefinition
import com.sucharu.sucharupro.domain.workflow.model.WorkflowStepExecution
import com.sucharu.sucharupro.domain.workflow.model.WorkflowStepType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WorkflowStepEngineAndSagaTest {

    private lateinit var stepEngine: WorkflowStepEngine
    private lateinit var sagaEngine: SagaCompensationEngine

    @Before
    fun setUp() {
        stepEngine = WorkflowStepEngine()
        sagaEngine = SagaCompensationEngine()
    }

    @Test
    fun testDeclarativeConditionStepEvaluation() {
        runBlocking {
            val step = WorkflowStepDefinition(
                stepId = "cond-1",
                definitionId = "def-1",
                versionId = "v1",
                projectId = "tenant_alpha",
                stepName = "Check Total Amount",
                stepType = WorkflowStepType.CONDITION,
                sequenceOrder = 1,
                config = mapOf("contextKey" to "tier", "expectedValue" to "VIP")
            )

            // Case 1: Matching condition
            val instanceVip = WorkflowInstance(
                workflowId = "wf-vip",
                projectId = "tenant_alpha",
                definitionId = "def-1",
                versionId = "v1",
                context = mapOf("tier" to "VIP"),
                actorId = "u1"
            )
            val resultMatch = stepEngine.executeStep(step, instanceVip)
            assertTrue(resultMatch is StepEngineResult.Succeeded)
            assertEquals("true", (resultMatch as StepEngineResult.Succeeded).outputData["branch"])

            // Case 2: Non-matching condition
            val instanceRegular = WorkflowInstance(
                workflowId = "wf-reg",
                projectId = "tenant_alpha",
                definitionId = "def-1",
                versionId = "v1",
                context = mapOf("tier" to "REGULAR"),
                actorId = "u1"
            )
            val resultMismatch = stepEngine.executeStep(step, instanceRegular)
            assertTrue(resultMismatch is StepEngineResult.Succeeded)
            assertEquals("false", (resultMismatch as StepEngineResult.Succeeded).outputData["branch"])
        }
    }

    @Test
    fun testApprovalStepRequiresApprovalResult() {
        runBlocking {
            val step = WorkflowStepDefinition(
                stepId = "appr-1",
                definitionId = "def-1",
                versionId = "v1",
                projectId = "tenant_alpha",
                stepName = "High Value Approval",
                stepType = WorkflowStepType.APPROVAL,
                sequenceOrder = 2,
                config = mapOf("policyId" to "POL-MANAGER-VAL", "title" to "Order > 50,000 Approval")
            )

            val instance = WorkflowInstance(
                workflowId = "wf-1",
                projectId = "tenant_alpha",
                definitionId = "def-1",
                versionId = "v1",
                actorId = "u1"
            )

            val result = stepEngine.executeStep(step, instance)
            assertTrue(result is StepEngineResult.RequiresApproval)
            val req = result as StepEngineResult.RequiresApproval
            assertEquals("POL-MANAGER-VAL", req.approvalPolicyId)
            assertEquals("Order > 50,000 Approval", req.title)
        }
    }

    @Test
    fun testSagaReverseCompensationRollback() {
        runBlocking {
            val instance = WorkflowInstance(
                workflowId = "wf-saga-1",
                projectId = "tenant_alpha",
                definitionId = "def-order",
                versionId = "v1",
                actorId = "u1"
            )

            val step1 = WorkflowStepDefinition(
                stepId = "step-reserve-inv",
                definitionId = "def-order",
                versionId = "v1",
                projectId = "tenant_alpha",
                stepName = "Reserve Inventory",
                stepType = WorkflowStepType.ACTION,
                sequenceOrder = 1,
                compensationStepId = "comp-release-inv"
            )

            val step2 = WorkflowStepDefinition(
                stepId = "step-charge-card",
                definitionId = "def-order",
                versionId = "v1",
                projectId = "tenant_alpha",
                stepName = "Charge Payment",
                stepType = WorkflowStepType.ACTION,
                sequenceOrder = 2,
                compensationStepId = "comp-refund-card"
            )

            val step3 = WorkflowStepDefinition(
                stepId = "step-print-plate",
                definitionId = "def-order",
                versionId = "v1",
                projectId = "tenant_alpha",
                stepName = "Laser Plate Creation",
                stepType = WorkflowStepType.ACTION,
                sequenceOrder = 3
            )

            val stepExec1 = WorkflowStepExecution(
                stepExecutionId = "exec-1",
                projectId = "tenant_alpha",
                workflowId = "wf-saga-1",
                executionId = instance.executionId,
                stepId = "step-reserve-inv",
                stepName = "Reserve Inventory",
                stepType = WorkflowStepType.ACTION,
                status = StepExecutionStatus.SUCCEEDED,
                completedAt = 1000L
            )

            val stepExec2 = WorkflowStepExecution(
                stepExecutionId = "exec-2",
                projectId = "tenant_alpha",
                workflowId = "wf-saga-1",
                executionId = instance.executionId,
                stepId = "step-charge-card",
                stepName = "Charge Payment",
                stepType = WorkflowStepType.ACTION,
                status = StepExecutionStatus.SUCCEEDED,
                completedAt = 2000L
            )

            // Step 3 failed -> Trigger Saga Reverse Compensation
            val sagaResult = sagaEngine.compensateWorkflow(
                instance = instance,
                stepDefinitions = listOf(step1, step2, step3),
                stepExecutions = listOf(stepExec1, stepExec2)
            )

            assertTrue(sagaResult.isFullyCompensated)
            assertEquals(2, sagaResult.compensations.size)

            // Compensation must execute in reverse completion order: step-charge-card first, then step-reserve-inv
            assertEquals("step-charge-card", sagaResult.compensations[0].stepId)
            assertEquals(CompensationStatus.COMPENSATED, sagaResult.compensations[0].status)

            assertEquals("step-reserve-inv", sagaResult.compensations[1].stepId)
            assertEquals(CompensationStatus.COMPENSATED, sagaResult.compensations[1].status)
        }
    }
}

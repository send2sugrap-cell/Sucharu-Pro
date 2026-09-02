package com.sucharu.sucharupro.data.workflow.governance

import com.sucharu.sucharupro.domain.workflow.governance.WorkflowDefinitionValidator
import com.sucharu.sucharupro.domain.workflow.model.StepRetryPolicy
import com.sucharu.sucharupro.domain.workflow.model.WorkflowStepDefinition
import com.sucharu.sucharupro.domain.workflow.model.WorkflowStepType
import com.sucharu.sucharupro.domain.workflow.model.WorkflowVersion
import org.junit.Assert.*
import org.junit.Test

class WorkflowDefinitionGovernanceTest {

    @Test
    fun testValidWorkflowVersionPassesValidation() {
        val version = WorkflowVersion(
            versionId = "v1",
            definitionId = "def-01",
            projectId = "proj-1",
            publishedBy = "usr_admin",
            steps = listOf(
                WorkflowStepDefinition(
                    stepId = "step-1",
                    definitionId = "def-01",
                    versionId = "v1",
                    projectId = "proj-1",
                    stepName = "Preflight Check",
                    stepType = WorkflowStepType.ACTION,
                    sequenceOrder = 1,
                    config = mapOf("handler" to "action.preflight", "nextStepId" to "step-2")
                ),
                WorkflowStepDefinition(
                    stepId = "step-2",
                    definitionId = "def-01",
                    versionId = "v1",
                    projectId = "proj-1",
                    stepName = "Plate Exposure",
                    stepType = WorkflowStepType.JOB,
                    sequenceOrder = 2,
                    config = mapOf("jobType" to "job.laser_plate", "nextStepId" to "step-3")
                ),
                WorkflowStepDefinition(
                    stepId = "step-3",
                    definitionId = "def-01",
                    versionId = "v1",
                    projectId = "proj-1",
                    stepName = "Finish",
                    stepType = WorkflowStepType.END,
                    sequenceOrder = 3
                )
            )
        )

        val result = WorkflowDefinitionValidator.validateVersion(version)
        assertTrue("Expected valid workflow version", result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun testDuplicateStepIdIsRejected() {
        val version = WorkflowVersion(
            versionId = "v1",
            definitionId = "def-01",
            projectId = "proj-1",
            publishedBy = "usr_admin",
            steps = listOf(
                WorkflowStepDefinition(
                    stepId = "step-dup",
                    definitionId = "def-01",
                    versionId = "v1",
                    projectId = "proj-1",
                    stepName = "Step 1",
                    stepType = WorkflowStepType.ACTION,
                    sequenceOrder = 1,
                    config = mapOf("handler" to "action.1", "nextStepId" to "step-dup")
                ),
                WorkflowStepDefinition(
                    stepId = "step-dup",
                    definitionId = "def-01",
                    versionId = "v1",
                    projectId = "proj-1",
                    stepName = "Step 2",
                    stepType = WorkflowStepType.END,
                    sequenceOrder = 2
                )
            )
        )

        val result = WorkflowDefinitionValidator.validateVersion(version)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Duplicate step ID") })
    }

    @Test
    fun testSelfReferentialCycleIsRejected() {
        val version = WorkflowVersion(
            versionId = "v1",
            definitionId = "def-01",
            projectId = "proj-1",
            publishedBy = "usr_admin",
            steps = listOf(
                WorkflowStepDefinition(
                    stepId = "step-loop",
                    definitionId = "def-01",
                    versionId = "v1",
                    projectId = "proj-1",
                    stepName = "Loop Step",
                    stepType = WorkflowStepType.ACTION,
                    sequenceOrder = 1,
                    config = mapOf("handler" to "action.loop", "nextStepId" to "step-loop")
                ),
                WorkflowStepDefinition(
                    stepId = "step-end",
                    definitionId = "def-01",
                    versionId = "v1",
                    projectId = "proj-1",
                    stepName = "End Step",
                    stepType = WorkflowStepType.END,
                    sequenceOrder = 2
                )
            )
        )

        val result = WorkflowDefinitionValidator.validateVersion(version)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("direct cycle") })
    }

    @Test
    fun testMissingTerminalStateIsRejected() {
        val version = WorkflowVersion(
            versionId = "v1",
            definitionId = "def-01",
            projectId = "proj-1",
            publishedBy = "usr_admin",
            steps = listOf(
                WorkflowStepDefinition(
                    stepId = "step-1",
                    definitionId = "def-01",
                    versionId = "v1",
                    projectId = "proj-1",
                    stepName = "Step 1",
                    stepType = WorkflowStepType.ACTION,
                    sequenceOrder = 1,
                    config = mapOf("handler" to "action.1", "nextStepId" to "step-2")
                ),
                WorkflowStepDefinition(
                    stepId = "step-2",
                    definitionId = "def-01",
                    versionId = "v1",
                    projectId = "proj-1",
                    stepName = "Step 2",
                    stepType = WorkflowStepType.ACTION,
                    sequenceOrder = 2,
                    config = mapOf("handler" to "action.2", "nextStepId" to "step-1")
                )
            )
        )

        val result = WorkflowDefinitionValidator.validateVersion(version)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("terminal step") })
    }
}

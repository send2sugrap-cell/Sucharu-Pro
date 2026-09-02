package com.sucharu.sucharupro.domain.workflow.engine

import com.sucharu.sucharupro.domain.workflow.model.StepExecutionStatus
import com.sucharu.sucharupro.domain.workflow.model.WorkflowInstance
import com.sucharu.sucharupro.domain.workflow.model.WorkflowStepDefinition
import com.sucharu.sucharupro.domain.workflow.model.WorkflowStepExecution
import com.sucharu.sucharupro.domain.workflow.model.WorkflowStepType
import java.util.UUID

/**
 * Result of executing a workflow step.
 */
sealed class StepEngineResult {
    data class Succeeded(val outputData: Map<String, String> = emptyMap()) : StepEngineResult()
    data class Paused(val reason: String, val waitingOn: String? = null) : StepEngineResult()
    data class RequiresApproval(val approvalPolicyId: String, val title: String, val summary: String?) : StepEngineResult()
    data class RequiresJobExecution(val jobType: String, val payloadJson: String) : StepEngineResult()
    data class Failed(val errorMessage: String, val failureClassification: String = "TRANSIENT", val retryable: Boolean = true) : StepEngineResult()
}

/**
 * Declarative step execution engine for Sucharu Pro (INFRA-04 Step 05).
 */
class WorkflowStepEngine {

    /**
     * Executes or evaluates a single workflow step definition against instance context.
     */
    suspend fun executeStep(
        step: WorkflowStepDefinition,
        instance: WorkflowInstance,
        executionHistory: List<WorkflowStepExecution> = emptyList()
    ): StepEngineResult {
        return when (step.stepType) {
            WorkflowStepType.ACTION -> executeActionStep(step, instance)
            WorkflowStepType.CONDITION -> evaluateConditionStep(step, instance)
            WorkflowStepType.DELAY -> evaluateDelayStep(step, instance)
            WorkflowStepType.EVENT_WAIT -> StepEngineResult.Paused("Waiting for domain event: ${step.config["eventType"]}")
            WorkflowStepType.APPROVAL -> {
                val policyId = step.config["policyId"] ?: "DEFAULT_POLICY"
                val title = step.config["title"] ?: "Approval Required for ${step.stepName}"
                val summary = step.config["summary"] ?: "Workflow step ${step.stepName} requires authorized review"
                StepEngineResult.RequiresApproval(policyId, title, summary)
            }
            WorkflowStepType.JOB -> {
                val jobType = step.config["jobType"] ?: "workflow.generic_job"
                val payload = step.config["payload"] ?: "{}"
                StepEngineResult.RequiresJobExecution(jobType, payload)
            }
            WorkflowStepType.NOTIFICATION -> {
                StepEngineResult.Succeeded(mapOf("notificationDispatched" to "true"))
            }
            WorkflowStepType.WEBHOOK -> {
                StepEngineResult.Succeeded(mapOf("webhookTriggered" to "true"))
            }
            WorkflowStepType.COMPENSATION -> {
                StepEngineResult.Succeeded(mapOf("compensated" to "true"))
            }
            WorkflowStepType.END -> {
                StepEngineResult.Succeeded(mapOf("workflowEnd" to "true"))
            }
        }
    }

    private fun executeActionStep(step: WorkflowStepDefinition, instance: WorkflowInstance): StepEngineResult {
        val actionType = step.config["actionType"] ?: "NOOP"
        return StepEngineResult.Succeeded(mapOf("executedAction" to actionType))
    }

    private fun evaluateConditionStep(step: WorkflowStepDefinition, instance: WorkflowInstance): StepEngineResult {
        val key = step.config["contextKey"]
        val expected = step.config["expectedValue"]
        if (key != null && expected != null) {
            val actual = instance.context[key]
            if (actual == expected) {
                return StepEngineResult.Succeeded(mapOf("conditionMet" to "true", "branch" to "true"))
            } else {
                return StepEngineResult.Succeeded(mapOf("conditionMet" to "false", "branch" to "false"))
            }
        }
        return StepEngineResult.Succeeded(mapOf("conditionMet" to "true"))
    }

    private fun evaluateDelayStep(step: WorkflowStepDefinition, instance: WorkflowInstance): StepEngineResult {
        val delayMs = step.config["delayMs"]?.toLongOrNull() ?: 0L
        if (delayMs > 0) {
            return StepEngineResult.Paused("Delay of ${delayMs}ms in progress", waitingOn = "DELAY")
        }
        return StepEngineResult.Succeeded()
    }
}

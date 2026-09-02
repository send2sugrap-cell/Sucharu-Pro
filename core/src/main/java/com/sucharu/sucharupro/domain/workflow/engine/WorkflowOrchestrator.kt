package com.sucharu.sucharupro.domain.workflow.engine

import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.workflow.model.StepExecutionStatus
import com.sucharu.sucharupro.domain.workflow.model.WorkflowInstance
import com.sucharu.sucharupro.domain.workflow.model.WorkflowStatus
import com.sucharu.sucharupro.domain.workflow.model.WorkflowStepDefinition
import com.sucharu.sucharupro.domain.workflow.model.WorkflowStepExecution
import com.sucharu.sucharupro.domain.workflow.model.WorkflowVersion
import java.util.UUID

/**
 * Result of advancing workflow orchestrator.
 */
sealed class OrchestratorResult {
    data class Completed(val instance: WorkflowInstance) : OrchestratorResult()
    data class Advanced(val instance: WorkflowInstance, val nextStepId: String?) : OrchestratorResult()
    data class WaitingApproval(val instance: WorkflowInstance, val approvalPolicyId: String, val title: String) : OrchestratorResult()
    data class WaitingJob(val instance: WorkflowInstance, val jobType: String, val payloadJson: String) : OrchestratorResult()
    data class WaitingEvent(val instance: WorkflowInstance, val reason: String) : OrchestratorResult()
    data class Failed(val instance: WorkflowInstance, val errorMessage: String, val compensationResult: SagaCompensationResult? = null) : OrchestratorResult()
    data class Paused(val instance: WorkflowInstance) : OrchestratorResult()
    data class Cancelled(val instance: WorkflowInstance) : OrchestratorResult()
}

/**
 * Domain-neutral workflow orchestration coordinator (INFRA-04 Step 05).
 */
class WorkflowOrchestrator(
    private val stepEngine: WorkflowStepEngine = WorkflowStepEngine(),
    private val sagaEngine: SagaCompensationEngine = SagaCompensationEngine()
) {

    /**
     * Starts a new workflow instance from definition version.
     */
    fun startWorkflow(
        instance: WorkflowInstance,
        version: WorkflowVersion
    ): Pair<WorkflowInstance, String?> {
        val firstStep = version.steps.minByOrNull { it.sequenceOrder }
        val (runningInstance, _) = WorkflowStateMachine.transition(
            instance = instance,
            targetStatus = WorkflowStatus.RUNNING,
            triggerType = "START_WORKFLOW"
        )
        val updated = runningInstance.copy(currentStepId = firstStep?.stepId)
        return Pair(updated, firstStep?.stepId)
    }

    /**
     * Advances the workflow by executing the current step.
     */
    suspend fun advanceWorkflow(
        instance: WorkflowInstance,
        version: WorkflowVersion,
        stepExecutions: List<WorkflowStepExecution> = emptyList(),
        tenantContext: TenantContext
    ): OrchestratorResult {
        require(instance.projectId == tenantContext.projectId) {
            "Tenant isolation violation: instance '${instance.projectId}' != tenant '${tenantContext.projectId}'"
        }

        if (instance.status != WorkflowStatus.RUNNING) {
            return OrchestratorResult.Paused(instance)
        }

        val currentStep = version.steps.firstOrNull { it.stepId == instance.currentStepId }
            ?: return completeWorkflow(instance)

        val result = stepEngine.executeStep(currentStep, instance, stepExecutions)

        return when (result) {
            is StepEngineResult.Succeeded -> {
                val nextStep = version.steps
                    .filter { it.sequenceOrder > currentStep.sequenceOrder }
                    .minByOrNull { it.sequenceOrder }

                if (nextStep == null) {
                    completeWorkflow(instance)
                } else {
                    val updated = instance.copy(
                        currentStepId = nextStep.stepId,
                        context = instance.context + result.outputData,
                        updatedAt = System.currentTimeMillis()
                    )
                    OrchestratorResult.Advanced(updated, nextStep.stepId)
                }
            }
            is StepEngineResult.RequiresApproval -> {
                val (waitingInstance, _) = WorkflowStateMachine.transition(
                    instance = instance,
                    targetStatus = WorkflowStatus.WAITING_APPROVAL,
                    triggerType = "APPROVAL_REQUESTED"
                )
                OrchestratorResult.WaitingApproval(waitingInstance, result.approvalPolicyId, result.title)
            }
            is StepEngineResult.RequiresJobExecution -> {
                val (waitingInstance, _) = WorkflowStateMachine.transition(
                    instance = instance,
                    targetStatus = WorkflowStatus.WAITING,
                    triggerType = "JOB_DELEGATED"
                )
                OrchestratorResult.WaitingJob(waitingInstance, result.jobType, result.payloadJson)
            }
            is StepEngineResult.Paused -> {
                val (waitingInstance, _) = WorkflowStateMachine.transition(
                    instance = instance,
                    targetStatus = WorkflowStatus.WAITING,
                    triggerType = "EVENT_WAIT"
                )
                OrchestratorResult.WaitingEvent(waitingInstance, result.reason)
            }
            is StepEngineResult.Failed -> {
                failWorkflowWithCompensation(instance, version.steps, stepExecutions, result.errorMessage)
            }
        }
    }

    /**
     * Resumes workflow after approval or external event completion.
     */
    fun resumeWorkflow(
        instance: WorkflowInstance,
        version: WorkflowVersion,
        contextUpdates: Map<String, String> = emptyMap()
    ): WorkflowInstance {
        val (resumed, _) = WorkflowStateMachine.transition(
            instance = instance,
            targetStatus = WorkflowStatus.RUNNING,
            triggerType = "RESUME_WORKFLOW"
        )
        return resumed.copy(
            context = resumed.context + contextUpdates,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Completes workflow successfully.
     */
    private fun completeWorkflow(instance: WorkflowInstance): OrchestratorResult {
        val (completedInstance, _) = WorkflowStateMachine.transition(
            instance = instance,
            targetStatus = WorkflowStatus.COMPLETED,
            triggerType = "ALL_STEPS_COMPLETED"
        )
        return OrchestratorResult.Completed(completedInstance)
    }

    /**
     * Fails workflow and coordinates saga reverse compensation.
     */
    suspend fun failWorkflowWithCompensation(
        instance: WorkflowInstance,
        stepDefinitions: List<WorkflowStepDefinition>,
        stepExecutions: List<WorkflowStepExecution>,
        errorMessage: String
    ): OrchestratorResult.Failed {
        val (compensatingInstance, _) = WorkflowStateMachine.transition(
            instance = instance,
            targetStatus = WorkflowStatus.COMPENSATING,
            triggerType = "FAILURE_TRIGGERED",
            errorMessage = errorMessage
        )

        val sagaResult = sagaEngine.compensateWorkflow(compensatingInstance, stepDefinitions, stepExecutions)

        val finalStatus = if (sagaResult.isFullyCompensated) WorkflowStatus.FAILED else WorkflowStatus.DEAD_LETTER
        val (finalInstance, _) = WorkflowStateMachine.transition(
            instance = compensatingInstance,
            targetStatus = finalStatus,
            triggerType = "COMPENSATION_FINISHED",
            errorMessage = errorMessage
        )

        return OrchestratorResult.Failed(finalInstance, errorMessage, sagaResult)
    }

    /**
     * Pauses an active or running workflow instance.
     */
    fun pauseWorkflow(
        instance: WorkflowInstance,
        actorId: String? = null,
        reason: String? = null
    ): WorkflowInstance {
        val (paused, _) = WorkflowStateMachine.transition(
            instance = instance,
            targetStatus = WorkflowStatus.PAUSED,
            triggerType = "MANUAL_PAUSE",
            actorId = actorId ?: instance.actorId,
            errorMessage = reason
        )
        return paused
    }

    /**
     * Cancels an active or paused workflow instance.
     */
    fun cancelWorkflow(
        instance: WorkflowInstance,
        actorId: String,
        reason: String
    ): WorkflowInstance {
        val (cancelled, _) = WorkflowStateMachine.transition(
            instance = instance,
            targetStatus = WorkflowStatus.CANCELLED,
            triggerType = "MANUAL_CANCELLATION",
            actorId = actorId,
            errorMessage = reason
        )
        return cancelled
    }
}

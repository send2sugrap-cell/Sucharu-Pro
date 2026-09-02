package com.sucharu.sucharupro.domain.workflow.engine

import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.domain.workflow.model.WorkflowInstance
import com.sucharu.sucharupro.domain.workflow.model.WorkflowStatus
import com.sucharu.sucharupro.domain.workflow.model.WorkflowTransition
import java.util.UUID

/**
 * Strict workflow state machine and transition validator (INFRA-04 Step 05).
 */
object WorkflowStateMachine {

    private val legalTransitions: Map<WorkflowStatus, Set<WorkflowStatus>> = mapOf(
        WorkflowStatus.DRAFT to setOf(WorkflowStatus.ACTIVE, WorkflowStatus.RUNNING, WorkflowStatus.CANCELLED),
        WorkflowStatus.ACTIVE to setOf(WorkflowStatus.RUNNING, WorkflowStatus.PAUSED, WorkflowStatus.CANCELLED),
        WorkflowStatus.RUNNING to setOf(
            WorkflowStatus.WAITING,
            WorkflowStatus.WAITING_APPROVAL,
            WorkflowStatus.PAUSED,
            WorkflowStatus.COMPLETED,
            WorkflowStatus.FAILED,
            WorkflowStatus.COMPENSATING,
            WorkflowStatus.CANCELLED,
            WorkflowStatus.TIMED_OUT,
            WorkflowStatus.DEAD_LETTER
        ),
        WorkflowStatus.WAITING to setOf(
            WorkflowStatus.RUNNING,
            WorkflowStatus.PAUSED,
            WorkflowStatus.CANCELLED,
            WorkflowStatus.TIMED_OUT,
            WorkflowStatus.FAILED
        ),
        WorkflowStatus.WAITING_APPROVAL to setOf(
            WorkflowStatus.RUNNING,
            WorkflowStatus.PAUSED,
            WorkflowStatus.CANCELLED,
            WorkflowStatus.FAILED,
            WorkflowStatus.COMPENSATING,
            WorkflowStatus.TIMED_OUT
        ),
        WorkflowStatus.PAUSED to setOf(
            WorkflowStatus.RUNNING,
            WorkflowStatus.ACTIVE,
            WorkflowStatus.CANCELLED
        ),
        WorkflowStatus.COMPENSATING to setOf(
            WorkflowStatus.FAILED,
            WorkflowStatus.DEAD_LETTER
        ),
        WorkflowStatus.DEAD_LETTER to setOf(
            WorkflowStatus.RUNNING,
            WorkflowStatus.CANCELLED
        ),
        WorkflowStatus.COMPLETED to emptySet(),
        WorkflowStatus.FAILED to setOf(WorkflowStatus.COMPENSATING, WorkflowStatus.DEAD_LETTER),
        WorkflowStatus.CANCELLED to emptySet(),
        WorkflowStatus.TIMED_OUT to setOf(WorkflowStatus.COMPENSATING, WorkflowStatus.DEAD_LETTER)
    )

    /**
     * Validates if transition from [currentStatus] to [targetStatus] is legally permitted.
     */
    fun canTransition(currentStatus: WorkflowStatus, targetStatus: WorkflowStatus): Boolean {
        if (currentStatus == targetStatus) return true
        return legalTransitions[currentStatus]?.contains(targetStatus) == true
    }

    /**
     * Enforces transition validation and returns updated [WorkflowInstance] along with [WorkflowTransition] audit record.
     */
    fun transition(
        instance: WorkflowInstance,
        targetStatus: WorkflowStatus,
        triggerType: String,
        actorType: PrincipalType = instance.actorType,
        actorId: String = instance.actorId,
        principalType: PrincipalType = instance.principalType,
        metadata: Map<String, String> = emptyMap(),
        errorMessage: String? = null
    ): Pair<WorkflowInstance, WorkflowTransition> {
        if (!canTransition(instance.status, targetStatus)) {
            throw IllegalStateException(
                "Illegal workflow state transition from '${instance.status}' to '$targetStatus' for workflow '${instance.workflowId}'"
            )
        }

        val now = System.currentTimeMillis()
        val updated = instance.copy(
            status = targetStatus,
            updatedAt = now,
            completedAt = if (targetStatus == WorkflowStatus.COMPLETED) now else instance.completedAt,
            failedAt = if (targetStatus == WorkflowStatus.FAILED || targetStatus == WorkflowStatus.TIMED_OUT) now else instance.failedAt,
            errorMessage = errorMessage ?: instance.errorMessage
        )

        val transitionRecord = WorkflowTransition(
            transitionId = UUID.randomUUID().toString(),
            projectId = instance.projectId,
            workflowId = instance.workflowId,
            executionId = instance.executionId,
            fromStatus = instance.status,
            toStatus = targetStatus,
            triggerType = triggerType,
            actorType = actorType,
            actorId = actorId,
            principalType = principalType,
            metadata = metadata,
            transitionedAt = now
        )

        return Pair(updated, transitionRecord)
    }
}

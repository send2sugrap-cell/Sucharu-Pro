package com.sucharu.sucharupro.domain.validation.communication.automation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.automation.AutomationExecutionStatus

/**
 * Validates execution lifecycle transitions and terminal states (Module 10 Step 08).
 */
object CommunicationAutomationLifecycleValidator {

    private val allowedTransitions: Map<AutomationExecutionStatus, Set<AutomationExecutionStatus>> = mapOf(
        AutomationExecutionStatus.RECEIVED to setOf(
            AutomationExecutionStatus.EVALUATING,
            AutomationExecutionStatus.SUPPRESSED,
            AutomationExecutionStatus.FAILED,
            AutomationExecutionStatus.CANCELLED
        ),
        AutomationExecutionStatus.EVALUATING to setOf(
            AutomationExecutionStatus.MATCHED,
            AutomationExecutionStatus.SUPPRESSED,
            AutomationExecutionStatus.FAILED,
            AutomationExecutionStatus.CANCELLED
        ),
        AutomationExecutionStatus.MATCHED to setOf(
            AutomationExecutionStatus.QUEUED,
            AutomationExecutionStatus.SCHEDULED,
            AutomationExecutionStatus.DISPATCHING,
            AutomationExecutionStatus.SUPPRESSED,
            AutomationExecutionStatus.FAILED,
            AutomationExecutionStatus.CANCELLED
        ),
        AutomationExecutionStatus.QUEUED to setOf(
            AutomationExecutionStatus.DISPATCHING,
            AutomationExecutionStatus.CANCELLED,
            AutomationExecutionStatus.FAILED
        ),
        AutomationExecutionStatus.SCHEDULED to setOf(
            AutomationExecutionStatus.QUEUED,
            AutomationExecutionStatus.DISPATCHING,
            AutomationExecutionStatus.CANCELLED
        ),
        AutomationExecutionStatus.DISPATCHING to setOf(
            AutomationExecutionStatus.DISPATCHED,
            AutomationExecutionStatus.COMPLETED,
            AutomationExecutionStatus.FAILED
        ),
        AutomationExecutionStatus.DISPATCHED to setOf(
            AutomationExecutionStatus.COMPLETED
        ),
        AutomationExecutionStatus.COMPLETED to emptySet(),
        AutomationExecutionStatus.SUPPRESSED to emptySet(),
        AutomationExecutionStatus.FAILED to emptySet(),
        AutomationExecutionStatus.CANCELLED to emptySet()
    )

    fun validateTransition(
        from: AutomationExecutionStatus,
        to: AutomationExecutionStatus
    ): DomainResult<Unit> {
        if (from == to) return DomainResult.Success(Unit)

        if (from.isTerminal) {
            return DomainResult.Error(message = "Cannot transition from terminal execution state '$from'.")
        }

        val permitted = allowedTransitions[from] ?: emptySet()
        return if (to in permitted) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Illegal automation execution transition from '$from' to '$to'.")
        }
    }
}

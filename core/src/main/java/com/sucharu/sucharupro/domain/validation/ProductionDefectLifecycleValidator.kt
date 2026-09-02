package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.DefectStatus
import com.sucharu.sucharupro.domain.model.qc.ProductionDefect

/**
 * Strict state machine transition validator for [ProductionDefect] (Module 06 Step 04).
 */
object ProductionDefectLifecycleValidator {

    /**
     * Validates if a transition from the defect's current status to [targetStatus] is allowed.
     */
    fun validateStatusTransition(
        defect: ProductionDefect,
        targetStatus: DefectStatus
    ): DomainResult<Unit> {
        if (defect.status == targetStatus) {
            return DomainResult.Error(
                message = "Defect is already in '${defect.status.defaultLabel}' status."
            )
        }

        if (defect.isTerminal) {
            return DomainResult.Error(
                message = "Cannot transition terminal defect '${defect.defectId}' (Current status: ${defect.status.defaultLabel})."
            )
        }

        if (!defect.status.canTransitionTo(targetStatus)) {
            return DomainResult.Error(
                message = "Invalid defect status transition from '${defect.status.defaultLabel}' to '${targetStatus.defaultLabel}'."
            )
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates cancellation transition prerequisites.
     */
    fun validateCancellation(
        defect: ProductionDefect,
        reason: String?
    ): DomainResult<Unit> {
        if (defect.isTerminal) {
            return DomainResult.Error(
                message = "Cannot cancel already terminal defect '${defect.defectId}' (Status: ${defect.status.defaultLabel})."
            )
        }

        if (reason.isNullOrBlank()) {
            return DomainResult.Error(message = "Cancellation reason cannot be blank.")
        }

        return DomainResult.Success(Unit)
    }
}

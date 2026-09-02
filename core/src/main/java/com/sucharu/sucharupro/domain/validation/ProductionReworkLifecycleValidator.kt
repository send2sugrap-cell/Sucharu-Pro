package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ProductionRework
import com.sucharu.sucharupro.domain.model.qc.ReworkStatus

/**
 * Strict state machine transition validator for [ProductionRework] (Module 06 Step 05).
 */
object ProductionReworkLifecycleValidator {

    /**
     * Validates whether a transition from the rework's current status to [targetStatus] is permissible.
     */
    fun validateStatusTransition(
        rework: ProductionRework,
        targetStatus: ReworkStatus
    ): DomainResult<Unit> {
        if (rework.status == targetStatus) {
            return DomainResult.Error(
                message = "Rework is already in '${rework.status.defaultLabel}' status."
            )
        }

        if (rework.isTerminal) {
            return DomainResult.Error(
                message = "Cannot transition terminal rework '${rework.reworkId}' (Current status: ${rework.status.defaultLabel})."
            )
        }

        if (rework.isReturnedToQc) {
            return DomainResult.Error(
                message = "Cannot transition rework '${rework.reworkId}' because it has been returned to QC (Protected boundary state)."
            )
        }

        if (!rework.status.canTransitionTo(targetStatus)) {
            return DomainResult.Error(
                message = "Invalid rework status transition from '${rework.status.defaultLabel}' to '${targetStatus.defaultLabel}'."
            )
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates cancellation transition prerequisites.
     */
    fun validateCancellation(
        rework: ProductionRework,
        reason: String?
    ): DomainResult<Unit> {
        if (rework.isTerminal) {
            return DomainResult.Error(
                message = "Cannot cancel already terminal rework '${rework.reworkId}' (Status: ${rework.status.defaultLabel})."
            )
        }

        if (rework.isReturnedToQc) {
            return DomainResult.Error(
                message = "Cannot cancel rework '${rework.reworkId}' after it has been returned to QC."
            )
        }

        if (reason.isNullOrBlank()) {
            return DomainResult.Error(message = "Cancellation reason cannot be blank.")
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates rejection transition prerequisites.
     */
    fun validateRejection(
        rework: ProductionRework,
        reason: String?
    ): DomainResult<Unit> {
        if (rework.isTerminal) {
            return DomainResult.Error(
                message = "Cannot reject already terminal rework '${rework.reworkId}' (Status: ${rework.status.defaultLabel})."
            )
        }

        if (rework.isReturnedToQc) {
            return DomainResult.Error(
                message = "Cannot reject rework '${rework.reworkId}' after it has been returned to QC."
            )
        }

        if (rework.status != ReworkStatus.REQUESTED && rework.status != ReworkStatus.UNDER_REVIEW) {
            return DomainResult.Error(
                message = "Cannot reject rework '${rework.reworkId}' in status '${rework.status.defaultLabel}' (Must be REQUESTED or UNDER_REVIEW)."
            )
        }

        if (reason.isNullOrBlank()) {
            return DomainResult.Error(message = "Rejection reason cannot be blank.")
        }

        return DomainResult.Success(Unit)
    }
}

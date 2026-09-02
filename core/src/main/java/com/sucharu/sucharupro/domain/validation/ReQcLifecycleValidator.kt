package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ReQcFailureReason
import com.sucharu.sucharupro.domain.model.qc.ReQcInspection
import com.sucharu.sucharupro.domain.model.qc.ReQcStatus

/**
 * Strict state machine transition validator for [ReQcInspection] (Module 06 Step 06).
 */
object ReQcLifecycleValidator {

    /**
     * Validates whether a transition from the Re-QC's current status to [targetStatus] is permissible.
     */
    fun validateStatusTransition(
        reQc: ReQcInspection,
        targetStatus: ReQcStatus
    ): DomainResult<Unit> {
        if (reQc.status == targetStatus) {
            return DomainResult.Error(
                message = "Re-QC is already in '${reQc.status.defaultLabel}' status."
            )
        }

        if (reQc.isTerminal) {
            return DomainResult.Error(
                message = "Cannot transition terminal Re-QC '${reQc.reQcId}' (Current status: ${reQc.status.defaultLabel})."
            )
        }

        if (reQc.status == ReQcStatus.RETURNED_TO_REWORK) {
            return DomainResult.Error(
                message = "Cannot transition Re-QC '${reQc.reQcId}' because it has been returned to rework (Protected boundary state)."
            )
        }

        if (!reQc.status.canTransitionTo(targetStatus)) {
            return DomainResult.Error(
                message = "Invalid Re-QC status transition from '${reQc.status.defaultLabel}' to '${targetStatus.defaultLabel}'."
            )
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates inspection start prerequisites.
     */
    fun validateInspectionStart(
        reQc: ReQcInspection,
        inspectorId: String?
    ): DomainResult<Unit> {
        if (reQc.isTerminal) {
            return DomainResult.Error(
                message = "Cannot start inspection for terminal Re-QC '${reQc.reQcId}' (Status: ${reQc.status.defaultLabel})."
            )
        }

        if (reQc.status == ReQcStatus.RETURNED_TO_REWORK) {
            return DomainResult.Error(
                message = "Cannot start inspection for Re-QC '${reQc.reQcId}' after it has been returned to rework."
            )
        }

        if (reQc.status != ReQcStatus.PENDING && reQc.status != ReQcStatus.ASSIGNED) {
            return DomainResult.Error(
                message = "Cannot start inspection for Re-QC '${reQc.reQcId}' in status '${reQc.status.defaultLabel}' (Must be PENDING or ASSIGNED)."
            )
        }

        if (inspectorId.isNullOrBlank() && reQc.assignedInspectorId.isNullOrBlank()) {
            return DomainResult.Error(message = "Inspector ID cannot be blank when starting inspection.")
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates PASS transition prerequisites.
     */
    fun validatePassTransition(
        reQc: ReQcInspection
    ): DomainResult<Unit> {
        if (reQc.isTerminal) {
            return DomainResult.Error(
                message = "Cannot pass already terminal Re-QC '${reQc.reQcId}' (Status: ${reQc.status.defaultLabel})."
            )
        }

        if (reQc.status != ReQcStatus.IN_INSPECTION) {
            return DomainResult.Error(
                message = "Cannot pass Re-QC '${reQc.reQcId}' because it is not in IN_INSPECTION status (Current: ${reQc.status.defaultLabel})."
            )
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates FAIL transition prerequisites.
     */
    fun validateFailTransition(
        reQc: ReQcInspection,
        failureReason: ReQcFailureReason?,
        failureNotes: String?
    ): DomainResult<Unit> {
        if (reQc.isTerminal) {
            return DomainResult.Error(
                message = "Cannot fail already terminal Re-QC '${reQc.reQcId}' (Status: ${reQc.status.defaultLabel})."
            )
        }

        if (reQc.status != ReQcStatus.IN_INSPECTION) {
            return DomainResult.Error(
                message = "Cannot fail Re-QC '${reQc.reQcId}' because it is not in IN_INSPECTION status (Current: ${reQc.status.defaultLabel})."
            )
        }

        if (failureReason == null) {
            return DomainResult.Error(message = "Failure reason is mandatory when failing Re-QC.")
        }

        if (failureNotes.isNullOrBlank()) {
            return DomainResult.Error(message = "Failure notes are mandatory when failing Re-QC.")
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates transition to RETURNED_TO_REWORK.
     */
    fun validateReturnToRework(
        reQc: ReQcInspection,
        actorId: String?
    ): DomainResult<Unit> {
        if (reQc.isTerminal) {
            return DomainResult.Error(
                message = "Cannot return terminal Re-QC '${reQc.reQcId}' to rework (Status: ${reQc.status.defaultLabel})."
            )
        }

        if (reQc.status != ReQcStatus.FAILED) {
            return DomainResult.Error(
                message = "Cannot return Re-QC '${reQc.reQcId}' to rework in status '${reQc.status.defaultLabel}' (Must be FAILED)."
            )
        }

        if (actorId.isNullOrBlank()) {
            return DomainResult.Error(message = "Actor identifier is mandatory when returning Re-QC to rework.")
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates cancellation transition prerequisites.
     */
    fun validateCancellation(
        reQc: ReQcInspection,
        reason: String?
    ): DomainResult<Unit> {
        if (reQc.isTerminal) {
            return DomainResult.Error(
                message = "Cannot cancel already terminal Re-QC '${reQc.reQcId}' (Status: ${reQc.status.defaultLabel})."
            )
        }

        if (reQc.status == ReQcStatus.RETURNED_TO_REWORK) {
            return DomainResult.Error(
                message = "Cannot cancel Re-QC '${reQc.reQcId}' after it has been returned to rework."
            )
        }

        if (reQc.status == ReQcStatus.FAILED) {
            return DomainResult.Error(
                message = "Cannot cancel failed Re-QC '${reQc.reQcId}' (Must be returned to rework or preserved as failure record)."
            )
        }

        if (reason.isNullOrBlank()) {
            return DomainResult.Error(message = "Cancellation reason cannot be blank.")
        }

        return DomainResult.Success(Unit)
    }
}

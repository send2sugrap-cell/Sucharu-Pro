package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.FinalQcInspection
import com.sucharu.sucharupro.domain.model.qc.FinalQcStatus

/**
 * Domain validator enforcing state machine lifecycle rules, terminal immutability, and cancellation bounds for Final QC (Module 06 Step 07).
 */
object FinalQcLifecycleValidator {

    /**
     * Validates if transitioning from [current] to [target] is permitted.
     */
    fun validateTransition(current: FinalQcStatus, target: FinalQcStatus): DomainResult<Unit> {
        if (!current.canTransitionTo(target)) {
            return DomainResult.Error(message = 
                "Illegal Final QC status transition from $current to $target."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Ensures an immutable terminal record cannot be modified.
     */
    fun validateTerminalImmutability(inspection: FinalQcInspection): DomainResult<Unit> {
        if (inspection.isTerminal) {
            return DomainResult.Error(message = 
                "Final QC record ${inspection.finalQcId} is in terminal state '${inspection.status}' and cannot be modified."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates prerequisites for starting an inspection.
     */
    fun validateStartInspection(inspection: FinalQcInspection): DomainResult<Unit> {
        if (inspection.isTerminal) {
            return DomainResult.Error(message = "Cannot start inspection on terminal Final QC record ${inspection.finalQcId}.")
        }
        if (inspection.status !in setOf(FinalQcStatus.DRAFT, FinalQcStatus.PENDING, FinalQcStatus.ASSIGNED, FinalQcStatus.BLOCKED, FinalQcStatus.FAILED)) {
            return DomainResult.Error(message = "Cannot start inspection when status is ${inspection.status}.")
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates prerequisites for cancelling a Final QC record.
     */
    fun validateCancellation(inspection: FinalQcInspection, reason: String): DomainResult<Unit> {
        if (reason.isBlank()) {
            return DomainResult.Error(message = "Cancellation reason cannot be blank.")
        }
        if (inspection.isTerminal) {
            return DomainResult.Error(message = "Cannot cancel already terminal Final QC record ${inspection.finalQcId} (Status: ${inspection.status}).")
        }
        if (inspection.status == FinalQcStatus.RELEASED) {
            return DomainResult.Error(message = "Cannot cancel a Final QC record that has already been released.")
        }
        if (inspection.status == FinalQcStatus.PASSED) {
            return DomainResult.Error(message = "Cannot cancel a Final QC inspection that has already PASSED.")
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates that an inspection is ready to be released.
     */
    fun validateReleaseReady(inspection: FinalQcInspection): DomainResult<Unit> {
        if (inspection.status == FinalQcStatus.RELEASED) {
            return DomainResult.Error(message = "Final QC record ${inspection.finalQcId} has already been released.")
        }
        if (inspection.status != FinalQcStatus.PASSED) {
            return DomainResult.Error(message = "Cannot authorize production release: Final QC status must be PASSED (Current: ${inspection.status}).")
        }
        return DomainResult.Success(Unit)
    }
}

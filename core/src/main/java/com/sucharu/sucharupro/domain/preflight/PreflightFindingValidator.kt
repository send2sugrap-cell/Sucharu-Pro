package com.sucharu.sucharupro.domain.preflight

import com.sucharu.sucharupro.domain.model.common.DomainResult

/**
 * Domain Validator for Preflight Finding governance transitions and corrections.
 */
object PreflightFindingValidator {

    fun validateFindingStatusTransition(
        currentStatus: PreflightFindingStatus,
        newStatus: PreflightFindingStatus
    ): DomainResult<Unit> {
        if (currentStatus.isTerminal) {
            return DomainResult.Error(message = "Terminal finding status '${currentStatus.name}' cannot be modified or transitioned to '${newStatus.name}'.")
        }
        return DomainResult.Success(Unit)
    }

    fun validateCorrectionSubmission(
        description: String,
        submittedBy: String
    ): DomainResult<Unit> {
        if (description.isBlank()) {
            return DomainResult.Error(message = "Correction description cannot be blank.")
        }
        if (submittedBy.isBlank()) {
            return DomainResult.Error(message = "Correction submittedBy actor ID cannot be blank.")
        }
        return DomainResult.Success(Unit)
    }

    fun validateWaiver(
        waiverReason: String,
        waivedBy: String
    ): DomainResult<Unit> {
        if (waiverReason.isBlank()) {
            return DomainResult.Error(message = "Waiver justification reason cannot be blank.")
        }
        if (waivedBy.isBlank()) {
            return DomainResult.Error(message = "WaivedBy manager actor ID cannot be blank.")
        }
        return DomainResult.Success(Unit)
    }
}

package com.sucharu.sucharupro.domain.preflight

import com.sucharu.sucharupro.domain.model.common.DomainResult

/**
 * Domain Validator for Preflight Runs and Execution Contexts.
 */
object PreflightValidator {

    fun validateRun(run: PreflightRun): DomainResult<Unit> {
        if (run.preflightRunId.isBlank()) {
            return DomainResult.Error(message = "Preflight run ID cannot be blank.")
        }
        if (run.tenantId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID cannot be blank.")
        }
        if (run.artworkId.isBlank()) {
            return DomainResult.Error(message = "Artwork ID cannot be blank.")
        }
        if (run.requestedBy.isBlank()) {
            return DomainResult.Error(message = "Requested by actor ID cannot be blank.")
        }
        return DomainResult.Success(Unit)
    }

    fun validateStatusTransition(currentStatus: PreflightRunStatus, newStatus: PreflightRunStatus): DomainResult<Unit> {
        if (currentStatus.isTerminal) {
            return DomainResult.Error(message = "Terminal preflight run status '${currentStatus.name}' cannot be modified or transitioned to '${newStatus.name}'.")
        }
        return DomainResult.Success(Unit)
    }
}

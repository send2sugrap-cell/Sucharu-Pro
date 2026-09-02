package com.sucharu.sucharupro.domain.validation.returns

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.returns.ReturnException
import com.sucharu.sucharupro.domain.model.returns.ReturnExceptionStatus

/**
 * Domain validator for Return Governance Exceptions and State Transitions (Module 11 Step 06).
 */
object ReturnGovernanceValidator {

    /**
     * Validates transition from current status to target status.
     */
    fun validateStateTransition(
        existing: ReturnException,
        targetStatus: ReturnExceptionStatus
    ): DomainResult<Unit> {
        if (existing.status.isTerminal) {
            return DomainResult.Error(
                message = "Exception '${existing.exceptionId}' is already in terminal state '${existing.status}' and cannot transition to '$targetStatus'."
            )
        }

        val isValid = when (existing.status) {
            ReturnExceptionStatus.OPEN -> targetStatus == ReturnExceptionStatus.ACKNOWLEDGED ||
                targetStatus == ReturnExceptionStatus.RESOLVED ||
                targetStatus == ReturnExceptionStatus.DISMISSED
            ReturnExceptionStatus.ACKNOWLEDGED -> targetStatus == ReturnExceptionStatus.RESOLVED ||
                targetStatus == ReturnExceptionStatus.DISMISSED
            ReturnExceptionStatus.RESOLVED, ReturnExceptionStatus.DISMISSED -> false
        }

        return if (isValid) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(
                message = "Invalid state transition for exception '${existing.exceptionId}': cannot move from '${existing.status}' to '$targetStatus'."
            )
        }
    }

    /**
     * Validates resolution payload requirements.
     */
    fun validateResolution(
        actorId: String,
        resolutionNotes: String
    ): DomainResult<Unit> {
        if (actorId.isBlank()) {
            return DomainResult.Error(message = "Actor ID cannot be blank.")
        }
        if (resolutionNotes.isBlank()) {
            return DomainResult.Error(message = "Resolution notes are mandatory when resolving or dismissing an exception.")
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates optimistic concurrency version.
     */
    fun validateVersion(
        existingVersion: Long,
        expectedVersion: Long
    ): DomainResult<Unit> {
        return if (existingVersion == expectedVersion) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(
                message = "Concurrency conflict: expected version $expectedVersion but found $existingVersion. Record has been modified by another user."
            )
        }
    }
}

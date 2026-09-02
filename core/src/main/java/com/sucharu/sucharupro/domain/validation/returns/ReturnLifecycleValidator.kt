package com.sucharu.sucharupro.domain.validation.returns

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.returns.ReturnStatus

/**
 * Domain service enforcing explicit Return lifecycle state transitions (Module 11 Step 01).
 *
 * Valid transitions:
 *   REQUESTED       → UNDER_INSPECTION | CANCELLED
 *   UNDER_INSPECTION → APPROVED | REJECTED | CANCELLED
 *   APPROVED        → RETURN_RECEIVED | CANCELLED
 *   RETURN_RECEIVED → PROCESSED
 *   REJECTED        → (terminal — no further transitions)
 *   PROCESSED       → (terminal — no further transitions)
 *   CANCELLED       → (terminal — no further transitions)
 *
 * Uses the canonical DomainResult<Unit> pattern consistent with the rest of the
 * validation layer (e.g. DeliveryReturnLifecycleValidator).
 */
object ReturnLifecycleValidator {

    /**
     * Validates if a transition from [current] to [target] status is allowed.
     *
     * A no-op self-transition (current == target) is considered valid to
     * avoid spurious errors on idempotent updates.
     */
    fun validateTransition(
        current: ReturnStatus,
        target: ReturnStatus
    ): DomainResult<Unit> {
        if (current == target) return DomainResult.Success(Unit)

        val isValid = when (current) {
            ReturnStatus.REQUESTED ->
                target == ReturnStatus.UNDER_INSPECTION || target == ReturnStatus.CANCELLED

            ReturnStatus.UNDER_INSPECTION ->
                target == ReturnStatus.APPROVED ||
                    target == ReturnStatus.REJECTED ||
                    target == ReturnStatus.CANCELLED

            ReturnStatus.APPROVED ->
                target == ReturnStatus.RETURN_RECEIVED || target == ReturnStatus.CANCELLED

            ReturnStatus.RETURN_RECEIVED ->
                target == ReturnStatus.PROCESSED

            // Terminal states — no further transitions allowed
            ReturnStatus.REJECTED  -> false
            ReturnStatus.PROCESSED -> false
            ReturnStatus.CANCELLED -> false
        }

        return if (isValid) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(
                message = "Invalid lifecycle transition: $current → $target. " +
                    "This transition is not permitted by the Return domain lifecycle rules."
            )
        }
    }
}

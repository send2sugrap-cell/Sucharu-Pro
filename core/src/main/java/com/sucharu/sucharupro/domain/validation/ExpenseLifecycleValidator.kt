package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.finance.ExpenseStatus

/**
 * Finite state machine validator for Expense lifecycle transitions (Module 09 Step 06).
 */
object ExpenseLifecycleValidator {

    fun validateTransition(
        from: ExpenseStatus,
        to: ExpenseStatus
    ): DomainResult<Unit> {
        if (from == to) {
            return DomainResult.Success(Unit)
        }

        if (from.isTerminal) {
            return DomainResult.Error(
                message = "Invalid transition: Terminal expense status '$from' cannot transition to '$to'."
            )
        }

        val isValid = when (from) {
            ExpenseStatus.DRAFT -> to == ExpenseStatus.PENDING ||
                    to == ExpenseStatus.APPROVED ||
                    to == ExpenseStatus.POSTED ||
                    to == ExpenseStatus.CANCELLED

            ExpenseStatus.PENDING -> to == ExpenseStatus.APPROVED ||
                    to == ExpenseStatus.POSTED ||
                    to == ExpenseStatus.REJECTED ||
                    to == ExpenseStatus.CANCELLED

            ExpenseStatus.APPROVED -> to == ExpenseStatus.POSTED ||
                    to == ExpenseStatus.CANCELLED

            ExpenseStatus.POSTED,
            ExpenseStatus.REJECTED,
            ExpenseStatus.CANCELLED -> false
        }

        return if (isValid) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(
                message = "Illegal expense status transition from '$from' to '$to'."
            )
        }
    }
}

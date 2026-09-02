package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentStatus

/**
 * Finite state machine validator for Financial Adjustment and Refund lifecycle transitions (Module 09 Step 07).
 */
object FinancialAdjustmentLifecycleValidator {

    fun validateTransition(
        from: FinancialAdjustmentStatus,
        to: FinancialAdjustmentStatus
    ): DomainResult<Unit> {
        if (from == to) {
            return DomainResult.Success(Unit)
        }

        if (from.isTerminal) {
            return DomainResult.Error(
                message = "Invalid transition: Terminal status '$from' cannot transition to '$to'."
            )
        }

        val isValid = when (from) {
            FinancialAdjustmentStatus.DRAFT -> to == FinancialAdjustmentStatus.PENDING ||
                    to == FinancialAdjustmentStatus.APPROVED ||
                    to == FinancialAdjustmentStatus.POSTED ||
                    to == FinancialAdjustmentStatus.CANCELLED

            FinancialAdjustmentStatus.PENDING -> to == FinancialAdjustmentStatus.APPROVED ||
                    to == FinancialAdjustmentStatus.POSTED ||
                    to == FinancialAdjustmentStatus.REJECTED ||
                    to == FinancialAdjustmentStatus.CANCELLED

            FinancialAdjustmentStatus.APPROVED -> to == FinancialAdjustmentStatus.POSTED ||
                    to == FinancialAdjustmentStatus.CANCELLED

            FinancialAdjustmentStatus.POSTED,
            FinancialAdjustmentStatus.REJECTED,
            FinancialAdjustmentStatus.CANCELLED -> false
        }

        return if (isValid) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(
                message = "Illegal status transition from '$from' to '$to'."
            )
        }
    }
}

package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionStatus

/**
 * Validates financial transaction lifecycle state machine transitions (Module 09 Step 01).
 *
 * Lifecycle:
 * DRAFT ──► PENDING, CANCELLED
 * PENDING ──► POSTED, REJECTED, CANCELLED
 * POSTED is immutable and terminal
 * CANCELLED is terminal
 * REJECTED is terminal
 */
object FinancialTransactionLifecycleValidator {

    fun validateTransition(
        currentStatus: FinancialTransactionStatus,
        targetStatus: FinancialTransactionStatus
    ): DomainResult<Unit> {
        if (currentStatus == targetStatus) {
            return DomainResult.Success(Unit)
        }

        if (currentStatus == FinancialTransactionStatus.POSTED) {
            return DomainResult.Error(
                message = "Posted financial transactions cannot transition to any other status. Attempted: $targetStatus"
            )
        }

        if (currentStatus == FinancialTransactionStatus.CANCELLED) {
            return DomainResult.Error(
                message = "Cancelled financial transactions cannot transition to any other status. Attempted: $targetStatus"
            )
        }

        if (currentStatus == FinancialTransactionStatus.REJECTED) {
            return DomainResult.Error(
                message = "Rejected financial transactions cannot transition to any other status. Attempted: $targetStatus"
            )
        }

        val isValid = when (currentStatus) {
            FinancialTransactionStatus.DRAFT -> {
                targetStatus == FinancialTransactionStatus.PENDING ||
                targetStatus == FinancialTransactionStatus.CANCELLED
            }
            FinancialTransactionStatus.PENDING -> {
                targetStatus == FinancialTransactionStatus.POSTED ||
                targetStatus == FinancialTransactionStatus.REJECTED ||
                targetStatus == FinancialTransactionStatus.CANCELLED
            }
            FinancialTransactionStatus.POSTED,
            FinancialTransactionStatus.REJECTED,
            FinancialTransactionStatus.CANCELLED -> false
        }

        return if (isValid) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(
                message = "Illegal financial transaction status transition from '$currentStatus' to '$targetStatus'."
            )
        }
    }
}

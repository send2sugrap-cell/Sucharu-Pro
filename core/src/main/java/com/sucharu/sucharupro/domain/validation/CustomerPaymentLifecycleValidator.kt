package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.finance.CustomerPaymentStatus

/**
 * State machine validator for Customer Payment lifecycle (Module 09 Step 03).
 */
object CustomerPaymentLifecycleValidator {

    fun validateTransition(
        currentStatus: CustomerPaymentStatus,
        targetStatus: CustomerPaymentStatus
    ): DomainResult<Unit> {
        if (currentStatus == targetStatus) return DomainResult.Success(Unit)

        if (currentStatus.isTerminal) {
            return DomainResult.Error(
                message = "Terminal customer payment status '$currentStatus' cannot transition to any other status. Attempted: $targetStatus"
            )
        }

        val isValid = when (currentStatus) {
            CustomerPaymentStatus.DRAFT -> {
                targetStatus == CustomerPaymentStatus.PENDING ||
                targetStatus == CustomerPaymentStatus.POSTED ||
                targetStatus == CustomerPaymentStatus.CANCELLED
            }
            CustomerPaymentStatus.PENDING -> {
                targetStatus == CustomerPaymentStatus.POSTED ||
                targetStatus == CustomerPaymentStatus.REJECTED ||
                targetStatus == CustomerPaymentStatus.CANCELLED
            }
            CustomerPaymentStatus.POSTED,
            CustomerPaymentStatus.REJECTED,
            CustomerPaymentStatus.CANCELLED -> false
        }

        return if (isValid) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(
                message = "Illegal customer payment status transition from '$currentStatus' to '$targetStatus'."
            )
        }
    }
}

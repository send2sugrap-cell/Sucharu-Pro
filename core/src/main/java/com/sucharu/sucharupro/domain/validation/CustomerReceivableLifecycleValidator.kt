package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.finance.CustomerReceivableStatus

/**
 * Validates legal state transitions for Customer Receivables (Module 09 Step 02).
 */
object CustomerReceivableLifecycleValidator {

    fun validateTransition(
        currentStatus: CustomerReceivableStatus,
        targetStatus: CustomerReceivableStatus
    ): DomainResult<Unit> {
        if (currentStatus == targetStatus) {
            return DomainResult.Success(Unit)
        }

        if (currentStatus == CustomerReceivableStatus.SETTLED) {
            return DomainResult.Error(
                message = "Settled customer receivables cannot transition to any other status. Attempted: $targetStatus"
            )
        }

        if (currentStatus == CustomerReceivableStatus.CANCELLED) {
            return DomainResult.Error(
                message = "Cancelled customer receivables cannot transition to any other status. Attempted: $targetStatus"
            )
        }

        val isValid = when (currentStatus) {
            CustomerReceivableStatus.OPEN -> {
                targetStatus == CustomerReceivableStatus.PARTIALLY_SETTLED ||
                targetStatus == CustomerReceivableStatus.SETTLED ||
                targetStatus == CustomerReceivableStatus.OVERDUE ||
                targetStatus == CustomerReceivableStatus.CANCELLED
            }
            CustomerReceivableStatus.OVERDUE -> {
                targetStatus == CustomerReceivableStatus.PARTIALLY_SETTLED ||
                targetStatus == CustomerReceivableStatus.SETTLED ||
                targetStatus == CustomerReceivableStatus.CANCELLED
            }
            CustomerReceivableStatus.PARTIALLY_SETTLED -> {
                targetStatus == CustomerReceivableStatus.SETTLED ||
                targetStatus == CustomerReceivableStatus.OVERDUE ||
                targetStatus == CustomerReceivableStatus.CANCELLED
            }
            CustomerReceivableStatus.SETTLED,
            CustomerReceivableStatus.CANCELLED -> false
        }

        return if (isValid) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(
                message = "Illegal customer receivable status transition from '$currentStatus' to '$targetStatus'."
            )
        }
    }
}

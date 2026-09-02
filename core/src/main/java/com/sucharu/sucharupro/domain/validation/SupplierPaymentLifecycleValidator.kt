package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.finance.SupplierPaymentStatus

/**
 * Finite state machine validator for Supplier Payment lifecycle transitions (Module 09 Step 05).
 */
object SupplierPaymentLifecycleValidator {

    fun validateTransition(
        from: SupplierPaymentStatus,
        to: SupplierPaymentStatus
    ): DomainResult<Unit> {
        if (from == to) {
            return DomainResult.Success(Unit)
        }

        if (from.isTerminal) {
            return DomainResult.Error(
                message = "Invalid transition: Terminal payment status '$from' cannot transition to '$to'."
            )
        }

        val isValid = when (from) {
            SupplierPaymentStatus.DRAFT -> to == SupplierPaymentStatus.PENDING ||
                    to == SupplierPaymentStatus.APPROVED ||
                    to == SupplierPaymentStatus.POSTED ||
                    to == SupplierPaymentStatus.CANCELLED

            SupplierPaymentStatus.PENDING -> to == SupplierPaymentStatus.APPROVED ||
                    to == SupplierPaymentStatus.POSTED ||
                    to == SupplierPaymentStatus.REJECTED ||
                    to == SupplierPaymentStatus.CANCELLED

            SupplierPaymentStatus.APPROVED -> to == SupplierPaymentStatus.POSTED ||
                    to == SupplierPaymentStatus.CANCELLED

            SupplierPaymentStatus.POSTED,
            SupplierPaymentStatus.REJECTED,
            SupplierPaymentStatus.CANCELLED -> false
        }

        return if (isValid) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(
                message = "Illegal supplier payment status transition from '$from' to '$to'."
            )
        }
    }
}

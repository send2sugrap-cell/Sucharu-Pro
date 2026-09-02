package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.finance.VendorPayableStatus

/**
 * Finite state machine validator for Vendor Payable lifecycle transitions (Module 09 Step 04).
 */
object VendorPayableLifecycleValidator {

    fun validateTransition(
        from: VendorPayableStatus,
        to: VendorPayableStatus
    ): DomainResult<Unit> {
        if (from == to) {
            return DomainResult.Success(Unit)
        }

        if (from.isTerminal) {
            return DomainResult.Error(
                message = "Invalid lifecycle transition: Terminal status '$from' cannot transition to '$to'."
            )
        }

        val isValid = when (from) {
            VendorPayableStatus.DRAFT -> to == VendorPayableStatus.PENDING ||
                    to == VendorPayableStatus.APPROVED ||
                    to == VendorPayableStatus.CANCELLED

            VendorPayableStatus.PENDING -> to == VendorPayableStatus.APPROVED ||
                    to == VendorPayableStatus.DRAFT ||
                    to == VendorPayableStatus.CANCELLED

            VendorPayableStatus.APPROVED -> to == VendorPayableStatus.PARTIALLY_SETTLED ||
                    to == VendorPayableStatus.SETTLED ||
                    to == VendorPayableStatus.OVERDUE ||
                    to == VendorPayableStatus.CANCELLED

            VendorPayableStatus.OVERDUE -> to == VendorPayableStatus.PARTIALLY_SETTLED ||
                    to == VendorPayableStatus.SETTLED ||
                    to == VendorPayableStatus.CANCELLED

            VendorPayableStatus.PARTIALLY_SETTLED -> to == VendorPayableStatus.SETTLED ||
                    to == VendorPayableStatus.OVERDUE

            VendorPayableStatus.SETTLED,
            VendorPayableStatus.CANCELLED -> false
        }

        return if (isValid) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(
                message = "Illegal payable status transition from '$from' to '$to'."
            )
        }
    }
}

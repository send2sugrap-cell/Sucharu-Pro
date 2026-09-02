package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Validates that requested customer refund amounts do not exceed refundable balances (Module 09 Step 07).
 */
object RefundEligibilityValidator {

    fun validateRefundAmount(
        requestedRefundAmount: Money,
        refundableAvailableBalance: Money
    ): DomainResult<Unit> {
        if (!requestedRefundAmount.isPositive()) {
            return DomainResult.Error(message = "Refund amount must be strictly greater than zero.")
        }

        if (requestedRefundAmount > refundableAvailableBalance) {
            return DomainResult.Error(
                message = "Requested refund amount (${requestedRefundAmount.formatted()}) exceeds available refundable balance (${refundableAvailableBalance.formatted()})."
            )
        }

        return DomainResult.Success(Unit)
    }
}

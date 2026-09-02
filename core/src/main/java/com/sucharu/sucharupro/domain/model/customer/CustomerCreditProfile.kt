package com.sucharu.sucharupro.domain.model.customer

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Credit terms and limits for a customer profile in Sucharu Pro.
 *
 * Defines credit rules without managing transactional ledger state.
 * Due and ledger computations are deferred to finance modules.
 */
data class CustomerCreditProfile(
    /** Maximum allowed unpaid balance before credit orders are restricted. */
    val creditLimit: Money = Money.ZERO,

    /** Allowed credit repayment period in days (e.g. 15, 30, 45 days). */
    val paymentTermDays: Int = 0,

    /** Whether this customer is allowed credit transactions or must pay advance/cash. */
    val isCreditAllowed: Boolean = false,

    /** Whether advance deposit is strictly required for this customer. */
    val isAdvanceRequired: Boolean = true
) {
    init {
        require(!creditLimit.isNegative()) { "Credit limit cannot be negative." }
        require(paymentTermDays >= 0) { "Payment term days cannot be negative." }
    }

    companion object {
        /** Default cash-only profile with zero credit. */
        val DEFAULT_CASH_ONLY = CustomerCreditProfile(
            creditLimit = Money.ZERO,
            paymentTermDays = 0,
            isCreditAllowed = false,
            isAdvanceRequired = true
        )
    }
}

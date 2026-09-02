package com.sucharu.sucharupro.domain.model.order

/**
 * Commercial payment terms agreement for a Quotation or Order.
 */
data class PaymentTerms(
    val type: PaymentTermType = PaymentTermType.FULL_ADVANCE,
    val advancePercentage: Int = 100,
    val dueDays: Int = 0,
    val customDescription: String? = null
) {
    init {
        require(advancePercentage in 0..100) {
            "Advance percentage must be between 0 and 100 (was $advancePercentage)."
        }
        require(dueDays >= 0) {
            "Due days must be non-negative (was $dueDays)."
        }
    }

    companion object {
        val DEFAULT = PaymentTerms(
            type = PaymentTermType.FULL_ADVANCE,
            advancePercentage = 100,
            dueDays = 0
        )
        val CASH_ON_DELIVERY = PaymentTerms(
            type = PaymentTermType.ON_DELIVERY,
            advancePercentage = 0,
            dueDays = 0
        )
        val HALF_DEPOSIT = PaymentTerms(
            type = PaymentTermType.PARTIAL_ADVANCE,
            advancePercentage = 50,
            dueDays = 0
        )
    }
}

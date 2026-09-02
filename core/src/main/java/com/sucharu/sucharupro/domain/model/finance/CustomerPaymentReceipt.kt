package com.sucharu.sucharupro.domain.model.finance

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Immutable financial payment receipt issued upon posting a customer payment (Module 09 Step 03).
 */
data class CustomerPaymentReceipt(
    val receiptId: String,
    val receiptNo: String,
    val projectId: String,
    val paymentId: String,
    val customerId: String,
    val receivableId: String,
    val amount: Money,
    val currency: String = "BDT",
    val paymentMethod: CustomerPaymentMethod,
    val paymentReference: String? = null,
    val paymentDate: Long,
    val issuedBy: String,
    val issuedAt: Long = System.currentTimeMillis(),
    val notes: String? = null
) {
    init {
        require(receiptId.isNotBlank()) { "Receipt ID cannot be blank." }
        require(receiptNo.isNotBlank()) { "Receipt Number cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(paymentId.isNotBlank()) { "Payment ID cannot be blank." }
        require(customerId.isNotBlank()) { "Customer ID cannot be blank." }
        require(receivableId.isNotBlank()) { "Receivable ID cannot be blank." }
        require(issuedBy.isNotBlank()) { "Issued By cannot be blank." }
        require(amount.isPositive()) { "Receipt amount must be strictly positive (> 0)." }
        require(currency.length == 3 && currency.all { it.isUpperCase() }) {
            "Currency code must be a 3-letter uppercase string (e.g. 'BDT'). Provided: '$currency'"
        }
        require(issuedAt > 0) { "Issued timestamp must be positive." }
    }
}

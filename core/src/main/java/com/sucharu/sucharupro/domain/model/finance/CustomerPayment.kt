package com.sucharu.sucharupro.domain.model.finance

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Core aggregate root representing a customer payment event in Sucharu Pro (Module 09 Step 03).
 */
data class CustomerPayment(
    val paymentId: String,
    val paymentNo: String,
    val projectId: String,
    val customerId: String,
    val receivableId: String,
    val financialTransactionId: String? = null,
    val receiptId: String? = null,
    val idempotencyKey: String? = null,
    val amount: Money,
    val currency: String = "BDT",
    val paymentMethod: CustomerPaymentMethod,
    val paymentReference: String? = null,
    val paymentDate: Long = System.currentTimeMillis(),
    val status: CustomerPaymentStatus = CustomerPaymentStatus.DRAFT,
    val notes: String? = null,
    val createdBy: String,
    val postedBy: String? = null,
    val rejectedBy: String? = null,
    val cancelledBy: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val postedAt: Long? = null,
    val cancelledAt: Long? = null,
    val cancellationReason: String? = null,
    val rejectionReason: String? = null
) {
    init {
        require(paymentId.isNotBlank()) { "Payment ID cannot be blank." }
        require(paymentNo.isNotBlank()) { "Payment Number cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(customerId.isNotBlank()) { "Customer ID cannot be blank." }
        require(receivableId.isNotBlank()) { "Receivable ID cannot be blank." }
        require(createdBy.isNotBlank()) { "Created By cannot be blank." }
        require(amount.isPositive()) { "Payment amount must be strictly positive (> 0)." }
        require(currency.length == 3 && currency.all { it.isUpperCase() }) {
            "Currency code must be a 3-letter uppercase string (e.g. 'BDT'). Provided: '$currency'"
        }
        require(paymentDate > 0) { "Payment date must be positive." }
        require(createdAt > 0) { "Creation timestamp must be positive." }
        require(updatedAt >= createdAt) { "Updated timestamp cannot precede creation timestamp." }
    }
}

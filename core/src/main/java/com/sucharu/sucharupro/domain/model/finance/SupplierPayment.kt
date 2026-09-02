package com.sucharu.sucharupro.domain.model.finance

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Core aggregate root representing an outbound supplier/vendor payment disbursement (Module 09 Step 05).
 */
data class SupplierPayment(
    val paymentId: String,
    val paymentNo: String,
    val projectId: String,
    val vendorId: String,
    val payableId: String,
    val financialTransactionId: String? = null,
    val amount: Money,
    val currency: String = "BDT",
    val paymentMethod: SupplierPaymentMethod,
    val paymentReference: String? = null,
    val paymentDate: Long = System.currentTimeMillis(),
    val status: SupplierPaymentStatus = SupplierPaymentStatus.DRAFT,
    val notes: String? = null,
    val idempotencyKey: String? = null,
    val createdBy: String,
    val updatedBy: String? = null,
    val submittedBy: String? = null,
    val approvedBy: String? = null,
    val rejectedBy: String? = null,
    val cancelledBy: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val submittedAt: Long? = null,
    val approvedAt: Long? = null,
    val rejectedAt: Long? = null,
    val cancelledAt: Long? = null,
    val postedAt: Long? = null,
    val cancellationReason: String? = null
) {
    init {
        require(paymentId.isNotBlank()) { "Payment ID cannot be blank." }
        require(paymentNo.isNotBlank()) { "Payment Number cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(vendorId.isNotBlank()) { "Vendor ID cannot be blank." }
        require(payableId.isNotBlank()) { "Payable ID cannot be blank." }
        require(createdBy.isNotBlank()) { "Created By cannot be blank." }
        require(amount.isPositive()) { "Payment amount must be strictly positive (> 0)." }
        require(currency.length == 3 && currency.all { it.isUpperCase() }) {
            "Currency code must be a 3-letter uppercase string (e.g. 'BDT'). Provided: '$currency'"
        }
        if (paymentMethod.requiresReference) {
            require(!paymentReference.isNullOrBlank()) {
                "Payment reference (e.g. Cheque No, EFT Trx ID, bKash Trx ID) is required for payment method '${paymentMethod.defaultLabel}'."
            }
        }
        require(paymentDate > 0) { "Payment date must be a valid positive timestamp." }
        require(createdAt > 0) { "Creation timestamp must be positive." }
        require(updatedAt >= createdAt) { "Updated timestamp cannot precede creation timestamp." }
    }
}

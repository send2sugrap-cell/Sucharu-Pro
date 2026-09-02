package com.sucharu.sucharupro.domain.model.finance

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Aggregate root representing actual money disbursed / returned to a customer (Module 09 Step 07).
 */
data class CustomerRefund(
    val refundId: String,
    val refundNo: String,
    val projectId: String,
    val customerId: String,
    val adjustmentId: String? = null,
    val sourcePaymentId: String? = null,
    val receivableId: String? = null,
    val amount: Money,
    val currency: String = "BDT",
    val refundMethod: CustomerRefundMethod,
    val refundReference: String? = null,
    val reason: String,
    val status: FinancialAdjustmentStatus = FinancialAdjustmentStatus.DRAFT,
    val financialTransactionId: String? = null,
    val idempotencyKey: String? = null,
    val createdBy: String,
    val updatedBy: String? = null,
    val submittedBy: String? = null,
    val approvedBy: String? = null,
    val postedBy: String? = null,
    val rejectedBy: String? = null,
    val cancelledBy: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val submittedAt: Long? = null,
    val approvedAt: Long? = null,
    val postedAt: Long? = null,
    val rejectedAt: Long? = null,
    val cancelledAt: Long? = null,
    val cancellationReason: String? = null
) {
    init {
        require(refundId.isNotBlank()) { "Refund ID cannot be blank." }
        require(refundNo.isNotBlank()) { "Refund No cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(customerId.isNotBlank()) { "Customer ID cannot be blank." }
        require(amount.isPositive()) { "Refund amount must be strictly positive (> 0)." }
        require(currency.length == 3 && currency.all { it.isUpperCase() }) { "Currency must be 3 uppercase letters." }
        require(reason.isNotBlank()) { "Refund reason cannot be blank." }
        require(createdBy.isNotBlank()) { "Created By cannot be blank." }
        if (refundMethod.requiresReference) {
            require(!refundReference.isNullOrBlank()) {
                "Payment/Refund reference is required for payment method '${refundMethod.defaultLabel}'."
            }
        }
        require(createdAt > 0) { "Creation timestamp must be positive." }
        require(updatedAt >= createdAt) { "Updated timestamp cannot precede creation timestamp." }
    }
}

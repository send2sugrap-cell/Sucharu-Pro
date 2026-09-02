package com.sucharu.sucharupro.domain.model.finance

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Core aggregate root representing a financial adjustment, credit note, or debit note (Module 09 Step 07).
 */
data class FinancialAdjustment(
    val adjustmentId: String,
    val adjustmentNo: String,
    val projectId: String,
    val adjustmentType: FinancialAdjustmentType,
    val direction: FinancialAdjustmentDirection,
    val status: FinancialAdjustmentStatus = FinancialAdjustmentStatus.DRAFT,
    val amount: Money,
    val currency: String = "BDT",
    val customerId: String? = null,
    val vendorId: String? = null,
    val referenceType: FinancialReferenceType,
    val referenceId: String,
    val reasonCode: String,
    val reason: String,
    val description: String,
    val notes: String? = null,
    val financialTransactionId: String? = null,
    val relatedReceivableId: String? = null,
    val relatedPayableId: String? = null,
    val relatedPaymentId: String? = null,
    val relatedSupplierPaymentId: String? = null,
    val creditNoteId: String? = null,
    val debitNoteId: String? = null,
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
        require(adjustmentId.isNotBlank()) { "Adjustment ID cannot be blank." }
        require(adjustmentNo.isNotBlank()) { "Adjustment No cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(referenceId.isNotBlank()) { "Reference ID cannot be blank." }
        require(amount.isPositive()) { "Adjustment amount must be strictly positive (> 0)." }
        require(currency.length == 3 && currency.all { it.isUpperCase() }) { "Currency must be 3 uppercase letters." }
        require(reason.isNotBlank()) { "Reason cannot be blank." }
        require(description.isNotBlank()) { "Description cannot be blank." }
        require(createdBy.isNotBlank()) { "Created By cannot be blank." }
        if (adjustmentType.isCustomerFacing) {
            require(!customerId.isNullOrBlank()) { "Customer ID is required for customer-facing adjustment '${adjustmentType.defaultLabel}'." }
            require(vendorId.isNullOrBlank()) { "Vendor ID cannot be set on customer-facing adjustment." }
        }
        if (adjustmentType.isVendorFacing) {
            require(!vendorId.isNullOrBlank()) { "Vendor ID is required for vendor-facing adjustment '${adjustmentType.defaultLabel}'." }
            require(customerId.isNullOrBlank()) { "Customer ID cannot be set on vendor-facing adjustment." }
        }
        require(createdAt > 0) { "Creation timestamp must be positive." }
        require(updatedAt >= createdAt) { "Updated timestamp cannot precede creation timestamp." }
    }
}

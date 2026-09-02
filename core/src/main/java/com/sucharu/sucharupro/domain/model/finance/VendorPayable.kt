package com.sucharu.sucharupro.domain.model.finance

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Core aggregate root representing a supplier/vendor payable liability in Sucharu Pro (Module 09 Step 04).
 */
data class VendorPayable(
    val payableId: String,
    val payableNo: String,
    val projectId: String,
    val vendorId: String,
    val referenceType: FinancialReferenceType,
    val referenceId: String,
    val supplierInvoiceNo: String? = null,
    val financialTransactionId: String? = null,
    val originalAmount: Money,
    val settledAmount: Money = Money.ZERO,
    val currency: String = "BDT",
    val dueDate: Long,
    val payableDate: Long = System.currentTimeMillis(),
    val status: VendorPayableStatus = VendorPayableStatus.DRAFT,
    val agingBucket: VendorPayableAgingBucket = VendorPayableAgingBucket.CURRENT,
    val description: String,
    val notes: String? = null,
    val createdBy: String,
    val updatedBy: String? = null,
    val approvedBy: String? = null,
    val cancelledBy: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val approvedAt: Long? = null,
    val settledAt: Long? = null,
    val cancelledAt: Long? = null,
    val cancellationReason: String? = null
) {
    /**
     * Outstanding liability balance owed to the vendor.
     * Guaranteed invariant: originalAmount - settledAmount >= Money.ZERO
     */
    val outstandingAmount: Money
        get() {
            return if (settledAmount >= originalAmount) {
                Money.ZERO
            } else {
                originalAmount - settledAmount
            }
        }

    init {
        require(payableId.isNotBlank()) { "Payable ID cannot be blank." }
        require(payableNo.isNotBlank()) { "Payable Number cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(vendorId.isNotBlank()) { "Vendor ID cannot be blank." }
        require(referenceId.isNotBlank()) { "Reference ID cannot be blank." }
        require(description.isNotBlank()) { "Description cannot be blank." }
        require(createdBy.isNotBlank()) { "Created By cannot be blank." }
        require(originalAmount.isPositive()) { "Original amount must be strictly positive (> 0)." }
        require(!settledAmount.isNegative()) { "Settled amount cannot be negative." }
        require(settledAmount <= originalAmount) {
            "Settled amount (${settledAmount.formatted()}) cannot exceed original payable amount (${originalAmount.formatted()})."
        }
        require(currency.length == 3 && currency.all { it.isUpperCase() }) {
            "Currency code must be a 3-letter uppercase string (e.g. 'BDT'). Provided: '$currency'"
        }
        require(dueDate > 0) { "Due date timestamp must be positive." }
        require(createdAt > 0) { "Creation timestamp must be positive." }
        require(updatedAt >= createdAt) { "Updated timestamp cannot precede creation timestamp." }
    }
}

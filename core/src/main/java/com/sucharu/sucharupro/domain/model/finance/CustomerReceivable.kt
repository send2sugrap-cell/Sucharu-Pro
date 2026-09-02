package com.sucharu.sucharupro.domain.model.finance

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Core aggregate root representing a customer receivable obligation in Sucharu Pro (Module 09 Step 02).
 *
 * Tracks the financial due resulting from a commercial order/delivery/invoice reference,
 * maintaining deterministic outstanding calculations and aging classifications.
 */
data class CustomerReceivable(
    val receivableId: String,
    val receivableNo: String,
    val projectId: String,
    val customerId: String,
    val referenceType: FinancialReferenceType,
    val referenceId: String,
    val financialTransactionId: String? = null,
    val originalAmount: Money,
    val settledAmount: Money = Money.ZERO,
    val currency: String = "BDT",
    val dueDate: Long,
    val status: CustomerReceivableStatus = CustomerReceivableStatus.OPEN,
    val agingBucket: ReceivableAgingBucket = ReceivableAgingBucket.CURRENT,
    val description: String,
    val notes: String? = null,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val settledAt: Long? = null,
    val cancelledAt: Long? = null,
    val cancellationReason: String? = null
) {
    /**
     * Outstanding unpaid due balance.
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
        require(receivableId.isNotBlank()) { "Receivable ID cannot be blank." }
        require(receivableNo.isNotBlank()) { "Receivable Number cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(customerId.isNotBlank()) { "Customer ID cannot be blank." }
        require(referenceId.isNotBlank()) { "Reference ID cannot be blank." }
        require(description.isNotBlank()) { "Description cannot be blank." }
        require(createdBy.isNotBlank()) { "Created By cannot be blank." }
        require(originalAmount.isPositive()) { "Original amount must be strictly positive (> 0)." }
        require(!settledAmount.isNegative()) { "Settled amount cannot be negative." }
        require(settledAmount <= originalAmount) {
            "Settled amount (${settledAmount.formatted()}) cannot exceed original receivable amount (${originalAmount.formatted()})."
        }
        require(currency.length == 3 && currency.all { it.isUpperCase() }) {
            "Currency code must be a 3-letter uppercase string (e.g. 'BDT'). Provided: '$currency'"
        }
        require(createdAt > 0) { "Created timestamp must be positive." }
        require(updatedAt >= createdAt) { "Updated timestamp cannot precede creation timestamp." }
    }
}

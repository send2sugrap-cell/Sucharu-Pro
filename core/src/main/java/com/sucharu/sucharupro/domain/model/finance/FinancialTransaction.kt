package com.sucharu.sucharupro.domain.model.finance

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Core aggregate root representing a financial event/transaction in Sucharu Pro (Module 09 Step 01).
 *
 * Implements immutable posted identity, strong decimal precision via [Money],
 * and deterministic business reference traceability.
 */
data class FinancialTransaction(
    val transactionId: String,
    val projectId: String,
    val transactionNo: String,
    val transactionType: FinancialTransactionType,
    val transactionStatus: FinancialTransactionStatus = FinancialTransactionStatus.DRAFT,
    val entryType: FinancialEntryType,
    val amount: Money,
    val currency: String = "BDT",
    val referenceType: FinancialReferenceType,
    val referenceId: String,
    val customerId: String? = null,
    val vendorId: String? = null,
    val transactionDate: Long,
    val description: String,
    val notes: String? = null,
    val postedBy: String? = null,
    val postedAt: Long? = null,
    val rejectedBy: String? = null,
    val rejectedAt: Long? = null,
    val rejectionReason: String? = null,
    val cancelledBy: String? = null,
    val cancelledAt: Long? = null,
    val cancellationReason: String? = null,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(transactionId.isNotBlank()) { "Transaction ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(transactionNo.isNotBlank()) { "Transaction Number cannot be blank." }
        require(referenceId.isNotBlank()) { "Reference ID cannot be blank." }
        require(description.isNotBlank()) { "Description cannot be blank." }
        require(createdBy.isNotBlank()) { "Created By cannot be blank." }
        require(amount.isPositive()) { "Transaction amount must be strictly positive (> 0)." }
        require(currency.length == 3 && currency.all { it.isUpperCase() }) {
            "Currency code must be a 3-letter uppercase string (e.g. 'BDT'). Provided: '$currency'"
        }
        require(createdAt > 0) { "Created timestamp must be positive." }
        require(updatedAt >= createdAt) { "Updated timestamp cannot precede creation timestamp." }
    }
}

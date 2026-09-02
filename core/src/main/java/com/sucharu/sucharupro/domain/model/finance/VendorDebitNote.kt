package com.sucharu.sucharupro.domain.model.finance

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Immutable financial document generated when a Vendor Debit Note is posted (Module 09 Step 07).
 */
data class VendorDebitNote(
    val debitNoteId: String,
    val debitNoteNo: String,
    val projectId: String,
    val adjustmentId: String,
    val vendorId: String,
    val referenceType: FinancialReferenceType,
    val referenceId: String,
    val amount: Money,
    val currency: String = "BDT",
    val reason: String,
    val issuedBy: String,
    val issuedAt: Long = System.currentTimeMillis(),
    val financialTransactionId: String,
    val notes: String? = null
) {
    init {
        require(debitNoteId.isNotBlank()) { "Debit Note ID cannot be blank." }
        require(debitNoteNo.isNotBlank()) { "Debit Note No cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(adjustmentId.isNotBlank()) { "Adjustment ID cannot be blank." }
        require(vendorId.isNotBlank()) { "Vendor ID cannot be blank." }
        require(referenceId.isNotBlank()) { "Reference ID cannot be blank." }
        require(amount.isPositive()) { "Debit Note amount must be strictly positive." }
        require(currency.length == 3 && currency.all { it.isUpperCase() }) { "Currency must be 3 uppercase letters." }
        require(reason.isNotBlank()) { "Reason cannot be blank." }
        require(issuedBy.isNotBlank()) { "Issued By cannot be blank." }
        require(financialTransactionId.isNotBlank()) { "Financial Transaction ID cannot be blank." }
        require(issuedAt > 0) { "Issue timestamp must be positive." }
    }
}

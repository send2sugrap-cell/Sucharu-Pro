package com.sucharu.sucharupro.domain.model.finance

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Immutable ledger entry representing an authoritative posted financial debit/credit line (Module 09 Step 01).
 */
data class FinancialLedgerEntry(
    val entryId: String,
    val transactionId: String,
    val projectId: String,
    val entryNo: String,
    val entryType: FinancialEntryType,
    val amount: Money,
    val currency: String = "BDT",
    val accountHead: String,
    val referenceType: FinancialReferenceType,
    val referenceId: String,
    val entryDate: Long,
    val narration: String,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    init {
        require(entryId.isNotBlank()) { "Entry ID cannot be blank." }
        require(transactionId.isNotBlank()) { "Transaction ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(entryNo.isNotBlank()) { "Entry Number cannot be blank." }
        require(accountHead.isNotBlank()) { "Account Head cannot be blank." }
        require(narration.isNotBlank()) { "Narration cannot be blank." }
        require(createdBy.isNotBlank()) { "Created By cannot be blank." }
        require(amount.isPositive()) { "Ledger entry amount must be strictly positive (> 0)." }
        require(currency.length == 3 && currency.all { it.isUpperCase() }) {
            "Currency code must be a 3-letter uppercase string (e.g. 'BDT'). Provided: '$currency'"
        }
        require(createdAt > 0) { "Created timestamp must be positive." }
    }
}

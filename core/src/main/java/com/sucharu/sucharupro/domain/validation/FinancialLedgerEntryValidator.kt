package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.finance.FinancialLedgerEntry

/**
 * Domain validator for Financial Ledger Entries (Module 09 Step 01).
 */
object FinancialLedgerEntryValidator {

    fun validateEntry(
        entry: FinancialLedgerEntry,
        expectedProjectId: String? = null
    ): DomainResult<Unit> {
        if (entry.entryId.isBlank()) {
            return DomainResult.Error(message = "Ledger Entry ID cannot be blank.")
        }
        if (entry.transactionId.isBlank()) {
            return DomainResult.Error(message = "Transaction ID cannot be blank.")
        }
        if (entry.projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        if (expectedProjectId != null && entry.projectId != expectedProjectId) {
            return DomainResult.Error(
                message = "Project ID mismatch. Expected '$expectedProjectId' but got '${entry.projectId}'."
            )
        }
        if (entry.entryNo.isBlank()) {
            return DomainResult.Error(message = "Entry Number cannot be blank.")
        }
        if (entry.accountHead.isBlank()) {
            return DomainResult.Error(message = "Account Head cannot be blank.")
        }
        if (entry.narration.isBlank()) {
            return DomainResult.Error(message = "Narration cannot be blank.")
        }
        if (entry.createdBy.isBlank()) {
            return DomainResult.Error(message = "Created By cannot be blank.")
        }
        if (!entry.amount.isPositive()) {
            return DomainResult.Error(message = "Ledger entry amount must be strictly positive (> 0).")
        }
        if (entry.currency.length != 3 || !entry.currency.all { it.isUpperCase() }) {
            return DomainResult.Error(
                message = "Currency code must be a 3-letter uppercase string (e.g. 'BDT'). Provided: '${entry.currency}'."
            )
        }
        if (entry.createdAt <= 0) {
            return DomainResult.Error(message = "Created timestamp must be positive.")
        }
        return DomainResult.Success(Unit)
    }
}

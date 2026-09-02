package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.finance.FinancialTransaction
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionStatus

/**
 * Domain validator for Financial Transactions (Module 09 Step 01).
 */
object FinancialTransactionValidator {

    fun validateTransaction(
        transaction: FinancialTransaction,
        expectedProjectId: String? = null
    ): DomainResult<Unit> {
        if (transaction.transactionId.isBlank()) {
            return DomainResult.Error(message = "Transaction ID cannot be blank.")
        }
        if (transaction.projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        if (expectedProjectId != null && transaction.projectId != expectedProjectId) {
            return DomainResult.Error(
                message = "Project ID mismatch. Expected '$expectedProjectId' but got '${transaction.projectId}'."
            )
        }
        if (transaction.transactionNo.isBlank()) {
            return DomainResult.Error(message = "Transaction Number cannot be blank.")
        }
        if (transaction.referenceId.isBlank()) {
            return DomainResult.Error(message = "Reference ID cannot be blank.")
        }
        if (transaction.description.isBlank()) {
            return DomainResult.Error(message = "Description cannot be blank.")
        }
        if (transaction.createdBy.isBlank()) {
            return DomainResult.Error(message = "Created By cannot be blank.")
        }
        if (!transaction.amount.isPositive()) {
            return DomainResult.Error(message = "Transaction amount must be strictly positive (> 0).")
        }
        if (transaction.currency.length != 3 || !transaction.currency.all { it.isUpperCase() }) {
            return DomainResult.Error(
                message = "Currency code must be a 3-letter uppercase string (e.g. 'BDT'). Provided: '${transaction.currency}'."
            )
        }
        if (transaction.createdAt <= 0) {
            return DomainResult.Error(message = "Created timestamp must be positive.")
        }
        if (transaction.updatedAt < transaction.createdAt) {
            return DomainResult.Error(message = "Updated timestamp cannot precede creation timestamp.")
        }

        // Validate state-specific invariants
        when (transaction.transactionStatus) {
            FinancialTransactionStatus.POSTED -> {
                if (transaction.postedBy.isNullOrBlank()) {
                    return DomainResult.Error(message = "Posted transactions must have a valid postedBy user ID.")
                }
                if (transaction.postedAt == null || transaction.postedAt <= 0) {
                    return DomainResult.Error(message = "Posted transactions must have a valid positive postedAt timestamp.")
                }
            }
            FinancialTransactionStatus.REJECTED -> {
                if (transaction.rejectedBy.isNullOrBlank()) {
                    return DomainResult.Error(message = "Rejected transactions must have a valid rejectedBy user ID.")
                }
                if (transaction.rejectionReason.isNullOrBlank()) {
                    return DomainResult.Error(message = "Rejected transactions must provide a rejection reason.")
                }
            }
            FinancialTransactionStatus.CANCELLED -> {
                if (transaction.cancelledBy.isNullOrBlank()) {
                    return DomainResult.Error(message = "Cancelled transactions must have a valid cancelledBy user ID.")
                }
                if (transaction.cancellationReason.isNullOrBlank()) {
                    return DomainResult.Error(message = "Cancelled transactions must provide a cancellation reason.")
                }
            }
            FinancialTransactionStatus.DRAFT,
            FinancialTransactionStatus.PENDING -> {
                // Pre-posting states
            }
        }

        return DomainResult.Success(Unit)
    }

    fun validateImmutabilityOnUpdate(
        existing: FinancialTransaction,
        updated: FinancialTransaction
    ): DomainResult<Unit> {
        if (existing.transactionStatus == FinancialTransactionStatus.POSTED) {
            return DomainResult.Error(
                message = "Posted financial transaction '${existing.transactionId}' is immutable and cannot be modified."
            )
        }
        if (existing.transactionStatus == FinancialTransactionStatus.CANCELLED) {
            return DomainResult.Error(
                message = "Cancelled financial transaction '${existing.transactionId}' cannot be modified."
            )
        }
        if (existing.transactionStatus == FinancialTransactionStatus.REJECTED) {
            return DomainResult.Error(
                message = "Rejected financial transaction '${existing.transactionId}' cannot be modified."
            )
        }
        if (existing.transactionId != updated.transactionId) {
            return DomainResult.Error(message = "Transaction ID is immutable and cannot be changed.")
        }
        if (existing.projectId != updated.projectId) {
            return DomainResult.Error(message = "Project ID is immutable and cannot be changed.")
        }
        if (existing.createdBy != updated.createdBy) {
            return DomainResult.Error(message = "Original creator cannot be changed.")
        }
        if (existing.createdAt != updated.createdAt) {
            return DomainResult.Error(message = "Original creation timestamp cannot be changed.")
        }
        return DomainResult.Success(Unit)
    }
}

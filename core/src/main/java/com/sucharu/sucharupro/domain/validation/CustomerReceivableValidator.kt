package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.finance.CustomerReceivable
import com.sucharu.sucharupro.domain.model.finance.CustomerReceivableStatus

/**
 * Invariant validator for Customer Receivable obligations (Module 09 Step 02).
 */
object CustomerReceivableValidator {

    fun validateReceivable(
        receivable: CustomerReceivable,
        expectedProjectId: String? = null
    ): DomainResult<Unit> {
        if (receivable.receivableId.isBlank()) {
            return DomainResult.Error(message = "Receivable ID cannot be blank.")
        }
        if (receivable.receivableNo.isBlank()) {
            return DomainResult.Error(message = "Receivable Number cannot be blank.")
        }
        if (receivable.projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        if (expectedProjectId != null && receivable.projectId != expectedProjectId) {
            return DomainResult.Error(
                message = "Project ID mismatch. Expected '$expectedProjectId' but got '${receivable.projectId}'."
            )
        }
        if (receivable.customerId.isBlank()) {
            return DomainResult.Error(message = "Customer ID cannot be blank.")
        }
        if (receivable.referenceId.isBlank()) {
            return DomainResult.Error(message = "Reference ID cannot be blank.")
        }
        if (receivable.description.isBlank()) {
            return DomainResult.Error(message = "Description cannot be blank.")
        }
        if (receivable.createdBy.isBlank()) {
            return DomainResult.Error(message = "Created By cannot be blank.")
        }
        if (!receivable.originalAmount.isPositive()) {
            return DomainResult.Error(message = "Original amount must be strictly positive (> 0).")
        }
        if (receivable.settledAmount.isNegative()) {
            return DomainResult.Error(message = "Settled amount cannot be negative.")
        }
        if (receivable.settledAmount > receivable.originalAmount) {
            return DomainResult.Error(
                message = "Settled amount (${receivable.settledAmount.formatted()}) cannot exceed original receivable amount (${receivable.originalAmount.formatted()})."
            )
        }
        if (receivable.currency.length != 3 || !receivable.currency.all { it.isUpperCase() }) {
            return DomainResult.Error(
                message = "Currency code must be a 3-letter uppercase string (e.g. 'BDT'). Provided: '${receivable.currency}'."
            )
        }
        if (receivable.dueDate <= 0) {
            return DomainResult.Error(message = "Due date timestamp must be positive.")
        }
        if (receivable.createdAt <= 0) {
            return DomainResult.Error(message = "Created timestamp must be positive.")
        }
        if (receivable.updatedAt < receivable.createdAt) {
            return DomainResult.Error(message = "Updated timestamp cannot precede creation timestamp.")
        }

        if (receivable.status == CustomerReceivableStatus.CANCELLED) {
            if (receivable.cancellationReason.isNullOrBlank()) {
                return DomainResult.Error(message = "Cancelled receivables must include a cancellation reason.")
            }
            if (receivable.cancelledAt == null || receivable.cancelledAt <= 0) {
                return DomainResult.Error(message = "Cancelled receivables must include a valid positive cancelledAt timestamp.")
            }
        }

        if (receivable.status == CustomerReceivableStatus.SETTLED) {
            if (!receivable.outstandingAmount.isZero()) {
                return DomainResult.Error(message = "Settled receivables must have an outstanding due balance of zero.")
            }
        }

        return DomainResult.Success(Unit)
    }

    fun validateImmutabilityOnUpdate(
        existing: CustomerReceivable,
        updated: CustomerReceivable
    ): DomainResult<Unit> {
        if (existing.status.isTerminal) {
            return DomainResult.Error(
                message = "Terminal customer receivable '${existing.receivableId}' (${existing.status}) is immutable and cannot be updated."
            )
        }
        if (existing.receivableId != updated.receivableId) {
            return DomainResult.Error(message = "Receivable ID is immutable and cannot be changed.")
        }
        if (existing.projectId != updated.projectId) {
            return DomainResult.Error(message = "Project ID is immutable and cannot be changed.")
        }
        if (existing.customerId != updated.customerId) {
            return DomainResult.Error(message = "Customer ID is immutable and cannot be changed.")
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

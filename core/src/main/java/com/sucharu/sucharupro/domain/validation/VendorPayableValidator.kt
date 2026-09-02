package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.VendorPayable

/**
 * Domain invariants and payload validator for Vendor Payables (Module 09 Step 04).
 */
object VendorPayableValidator {

    fun validateCreatePayload(
        projectId: String,
        vendorId: String,
        referenceType: FinancialReferenceType,
        referenceId: String,
        originalAmount: Money,
        currency: String,
        dueDate: Long,
        description: String,
        actorId: String
    ): DomainResult<Unit> {
        if (projectId.isBlank()) return DomainResult.Error(message = "Project ID cannot be blank.")
        if (vendorId.isBlank()) return DomainResult.Error(message = "Vendor ID cannot be blank.")
        if (referenceId.isBlank()) return DomainResult.Error(message = "Reference ID cannot be blank.")
        if (description.isBlank()) return DomainResult.Error(message = "Description cannot be blank.")
        if (actorId.isBlank()) return DomainResult.Error(message = "Actor ID cannot be blank.")

        if (!originalAmount.isPositive()) {
            return DomainResult.Error(message = "Original amount must be strictly greater than zero.")
        }

        if (currency.length != 3 || !currency.all { it.isUpperCase() }) {
            return DomainResult.Error(message = "Currency code must be a 3-letter uppercase string (e.g. 'BDT'). Provided: '$currency'")
        }

        if (dueDate <= 0) {
            return DomainResult.Error(message = "Due date must be a valid positive timestamp.")
        }

        return DomainResult.Success(Unit)
    }

    fun validateSettlement(
        payable: VendorPayable,
        settlementAmount: Money
    ): DomainResult<Unit> {
        if (!settlementAmount.isPositive()) {
            return DomainResult.Error(message = "Settlement amount must be strictly positive (> 0).")
        }

        if (payable.status.isTerminal) {
            return DomainResult.Error(
                message = "Cannot record settlement on terminal payable '${payable.payableNo}' (Status: ${payable.status.name})."
            )
        }

        if (settlementAmount > payable.outstandingAmount) {
            return DomainResult.Error(
                message = "Settlement amount (${settlementAmount.formatted()}) exceeds current outstanding liability (${payable.outstandingAmount.formatted()}). Over-settlement is strictly prohibited."
            )
        }

        return DomainResult.Success(Unit)
    }
}

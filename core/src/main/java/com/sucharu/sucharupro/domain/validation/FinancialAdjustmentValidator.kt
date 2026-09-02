package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentDirection
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentType
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType

/**
 * Domain invariants and payload validation for Financial Adjustments (Module 09 Step 07).
 */
object FinancialAdjustmentValidator {

    fun validateCreatePayload(
        projectId: String,
        adjustmentType: FinancialAdjustmentType,
        direction: FinancialAdjustmentDirection,
        amount: Money,
        currency: String,
        customerId: String?,
        vendorId: String?,
        referenceType: FinancialReferenceType,
        referenceId: String,
        reasonCode: String,
        reason: String,
        description: String,
        actorId: String
    ): DomainResult<Unit> {
        if (projectId.isBlank()) return DomainResult.Error(message = "Project ID cannot be blank.")
        if (referenceId.isBlank()) return DomainResult.Error(message = "Reference ID cannot be blank.")
        if (reasonCode.isBlank()) return DomainResult.Error(message = "Reason Code cannot be blank.")
        if (reason.isBlank()) return DomainResult.Error(message = "Reason cannot be blank.")
        if (description.isBlank()) return DomainResult.Error(message = "Description cannot be blank.")
        if (actorId.isBlank()) return DomainResult.Error(message = "Actor ID cannot be blank.")

        if (!amount.isPositive()) {
            return DomainResult.Error(message = "Adjustment amount must be strictly greater than zero.")
        }

        if (currency.length != 3 || !currency.all { it.isUpperCase() }) {
            return DomainResult.Error(message = "Currency code must be a 3-letter uppercase string (e.g. 'BDT'). Provided: '$currency'")
        }

        if (adjustmentType.isCustomerFacing) {
            if (customerId.isNullOrBlank()) {
                return DomainResult.Error(
                    message = "Customer ID is required for customer-facing adjustment '${adjustmentType.defaultLabel}'."
                )
            }
            if (!vendorId.isNullOrBlank()) {
                return DomainResult.Error(
                    message = "Vendor ID cannot be set on customer-facing adjustment '${adjustmentType.defaultLabel}'."
                )
            }
        }

        if (adjustmentType.isVendorFacing) {
            if (vendorId.isNullOrBlank()) {
                return DomainResult.Error(
                    message = "Vendor ID is required for vendor-facing adjustment '${adjustmentType.defaultLabel}'."
                )
            }
            if (!customerId.isNullOrBlank()) {
                return DomainResult.Error(
                    message = "Customer ID cannot be set on vendor-facing adjustment '${adjustmentType.defaultLabel}'."
                )
            }
        }

        return DomainResult.Success(Unit)
    }

    fun validateReceivableAdjustmentLimit(
        adjustmentAmount: Money,
        receivableOutstandingAmount: Money
    ): DomainResult<Unit> {
        if (adjustmentAmount > receivableOutstandingAmount) {
            return DomainResult.Error(
                message = "Adjustment amount (${adjustmentAmount.formatted()}) cannot exceed receivable outstanding amount (${receivableOutstandingAmount.formatted()})."
            )
        }
        return DomainResult.Success(Unit)
    }

    fun validatePayableAdjustmentLimit(
        adjustmentAmount: Money,
        payableOutstandingAmount: Money
    ): DomainResult<Unit> {
        if (adjustmentAmount > payableOutstandingAmount) {
            return DomainResult.Error(
                message = "Adjustment amount (${adjustmentAmount.formatted()}) cannot exceed payable outstanding amount (${payableOutstandingAmount.formatted()})."
            )
        }
        return DomainResult.Success(Unit)
    }
}

package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.SupplierPaymentMethod
import com.sucharu.sucharupro.domain.model.finance.VendorPayable

/**
 * Domain invariants and payload validation for Supplier Payments (Module 09 Step 05).
 */
object SupplierPaymentValidator {

    fun validateCreatePayload(
        projectId: String,
        vendorId: String,
        payableId: String,
        amount: Money,
        currency: String,
        paymentMethod: SupplierPaymentMethod,
        paymentReference: String?,
        paymentDate: Long,
        actorId: String
    ): DomainResult<Unit> {
        if (projectId.isBlank()) return DomainResult.Error(message = "Project ID cannot be blank.")
        if (vendorId.isBlank()) return DomainResult.Error(message = "Vendor ID cannot be blank.")
        if (payableId.isBlank()) return DomainResult.Error(message = "Payable ID cannot be blank.")
        if (actorId.isBlank()) return DomainResult.Error(message = "Actor ID cannot be blank.")

        if (!amount.isPositive()) {
            return DomainResult.Error(message = "Payment amount must be strictly greater than zero.")
        }

        if (currency.length != 3 || !currency.all { it.isUpperCase() }) {
            return DomainResult.Error(message = "Currency code must be a 3-letter uppercase string (e.g. 'BDT'). Provided: '$currency'")
        }

        if (paymentMethod.requiresReference && paymentReference.isNullOrBlank()) {
            return DomainResult.Error(
                message = "Payment reference (e.g. Cheque No, Bank Trx ID) is required for payment method '${paymentMethod.defaultLabel}'."
            )
        }

        if (paymentDate <= 0) {
            return DomainResult.Error(message = "Payment date must be a valid positive timestamp.")
        }

        return DomainResult.Success(Unit)
    }

    fun validatePayableCompatibility(
        payable: VendorPayable,
        projectId: String,
        vendorId: String,
        paymentAmount: Money
    ): DomainResult<Unit> {
        if (payable.projectId != projectId) {
            return DomainResult.Error(
                message = "Payable #${payable.payableNo} belongs to project '${payable.projectId}', not '$projectId'."
            )
        }

        if (payable.vendorId != vendorId) {
            return DomainResult.Error(
                message = "Payable #${payable.payableNo} is for vendor '${payable.vendorId}', but payment is for '$vendorId'."
            )
        }

        if (payable.status.isTerminal) {
            return DomainResult.Error(
                message = "Cannot make payment against terminal payable #${payable.payableNo} (Status: ${payable.status.name})."
            )
        }

        if (payable.outstandingAmount.isZero()) {
            return DomainResult.Error(
                message = "Payable #${payable.payableNo} is already fully settled."
            )
        }

        if (paymentAmount > payable.outstandingAmount) {
            return DomainResult.Error(
                message = "Payment amount (${paymentAmount.formatted()}) exceeds current outstanding liability (${payable.outstandingAmount.formatted()}) for payable #${payable.payableNo}. Overpayment is prohibited."
            )
        }

        return DomainResult.Success(Unit)
    }
}

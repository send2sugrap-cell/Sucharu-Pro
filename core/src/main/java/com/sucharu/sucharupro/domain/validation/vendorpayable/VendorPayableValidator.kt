package com.sucharu.sucharupro.domain.validation.vendorpayable

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorpayable.VendorPayable
import com.sucharu.sucharupro.domain.model.vendorpayable.VendorPayablePaymentMethod
import com.sucharu.sucharupro.domain.model.vendorpayable.VendorPayablePaymentTerms
import com.sucharu.sucharupro.domain.model.vendorpayable.VendorPayableStatus
import java.math.BigDecimal

/**
 * Domain validator enforcing business rules, financial precision, separation of duties,
 * and payment allocation integrity for Vendor Payables (Module 15 Step 02).
 */
object VendorPayableValidator {

    fun validateCreatePayload(
        tenantId: String,
        projectId: String,
        vendorId: String,
        originalAmount: BigDecimal,
        currency: String,
        issueDate: Long,
        paymentTerms: VendorPayablePaymentTerms,
        customTermDays: Int?,
        description: String,
        createdBy: String
    ): DomainResult<Unit> {
        if (tenantId.isBlank()) return DomainResult.Error(message = "Tenant ID cannot be blank.")
        if (projectId.isBlank()) return DomainResult.Error(message = "Project ID cannot be blank.")
        if (vendorId.isBlank()) return DomainResult.Error(message = "Vendor ID cannot be blank.")
        if (createdBy.isBlank()) return DomainResult.Error(message = "Created by cannot be blank.")
        if (description.isBlank()) return DomainResult.Error(message = "Payable description cannot be blank.")

        if (originalAmount <= BigDecimal.ZERO) {
            return DomainResult.Error(message = "Payable amount must be strictly greater than zero.")
        }
        if (originalAmount.scale() > 4) {
            return DomainResult.Error(message = "Payable amount cannot have more than 4 decimal places of precision.")
        }

        if (currency.length != 3 || !currency.all { it.isUpperCase() }) {
            return DomainResult.Error(message = "Currency code must be a 3-letter uppercase ISO code (e.g. 'BDT'). Provided: '$currency'")
        }

        if (issueDate <= 0L) {
            return DomainResult.Error(message = "Issue date must be a valid positive timestamp.")
        }

        if (paymentTerms == VendorPayablePaymentTerms.CUSTOM) {
            if (customTermDays == null || customTermDays < 0) {
                return DomainResult.Error(message = "Custom payment terms require a non-negative number of days.")
            }
        }

        return DomainResult.Success(Unit)
    }

    fun validateUpdateDraft(payable: VendorPayable): DomainResult<Unit> {
        if (!payable.status.canBeEdited) {
            return DomainResult.Error(message = "Only DRAFT or REJECTED payables can be edited. Current status: ${payable.status.name}")
        }
        return DomainResult.Success(Unit)
    }

    fun validateSubmit(payable: VendorPayable): DomainResult<Unit> {
        if (!payable.status.canBeSubmitted) {
            return DomainResult.Error(message = "Only DRAFT or REJECTED payables can be submitted for approval. Current status: ${payable.status.name}")
        }
        return DomainResult.Success(Unit)
    }

    fun validateApprove(
        payable: VendorPayable,
        actorId: String,
        isSuperAdmin: Boolean = false
    ): DomainResult<Unit> {
        if (!payable.status.canBeApproved) {
            return DomainResult.Error(message = "Only SUBMITTED payables can be approved. Current status: ${payable.status.name}")
        }
        // Separation of Duties
        if (!isSuperAdmin && payable.createdBy == actorId) {
            return DomainResult.Error(message = "Separation of duties violation: The payable creator ($actorId) cannot approve their own payable.")
        }
        return DomainResult.Success(Unit)
    }

    fun validateReject(
        payable: VendorPayable,
        reason: String
    ): DomainResult<Unit> {
        if (payable.status != VendorPayableStatus.SUBMITTED) {
            return DomainResult.Error(message = "Only SUBMITTED payables can be rejected. Current status: ${payable.status.name}")
        }
        if (reason.isBlank()) {
            return DomainResult.Error(message = "A non-blank reason is required to reject a payable.")
        }
        return DomainResult.Success(Unit)
    }

    fun validateCancel(
        payable: VendorPayable,
        reason: String
    ): DomainResult<Unit> {
        if (payable.status.isTerminal) {
            return DomainResult.Error(message = "Cannot cancel a payable in terminal status: ${payable.status.name}")
        }
        if (payable.paidAmount > BigDecimal.ZERO) {
            return DomainResult.Error(message = "Cannot cancel a payable with recorded payments. Use VOID if reversal is authorized.")
        }
        if (reason.isBlank()) {
            return DomainResult.Error(message = "A non-blank reason is required to cancel a payable.")
        }
        return DomainResult.Success(Unit)
    }

    fun validateVoid(
        payable: VendorPayable,
        reason: String
    ): DomainResult<Unit> {
        if (payable.status != VendorPayableStatus.APPROVED && payable.status != VendorPayableStatus.PARTIALLY_PAID) {
            return DomainResult.Error(message = "Only APPROVED or PARTIALLY_PAID payables can be voided. Current status: ${payable.status.name}")
        }
        if (reason.isBlank()) {
            return DomainResult.Error(message = "A non-blank reason is required to void a payable.")
        }
        return DomainResult.Success(Unit)
    }

    fun validatePaymentAllocation(
        payable: VendorPayable,
        amount: BigDecimal,
        paymentMethod: VendorPayablePaymentMethod,
        paymentReference: String?,
        paymentDate: Long
    ): DomainResult<Unit> {
        if (!payable.status.canReceivePayment) {
            return DomainResult.Error(message = "Payments can only be allocated to APPROVED or PARTIALLY_PAID payables. Current status: ${payable.status.name}")
        }
        if (amount <= BigDecimal.ZERO) {
            return DomainResult.Error(message = "Payment allocation amount must be strictly greater than zero.")
        }
        if (amount.scale() > 4) {
            return DomainResult.Error(message = "Payment allocation amount cannot have more than 4 decimal places of precision.")
        }
        if (amount > payable.outstandingAmount) {
            return DomainResult.Error(
                message = "Payment allocation amount (${amount.toPlainString()}) exceeds outstanding payable liability (${payable.outstandingAmount.toPlainString()}). Over-allocation is strictly prohibited."
            )
        }
        if (paymentMethod.requiresReference && paymentReference.isNullOrBlank()) {
            return DomainResult.Error(message = "Payment reference is required for payment method '${paymentMethod.name}'.")
        }
        if (paymentDate <= 0L) {
            return DomainResult.Error(message = "Payment date must be a valid positive timestamp.")
        }
        return DomainResult.Success(Unit)
    }

    fun validateStatusTransition(
        current: VendorPayableStatus,
        target: VendorPayableStatus
    ): DomainResult<Unit> {
        if (!current.canTransitionTo(target)) {
            return DomainResult.Error(message = "Invalid status transition from ${current.name} to ${target.name}.")
        }
        return DomainResult.Success(Unit)
    }
}

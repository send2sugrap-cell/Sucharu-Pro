package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.finance.CustomerPayment
import com.sucharu.sucharupro.domain.model.finance.CustomerPaymentReceipt
import com.sucharu.sucharupro.domain.model.finance.CustomerPaymentStatus

/**
 * Validates domain invariants for Customer Payment and Receipt records (Module 09 Step 03).
 */
object CustomerPaymentValidator {

    fun validatePayment(
        payment: CustomerPayment,
        expectedProjectId: String? = null
    ): DomainResult<Unit> {
        if (payment.paymentId.isBlank()) return DomainResult.Error(message = "Payment ID cannot be blank.")
        if (payment.paymentNo.isBlank()) return DomainResult.Error(message = "Payment Number cannot be blank.")
        if (payment.projectId.isBlank()) return DomainResult.Error(message = "Project ID cannot be blank.")
        if (expectedProjectId != null && payment.projectId != expectedProjectId) {
            return DomainResult.Error(
                message = "Project ID mismatch. Expected '$expectedProjectId' but got '${payment.projectId}'."
            )
        }
        if (payment.customerId.isBlank()) return DomainResult.Error(message = "Customer ID cannot be blank.")
        if (payment.receivableId.isBlank()) return DomainResult.Error(message = "Receivable ID cannot be blank.")
        if (payment.createdBy.isBlank()) return DomainResult.Error(message = "Created By cannot be blank.")
        if (!payment.amount.isPositive()) return DomainResult.Error(message = "Payment amount must be strictly positive (> 0).")
        if (payment.currency.length != 3 || !payment.currency.all { it.isUpperCase() }) {
            return DomainResult.Error(
                message = "Currency code must be a 3-letter uppercase string (e.g. 'BDT'). Provided: '${payment.currency}'."
            )
        }
        if (payment.paymentDate <= 0) return DomainResult.Error(message = "Payment date timestamp must be positive.")
        if (payment.createdAt <= 0) return DomainResult.Error(message = "Created timestamp must be positive.")
        if (payment.updatedAt < payment.createdAt) return DomainResult.Error(message = "Updated timestamp cannot precede creation timestamp.")

        if (payment.paymentMethod.requiresReference && payment.paymentReference.isNullOrBlank()) {
            return DomainResult.Error(
                message = "Payment method '${payment.paymentMethod.defaultLabel}' requires a non-blank payment reference (e.g. Bank Txn ID, Cheque No, Mobile Txn ID)."
            )
        }

        if (payment.status == CustomerPaymentStatus.POSTED) {
            if (payment.postedBy.isNullOrBlank()) return DomainResult.Error(message = "Posted payment must record postedBy actor.")
            if (payment.postedAt == null || payment.postedAt <= 0) return DomainResult.Error(message = "Posted payment must record a positive postedAt timestamp.")
            if (payment.receiptId.isNullOrBlank()) return DomainResult.Error(message = "Posted payment must link to an issued receipt.")
            if (payment.financialTransactionId.isNullOrBlank()) return DomainResult.Error(message = "Posted payment must link to a financial transaction.")
        }

        if (payment.status == CustomerPaymentStatus.CANCELLED) {
            if (payment.cancelledBy.isNullOrBlank()) return DomainResult.Error(message = "Cancelled payment must record cancelledBy actor.")
            if (payment.cancelledAt == null || payment.cancelledAt <= 0) return DomainResult.Error(message = "Cancelled payment must record a positive cancelledAt timestamp.")
            if (payment.cancellationReason.isNullOrBlank()) return DomainResult.Error(message = "Cancelled payment must record a cancellation reason.")
        }

        if (payment.status == CustomerPaymentStatus.REJECTED) {
            if (payment.rejectedBy.isNullOrBlank()) return DomainResult.Error(message = "Rejected payment must record rejectedBy actor.")
        }

        return DomainResult.Success(Unit)
    }

    fun validateReceipt(
        receipt: CustomerPaymentReceipt,
        expectedProjectId: String? = null
    ): DomainResult<Unit> {
        if (receipt.receiptId.isBlank()) return DomainResult.Error(message = "Receipt ID cannot be blank.")
        if (receipt.receiptNo.isBlank()) return DomainResult.Error(message = "Receipt Number cannot be blank.")
        if (receipt.projectId.isBlank()) return DomainResult.Error(message = "Project ID cannot be blank.")
        if (expectedProjectId != null && receipt.projectId != expectedProjectId) {
            return DomainResult.Error(
                message = "Project ID mismatch. Expected '$expectedProjectId' but got '${receipt.projectId}'."
            )
        }
        if (receipt.paymentId.isBlank()) return DomainResult.Error(message = "Payment ID cannot be blank.")
        if (receipt.customerId.isBlank()) return DomainResult.Error(message = "Customer ID cannot be blank.")
        if (receipt.receivableId.isBlank()) return DomainResult.Error(message = "Receivable ID cannot be blank.")
        if (receipt.issuedBy.isBlank()) return DomainResult.Error(message = "Issued By cannot be blank.")
        if (!receipt.amount.isPositive()) return DomainResult.Error(message = "Receipt amount must be strictly positive (> 0).")
        if (receipt.currency.length != 3 || !receipt.currency.all { it.isUpperCase() }) {
            return DomainResult.Error(
                message = "Currency code must be a 3-letter uppercase string (e.g. 'BDT'). Provided: '${receipt.currency}'."
            )
        }
        if (receipt.issuedAt <= 0) return DomainResult.Error(message = "Issued timestamp must be positive.")

        return DomainResult.Success(Unit)
    }

    fun validateImmutabilityOnUpdate(
        existing: CustomerPayment,
        updated: CustomerPayment
    ): DomainResult<Unit> {
        if (existing.status.isTerminal) {
            return DomainResult.Error(
                message = "Terminal customer payment '${existing.paymentNo}' (${existing.status}) is immutable and cannot be updated."
            )
        }
        if (existing.paymentId != updated.paymentId) return DomainResult.Error(message = "Payment ID is immutable.")
        if (existing.projectId != updated.projectId) return DomainResult.Error(message = "Project ID is immutable.")
        if (existing.customerId != updated.customerId) return DomainResult.Error(message = "Customer ID is immutable.")
        if (existing.receivableId != updated.receivableId) return DomainResult.Error(message = "Receivable ID is immutable.")
        if (existing.createdBy != updated.createdBy) return DomainResult.Error(message = "Original creator is immutable.")
        if (existing.createdAt != updated.createdAt) return DomainResult.Error(message = "Creation timestamp is immutable.")

        return DomainResult.Success(Unit)
    }
}

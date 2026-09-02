package com.sucharu.sucharupro.domain.validation.customercollection

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customercollection.*
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoice
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import java.math.BigDecimal

object CustomerCollectionValidator {

    fun validateCustomer(customer: Customer?): DomainResult<Unit> {
        if (customer == null) {
            return DomainResult.Error(IllegalArgumentException("Customer does not exist."))
        }
        if (customer.status != CustomerStatusType.ACTIVE) {
            return DomainResult.Error(IllegalStateException("Customer is ${customer.status}. Only ACTIVE customers can undergo collection actions."))
        }
        return DomainResult.Success(Unit)
    }

    fun validateInvoice(
        invoice: CustomerInvoice?,
        expectedCustomerId: String
    ): DomainResult<Unit> {
        if (invoice == null) {
            return DomainResult.Error(IllegalArgumentException("Referenced invoice does not exist."))
        }
        if (invoice.customerId != expectedCustomerId) {
            return DomainResult.Error(IllegalArgumentException("Invoice '${invoice.invoiceId}' does not belong to customer '$expectedCustomerId'."))
        }
        if (invoice.status !in setOf(CustomerInvoiceStatus.ISSUED, CustomerInvoiceStatus.PARTIALLY_PAID)) {
            return DomainResult.Error(IllegalStateException("Invoice '${invoice.invoiceId}' is in status '${invoice.status}'. Collection action can only be linked to open invoices."))
        }
        if (invoice.dueAmount <= BigDecimal.ZERO) {
            return DomainResult.Error(IllegalStateException("Invoice '${invoice.invoiceId}' has no outstanding due amount."))
        }
        return DomainResult.Success(Unit)
    }

    fun validateCreateAction(
        customer: Customer?,
        invoice: CustomerInvoice?,
        scheduledAt: Long,
        assignedUserId: String?,
        actorId: String
    ): DomainResult<Unit> {
        val custCheck = validateCustomer(customer)
        if (custCheck is DomainResult.Error) return custCheck

        if (invoice != null) {
            val invCheck = validateInvoice(invoice, customer!!.customerId)
            if (invCheck is DomainResult.Error) return invCheck
        }

        if (scheduledAt <= 0) {
            return DomainResult.Error(IllegalArgumentException("Scheduled date/time must be a valid positive timestamp."))
        }
        if (actorId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("Actor ID is required."))
        }
        return DomainResult.Success(Unit)
    }

    fun validateRescheduleAction(
        action: CustomerCollectionAction?,
        newScheduledAt: Long,
        actorId: String
    ): DomainResult<Unit> {
        if (action == null) {
            return DomainResult.Error(IllegalArgumentException("Collection action does not exist."))
        }
        if (action.status == CollectionActionStatus.COMPLETED) {
            return DomainResult.Error(IllegalStateException("Cannot reschedule a completed collection action."))
        }
        if (action.status == CollectionActionStatus.CANCELLED) {
            return DomainResult.Error(IllegalStateException("Cannot reschedule a cancelled collection action."))
        }
        if (newScheduledAt <= 0) {
            return DomainResult.Error(IllegalArgumentException("New scheduled timestamp must be valid."))
        }
        if (actorId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("Actor ID is required."))
        }
        return DomainResult.Success(Unit)
    }

    fun validateCompleteAction(
        action: CustomerCollectionAction?,
        outcome: CollectionOutcomeType?,
        actorId: String
    ): DomainResult<Unit> {
        if (action == null) {
            return DomainResult.Error(IllegalArgumentException("Collection action does not exist."))
        }
        if (action.status == CollectionActionStatus.COMPLETED) {
            return DomainResult.Error(IllegalStateException("Collection action is already completed."))
        }
        if (action.status == CollectionActionStatus.CANCELLED) {
            return DomainResult.Error(IllegalStateException("Cannot complete a cancelled collection action."))
        }
        if (outcome == null) {
            return DomainResult.Error(IllegalArgumentException("Outcome type is mandatory when completing a collection action."))
        }
        if (actorId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("Actor ID is required."))
        }
        return DomainResult.Success(Unit)
    }

    fun validateCancelAction(
        action: CustomerCollectionAction?,
        reason: String?,
        actorId: String
    ): DomainResult<Unit> {
        if (action == null) {
            return DomainResult.Error(IllegalArgumentException("Collection action does not exist."))
        }
        if (action.status == CollectionActionStatus.COMPLETED) {
            return DomainResult.Error(IllegalStateException("Cannot cancel a completed collection action."))
        }
        if (action.status == CollectionActionStatus.CANCELLED) {
            return DomainResult.Error(IllegalStateException("Collection action is already cancelled."))
        }
        if (reason.isNullOrBlank()) {
            return DomainResult.Error(IllegalArgumentException("Cancellation reason is mandatory."))
        }
        if (actorId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("Actor ID is required."))
        }
        return DomainResult.Success(Unit)
    }

    fun validatePaymentPromise(
        customer: Customer?,
        invoice: CustomerInvoice?,
        promisedAmount: BigDecimal,
        promisedDate: Long,
        totalOutstanding: BigDecimal,
        actorId: String
    ): DomainResult<Unit> {
        val custCheck = validateCustomer(customer)
        if (custCheck is DomainResult.Error) return custCheck

        if (invoice != null) {
            val invCheck = validateInvoice(invoice, customer!!.customerId)
            if (invCheck is DomainResult.Error) return invCheck
        }

        if (promisedAmount <= BigDecimal.ZERO) {
            return DomainResult.Error(IllegalArgumentException("Promised amount must be positive. Provided: $promisedAmount"))
        }

        val maxAllowed = invoice?.dueAmount ?: totalOutstanding
        if (maxAllowed > BigDecimal.ZERO && promisedAmount > maxAllowed) {
            return DomainResult.Error(IllegalArgumentException("Promised amount ($promisedAmount) cannot exceed outstanding balance ($maxAllowed)."))
        }

        if (promisedDate <= 0) {
            return DomainResult.Error(IllegalArgumentException("Promised date must be a valid positive timestamp."))
        }
        if (actorId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("Actor ID is required."))
        }
        return DomainResult.Success(Unit)
    }
}

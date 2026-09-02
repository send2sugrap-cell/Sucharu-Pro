package com.sucharu.sucharupro.domain.validation.customercreditcontrol

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customercreditcontrol.CustomerPaymentTermsType
import java.math.BigDecimal

/**
 * Domain Validator for Customer Credit Limits, Payment Terms & Risk Control (Module 14 Step 07).
 */
object CustomerCreditControlValidator {

    fun validateProfileCreationOrUpdate(
        customer: Customer?,
        creditLimit: BigDecimal,
        paymentTermsType: CustomerPaymentTermsType,
        creditDays: Int
    ): DomainResult<Unit> {
        if (customer == null) {
            return DomainResult.Error(IllegalArgumentException("Customer not found."))
        }
        if (creditLimit < BigDecimal.ZERO) {
            return DomainResult.Error(IllegalArgumentException("Credit limit cannot be negative (provided: $creditLimit)."))
        }
        if (creditDays < 0) {
            return DomainResult.Error(IllegalArgumentException("Credit days cannot be negative (provided: $creditDays)."))
        }
        if (paymentTermsType == CustomerPaymentTermsType.PREPAID && creditLimit > BigDecimal.ZERO) {
            return DomainResult.Error(IllegalArgumentException("PREPAID payment terms cannot have a positive credit limit."))
        }
        if (paymentTermsType == CustomerPaymentTermsType.DUE_ON_RECEIPT && creditDays > 0) {
            return DomainResult.Error(IllegalArgumentException("DUE_ON_RECEIPT payment terms must have 0 credit days."))
        }
        return DomainResult.Success(Unit)
    }

    fun validateFinancialHold(
        customer: Customer?,
        reason: String,
        actorId: String
    ): DomainResult<Unit> {
        if (customer == null) {
            return DomainResult.Error(IllegalArgumentException("Customer not found."))
        }
        if (reason.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("Hold reason is mandatory."))
        }
        if (actorId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("Actor ID is mandatory."))
        }
        return DomainResult.Success(Unit)
    }

    fun validateHoldRelease(
        customer: Customer?,
        currentlyOnHold: Boolean,
        reason: String,
        actorId: String
    ): DomainResult<Unit> {
        if (customer == null) {
            return DomainResult.Error(IllegalArgumentException("Customer not found."))
        }
        if (!currentlyOnHold) {
            return DomainResult.Error(IllegalStateException("Customer is not currently on financial hold."))
        }
        if (reason.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("Release reason is mandatory."))
        }
        if (actorId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("Actor ID is mandatory."))
        }
        return DomainResult.Success(Unit)
    }

    fun validateCreditCheck(
        customer: Customer?,
        requestedExposure: BigDecimal
    ): DomainResult<Unit> {
        if (customer == null) {
            return DomainResult.Error(IllegalArgumentException("Customer not found."))
        }
        if (requestedExposure < BigDecimal.ZERO) {
            return DomainResult.Error(IllegalArgumentException("Requested exposure cannot be negative (provided: $requestedExposure)."))
        }
        return DomainResult.Success(Unit)
    }
}

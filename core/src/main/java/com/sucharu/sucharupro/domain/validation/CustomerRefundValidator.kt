package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.CustomerRefundMethod

/**
 * Domain invariants and payload validation for Customer Refunds (Module 09 Step 07).
 */
object CustomerRefundValidator {

    fun validateCreatePayload(
        projectId: String,
        customerId: String,
        amount: Money,
        currency: String,
        refundMethod: CustomerRefundMethod,
        refundReference: String?,
        reason: String,
        actorId: String
    ): DomainResult<Unit> {
        if (projectId.isBlank()) return DomainResult.Error(message = "Project ID cannot be blank.")
        if (customerId.isBlank()) return DomainResult.Error(message = "Customer ID cannot be blank.")
        if (reason.isBlank()) return DomainResult.Error(message = "Refund reason cannot be blank.")
        if (actorId.isBlank()) return DomainResult.Error(message = "Actor ID cannot be blank.")

        if (!amount.isPositive()) {
            return DomainResult.Error(message = "Refund amount must be strictly greater than zero.")
        }

        if (currency.length != 3 || !currency.all { it.isUpperCase() }) {
            return DomainResult.Error(message = "Currency code must be a 3-letter uppercase string (e.g. 'BDT'). Provided: '$currency'")
        }

        if (refundMethod.requiresReference && refundReference.isNullOrBlank()) {
            return DomainResult.Error(
                message = "Payment/Refund reference is required for payment method '${refundMethod.defaultLabel}'."
            )
        }

        return DomainResult.Success(Unit)
    }
}

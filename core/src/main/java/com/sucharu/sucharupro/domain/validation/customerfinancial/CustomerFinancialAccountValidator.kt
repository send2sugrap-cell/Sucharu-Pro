package com.sucharu.sucharupro.domain.validation.customerfinancial

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus

/**
 * Domain validator for Customer Financial Account operations (Module 14 Step 01).
 */
object CustomerFinancialAccountValidator {

    fun validateCreation(
        tenantId: String,
        projectId: String,
        customerId: String,
        currency: String
    ): DomainResult<Unit> {
        if (tenantId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("Tenant ID cannot be blank"))
        }
        if (projectId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("Project ID cannot be blank"))
        }
        if (customerId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("Customer ID cannot be blank"))
        }
        if (currency.isBlank() || currency.length !in 3..4) {
            return DomainResult.Error(IllegalArgumentException("Invalid currency code: '$currency'"))
        }
        return DomainResult.Success(Unit)
    }

    fun validateStatusTransition(
        account: CustomerFinancialAccount,
        targetStatus: CustomerFinancialAccountStatus,
        reason: String?
    ): DomainResult<Unit> {
        if (!account.status.canTransitionTo(targetStatus)) {
            return DomainResult.Error(
                IllegalStateException("Cannot transition Customer Financial Account from ${account.status} to $targetStatus")
            )
        }
        if (targetStatus == CustomerFinancialAccountStatus.SUSPENDED && reason.isNullOrBlank()) {
            return DomainResult.Error(
                IllegalArgumentException("A reason must be provided when suspending a Customer Financial Account")
            )
        }
        if (targetStatus == CustomerFinancialAccountStatus.CLOSED && reason.isNullOrBlank()) {
            return DomainResult.Error(
                IllegalArgumentException("A reason must be provided when closing a Customer Financial Account")
            )
        }
        return DomainResult.Success(Unit)
    }
}

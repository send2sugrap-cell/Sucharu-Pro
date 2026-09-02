package com.sucharu.sucharupro.domain.validation.customerledger

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus

/**
 * Validator for Customer Ledger, Statement, and Reconciliation operations (Module 14 Step 05).
 */
object CustomerLedgerValidator {

    fun validateStatementQuery(
        tenantId: String,
        projectId: String,
        customerId: String,
        fromDate: Long?,
        toDate: Long?,
        account: CustomerFinancialAccount?
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
        if (fromDate != null && toDate != null && fromDate > toDate) {
            return DomainResult.Error(IllegalArgumentException("fromDate ($fromDate) cannot be after toDate ($toDate)"))
        }
        if (account == null) {
            return DomainResult.Error(IllegalArgumentException("Customer financial account not found for customer '$customerId'"))
        }
        return DomainResult.Success(Unit)
    }

    fun validatePagination(
        limit: Int,
        offset: Int
    ): DomainResult<Unit> {
        if (limit <= 0 || limit > 500) {
            return DomainResult.Error(IllegalArgumentException("Limit must be between 1 and 500"))
        }
        if (offset < 0) {
            return DomainResult.Error(IllegalArgumentException("Offset cannot be negative"))
        }
        return DomainResult.Success(Unit)
    }

    fun validateReconciliation(
        tenantId: String,
        projectId: String,
        customerId: String,
        account: CustomerFinancialAccount?
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
        if (account == null) {
            return DomainResult.Error(IllegalArgumentException("Customer financial account not found for customer '$customerId'"))
        }
        return DomainResult.Success(Unit)
    }
}

package com.sucharu.sucharupro.domain.validation.customerinvoice

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoice
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceLine
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import java.math.BigDecimal

/**
 * Domain validator for Customer Invoice operations (Module 14 Step 02).
 */
object CustomerInvoiceValidator {

    fun validateDraftCreation(
        tenantId: String,
        projectId: String,
        customerId: String,
        financialAccountId: String,
        currency: String,
        lines: List<CustomerInvoiceLine>,
        account: CustomerFinancialAccount
    ): DomainResult<Unit> {
        if (tenantId.isBlank()) return DomainResult.Error(IllegalArgumentException("Tenant ID cannot be blank"))
        if (projectId.isBlank()) return DomainResult.Error(IllegalArgumentException("Project ID cannot be blank"))
        if (customerId.isBlank()) return DomainResult.Error(IllegalArgumentException("Customer ID cannot be blank"))
        if (financialAccountId.isBlank()) return DomainResult.Error(IllegalArgumentException("Financial Account ID cannot be blank"))
        if (currency.isBlank() || currency.length !in 3..4) {
            return DomainResult.Error(IllegalArgumentException("Invalid currency code: '$currency'"))
        }

        // Account ownership & active state validation
        if (account.customerId != customerId || account.tenantId != tenantId || account.projectId != projectId) {
            return DomainResult.Error(
                IllegalStateException("CustomerFinancialAccount does not match the invoice customer/tenant/project boundary")
            )
        }
        if (!account.status.canTransact) {
            return DomainResult.Error(
                IllegalStateException("CustomerFinancialAccount '${account.financialAccountId}' is ${account.status} and cannot be invoiced")
            )
        }

        // Line item validation
        if (lines.isEmpty()) {
            return DomainResult.Error(IllegalArgumentException("Invoice must contain at least one line item"))
        }
        for ((idx, line) in lines.withIndex()) {
            if (line.description.isBlank()) {
                return DomainResult.Error(IllegalArgumentException("Line item ${idx + 1} description cannot be blank"))
            }
            if (line.quantity <= BigDecimal.ZERO) {
                return DomainResult.Error(IllegalArgumentException("Line item ${idx + 1} quantity must be strictly positive"))
            }
            if (line.unitPrice < BigDecimal.ZERO) {
                return DomainResult.Error(IllegalArgumentException("Line item ${idx + 1} unit price cannot be negative"))
            }
            if (line.discount < BigDecimal.ZERO) {
                return DomainResult.Error(IllegalArgumentException("Line item ${idx + 1} discount cannot be negative"))
            }
            if (line.tax < BigDecimal.ZERO) {
                return DomainResult.Error(IllegalArgumentException("Line item ${idx + 1} tax cannot be negative"))
            }
        }
        return DomainResult.Success(Unit)
    }

    fun validateStatusTransition(
        invoice: CustomerInvoice,
        targetStatus: CustomerInvoiceStatus,
        reason: String?
    ): DomainResult<Unit> {
        if (!invoice.status.canTransitionTo(targetStatus)) {
            return DomainResult.Error(
                IllegalStateException("Cannot transition Customer Invoice from ${invoice.status} to $targetStatus")
            )
        }
        if (targetStatus == CustomerInvoiceStatus.CANCELLED && reason.isNullOrBlank()) {
            return DomainResult.Error(
                IllegalArgumentException("A reason must be provided when cancelling a Customer Invoice")
            )
        }
        if (targetStatus == CustomerInvoiceStatus.VOID && reason.isNullOrBlank()) {
            return DomainResult.Error(
                IllegalArgumentException("A reason must be provided when voiding a Customer Invoice")
            )
        }
        return DomainResult.Success(Unit)
    }
}

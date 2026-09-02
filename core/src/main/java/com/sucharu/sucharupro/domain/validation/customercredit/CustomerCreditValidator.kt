package com.sucharu.sucharupro.domain.validation.customercredit

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoice
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdjustmentType
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdvance
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdvanceStatus
import com.sucharu.sucharupro.domain.model.customercredit.CustomerRefund
import com.sucharu.sucharupro.domain.model.customercredit.CustomerRefundStatus
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPayment
import java.math.BigDecimal

/**
 * Domain validator for Customer Advances, Credits, Adjustments, and Refunds (Module 14 Step 04).
 */
object CustomerCreditValidator {

    fun validateAdvanceRecording(
        tenantId: String,
        projectId: String,
        customerId: String,
        financialAccountId: String,
        amount: BigDecimal,
        currency: String,
        account: CustomerFinancialAccount
    ): DomainResult<Unit> {
        if (tenantId.isBlank()) return DomainResult.Error(IllegalArgumentException("Tenant ID cannot be blank"))
        if (projectId.isBlank()) return DomainResult.Error(IllegalArgumentException("Project ID cannot be blank"))
        if (customerId.isBlank()) return DomainResult.Error(IllegalArgumentException("Customer ID cannot be blank"))
        if (financialAccountId.isBlank()) return DomainResult.Error(IllegalArgumentException("Financial Account ID cannot be blank"))
        if (amount <= BigDecimal.ZERO) {
            return DomainResult.Error(IllegalArgumentException("Advance amount must be strictly greater than zero. Received: $amount"))
        }
        if (currency.isBlank() || currency.length !in 3..4) {
            return DomainResult.Error(IllegalArgumentException("Invalid currency code: '$currency'"))
        }

        if (account.customerId != customerId || account.tenantId != tenantId || account.projectId != projectId) {
            return DomainResult.Error(
                IllegalStateException("CustomerFinancialAccount does not match the advance customer/tenant/project boundary")
            )
        }
        if (!account.status.canTransact) {
            return DomainResult.Error(
                IllegalStateException("CustomerFinancialAccount '${account.financialAccountId}' is ${account.status} and cannot receive advances")
            )
        }
        return DomainResult.Success(Unit)
    }

    fun validateCreditAllocation(
        tenantId: String,
        projectId: String,
        customerId: String,
        advance: CustomerAdvance?,
        availableCredit: BigDecimal,
        invoice: CustomerInvoice,
        allocationAmount: BigDecimal
    ): DomainResult<Unit> {
        if (allocationAmount <= BigDecimal.ZERO) {
            return DomainResult.Error(IllegalArgumentException("Allocation amount must be strictly greater than zero. Received: $allocationAmount"))
        }
        if (invoice.customerId != customerId || invoice.tenantId != tenantId || invoice.projectId != projectId) {
            return DomainResult.Error(
                IllegalStateException("Invoice '${invoice.invoiceId}' does not belong to customer '$customerId' or project/tenant scope")
            )
        }
        if (invoice.status !in setOf(CustomerInvoiceStatus.ISSUED, CustomerInvoiceStatus.PARTIALLY_PAID)) {
            return DomainResult.Error(
                IllegalStateException("Cannot allocate credit to invoice '${invoice.invoiceId}' in status ${invoice.status}. Invoice must be ISSUED or PARTIALLY_PAID.")
            )
        }
        if (allocationAmount > invoice.dueAmount) {
            return DomainResult.Error(
                IllegalArgumentException("Allocation amount ($allocationAmount) exceeds invoice due amount (${invoice.dueAmount}).")
            )
        }

        if (advance != null) {
            if (advance.customerId != customerId || advance.tenantId != tenantId || advance.projectId != projectId) {
                return DomainResult.Error(
                    IllegalStateException("Advance '${advance.advanceId}' does not match customer/tenant/project boundary")
                )
            }
            if (advance.status !in setOf(CustomerAdvanceStatus.RECORDED, CustomerAdvanceStatus.AVAILABLE, CustomerAdvanceStatus.ALLOCATED)) {
                return DomainResult.Error(
                    IllegalStateException("Advance '${advance.advanceId}' is in status ${advance.status} and cannot be allocated.")
                )
            }
            if (allocationAmount > advance.availableAmount) {
                return DomainResult.Error(
                    IllegalArgumentException("Allocation amount ($allocationAmount) exceeds advance available amount (${advance.availableAmount}).")
                )
            }
        } else {
            if (allocationAmount > availableCredit) {
                return DomainResult.Error(
                    IllegalArgumentException("Allocation amount ($allocationAmount) exceeds customer total available credit ($availableCredit).")
                )
            }
        }

        return DomainResult.Success(Unit)
    }

    fun validateAdjustment(
        tenantId: String,
        projectId: String,
        customerId: String,
        financialAccountId: String,
        amount: BigDecimal,
        type: CustomerAdjustmentType,
        reason: String,
        account: CustomerFinancialAccount,
        currentAvailableCredit: BigDecimal
    ): DomainResult<Unit> {
        if (amount <= BigDecimal.ZERO) {
            return DomainResult.Error(IllegalArgumentException("Adjustment amount must be strictly greater than zero. Received: $amount"))
        }
        if (reason.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("A mandatory reason must be provided for customer account adjustments."))
        }
        if (account.customerId != customerId || account.tenantId != tenantId || account.projectId != projectId) {
            return DomainResult.Error(
                IllegalStateException("CustomerFinancialAccount does not match customer/tenant/project scope")
            )
        }
        if (!account.status.canTransact) {
            return DomainResult.Error(
                IllegalStateException("CustomerFinancialAccount '${account.financialAccountId}' is ${account.status} and cannot be adjusted")
            )
        }
        if (type == CustomerAdjustmentType.DEBIT && amount > currentAvailableCredit) {
            return DomainResult.Error(
                IllegalArgumentException("Debit adjustment ($amount) exceeds available credit balance ($currentAvailableCredit). Negative credit is not permitted.")
            )
        }
        return DomainResult.Success(Unit)
    }

    fun validateRefundRequest(
        tenantId: String,
        projectId: String,
        customerId: String,
        amount: BigDecimal,
        reason: String,
        payment: CustomerPayment?,
        advance: CustomerAdvance?,
        totalAvailableCredit: BigDecimal
    ): DomainResult<Unit> {
        if (amount <= BigDecimal.ZERO) {
            return DomainResult.Error(IllegalArgumentException("Refund amount must be strictly greater than zero. Received: $amount"))
        }
        if (reason.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("A mandatory reason must be provided for customer refund requests."))
        }

        if (payment != null) {
            if (payment.customerId != customerId || payment.tenantId != tenantId || payment.projectId != projectId) {
                return DomainResult.Error(
                    IllegalStateException("Payment '${payment.paymentId}' does not belong to customer '$customerId' or project/tenant scope")
                )
            }
            if (payment.status.isCancelled) {
                return DomainResult.Error(
                    IllegalStateException("Cannot request refund against cancelled payment '${payment.paymentId}'.")
                )
            }
            if (amount > payment.amount) {
                return DomainResult.Error(
                    IllegalArgumentException("Refund amount ($amount) exceeds source payment amount (${payment.amount}).")
                )
            }
        } else if (advance != null) {
            if (advance.customerId != customerId || advance.tenantId != tenantId || advance.projectId != projectId) {
                return DomainResult.Error(
                    IllegalStateException("Advance '${advance.advanceId}' does not belong to customer '$customerId' or project/tenant scope")
                )
            }
            if (advance.status.isCancelled) {
                return DomainResult.Error(
                    IllegalStateException("Cannot request refund against cancelled advance '${advance.advanceId}'.")
                )
            }
            if (amount > advance.availableAmount) {
                return DomainResult.Error(
                    IllegalArgumentException("Refund amount ($amount) exceeds advance available amount (${advance.availableAmount}).")
                )
            }
        } else {
            if (amount > totalAvailableCredit) {
                return DomainResult.Error(
                    IllegalArgumentException("Refund amount ($amount) exceeds customer available credit ($totalAvailableCredit).")
                )
            }
        }
        return DomainResult.Success(Unit)
    }

    fun validateRefundTransition(
        refund: CustomerRefund,
        targetStatus: CustomerRefundStatus,
        reason: String?
    ): DomainResult<Unit> {
        if (!refund.status.canTransitionTo(targetStatus)) {
            return DomainResult.Error(
                IllegalStateException("Cannot transition Customer Refund from ${refund.status} to $targetStatus")
            )
        }
        if (targetStatus in setOf(CustomerRefundStatus.REJECTED, CustomerRefundStatus.CANCELLED) && reason.isNullOrBlank()) {
            return DomainResult.Error(
                IllegalArgumentException("A reason must be provided when rejecting or cancelling a Customer Refund")
            )
        }
        return DomainResult.Success(Unit)
    }
}

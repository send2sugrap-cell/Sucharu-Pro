package com.sucharu.sucharupro.domain.validation.customerpayment

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoice
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPayment
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentMethod
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentStatus
import java.math.BigDecimal

/**
 * Domain validator for Customer Payment operations (Module 14 Step 03).
 */
object CustomerPaymentValidator {

    fun validatePaymentRecording(
        tenantId: String,
        projectId: String,
        customerId: String,
        financialAccountId: String,
        amount: BigDecimal,
        currency: String,
        paymentMethod: CustomerPaymentMethod,
        referenceNumber: String?,
        account: CustomerFinancialAccount,
        invoice: CustomerInvoice?
    ): DomainResult<Unit> {
        if (tenantId.isBlank()) return DomainResult.Error(IllegalArgumentException("Tenant ID cannot be blank"))
        if (projectId.isBlank()) return DomainResult.Error(IllegalArgumentException("Project ID cannot be blank"))
        if (customerId.isBlank()) return DomainResult.Error(IllegalArgumentException("Customer ID cannot be blank"))
        if (financialAccountId.isBlank()) return DomainResult.Error(IllegalArgumentException("Financial Account ID cannot be blank"))
        if (amount <= BigDecimal.ZERO) {
            return DomainResult.Error(IllegalArgumentException("Payment amount must be strictly greater than zero. Received: $amount"))
        }
        if (currency.isBlank() || currency.length !in 3..4) {
            return DomainResult.Error(IllegalArgumentException("Invalid currency code: '$currency'"))
        }

        // Account validation
        if (account.customerId != customerId || account.tenantId != tenantId || account.projectId != projectId) {
            return DomainResult.Error(
                IllegalStateException("CustomerFinancialAccount does not match the payment customer/tenant/project boundary")
            )
        }
        if (!account.status.canTransact) {
            return DomainResult.Error(
                IllegalStateException("CustomerFinancialAccount '${account.financialAccountId}' is ${account.status} and cannot receive payments")
            )
        }

        // Direct invoice-linked validation
        if (invoice != null) {
            if (invoice.customerId != customerId || invoice.tenantId != tenantId || invoice.projectId != projectId) {
                return DomainResult.Error(
                    IllegalStateException("Invoice '${invoice.invoiceId}' does not belong to customer '$customerId' or project/tenant scope")
                )
            }
            if (invoice.status !in setOf(CustomerInvoiceStatus.ISSUED, CustomerInvoiceStatus.PARTIALLY_PAID)) {
                return DomainResult.Error(
                    IllegalStateException("Cannot record payment against invoice '${invoice.invoiceId}' with status ${invoice.status}. Invoice must be ISSUED or PARTIALLY_PAID.")
                )
            }
            if (amount > invoice.dueAmount) {
                return DomainResult.Error(
                    IllegalArgumentException(
                        "Payment amount ($amount) exceeds invoice outstanding due amount (${invoice.dueAmount}). Overpayment is not allowed."
                    )
                )
            }
        }

        return DomainResult.Success(Unit)
    }

    fun validateStatusTransition(
        payment: CustomerPayment,
        targetStatus: CustomerPaymentStatus,
        reason: String?
    ): DomainResult<Unit> {
        if (!payment.status.canTransitionTo(targetStatus)) {
            return DomainResult.Error(
                IllegalStateException("Cannot transition Customer Payment from ${payment.status} to $targetStatus")
            )
        }
        if (targetStatus == CustomerPaymentStatus.CANCELLED && reason.isNullOrBlank()) {
            return DomainResult.Error(
                IllegalArgumentException("A reason must be provided when cancelling a Customer Payment")
            )
        }
        return DomainResult.Success(Unit)
    }
}

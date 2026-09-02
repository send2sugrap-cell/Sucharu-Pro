package com.sucharu.sucharupro.domain.validation.customersettlement

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoice
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPayment
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentStatus
import com.sucharu.sucharupro.domain.model.customersettlement.CustomerPaymentAllocation
import com.sucharu.sucharupro.domain.model.customersettlement.CustomerPaymentAllocationStatus
import com.sucharu.sucharupro.domain.model.customersettlement.InvoiceAllocationRequestItem
import java.math.BigDecimal

/**
 * Domain Validator for Customer Financial Settlement & Payment Allocation (Module 14 Step 06).
 */
object CustomerSettlementValidator {

    fun validateAllocation(
        tenantId: String,
        projectId: String,
        payment: CustomerPayment?,
        invoice: CustomerInvoice?,
        account: CustomerFinancialAccount?,
        amount: BigDecimal,
        currentPaymentAllocatedAmount: BigDecimal
    ): DomainResult<Unit> {
        if (tenantId.isBlank() || projectId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("Tenant ID and Project ID cannot be blank."))
        }

        if (payment == null) {
            return DomainResult.Error(IllegalArgumentException("Payment not found."))
        }

        if (payment.status != CustomerPaymentStatus.CONFIRMED) {
            return DomainResult.Error(IllegalStateException("Only CONFIRMED payments can be allocated. Current status: ${payment.status}"))
        }

        if (invoice == null) {
            return DomainResult.Error(IllegalArgumentException("Invoice not found."))
        }

        if (invoice.status !in setOf(CustomerInvoiceStatus.ISSUED, CustomerInvoiceStatus.PARTIALLY_PAID)) {
            return DomainResult.Error(IllegalStateException("Invoice '${invoice.invoiceId}' is not eligible for payment allocation (status: ${invoice.status})."))
        }

        if (account == null) {
            return DomainResult.Error(IllegalArgumentException("Customer Financial Account not found."))
        }

        // Multi-tenant & customer boundaries
        if (payment.tenantId != tenantId || invoice.tenantId != tenantId) {
            return DomainResult.Error(IllegalArgumentException("Cross-tenant payment allocation is strictly prohibited."))
        }

        if (payment.projectId != projectId || invoice.projectId != projectId) {
            return DomainResult.Error(IllegalArgumentException("Cross-project payment allocation is strictly prohibited."))
        }

        if (payment.customerId != invoice.customerId) {
            return DomainResult.Error(IllegalArgumentException("Cross-customer payment allocation is strictly prohibited (Payment Customer: ${payment.customerId}, Invoice Customer: ${invoice.customerId})."))
        }

        // Monetary validation
        if (amount <= BigDecimal.ZERO) {
            return DomainResult.Error(IllegalArgumentException("Allocation amount must be strictly greater than zero."))
        }

        val unallocatedPayment = payment.amount.subtract(currentPaymentAllocatedAmount)
        if (amount > unallocatedPayment) {
            return DomainResult.Error(IllegalArgumentException("Allocation amount ($amount) exceeds unallocated payment balance ($unallocatedPayment)."))
        }

        if (amount > invoice.dueAmount) {
            return DomainResult.Error(IllegalArgumentException("Allocation amount ($amount) exceeds invoice outstanding due amount (${invoice.dueAmount})."))
        }

        return DomainResult.Success(Unit)
    }

    fun validateMultiAllocation(
        tenantId: String,
        projectId: String,
        payment: CustomerPayment?,
        account: CustomerFinancialAccount?,
        items: List<InvoiceAllocationRequestItem>,
        currentPaymentAllocatedAmount: BigDecimal
    ): DomainResult<Unit> {
        if (tenantId.isBlank() || projectId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("Tenant ID and Project ID cannot be blank."))
        }

        if (payment == null) {
            return DomainResult.Error(IllegalArgumentException("Payment not found."))
        }

        if (payment.status != CustomerPaymentStatus.CONFIRMED) {
            return DomainResult.Error(IllegalStateException("Only CONFIRMED payments can be allocated. Current status: ${payment.status}"))
        }

        if (account == null) {
            return DomainResult.Error(IllegalArgumentException("Customer Financial Account not found."))
        }

        if (items.isEmpty()) {
            return DomainResult.Error(IllegalArgumentException("Allocation list cannot be empty."))
        }

        var totalRequested = BigDecimal.ZERO
        val seenInvoices = mutableSetOf<String>()

        for (item in items) {
            if (item.invoiceId.isBlank()) {
                return DomainResult.Error(IllegalArgumentException("Invoice ID cannot be blank in allocation item."))
            }
            if (seenInvoices.contains(item.invoiceId)) {
                return DomainResult.Error(IllegalArgumentException("Duplicate invoice ID '${item.invoiceId}' in multi-allocation request."))
            }
            seenInvoices.add(item.invoiceId)

            if (item.amount <= BigDecimal.ZERO) {
                return DomainResult.Error(IllegalArgumentException("Allocation amount for invoice '${item.invoiceId}' must be strictly positive."))
            }
            totalRequested = totalRequested.add(item.amount)
        }

        val unallocatedPayment = payment.amount.subtract(currentPaymentAllocatedAmount)
        if (totalRequested > unallocatedPayment) {
            return DomainResult.Error(IllegalArgumentException("Total requested allocation ($totalRequested) exceeds unallocated payment balance ($unallocatedPayment)."))
        }

        return DomainResult.Success(Unit)
    }

    fun validateReversal(
        allocation: CustomerPaymentAllocation?,
        reason: String?
    ): DomainResult<Unit> {
        if (allocation == null) {
            return DomainResult.Error(IllegalArgumentException("Payment allocation not found."))
        }

        if (allocation.status != CustomerPaymentAllocationStatus.ALLOCATED) {
            return DomainResult.Error(IllegalStateException("Only active (ALLOCATED) allocations can be reversed. Current status: ${allocation.status}"))
        }

        if (reason.isNullOrBlank()) {
            return DomainResult.Error(IllegalArgumentException("Reversal reason is mandatory."))
        }

        return DomainResult.Success(Unit)
    }
}

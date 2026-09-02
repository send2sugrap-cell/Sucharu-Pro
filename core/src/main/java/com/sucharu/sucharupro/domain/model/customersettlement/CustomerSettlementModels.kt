package com.sucharu.sucharupro.domain.model.customersettlement

import java.math.BigDecimal

/**
 * Status of a Customer Payment Allocation (Module 14 Step 06).
 */
enum class CustomerPaymentAllocationStatus {
    ALLOCATED,
    REVERSED;

    val isAllocated: Boolean get() = this == ALLOCATED
    val isReversed: Boolean get() = this == REVERSED
}

/**
 * Customer Payment Allocation record linking a confirmed payment to an invoice.
 */
data class CustomerPaymentAllocation(
    val allocationId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val customerFinancialAccountId: String,
    val paymentId: String,
    val invoiceId: String,
    val allocatedAmount: BigDecimal,
    val currency: String = "BDT",
    val status: CustomerPaymentAllocationStatus = CustomerPaymentAllocationStatus.ALLOCATED,
    val reversalReason: String? = null,
    val idempotencyKey: String? = null,
    val allocatedAt: Long = System.currentTimeMillis(),
    val allocatedBy: String = "system",
    val reversedAt: Long? = null,
    val reversedBy: String? = null,
    val version: Long = 1L
)

/**
 * Single item in a multi-invoice allocation request.
 */
data class InvoiceAllocationRequestItem(
    val invoiceId: String,
    val amount: BigDecimal
)

/**
 * Unallocated payment snapshot.
 */
data class CustomerUnallocatedPayment(
    val paymentId: String,
    val paymentNumber: String,
    val customerId: String,
    val customerFinancialAccountId: String,
    val totalAmount: BigDecimal,
    val allocatedAmount: BigDecimal,
    val unallocatedAmount: BigDecimal,
    val currency: String = "BDT",
    val paymentDate: Long,
    val status: String
)

/**
 * Deterministic settlement summary of a customer.
 */
data class CustomerSettlementSummary(
    val customerId: String,
    val projectId: String,
    val customerFinancialAccountId: String,
    val totalInvoiced: BigDecimal,
    val totalPaid: BigDecimal,
    val totalAllocated: BigDecimal,
    val totalUnallocated: BigDecimal,
    val totalAvailableCredit: BigDecimal,
    val totalOutstanding: BigDecimal,
    val invoiceCount: Int,
    val partiallyPaidInvoiceCount: Int,
    val paidInvoiceCount: Int,
    val overdueInvoiceCount: Int = 0,
    val currency: String = "BDT"
)

/**
 * Result of an atomic settlement operation.
 */
data class CustomerSettlementResult(
    val paymentId: String,
    val totalAllocated: BigDecimal,
    val remainingUnallocated: BigDecimal,
    val allocations: List<CustomerPaymentAllocation>
)

/**
 * Immutable audit event for Customer Settlement & Payment Allocation.
 */
data class CustomerSettlementAuditEvent(
    val auditId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val allocationId: String? = null,
    val paymentId: String? = null,
    val invoiceId: String? = null,
    val actorId: String,
    val actorRole: String,
    val action: String,
    val previousStatus: String? = null,
    val newStatus: String? = null,
    val amount: BigDecimal? = null,
    val reason: String? = null,
    val occurredAt: Long = System.currentTimeMillis(),
    val metadataJson: String? = null
)

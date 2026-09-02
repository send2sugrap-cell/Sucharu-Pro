package com.sucharu.sucharupro.domain.model.customerinvoice

import java.math.BigDecimal

/**
 * Lifecycle status for Customer Invoices (Module 14 Step 02).
 */
enum class CustomerInvoiceStatus {
    DRAFT,
    ISSUED,
    PARTIALLY_PAID,
    PAID,
    CANCELLED,
    VOID;

    val isDraft: Boolean get() = this == DRAFT
    val isIssued: Boolean get() = this in setOf(ISSUED, PARTIALLY_PAID, PAID)
    val isSettled: Boolean get() = this == PAID
    val isTerminal: Boolean get() = this in setOf(CANCELLED, VOID)

    fun canTransitionTo(target: CustomerInvoiceStatus): Boolean {
        if (this == target) return true
        return when (this) {
            DRAFT -> target in setOf(ISSUED, CANCELLED)
            ISSUED -> target in setOf(PARTIALLY_PAID, PAID, CANCELLED, VOID)
            PARTIALLY_PAID -> target in setOf(PAID, VOID)
            PAID -> target in setOf(VOID)
            CANCELLED, VOID -> false // Terminal states
        }
    }
}

/**
 * Immutable line item within a Customer Invoice.
 */
data class CustomerInvoiceLine(
    val lineId: String,
    val invoiceId: String,
    val tenantId: String,
    val projectId: String,
    val description: String,
    val productId: String? = null,
    val jobId: String? = null,
    val quantity: BigDecimal = BigDecimal.ONE,
    val unit: String = "PCS",
    val unitPrice: BigDecimal = BigDecimal.ZERO,
    val discount: BigDecimal = BigDecimal.ZERO,
    val tax: BigDecimal = BigDecimal.ZERO,
    val lineTotal: BigDecimal = BigDecimal.ZERO,
    val notes: String? = null,
    val lineOrder: Int = 0
)

/**
 * Customer Invoice aggregate root (Module 14 Step 02).
 *
 * Establishes the authoritative financial receivable document for a Customer,
 * referencing the originating Order/Job and linking to the CustomerFinancialAccount.
 */
data class CustomerInvoice(
    val invoiceId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val customerFinancialAccountId: String,
    val invoiceNumber: String,
    val sourceOrderId: String? = null,
    val sourceJobId: String? = null,
    val issueDate: Long? = null,
    val dueDate: Long? = null,
    val currency: String = "BDT",
    val subtotal: BigDecimal = BigDecimal.ZERO,
    val discount: BigDecimal = BigDecimal.ZERO,
    val tax: BigDecimal = BigDecimal.ZERO,
    val adjustment: BigDecimal = BigDecimal.ZERO,
    val grandTotal: BigDecimal = BigDecimal.ZERO,
    val paidAmount: BigDecimal = BigDecimal.ZERO,
    val dueAmount: BigDecimal = BigDecimal.ZERO,
    val status: CustomerInvoiceStatus = CustomerInvoiceStatus.DRAFT,
    val lines: List<CustomerInvoiceLine> = emptyList(),
    val notes: String? = null,
    val cancellationReason: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "system",
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = "system",
    val version: Long = 1L
)

/**
 * Immutable financial audit event for Customer Invoice operations.
 */
data class CustomerInvoiceAuditEvent(
    val auditId: String,
    val invoiceId: String,
    val customerId: String,
    val tenantId: String,
    val projectId: String,
    val actorId: String,
    val actorRole: String,
    val action: String,
    val previousStatus: CustomerInvoiceStatus? = null,
    val newStatus: CustomerInvoiceStatus? = null,
    val reason: String? = null,
    val occurredAt: Long = System.currentTimeMillis(),
    val metadataJson: String? = null
)

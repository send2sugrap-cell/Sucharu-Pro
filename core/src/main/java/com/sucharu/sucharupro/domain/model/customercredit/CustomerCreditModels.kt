package com.sucharu.sucharupro.domain.model.customercredit

import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentMethod
import java.math.BigDecimal

/**
 * Status lifecycle for Customer Advances (Module 14 Step 04).
 */
enum class CustomerAdvanceStatus {
    RECORDED,
    AVAILABLE,
    ALLOCATED,
    EXHAUSTED,
    CANCELLED;

    val isAvailable: Boolean get() = this in setOf(RECORDED, AVAILABLE, ALLOCATED)
    val isExhausted: Boolean get() = this == EXHAUSTED
    val isCancelled: Boolean get() = this == CANCELLED
    val isTerminal: Boolean get() = this == CANCELLED || this == EXHAUSTED

    fun canTransitionTo(target: CustomerAdvanceStatus): Boolean {
        if (this == target) return true
        return when (this) {
            RECORDED -> target in setOf(AVAILABLE, ALLOCATED, EXHAUSTED, CANCELLED)
            AVAILABLE -> target in setOf(ALLOCATED, EXHAUSTED, CANCELLED)
            ALLOCATED -> target in setOf(AVAILABLE, EXHAUSTED, CANCELLED)
            EXHAUSTED -> target == AVAILABLE // Reopened if allocation is reversed
            CANCELLED -> false // Terminal state
        }
    }
}

/**
 * Status of a Customer Credit Allocation.
 */
enum class CustomerAllocationStatus {
    ALLOCATED,
    REVERSED;

    val isAllocated: Boolean get() = this == ALLOCATED
    val isReversed: Boolean get() = this == REVERSED
}

/**
 * Type of Customer Financial Adjustment.
 */
enum class CustomerAdjustmentType {
    CREDIT,
    DEBIT
}

/**
 * Status of Customer Financial Adjustment.
 */
enum class CustomerAdjustmentStatus {
    APPLIED,
    REVERSED
}

/**
 * Status lifecycle for Customer Refunds.
 */
enum class CustomerRefundStatus {
    REQUESTED,
    APPROVED,
    PROCESSED,
    COMPLETED,
    REJECTED,
    CANCELLED;

    val isTerminal: Boolean get() = this in setOf(COMPLETED, REJECTED, CANCELLED)

    fun canTransitionTo(target: CustomerRefundStatus): Boolean {
        if (this == target) return true
        return when (this) {
            REQUESTED -> target in setOf(APPROVED, REJECTED, CANCELLED)
            APPROVED -> target in setOf(PROCESSED, COMPLETED, REJECTED, CANCELLED)
            PROCESSED -> target in setOf(COMPLETED, CANCELLED)
            COMPLETED -> false
            REJECTED -> false
            CANCELLED -> false
        }
    }
}

/**
 * Entity type for credit audit events.
 */
enum class CustomerCreditEntityType {
    ADVANCE,
    ALLOCATION,
    ADJUSTMENT,
    REFUND
}

/**
 * Customer Advance aggregate root.
 */
data class CustomerAdvance(
    val advanceId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val customerFinancialAccountId: String,
    val advanceNumber: String,
    val amount: BigDecimal,
    val allocatedAmount: BigDecimal = BigDecimal.ZERO,
    val availableAmount: BigDecimal = amount,
    val currency: String = "BDT",
    val paymentMethod: CustomerPaymentMethod = CustomerPaymentMethod.CASH,
    val receiptDate: Long = System.currentTimeMillis(),
    val referenceNumber: String? = null,
    val externalReference: String? = null,
    val notes: String? = null,
    val status: CustomerAdvanceStatus = CustomerAdvanceStatus.RECORDED,
    val idempotencyKey: String? = null,
    val cancellationReason: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "system",
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = "system",
    val version: Long = 1L
)

/**
 * Credit Allocation record linking an advance/credit to an invoice.
 */
data class CustomerCreditAllocation(
    val allocationId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val customerFinancialAccountId: String,
    val advanceId: String? = null,
    val invoiceId: String,
    val allocatedAmount: BigDecimal,
    val currency: String = "BDT",
    val status: CustomerAllocationStatus = CustomerAllocationStatus.ALLOCATED,
    val reversalReason: String? = null,
    val idempotencyKey: String? = null,
    val allocatedAt: Long = System.currentTimeMillis(),
    val allocatedBy: String = "system",
    val reversedAt: Long? = null,
    val reversedBy: String? = null,
    val version: Long = 1L
)

/**
 * Customer Account Adjustment record.
 */
data class CustomerAdjustment(
    val adjustmentId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val customerFinancialAccountId: String,
    val adjustmentNumber: String,
    val adjustmentType: CustomerAdjustmentType,
    val amount: BigDecimal,
    val currency: String = "BDT",
    val reason: String,
    val referenceNumber: String? = null,
    val notes: String? = null,
    val status: CustomerAdjustmentStatus = CustomerAdjustmentStatus.APPLIED,
    val idempotencyKey: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "system",
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = "system",
    val version: Long = 1L
)

/**
 * Customer Refund aggregate root.
 */
data class CustomerRefund(
    val refundId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val customerFinancialAccountId: String,
    val paymentId: String? = null,
    val advanceId: String? = null,
    val refundNumber: String,
    val amount: BigDecimal,
    val currency: String = "BDT",
    val refundMethod: CustomerPaymentMethod = CustomerPaymentMethod.CASH,
    val reason: String,
    val status: CustomerRefundStatus = CustomerRefundStatus.REQUESTED,
    val rejectionReason: String? = null,
    val idempotencyKey: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "system",
    val approvedAt: Long? = null,
    val approvedBy: String? = null,
    val processedAt: Long? = null,
    val processedBy: String? = null,
    val completedAt: Long? = null,
    val completedBy: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = "system",
    val version: Long = 1L
)

/**
 * Aggregated view of customer credit balances and movements.
 */
data class CustomerCreditSummary(
    val customerId: String,
    val customerFinancialAccountId: String,
    val totalAdvances: BigDecimal,
    val totalAllocated: BigDecimal,
    val totalAvailableCredit: BigDecimal,
    val totalAdjustmentsCredit: BigDecimal,
    val totalAdjustmentsDebit: BigDecimal,
    val totalRefunds: BigDecimal,
    val currency: String = "BDT"
)

/**
 * Immutable audit event for Customer Credit operations.
 */
data class CustomerCreditAuditEvent(
    val auditId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val entityType: CustomerCreditEntityType,
    val entityId: String,
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

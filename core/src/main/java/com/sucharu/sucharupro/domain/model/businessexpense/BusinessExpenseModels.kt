package com.sucharu.sucharupro.domain.model.businessexpense

import java.math.BigDecimal

/**
 * Lifecycle status for Business Expenses (Module 15 Step 01).
 */
enum class BusinessExpenseStatus {
    DRAFT,
    SUBMITTED,
    APPROVED,
    POSTABLE,
    REJECTED,
    CANCELLED;

    val isDraft: Boolean get() = this == DRAFT
    val isSubmitted: Boolean get() = this == SUBMITTED
    val isApproved: Boolean get() = this == APPROVED
    val isPostable: Boolean get() = this == POSTABLE
    val isRejected: Boolean get() = this == REJECTED
    val isCancelled: Boolean get() = this == CANCELLED
    val isTerminal: Boolean get() = this == CANCELLED

    fun canTransitionTo(target: BusinessExpenseStatus): Boolean {
        if (this == target) return true
        return when (this) {
            DRAFT -> target in setOf(SUBMITTED, CANCELLED)
            SUBMITTED -> target in setOf(APPROVED, POSTABLE, REJECTED, CANCELLED)
            APPROVED -> target in setOf(POSTABLE, CANCELLED)
            POSTABLE -> target == CANCELLED
            REJECTED -> target in setOf(DRAFT, SUBMITTED, CANCELLED)
            CANCELLED -> false // Terminal state
        }
    }
}

/**
 * Payment disbursement methods for Business Expenses (Module 15 Step 01).
 */
enum class BusinessExpensePaymentMethod {
    CASH,
    BKASH,
    NAGAD,
    BANK,
    CARD,
    OTHER;

    val requiresReference: Boolean get() = this in setOf(BKASH, NAGAD, BANK, CARD)
}

/**
 * Configurable Expense Category entity (Module 15 Step 01).
 */
data class BusinessExpenseCategory(
    val categoryId: String,
    val tenantId: String,
    val projectId: String,
    val name: String,
    val code: String,
    val description: String? = null,
    val isActive: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Long = 1L
)

/**
 * Business Expense aggregate root (Module 15 Step 01).
 *
 * Establishes the authoritative financial expense source record and lifecycle foundation
 * before ledger posting (Step 02) or vendor payable settlement (Step 03/04).
 */
data class BusinessExpense(
    val expenseId: String,
    val tenantId: String,
    val projectId: String,
    val branchId: String? = null,
    val locationId: String? = null,
    val expenseNumber: String,
    val expenseCategoryId: String,
    val amount: BigDecimal,
    val currency: String = "BDT",
    val expenseDate: Long = System.currentTimeMillis(),
    val paymentMethod: BusinessExpensePaymentMethod = BusinessExpensePaymentMethod.CASH,
    val paymentReference: String? = null,
    val status: BusinessExpenseStatus = BusinessExpenseStatus.DRAFT,
    val vendorId: String? = null,
    val jobId: String? = null,
    val description: String,
    val notes: String? = null,
    val attachmentUrl: String? = null,
    val attachmentMetadata: String? = null,
    val idempotencyKey: String? = null,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val submittedBy: String? = null,
    val submittedAt: Long? = null,
    val approvedBy: String? = null,
    val approvedAt: Long? = null,
    val rejectedBy: String? = null,
    val rejectedAt: Long? = null,
    val rejectionReason: String? = null,
    val cancelledBy: String? = null,
    val cancelledAt: Long? = null,
    val cancellationReason: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String? = null,
    val version: Long = 1L
)

/**
 * Immutable audit event for Business Expense operations and lifecycle transitions.
 */
data class BusinessExpenseAuditEvent(
    val eventId: String,
    val tenantId: String,
    val projectId: String,
    val expenseId: String,
    val eventType: String,
    val actorId: String,
    val actorRole: String,
    val timestamp: Long = System.currentTimeMillis(),
    val correlationId: String? = null,
    val previousStatus: BusinessExpenseStatus? = null,
    val newStatus: BusinessExpenseStatus? = null,
    val reason: String? = null,
    val metadataJson: String? = null
)

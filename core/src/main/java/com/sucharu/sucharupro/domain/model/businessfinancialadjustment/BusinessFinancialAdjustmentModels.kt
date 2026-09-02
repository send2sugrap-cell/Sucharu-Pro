package com.sucharu.sucharupro.domain.model.businessfinancialadjustment

import java.math.BigDecimal

/**
 * Controlled financial adjustment types supported by the Sucharu Pro ERP platform.
 */
enum class BusinessFinancialAdjustmentType(val label: String, val category: String) {
    EXPENSE_CORRECTION("Expense Correction", "EXPENSE"),
    VENDOR_PAYABLE_ADJUSTMENT("Vendor Payable Adjustment", "PAYABLE"),
    VENDOR_PAYMENT_CORRECTION("Vendor Payment Correction", "PAYMENT"),
    CUSTOMER_REFUND("Customer Refund Recognition", "REFUND"),
    CUSTOMER_ACCOUNT_ADJUSTMENT("Customer Account Adjustment", "CUSTOMER"),
    OVERPAYMENT_CORRECTION("Overpayment Correction", "SETTLEMENT"),
    UNDERPAYMENT_CORRECTION("Underpayment Correction", "SETTLEMENT"),
    DUPLICATE_TRANSACTION_CORRECTION("Duplicate Transaction Correction", "CORRECTION"),
    ROUNDING_ADJUSTMENT("Rounding Adjustment", "SETTLEMENT"),
    WRITE_OFF("General Business Write-Off", "WRITE_OFF"),
    BAD_DEBT_WRITE_OFF("Bad Debt Write-Off", "WRITE_OFF"),
    VENDOR_LIABILITY_WRITE_OFF("Vendor Liability Write-Off", "WRITE_OFF"),
    SETTLEMENT_ADJUSTMENT("Settlement Dispute Adjustment", "SETTLEMENT"),
    RECONCILIATION_CORRECTION("Reconciliation Correction", "RECONCILIATION"),
    MANUAL_FINANCIAL_ADJUSTMENT("Manual Financial Adjustment", "GENERAL"),
    REVERSAL_REQUEST("Financial Transaction Reversal", "REVERSAL")
}

/**
 * Adjustment lifecycle states and terminal statuses.
 */
enum class AdjustmentStatus {
    DRAFT,
    SUBMITTED,
    UNDER_REVIEW,
    APPROVED,
    POSTED,
    RECONCILED,
    REJECTED,
    CANCELLED,
    VOIDED,
    REVERSAL_REQUESTED,
    REVERSAL_APPROVED,
    REVERSED;

    val isTerminal: Boolean
        get() = this in setOf(REJECTED, CANCELLED, VOIDED, REVERSED)

    val isEditable: Boolean
        get() = this in setOf(DRAFT, SUBMITTED)

    val canBeReviewed: Boolean
        get() = this == SUBMITTED

    val canBeApproved: Boolean
        get() = this in setOf(SUBMITTED, UNDER_REVIEW)

    val canBePosted: Boolean
        get() = this == APPROVED

    val canBeReversed: Boolean
        get() = this in setOf(POSTED, RECONCILED)
}

/**
 * Refund lifecycle states.
 */
enum class RefundStatus {
    REQUESTED,
    UNDER_REVIEW,
    APPROVED,
    POSTED,
    SETTLED,
    RECONCILED,
    REJECTED,
    CANCELLED;

    val isTerminal: Boolean
        get() = this in setOf(REJECTED, CANCELLED, SETTLED, RECONCILED)

    val canBeApproved: Boolean
        get() = this in setOf(REQUESTED, UNDER_REVIEW)

    val canBePosted: Boolean
        get() = this == APPROVED
}

/**
 * Write-off classifications.
 */
enum class BusinessFinancialWriteOffType(val label: String) {
    BAD_DEBT("Bad Debt Write-Off"),
    CUSTOMER_BALANCE("Customer Balance Write-Off"),
    VENDOR_LIABILITY("Vendor Liability Write-Off"),
    EXPENSE("Expense Disallowance / Write-Off"),
    OTHER_BUSINESS_WRITE_OFF("Other Approved Business Write-Off")
}

/**
 * Write-off lifecycle states.
 */
enum class WriteOffStatus {
    REQUESTED,
    UNDER_REVIEW,
    APPROVED,
    POSTED,
    RECONCILED,
    REJECTED,
    CANCELLED;

    val isTerminal: Boolean
        get() = this in setOf(REJECTED, CANCELLED, RECONCILED)

    val canBeApproved: Boolean
        get() = this in setOf(REQUESTED, UNDER_REVIEW)

    val canBePosted: Boolean
        get() = this == APPROVED
}

/**
 * Compensating posting types.
 */
enum class AdjustmentPostingType {
    DEBIT_COMPENSATING,
    CREDIT_COMPENSATING,
    REVERSAL_ENTRY,
    ROUNDING_ENTRY
}

/**
 * Primary source entity types for financial adjustments.
 */
enum class AdjustmentSourceType {
    BUSINESS_EXPENSE,
    VENDOR_PAYABLE,
    VENDOR_PAYMENT,
    CUSTOMER_INVOICE,
    CUSTOMER_PAYMENT,
    BUSINESS_LEDGER_POSTING,
    COST_ALLOCATION,
    COST_COMMITMENT,
    COST_ACCRUAL,
    SETTLEMENT_CASE,
    RECONCILIATION_DISCREPANCY,
    MANUAL
}

/**
 * Core Business Financial Adjustment entity.
 */
data class BusinessFinancialAdjustment(
    val id: String,
    val tenantId: String,
    val projectId: String,
    val adjustmentNumber: String,
    val adjustmentType: BusinessFinancialAdjustmentType,
    val sourceType: AdjustmentSourceType,
    val sourceId: String,
    val originalTransactionId: String? = null,
    val originalAmount: BigDecimal = BigDecimal.ZERO,
    val adjustmentAmount: BigDecimal = BigDecimal.ZERO,
    val effectiveAmount: BigDecimal = BigDecimal.ZERO,
    val currency: String = "BDT",
    val reason: String,
    val justification: String,
    val status: AdjustmentStatus = AdjustmentStatus.DRAFT,
    val periodId: String,
    val costCenterId: String? = null,
    val jobId: String? = null,
    val customerId: String? = null,
    val vendorId: String? = null,
    val createdBy: String,
    val reviewedBy: String? = null,
    val approvedBy: String? = null,
    val postedBy: String? = null,
    val cancelledBy: String? = null,
    val rejectedBy: String? = null,
    val reversalRequestedBy: String? = null,
    val reversalApprovedBy: String? = null,
    val reviewedAt: Long? = null,
    val approvedAt: Long? = null,
    val postedAt: Long? = null,
    val reversalRequestedAt: Long? = null,
    val reversalApprovedAt: Long? = null,
    val reversedAt: Long? = null,
    val ledgerPostingId: String? = null,
    val reversingPostingId: String? = null,
    val idempotencyKey: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Controlled Financial Refund entity.
 */
data class BusinessFinancialRefund(
    val id: String,
    val tenantId: String,
    val projectId: String,
    val refundNumber: String,
    val sourceType: AdjustmentSourceType,
    val sourceId: String,
    val customerId: String? = null,
    val vendorId: String? = null,
    val originalTransactionId: String? = null,
    val eligibleBalance: BigDecimal = BigDecimal.ZERO,
    val requestedAmount: BigDecimal,
    val approvedAmount: BigDecimal = BigDecimal.ZERO,
    val currency: String = "BDT",
    val refundReason: String,
    val paymentMethod: String = "BANK_TRANSFER",
    val status: RefundStatus = RefundStatus.REQUESTED,
    val periodId: String,
    val requestedBy: String,
    val approvedBy: String? = null,
    val postedBy: String? = null,
    val approvedAt: Long? = null,
    val postedAt: Long? = null,
    val settledAt: Long? = null,
    val ledgerPostingId: String? = null,
    val idempotencyKey: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Controlled Financial Write-Off entity.
 */
data class BusinessFinancialWriteOff(
    val id: String,
    val tenantId: String,
    val projectId: String,
    val writeOffNumber: String,
    val sourceType: AdjustmentSourceType,
    val sourceId: String,
    val writeOffType: BusinessFinancialWriteOffType,
    val eligibleBalance: BigDecimal = BigDecimal.ZERO,
    val amount: BigDecimal,
    val currency: String = "BDT",
    val reason: String,
    val justification: String,
    val status: WriteOffStatus = WriteOffStatus.REQUESTED,
    val periodId: String,
    val customerId: String? = null,
    val vendorId: String? = null,
    val requestedBy: String,
    val approvedBy: String? = null,
    val postedBy: String? = null,
    val approvedAt: Long? = null,
    val postedAt: Long? = null,
    val ledgerPostingId: String? = null,
    val idempotencyKey: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Record of a compensating posting in the business ledger.
 */
data class BusinessFinancialAdjustmentPosting(
    val id: String,
    val tenantId: String,
    val projectId: String,
    val adjustmentId: String,
    val postingNumber: String,
    val ledgerPostingId: String,
    val postingType: AdjustmentPostingType,
    val debitAccount: String,
    val creditAccount: String,
    val amount: BigDecimal,
    val currency: String = "BDT",
    val status: String = "POSTED",
    val postedBy: String,
    val postedAt: Long = System.currentTimeMillis(),
    val idempotencyKey: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Append-only audit record.
 */
data class BusinessFinancialAdjustmentAuditEvent(
    val id: String,
    val tenantId: String,
    val projectId: String,
    val entityType: String,
    val entityId: String,
    val eventType: String,
    val actorId: String,
    val actorRole: String,
    val timestamp: Long = System.currentTimeMillis(),
    val previousStatus: String? = null,
    val newStatus: String? = null,
    val reason: String? = null,
    val metadataJson: String? = null,
    val correlationId: String? = null,
    val idempotencyKey: String? = null
)

/**
 * Analytical summary of business financial adjustments.
 */
data class BusinessFinancialAdjustmentSummary(
    val tenantId: String,
    val projectId: String,
    val periodId: String? = null,
    val totalAdjustmentsCount: Int = 0,
    val pendingAdjustmentsCount: Int = 0,
    val approvedAdjustmentsCount: Int = 0,
    val postedAdjustmentsCount: Int = 0,
    val totalAdjustedAmount: BigDecimal = BigDecimal.ZERO,
    val totalRefundedAmount: BigDecimal = BigDecimal.ZERO,
    val totalWrittenOffAmount: BigDecimal = BigDecimal.ZERO,
    val totalReversedAmount: BigDecimal = BigDecimal.ZERO,
    val pendingApprovalAmount: BigDecimal = BigDecimal.ZERO,
    val postedAmount: BigDecimal = BigDecimal.ZERO,
    val unresolvedExceptionsCount: Int = 0,
    val calculatedAt: Long = System.currentTimeMillis()
)

/**
 * Financial exception item for governance dashboards.
 */
data class BusinessFinancialException(
    val id: String,
    val entityType: String,
    val entityId: String,
    val referenceNumber: String,
    val issueType: String,
    val severity: String,
    val description: String,
    val amount: BigDecimal,
    val status: String,
    val detectedAt: Long = System.currentTimeMillis()
)

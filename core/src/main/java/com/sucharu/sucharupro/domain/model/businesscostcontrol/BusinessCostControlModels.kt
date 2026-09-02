package com.sucharu.sucharupro.domain.model.businesscostcontrol

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Lifecycle status for business cost commitments.
 */
enum class BusinessCostCommitmentStatus {
    DRAFT,
    SUBMITTED,
    APPROVED,
    ACTIVE,
    PARTIALLY_CONSUMED,
    FULLY_CONSUMED,
    CANCELLED,
    EXPIRED,
    CLOSED;

    val isTerminal: Boolean
        get() = this in setOf(FULLY_CONSUMED, CANCELLED, EXPIRED, CLOSED)

    val isEditable: Boolean
        get() = this == DRAFT

    val canBeConsumed: Boolean
        get() = this in setOf(APPROVED, ACTIVE, PARTIALLY_CONSUMED)
}

/**
 * Source types originating business cost commitments.
 */
enum class BusinessCostCommitmentSourceType {
    VENDOR_WORK_ORDER,
    PURCHASE_COMMITMENT,
    SERVICE_COMMITMENT,
    TRANSPORT_COMMITMENT,
    MANUAL,
    OTHER
}

/**
 * Lifecycle status for cost accruals.
 */
enum class BusinessCostAccrualStatus {
    DRAFT,
    REVIEWED,
    APPROVED,
    POSTED,
    REVERSED,
    CANCELLED;

    val isPosted: Boolean
        get() = this == POSTED

    val isReversed: Boolean
        get() = this == REVERSED

    val canBePosted: Boolean
        get() = this == APPROVED

    val canBeReversed: Boolean
        get() = this == POSTED
}

/**
 * Status for business financial periods.
 */
enum class BusinessFinancialPeriodStatus {
    OPEN,
    SOFT_CLOSED,
    CLOSED;

    val isClosed: Boolean
        get() = this == CLOSED

    val allowsNewPosting: Boolean
        get() = this == OPEN
}

/**
 * Exception types flagged during cost control and reconciliation.
 */
enum class BusinessCostControlExceptionType {
    ACCRUAL_WITHOUT_PAYABLE,
    PAYABLE_WITHOUT_COMMITMENT,
    OVER_COMMITTED,
    OVER_ACCRUED,
    DOUBLE_RECOGNITION_RISK,
    CLOSED_PERIOD_VIOLATION
}

/**
 * Severity level for cost control exceptions.
 */
enum class BusinessCostControlSeverity {
    INFO,
    WARNING,
    CRITICAL
}

/**
 * Canonical Business Financial Period entity for cost control boundaries.
 */
data class BusinessFinancialPeriod(
    val id: String,
    val tenantId: String,
    val projectId: String,
    val periodCode: String,
    val periodName: String,
    val startDate: Long,
    val endDate: Long,
    val status: BusinessFinancialPeriodStatus = BusinessFinancialPeriodStatus.OPEN,
    val closedBy: String? = null,
    val closedAt: Long? = null,
    val closeReason: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "system",
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = "system",
    val version: Long = 1L
)

/**
 * Formal Business Cost Commitment entity.
 */
data class BusinessCostCommitment(
    val id: String,
    val tenantId: String,
    val projectId: String,
    val commitmentNumber: String,
    val vendorId: String? = null,
    val jobId: String? = null,
    val costCenterId: String? = null,
    val costCategoryId: String,
    val description: String,
    val committedAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val consumedAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val remainingAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val currency: String = "BDT",
    val commitmentDate: Long = System.currentTimeMillis(),
    val expectedDate: Long? = null,
    val periodId: String? = null,
    val status: BusinessCostCommitmentStatus = BusinessCostCommitmentStatus.DRAFT,
    val sourceType: BusinessCostCommitmentSourceType = BusinessCostCommitmentSourceType.MANUAL,
    val sourceId: String,
    val createdBy: String = "system",
    val approvedBy: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Long = 1L
)

/**
 * Append-only tracking of commitment consumption.
 */
data class BusinessCostCommitmentConsumption(
    val id: String,
    val commitmentId: String,
    val tenantId: String,
    val projectId: String,
    val sourceType: BusinessCostCommitmentSourceType,
    val sourceId: String,
    val amount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val currency: String = "BDT",
    val consumedAt: Long = System.currentTimeMillis(),
    val createdBy: String,
    val idempotencyKey: String? = null,
    val notes: String? = null
)

/**
 * Business Cost Accrual record for unbilled or recognized period liabilities.
 */
data class BusinessCostAccrual(
    val id: String,
    val tenantId: String,
    val projectId: String,
    val accrualNumber: String,
    val vendorId: String? = null,
    val jobId: String? = null,
    val costCenterId: String? = null,
    val costCategoryId: String,
    val description: String,
    val accrualAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val reversedAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val currency: String = "BDT",
    val accountingPeriodId: String,
    val accrualDate: Long = System.currentTimeMillis(),
    val sourceCommitmentId: String? = null,
    val sourceType: BusinessCostCommitmentSourceType = BusinessCostCommitmentSourceType.MANUAL,
    val sourceId: String,
    val status: BusinessCostAccrualStatus = BusinessCostAccrualStatus.DRAFT,
    val ledgerPostingId: String? = null,
    val reversalPostingId: String? = null,
    val createdBy: String,
    val reviewedBy: String? = null,
    val approvedBy: String? = null,
    val postedBy: String? = null,
    val reversedBy: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Long = 1L
) {
    val netAccrualAmount: BigDecimal
        get() = (accrualAmount - reversedAmount).setScale(4, RoundingMode.HALF_UP)
}

/**
 * Append-only record of an accrual reversal.
 */
data class BusinessCostAccrualReversal(
    val id: String,
    val tenantId: String,
    val projectId: String,
    val accrualId: String,
    val reversalAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val currency: String = "BDT",
    val reversalDate: Long = System.currentTimeMillis(),
    val accountingPeriodId: String,
    val reason: String,
    val ledgerPostingId: String,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val idempotencyKey: String? = null
)

/**
 * Append-only audit record for business cost control events.
 */
data class BusinessCostControlAuditEvent(
    val id: String,
    val tenantId: String,
    val projectId: String,
    val entityType: String,
    val entityId: String,
    val eventType: String,
    val actorUserId: String,
    val actorRole: String,
    val timestamp: Long = System.currentTimeMillis(),
    val correlationId: String? = null,
    val idempotencyKey: String? = null,
    val previousState: String? = null,
    val newState: String? = null,
    val amount: BigDecimal? = null,
    val currency: String? = null,
    val reason: String? = null,
    val metadata: String? = null
)

/**
 * Read-only analytical control exception item.
 */
data class BusinessCostControlException(
    val exceptionType: BusinessCostControlExceptionType,
    val description: String,
    val severity: BusinessCostControlSeverity,
    val sourceEntityId: String,
    val amount: BigDecimal? = null,
    val currency: String = "BDT"
)

/**
 * Executive Control Dashboard metrics.
 */
data class BusinessCostControlDashboard(
    val totalCommitments: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val activeCommitments: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val consumedCommitments: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val remainingCommitments: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val accruedCosts: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val unbilledLiabilities: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val vendorPayables: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val unreconciledAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val totalCommitmentCount: Int = 0,
    val activeCommitmentCount: Int = 0,
    val pendingAccrualCount: Int = 0,
    val exceptionCount: Int = 0,
    val currency: String = "BDT"
)

/**
 * Reconciliation Summary projection comparing commitments, actuals, accruals, and payables.
 */
data class BusinessCostReconciliationSummary(
    val commitmentAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val consumedAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val accruedAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val payableAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val paidAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val remainingCommitment: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val unbilledAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val unreconciledAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val exceptions: List<BusinessCostControlException> = emptyList(),
    val currency: String = "BDT"
)

/**
 * Period-End Control & Close Readiness Report.
 */
data class BusinessCostPeriodEndReport(
    val period: BusinessFinancialPeriod,
    val openCommitmentsAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val pendingAccrualsCount: Int = 0,
    val pendingAccrualsAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val postedAccrualsAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val unbilledLiabilitiesAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val unreconciledPayablesAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val exceptionsCount: Int = 0,
    val isReadyForClosure: Boolean = false,
    val warnings: List<String> = emptyList()
)

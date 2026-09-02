package com.sucharu.sucharupro.domain.model.businessreconciliation

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Supported domain types for financial reconciliation runs.
 */
enum class ReconciliationRunType {
    FULL_PERIOD,
    EXPENSE,
    VENDOR_PAYABLE,
    LEDGER,
    COST_ALLOCATION,
    COMMITMENT,
    ACCRUAL
}

/**
 * Explicit lifecycle status for reconciliation runs.
 */
enum class ReconciliationRunStatus {
    CREATED,
    RUNNING,
    COMPLETED,
    UNDER_REVIEW,
    APPROVED,
    FAILED;

    val isTerminal: Boolean
        get() = this in setOf(APPROVED, FAILED)

    val canBeApproved: Boolean
        get() = this in setOf(COMPLETED, UNDER_REVIEW)
}

/**
 * Severity level for financial discrepancies.
 */
enum class DiscrepancySeverity {
    INFO,
    WARNING,
    CRITICAL;

    val isBlockingForPeriodClose: Boolean
        get() = this == CRITICAL
}

/**
 * Discrepancy lifecycle status.
 */
enum class DiscrepancyStatus {
    OPEN,
    INVESTIGATING,
    RESOLVED,
    WAIVED,
    REJECTED;

    val isClosed: Boolean
        get() = this in setOf(RESOLVED, WAIVED, REJECTED)

    val isOpenOrInvestigating: Boolean
        get() = this in setOf(OPEN, INVESTIGATING)
}

/**
 * Specific deterministic discrepancy classification.
 */
enum class FinancialDiscrepancyType {
    AMOUNT_MISMATCH,
    MISSING_SOURCE,
    MISSING_POSTING,
    ORPHAN_POSTING,
    DUPLICATE_POSTING,
    DUPLICATE_ALLOCATION,
    OVER_ALLOCATION,
    PERIOD_MISMATCH,
    BALANCE_MISMATCH,
    PAYMENT_WITHOUT_PAYABLE,
    PAYABLE_WITHOUT_LIABILITY_POSTING,
    ACCRUAL_WITHOUT_SOURCE,
    MISSING_ACCRUAL_REVERSAL,
    COMMITMENT_OVER_CONSUMPTION,
    INVALID_CLASSIFICATION,
    INVALID_REVERSAL_REFERENCE
}

/**
 * Entity representing an executed or scheduled reconciliation run.
 */
data class BusinessFinancialReconciliationRun(
    val id: String,
    val tenantId: String,
    val projectId: String,
    val periodId: String,
    val runNumber: String,
    val runType: ReconciliationRunType = ReconciliationRunType.FULL_PERIOD,
    val status: ReconciliationRunStatus = ReconciliationRunStatus.CREATED,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val createdBy: String,
    val reviewedBy: String? = null,
    val approvedBy: String? = null,
    val totalRecordsChecked: Int = 0,
    val matchedRecords: Int = 0,
    val discrepancyCount: Int = 0,
    val criticalDiscrepancyCount: Int = 0,
    val warningCount: Int = 0,
    val checksum: String,
    val notes: String? = null,
    val idempotencyKey: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Entity representing a detected financial discrepancy.
 */
data class BusinessFinancialReconciliationDiscrepancy(
    val id: String,
    val tenantId: String,
    val projectId: String,
    val reconciliationRunId: String,
    val periodId: String,
    val discrepancyType: FinancialDiscrepancyType,
    val severity: DiscrepancySeverity = DiscrepancySeverity.CRITICAL,
    val sourceType: String,
    val sourceId: String,
    val expectedAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val actualAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val differenceAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val currency: String = "BDT",
    val description: String,
    val status: DiscrepancyStatus = DiscrepancyStatus.OPEN,
    val detectedAt: Long = System.currentTimeMillis(),
    val assignedTo: String? = null,
    val resolutionNote: String? = null,
    val resolvedBy: String? = null,
    val resolvedAt: Long? = null,
    val approvedBy: String? = null,
    val approvedAt: Long? = null,
    val linkedCorrectionType: String? = null,
    val linkedCorrectionId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Immutable snapshot capturing calculated totals and state for auditability.
 */
data class BusinessFinancialReconciliationSnapshot(
    val id: String,
    val tenantId: String,
    val projectId: String,
    val reconciliationRunId: String,
    val periodId: String,
    val snapshotData: String,
    val checksum: String,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Append-only audit record for reconciliation actions.
 */
data class BusinessFinancialReconciliationAuditEvent(
    val id: String,
    val tenantId: String,
    val projectId: String,
    val reconciliationRunId: String? = null,
    val discrepancyId: String? = null,
    val eventType: String,
    val actorId: String,
    val actorRole: String,
    val correlationId: String? = null,
    val idempotencyKey: String? = null,
    val reason: String? = null,
    val beforeState: String? = null,
    val afterState: String? = null,
    val checksum: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Period close readiness report evaluated before final hard-close.
 */
data class PeriodCloseReadiness(
    val periodId: String,
    val isReady: Boolean,
    val blockingIssues: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val reconciliationRunIds: List<String> = emptyList(),
    val unresolvedCriticalCount: Int = 0,
    val unresolvedWarningCount: Int = 0,
    val allRequiredRunsApproved: Boolean = false,
    val calculatedAt: Long = System.currentTimeMillis()
)

/**
 * High level dashboard summary of reconciliation health.
 */
data class ReconciliationDashboardSummary(
    val totalRuns: Int = 0,
    val approvedRuns: Int = 0,
    val openDiscrepancies: Int = 0,
    val criticalDiscrepancies: Int = 0,
    val resolvedDiscrepancies: Int = 0,
    val totalRecordsChecked: Int = 0,
    val totalMatchedRecords: Int = 0,
    val readyToClosePeriods: Int = 0
)

package com.sucharu.sucharupro.domain.model.businessfinancialgovernance

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Dimensions upon which budgets and forecasts can be assigned and tracked.
 */
enum class BusinessFinancialBudgetDimensionType(val displayName: String) {
    COST_CENTER("Cost Center"),
    COST_CATEGORY("Cost Category"),
    JOB("Job / Project"),
    BRANCH("Branch / Location"),
    EXPENSE_CATEGORY("Expense Category"),
    OVERALL_BUSINESS("Overall Business")
}

/**
 * Strict lifecycle states for financial budget governance.
 */
enum class BusinessFinancialBudgetStatus {
    DRAFT,
    SUBMITTED,
    REVIEWED,
    APPROVED,
    ACTIVE,
    REJECTED,
    REVISED,
    CLOSED;

    val isEditable: Boolean
        get() = this == DRAFT

    val canBeSubmitted: Boolean
        get() = this == DRAFT

    val canBeReviewed: Boolean
        get() = this == SUBMITTED

    val canBeApproved: Boolean
        get() = this in setOf(SUBMITTED, REVIEWED)

    val canBeActivated: Boolean
        get() = this == APPROVED

    val canBeRevised: Boolean
        get() = this in setOf(APPROVED, ACTIVE)

    val canBeClosed: Boolean
        get() = this == ACTIVE

    val isTerminal: Boolean
        get() = this in setOf(REJECTED, CLOSED)
}

/**
 * Variance classification based on utilization % and thresholds.
 */
enum class BudgetVarianceStatus(val displayName: String) {
    ON_TRACK("On Track"),
    WARNING("Warning - High Utilization"),
    OVER_BUDGET("Over Budget"),
    CRITICAL("Critical - Significant Overrun")
}

/**
 * Financial forecast scenarios.
 */
enum class ForecastScenarioType(val displayName: String) {
    BASELINE("Baseline Run-Rate"),
    OPTIMISTIC("Optimistic (Disciplined Spend)"),
    CONSERVATIVE("Conservative (Stress-Tested)")
}

/**
 * Management decision alert types for financial governance.
 */
enum class GovernanceAlertType(val displayName: String) {
    BUDGET_WARNING("Budget Utilization Warning"),
    OVER_BUDGET("Budget Exceeded"),
    HIGH_COMMITMENT_EXPOSURE("High Commitment Exposure"),
    HIGH_ACCRUAL_EXPOSURE("High Unposted Accrual Exposure"),
    PAYABLE_PRESSURE("Vendor Payable Overdue Pressure"),
    FORECAST_EXCEEDS_BUDGET("Projected Forecast Exceeds Budget"),
    PERIOD_END_RISK("Period Closing Feasibility Risk"),
    UNRESOLVED_RECONCILIATION_RISK("Unresolved Reconciliation Discrepancies"),
    EXCESSIVE_ADJUSTMENT_RISK("High Volume Financial Adjustments")
}

/**
 * Severity levels for governance alerts.
 */
enum class GovernanceAlertSeverity {
    INFO,
    WARNING,
    CRITICAL
}

/**
 * Lifecycle states for governance alerts.
 */
enum class GovernanceAlertStatus {
    OPEN,
    ACKNOWLEDGED,
    RESOLVED,
    DISMISSED;

    val isOpen: Boolean
        get() = this == OPEN

    val isActionable: Boolean
        get() = this in setOf(OPEN, ACKNOWLEDGED)

    val isTerminal: Boolean
        get() = this in setOf(RESOLVED, DISMISSED)
}

/**
 * Canonical Business Financial Budget entity.
 */
data class BusinessFinancialBudget(
    val id: String,
    val tenantId: String = "TENANT-001",
    val projectId: String = "PRJ-001",
    val budgetName: String,
    val periodId: String,
    val dimensionType: BusinessFinancialBudgetDimensionType,
    val dimensionId: String,
    val allocatedAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val currency: String = "BDT",
    val status: BusinessFinancialBudgetStatus = BusinessFinancialBudgetStatus.DRAFT,
    val version: Long = 1L,
    val effectiveStartDate: Long,
    val effectiveEndDate: Long,
    val description: String? = null,
    val createdBy: String = "system",
    val reviewedBy: String? = null,
    val approvedBy: String? = null,
    val approvedAt: Long? = null,
    val rejectionReason: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Historical approved budget revision snapshot entity.
 */
data class BusinessFinancialBudgetRevision(
    val id: String,
    val budgetId: String,
    val tenantId: String = "TENANT-001",
    val projectId: String = "PRJ-001",
    val version: Long,
    val previousAllocatedAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val newAllocatedAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val revisionReason: String,
    val revisedBy: String,
    val approvedBy: String? = null,
    val revisedAt: Long = System.currentTimeMillis(),
    val status: String = "APPROVED"
)

/**
 * Spending and utilization threshold configuration entity.
 */
data class BusinessFinancialBudgetThreshold(
    val id: String,
    val tenantId: String = "TENANT-001",
    val projectId: String = "PRJ-001",
    val thresholdName: String,
    val dimensionType: BusinessFinancialBudgetDimensionType = BusinessFinancialBudgetDimensionType.OVERALL_BUSINESS,
    val dimensionId: String = "ALL",
    val warningUtilizationPct: BigDecimal = BigDecimal("80.0000").setScale(4, RoundingMode.HALF_UP),
    val criticalUtilizationPct: BigDecimal = BigDecimal("100.0000").setScale(4, RoundingMode.HALF_UP),
    val largeExpenseThresholdAmount: BigDecimal = BigDecimal("50000.0000").setScale(4, RoundingMode.HALF_UP),
    val commitmentExposureThresholdPct: BigDecimal = BigDecimal("90.0000").setScale(4, RoundingMode.HALF_UP),
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Financial forecast projection aggregate entity.
 */
data class BusinessFinancialForecast(
    val id: String,
    val tenantId: String = "TENANT-001",
    val projectId: String = "PRJ-001",
    val forecastName: String,
    val periodId: String,
    val dimensionType: BusinessFinancialBudgetDimensionType,
    val dimensionId: String,
    val currency: String = "BDT",
    val actualYtdAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val projectedRemainingAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val forecastTotalAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val runRatePerDay: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val generatedAt: Long = System.currentTimeMillis(),
    val createdBy: String = "system"
)

/**
 * Forecast scenario projection.
 */
data class BusinessFinancialForecastScenario(
    val id: String,
    val forecastId: String,
    val tenantId: String = "TENANT-001",
    val projectId: String = "PRJ-001",
    val scenarioType: ForecastScenarioType,
    val projectedAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val varianceVsBudget: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val assumptionsJson: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Management decision alert record.
 */
data class BusinessFinancialGovernanceAlert(
    val id: String,
    val tenantId: String = "TENANT-001",
    val projectId: String = "PRJ-001",
    val alertType: GovernanceAlertType,
    val severity: GovernanceAlertSeverity = GovernanceAlertSeverity.WARNING,
    val sourceDimensionType: BusinessFinancialBudgetDimensionType,
    val sourceDimensionId: String,
    val message: String,
    val thresholdValue: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val currentValue: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val status: GovernanceAlertStatus = GovernanceAlertStatus.OPEN,
    val acknowledgedBy: String? = null,
    val acknowledgedAt: Long? = null,
    val acknowledgementNotes: String? = null,
    val resolvedBy: String? = null,
    val resolvedAt: Long? = null,
    val resolutionNotes: String? = null,
    val dismissalReason: String? = null,
    val periodId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Immutable append-only governance audit event.
 */
data class BusinessFinancialGovernanceAuditEvent(
    val id: String,
    val tenantId: String,
    val projectId: String,
    val actorId: String,
    val actorRole: String,
    val eventType: String,
    val outcome: String = "SUCCESS",
    val targetId: String? = null,
    val targetType: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val detailsJson: String? = null,
    val clientIp: String? = null,
    val correlationId: String? = null
)

// =========================================================================
// ANALYTICAL / PROJECTION MODELS (Non-Persisted)
// =========================================================================

/**
 * Dynamic Budget vs Actual comparison projected from canonical sources.
 */
data class BudgetVsActualComparison(
    val budgetId: String,
    val budgetName: String,
    val periodId: String,
    val dimensionType: BusinessFinancialBudgetDimensionType,
    val dimensionId: String,
    val currency: String,
    val allocatedBudget: BigDecimal,
    val actualSpend: BigDecimal,
    val committedExposure: BigDecimal,
    val accruedExposure: BigDecimal,
    val totalProjectedExposure: BigDecimal,
    val remainingBudget: BigDecimal,
    val remainingProjectedBudget: BigDecimal,
    val utilizationPercentage: BigDecimal,
    val projectedUtilizationPercentage: BigDecimal,
    val varianceAmount: BigDecimal,
    val varianceStatus: BudgetVarianceStatus
)

/**
 * Comprehensive Executive Governance Overview.
 */
data class ExecutiveGovernanceOverview(
    val tenantId: String,
    val projectId: String,
    val periodId: String?,
    val currency: String,
    val totalActiveBudgetsCount: Int,
    val totalAllocatedBudgetAmount: BigDecimal,
    val totalActualSpendAmount: BigDecimal,
    val totalCommittedExposureAmount: BigDecimal,
    val totalAccruedExposureAmount: BigDecimal,
    val totalProjectedExposureAmount: BigDecimal,
    val totalRemainingBudgetAmount: BigDecimal,
    val overallUtilizationPercentage: BigDecimal,
    val activeThresholdsCount: Int,
    val openAlertsCount: Int,
    val criticalAlertsCount: Int,
    val warningAlertsCount: Int,
    val comparisons: List<BudgetVsActualComparison>,
    val alerts: List<BusinessFinancialGovernanceAlert>,
    val forecasts: List<BusinessFinancialForecast>
)

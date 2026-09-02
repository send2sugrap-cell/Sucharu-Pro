package com.sucharu.sucharupro.domain.model.profitability

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Canonical Profitability Alert Dimensions.
 * Module 16 Step 09.
 */
enum class ProfitabilityAlertDimension {
    BUSINESS,
    PERIOD,
    CUSTOMER,
    PRODUCT,
    JOB,
    VENDOR,
    COST_COMPONENT,
    CROSS_DIMENSION,
    FORECAST
}

/**
 * Canonical Profitability Alert Types.
 */
enum class ProfitabilityAlertType {
    MARGIN_DECLINE,
    MARGIN_NEGATIVE,
    PROFIT_DECLINE,
    LOSS_MAKING,
    REVENUE_DECLINE,
    COST_SPIKE,
    COST_TO_REVENUE_SPIKE,
    UNIT_COST_SPIKE,
    CONTRIBUTION_MARGIN_DECLINE,
    CUSTOMER_PROFITABILITY_DECLINE,
    PRODUCT_PROFITABILITY_DECLINE,
    JOB_PROFITABILITY_DECLINE,
    VENDOR_COST_PRESSURE,
    VENDOR_DEPENDENCY_RISK,
    CUSTOMER_CONCENTRATION_RISK,
    VENDOR_CONCENTRATION_RISK,
    FORECAST_LOSS_RISK,
    FORECAST_MARGIN_DECLINE,
    FORECAST_CONFIDENCE_LOW,
    PROFITABILITY_HEALTH_DECLINE,
    PROFITABILITY_LEAKAGE,
    UNATTRIBUTED_REVENUE,
    UNATTRIBUTED_COST,
    RECONCILIATION_FAILURE,
    DATA_INTEGRITY_FAILURE,
    RECURRING_PROFITABILITY_ISSUE
}

/**
 * Canonical Deterministic Alert Severity.
 */
enum class ProfitabilityAlertSeverity {
    INFO,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Canonical Alert Status Lifecycle.
 */
enum class ProfitabilityAlertStatus {
    DETECTED,
    ACKNOWLEDGED,
    IN_REVIEW,
    ACTION_REQUIRED,
    ACTION_IN_PROGRESS,
    RESOLVED,
    DISMISSED,
    REOPENED,
    SUPPRESSED
}

/**
 * Direction of Threshold Deviation.
 */
enum class ProfitabilityAlertDirection {
    ABOVE_THRESHOLD,
    BELOW_THRESHOLD,
    DEVIATION
}

/**
 * Comparison Operators for Threshold Evaluation.
 */
enum class ComparisonOperator {
    GREATER_THAN,
    GREATER_THAN_OR_EQUAL,
    LESS_THAN,
    LESS_THAN_OR_EQUAL,
    EQUAL,
    ABSOLUTE_CHANGE,
    PERCENT_CHANGE
}

/**
 * Management Escalation Levels.
 */
enum class AlertEscalationLevel {
    NONE,
    WATCH,
    ESCALATE,
    URGENT,
    CRITICAL
}

/**
 * Management Action Lifecycle Statuses.
 */
enum class ManagementActionStatus {
    PROPOSED,
    APPROVED,
    ASSIGNED,
    IN_PROGRESS,
    BLOCKED,
    COMPLETED,
    CANCELLED,
    VERIFIED
}

/**
 * Canonical Deterministic Management Action Codes.
 */
enum class ManagementActionCode {
    REVIEW_JOB_COST,
    REVIEW_PRODUCT_PRICING,
    REVIEW_CUSTOMER_PRICING,
    REVIEW_VENDOR_PRICING,
    NEGOTIATE_VENDOR_COST,
    REVIEW_MATERIAL_USAGE,
    REVIEW_WASTAGE,
    REVIEW_REWORK,
    REVIEW_FINISHING_COST,
    REVIEW_PACKAGING_COST,
    REVIEW_TRANSPORT_COST,
    REVIEW_OVERHEAD_ALLOCATION,
    REVIEW_CUSTOMER_CONCENTRATION,
    REVIEW_VENDOR_DEPENDENCY,
    REVIEW_FORECAST,
    REVIEW_PROFITABILITY_LEAKAGE,
    RECONCILE_FINANCIAL_DATA,
    REVIEW_UNATTRIBUTED_ITEMS
}

/**
 * Configurable Tenant Alert Threshold Rule.
 */
data class ProfitabilityAlertRule(
    val ruleId: String,
    val tenantId: String,
    val projectId: String,
    val ruleName: String,
    val alertType: ProfitabilityAlertType,
    val dimensionType: ProfitabilityAlertDimension,
    val thresholdMetric: String,
    val thresholdValue: BigDecimal,
    val comparisonOperator: ComparisonOperator,
    val severity: ProfitabilityAlertSeverity,
    val enabled: Boolean = true,
    val description: String = "",
    val effectiveFrom: Long = 0L,
    val effectiveTo: Long? = null,
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Core Profitability Alert Record.
 */
data class ProfitabilityAlert(
    val alertId: String,
    val tenantId: String,
    val projectId: String,
    val alertType: ProfitabilityAlertType,
    val severity: ProfitabilityAlertSeverity,
    val status: ProfitabilityAlertStatus,
    val dimensionType: ProfitabilityAlertDimension,
    val dimensionId: String,
    val dimensionLabel: String,
    val periodId: String? = null,
    val sourceModule: String,
    val sourceStep: String,
    val sourceEntityType: String,
    val sourceEntityId: String,
    val triggerMetric: String,
    val observedValue: BigDecimal,
    val thresholdValue: BigDecimal,
    val direction: ProfitabilityAlertDirection,
    val financialImpact: BigDecimal,
    val detectedAt: Long = System.currentTimeMillis(),
    val firstDetectedAt: Long = detectedAt,
    val lastDetectedAt: Long = detectedAt,
    val occurrenceCount: Int = 1,
    val fingerprint: String,
    val integrityHash: String,
    val explanation: String,
    val recommendedActionCode: ManagementActionCode?,
    val isRecurring: Boolean = false,
    val ruleId: String? = null,
    val acknowledgedAt: Long? = null,
    val acknowledgedBy: String? = null,
    val resolvedAt: Long? = null,
    val resolvedBy: String? = null,
    val resolutionNotes: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * History Occurrence of an Alert.
 */
data class ProfitabilityAlertOccurrence(
    val occurrenceId: String,
    val alertId: String,
    val tenantId: String,
    val detectedAt: Long,
    val observedValue: BigDecimal,
    val financialImpact: BigDecimal,
    val previousStatus: ProfitabilityAlertStatus,
    val triggerDetails: String,
    val sourceSnapshotId: String? = null
)

/**
 * Explicit Human/System Management Remediation Action.
 */
data class ProfitabilityManagementAction(
    val actionId: String,
    val alertId: String,
    val tenantId: String,
    val projectId: String,
    val actionCode: ManagementActionCode,
    val actionTitle: String,
    val actionDescription: String,
    val priorityScore: BigDecimal, // 0.0000 - 100.0000
    val status: ManagementActionStatus,
    val assignedTo: String? = null,
    val assignedBy: String? = null,
    val dueAt: Long? = null,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val verifiedAt: Long? = null,
    val verifiedBy: String? = null,
    val expectedFinancialImpact: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val realizedFinancialImpact: BigDecimal? = null,
    val outcomeNotes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val integrityHash: String
)

/**
 * Post-Action Performance Improvement Outcome.
 */
data class ProfitabilityActionOutcome(
    val outcomeId: String,
    val actionId: String,
    val alertId: String,
    val tenantId: String,
    val evaluatedAt: Long,
    val metricBefore: BigDecimal,
    val metricAfter: BigDecimal,
    val improvementPercentage: BigDecimal,
    val realizedSavingsOrRevenue: BigDecimal,
    val isEffective: Boolean,
    val evaluationNotes: String
)

/**
 * Grouped Cross-Dimensional Event Correlation.
 */
data class ProfitabilityAlertCorrelation(
    val correlationId: String,
    val tenantId: String,
    val projectId: String,
    val correlationTitle: String,
    val primaryDimension: ProfitabilityAlertDimension,
    val primaryEntityId: String,
    val primaryEntityLabel: String,
    val correlatedAlertIds: List<String>,
    val compositeSeverity: ProfitabilityAlertSeverity,
    val totalFinancialImpact: BigDecimal, // deduplicated reference
    val correlationReason: String,
    val detectedAt: Long = System.currentTimeMillis()
)

/**
 * Forward-Looking Risk Escalation State.
 */
data class ProfitabilityAlertEscalation(
    val escalationId: String,
    val alertId: String,
    val tenantId: String,
    val escalationLevel: AlertEscalationLevel,
    val ageInHours: Long,
    val recurrenceCount: Int,
    val isActionOverdue: Boolean,
    val financialImpact: BigDecimal,
    val justification: String,
    val calculatedAt: Long = System.currentTimeMillis()
)

/**
 * Management Monitoring Analytical Snapshot.
 */
data class ProfitabilityMonitoringSnapshot(
    val snapshotId: String,
    val tenantId: String,
    val projectId: String,
    val periodId: String? = null,
    val totalActiveAlerts: Int,
    val criticalAlertCount: Int,
    val highAlertCount: Int,
    val mediumAlertCount: Int,
    val lowAlertCount: Int,
    val totalUnresolvedFinancialImpact: BigDecimal,
    val openActionCount: Int,
    val overdueActionCount: Int,
    val recurringIssueCount: Int,
    val escalatedAlertCount: Int,
    val severityDistribution: Map<ProfitabilityAlertSeverity, Int>,
    val dimensionDistribution: Map<ProfitabilityAlertDimension, Int>,
    val generatedAt: Long = System.currentTimeMillis(),
    val integrityHash: String
)

/**
 * Immutable Audit Event for Alert Engine.
 */
data class ProfitabilityAlertAuditEvent(
    val eventId: String,
    val tenantId: String,
    val projectId: String,
    val alertId: String,
    val action: String,
    val actorId: String,
    val actorRole: String,
    val previousState: String? = null,
    val newState: String,
    val notes: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Lineage and Provenance Record for Alert Origin.
 */
data class ProfitabilityAlertProvenance(
    val provenanceId: String,
    val alertId: String,
    val tenantId: String,
    val sourceModule: String,
    val sourceStep: String,
    val sourceEntityType: String,
    val sourceEntityId: String,
    val metricKey: String,
    val metricValue: BigDecimal,
    val calculationTimestamp: Long,
    val provenanceHash: String
)

/**
 * Non-Mutating Reconciliation Result.
 */
data class ProfitabilityAlertReconciliationAssertion(
    val tenantId: String,
    val projectId: String,
    val isBalanced: Boolean,
    val totalAlertsChecked: Int,
    val totalFinancialImpact: BigDecimal,
    val aggregatedImpactFromAlerts: BigDecimal,
    val discrepancyAmount: BigDecimal,
    val openAlertsCountMatches: Boolean,
    val actionCountsMatch: Boolean,
    val provenanceIntegrityMatches: Boolean,
    val checkedAt: Long = System.currentTimeMillis(),
    val assertions: List<String> = emptyList()
)

/**
 * AI-Agent Canonical Read-Only Handoff Contract.
 * Module 16 Step 09.
 */
data class Module16Step09ProfitabilityAlertHandoffContract(
    val tenantId: String,
    val projectId: String,
    val snapshotId: String,
    val totalActiveAlerts: Int,
    val criticalAlertCount: Int,
    val highAlertCount: Int,
    val totalUnresolvedFinancialImpact: BigDecimal,
    val criticalAlerts: List<ProfitabilityAlert>,
    val highPriorityActions: List<ProfitabilityManagementAction>,
    val activeCorrelations: List<ProfitabilityAlertCorrelation>,
    val topEscalations: List<ProfitabilityAlertEscalation>,
    val overallHealthRisk: AlertEscalationLevel,
    val handoffIntegrityHash: String,
    val generatedAt: Long = System.currentTimeMillis(),
    val contractVersion: String = "1.0.0"
)

/**
 * Authoritative Evaluation Source Items.
 */
data class JobProfitabilityEvaluationItem(
    val jobId: String,
    val jobCode: String,
    val customerId: String,
    val revenue: BigDecimal,
    val actualCost: BigDecimal,
    val grossProfit: BigDecimal,
    val grossMarginPercentage: BigDecimal,
    val materialCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val labourCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val machineCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val vendorCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val reworkCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val wastageCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
)

data class ProductProfitabilityEvaluationItem(
    val productId: String,
    val productCode: String,
    val productName: String,
    val totalRevenue: BigDecimal,
    val totalCost: BigDecimal,
    val grossProfit: BigDecimal,
    val grossMarginPercentage: BigDecimal,
    val unitCost: BigDecimal,
    val averageSellingPrice: BigDecimal,
    val totalUnits: Long
)

data class CustomerProfitabilityEvaluationItem(
    val customerId: String,
    val customerCode: String,
    val customerName: String,
    val totalRevenue: BigDecimal,
    val totalCost: BigDecimal,
    val grossProfit: BigDecimal,
    val grossMarginPercentage: BigDecimal,
    val contributionMarginPercentage: BigDecimal,
    val revenueSharePercentage: BigDecimal
)

data class VendorProfitabilityEvaluationItem(
    val vendorId: String,
    val vendorCode: String,
    val vendorName: String,
    val totalSpend: BigDecimal,
    val spendSharePercentage: BigDecimal,
    val costPressureScore: BigDecimal,
    val dependencyRiskScore: BigDecimal
)

data class PeriodProfitabilityEvaluationItem(
    val periodId: String,
    val totalRevenue: BigDecimal,
    val totalCost: BigDecimal,
    val grossProfit: BigDecimal,
    val grossMarginPercentage: BigDecimal,
    val profitDeclinePercentage: BigDecimal? = null,
    val marginDeclinePercentage: BigDecimal? = null
)

data class CrossDimensionEvaluationItem(
    val entityType: String,
    val entityId: String,
    val entityLabel: String,
    val healthScore: BigDecimal,
    val leakageAmount: BigDecimal,
    val concentrationPercentage: BigDecimal,
    val primaryLeakageComponent: String?
)

data class ForecastEvaluationItem(
    val forecastId: String,
    val targetScope: String,
    val targetEntityId: String,
    val targetEntityLabel: String,
    val horizon: String,
    val projectedRevenue: BigDecimal,
    val projectedTotalCost: BigDecimal,
    val projectedGrossProfit: BigDecimal,
    val projectedGrossMarginPercentage: BigDecimal?,
    val confidenceScore: BigDecimal,
    val riskLevel: String,
    val isLossProjected: Boolean
)

data class DataIntegrityEvaluationItem(
    val sourceModule: String,
    val sourceStep: String,
    val sourceEntityId: String,
    val issueType: String,
    val discrepancyAmount: BigDecimal,
    val description: String
)

/**
 * Composite Evaluation Payload for Profitability Alert Rule Engine.
 */
data class ProfitabilityEvaluationPayload(
    val tenantId: String,
    val projectId: String,
    val periodId: String? = null,
    val jobs: List<JobProfitabilityEvaluationItem> = emptyList(),
    val products: List<ProductProfitabilityEvaluationItem> = emptyList(),
    val customers: List<CustomerProfitabilityEvaluationItem> = emptyList(),
    val vendors: List<VendorProfitabilityEvaluationItem> = emptyList(),
    val periods: List<PeriodProfitabilityEvaluationItem> = emptyList(),
    val crossDimensionItems: List<CrossDimensionEvaluationItem> = emptyList(),
    val forecasts: List<ForecastEvaluationItem> = emptyList(),
    val integrityIssues: List<DataIntegrityEvaluationItem> = emptyList()
)


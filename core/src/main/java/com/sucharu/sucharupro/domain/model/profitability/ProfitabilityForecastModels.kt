package com.sucharu.sucharupro.domain.model.profitability

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Target Scope for Profitability Forecasting (Module 16 Step 08).
 */
enum class ProfitabilityForecastScope {
    BUSINESS,
    PERIOD,
    CUSTOMER,
    PRODUCT,
    JOB,
    VENDOR
}

/**
 * Forecasting Method used to compute the projection.
 */
enum class ProfitabilityForecastMethod {
    HISTORICAL_BASELINE,
    ROLLING_AVERAGE,
    WEIGHTED_ROLLING_AVERAGE,
    TREND_BASED,
    DRIVER_BASED,
    SCENARIO_BASED
}

/**
 * Scenario Classification for forward-looking sensitivity analysis.
 */
enum class ProfitabilityScenarioType {
    BASELINE,
    OPTIMISTIC,
    PESSIMISTIC,
    CUSTOM
}

/**
 * Standard Configurable Forecast Horizon.
 */
enum class ForecastHorizon(val periodCount: Int, val label: String) {
    NEXT_1_PERIOD(1, "Next 1 Period"),
    NEXT_3_PERIODS(3, "Next 3 Periods"),
    NEXT_6_PERIODS(6, "Next 6 Periods"),
    NEXT_12_PERIODS(12, "Next 12 Periods")
}

/**
 * Confidence Level derived from data freshness, completeness, and historical depth.
 */
enum class ForecastConfidenceLevel {
    HIGH,
    MODERATE,
    LOW,
    INSUFFICIENT_DATA
}

/**
 * Forward-Looking Profitability Risk Level.
 */
enum class ForecastRiskLevel {
    VERY_LOW,
    LOW,
    MODERATE,
    HIGH,
    VERY_HIGH,
    DATA_INSUFFICIENT
}

/**
 * Structured Management Insight Category.
 */
enum class ForecastInsightType {
    PROJECTED_MARGIN_DECLINE,
    PROJECTED_PROFIT_DECLINE,
    REVENUE_GROWTH,
    COST_PRESSURE,
    MATERIAL_COST_PRESSURE,
    VENDOR_COST_PRESSURE,
    LABOUR_COST_PRESSURE,
    REWORK_RISK,
    WASTAGE_RISK,
    CUSTOMER_CONCENTRATION_RISK,
    VENDOR_DEPENDENCY_RISK,
    PROFITABILITY_IMPROVEMENT,
    BREAK_EVEN_RISK,
    NEGATIVE_PROFIT_FORECAST,
    DATA_QUALITY_WARNING
}

/**
 * Severity of the Management Insight.
 */
enum class ForecastInsightSeverity {
    INFO,
    WARNING,
    CRITICAL
}

/**
 * Lifecycle Status of a Forecast Snapshot.
 */
enum class ForecastStatus {
    DRAFT,
    ACTIVE,
    SUPERSEDED,
    ARCHIVED
}

/**
 * Immutable Analytical Profitability Forecast Snapshot.
 * Represents a forward-looking projection for a given target entity and horizon.
 * Module 16 Step 08.
 */
data class ProfitabilityForecastSnapshot(
    val forecastId: String,
    val tenantId: String,
    val projectId: String,
    val forecastVersion: Int = 1,
    val forecastMethod: ProfitabilityForecastMethod,
    val scenarioType: ProfitabilityScenarioType = ProfitabilityScenarioType.BASELINE,
    val scenarioId: String? = null,
    val targetScope: ProfitabilityForecastScope,
    val targetEntityId: String,
    val targetEntityLabel: String,
    val historicalPeriodStart: String,
    val historicalPeriodEnd: String,
    val forecastPeriodStart: String,
    val forecastPeriodEnd: String,
    val horizon: ForecastHorizon = ForecastHorizon.NEXT_1_PERIOD,
    val currency: String = "BDT",
    val status: ForecastStatus = ForecastStatus.ACTIVE,

    // Core Projected Financials (Scale = 4, HALF_UP)
    val projectedRevenue: BigDecimal,
    val projectedTotalCost: BigDecimal,
    val projectedGrossProfit: BigDecimal,
    val projectedGrossMarginPercentage: BigDecimal?,
    val projectedContribution: BigDecimal,
    val projectedContributionMarginPercentage: BigDecimal?,

    // Volumetric & Unit Economics Projections
    val projectedUnits: Long = 0L,
    val projectedRevenuePerUnit: BigDecimal? = null,
    val projectedCostPerUnit: BigDecimal? = null,
    val projectedProfitPerUnit: BigDecimal? = null,

    // Baseline References (Historical Anchor)
    val baselineRevenue: BigDecimal? = null,
    val baselineCost: BigDecimal? = null,
    val baselineGrossProfit: BigDecimal? = null,
    val baselineGrossMarginPercentage: BigDecimal? = null,

    // Variance vs Baseline
    val projectedRevenueDelta: BigDecimal? = null,
    val projectedCostDelta: BigDecimal? = null,
    val projectedProfitDelta: BigDecimal? = null,
    val projectedMarginDeltaPercentage: BigDecimal? = null,

    // Break-Even Metrics
    val breakEvenRevenue: BigDecimal? = null,
    val breakEvenUnits: Long? = null,
    val marginOfSafetyPercentage: BigDecimal? = null,
    val isBreakEvenAttainable: Boolean = true,

    // Scores & Quality Assessment
    val confidenceScore: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP), // 0.0000 - 100.0000
    val confidenceLevel: ForecastConfidenceLevel = ForecastConfidenceLevel.MODERATE,
    val riskLevel: ForecastRiskLevel = ForecastRiskLevel.LOW,
    val sourceReadiness: PeriodSourceReadiness = PeriodSourceReadiness.READY,

    // Sub-Entities & Breakdown
    val components: List<ProfitabilityForecastComponent> = emptyList(),
    val assumptions: List<ProfitabilityScenarioAssumption> = emptyList(),
    val insights: List<ForecastManagementInsight> = emptyList(),
    val provenanceRecords: List<ProfitabilityForecastProvenance> = emptyList(),

    // Audit & Integrity
    val generatedAt: Long = System.currentTimeMillis(),
    val generatedBy: String = "SYSTEM",
    val calculationVersion: String = "PROFITABILITY_FORECAST_V1",
    val integrityHash: String = "",
    val hashAlgorithm: String = "SHA-256",
    val warnings: List<String> = emptyList()
)

/**
 * Projected Cost Component (12 Canonical Cost Components from Module 16 Step 02).
 */
data class ProfitabilityForecastComponent(
    val componentId: String,
    val forecastId: String,
    val tenantId: String,
    val componentType: JobCostComponentType,
    val projectedAmount: BigDecimal,
    val percentageOfTotalCost: BigDecimal,
    val baselineAmount: BigDecimal? = null,
    val deltaAmount: BigDecimal? = null,
    val growthRatePercentage: BigDecimal? = null,
    val driverDescription: String? = null
)

/**
 * Explicit Scenario Assumption applied to transform historical baselines into forward projections.
 */
data class ProfitabilityScenarioAssumption(
    val assumptionId: String,
    val scenarioId: String,
    val tenantId: String,
    val parameterKey: String,
    val parameterName: String,
    val parameterDescription: String? = null,
    val adjustmentPercentage: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val absoluteAdjustmentAmount: BigDecimal? = null,
    val isApplied: Boolean = true
)

/**
 * Complete Scenario Definition for Sensitivity and What-If Analysis.
 */
data class ProfitabilityScenario(
    val scenarioId: String,
    val tenantId: String,
    val projectId: String,
    val scenarioName: String,
    val scenarioType: ProfitabilityScenarioType,
    val description: String? = null,
    val targetScope: ProfitabilityForecastScope = ProfitabilityForecastScope.BUSINESS,
    val revenueAdjustmentPercentage: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val volumeAdjustmentPercentage: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val materialCostAdjustmentPercentage: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val labourCostAdjustmentPercentage: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val machineCostAdjustmentPercentage: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val vendorCostAdjustmentPercentage: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val reworkAdjustmentPercentage: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val wastageAdjustmentPercentage: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val indirectCostAdjustmentPercentage: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val assumptions: List<ProfitabilityScenarioAssumption> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "SYSTEM",
    val isDefault: Boolean = false
)

/**
 * Side-by-Side Scenario Comparison Item.
 */
data class ProfitabilityScenarioComparisonItem(
    val scenarioId: String,
    val scenarioName: String,
    val scenarioType: ProfitabilityScenarioType,
    val projectedRevenue: BigDecimal,
    val projectedTotalCost: BigDecimal,
    val projectedGrossProfit: BigDecimal,
    val projectedGrossMarginPercentage: BigDecimal?,
    val projectedContribution: BigDecimal,
    val projectedContributionMarginPercentage: BigDecimal?,
    val projectedUnits: Long,
    val revenueDeltaFromBaseline: BigDecimal,
    val costDeltaFromBaseline: BigDecimal,
    val profitDeltaFromBaseline: BigDecimal,
    val marginDeltaFromBaselinePercentage: BigDecimal?,
    val financialImpact: BigDecimal,
    val riskLevel: ForecastRiskLevel
)

/**
 * Side-by-Side Comparison Container across multiple scenarios.
 */
data class ProfitabilityScenarioComparison(
    val comparisonId: String,
    val tenantId: String,
    val projectId: String,
    val baselineForecastId: String,
    val targetScope: ProfitabilityForecastScope,
    val targetEntityId: String,
    val horizon: ForecastHorizon,
    val baselineScenario: ProfitabilityScenarioComparisonItem,
    val comparedScenarios: List<ProfitabilityScenarioComparisonItem>,
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * Structured Forward-Looking Management Insight.
 */
data class ForecastManagementInsight(
    val insightId: String,
    val forecastId: String,
    val tenantId: String,
    val insightType: ForecastInsightType,
    val severity: ForecastInsightSeverity,
    val dimensionType: ProfitabilityForecastScope,
    val targetEntityId: String,
    val targetEntityLabel: String,
    val title: String,
    val explanation: String,
    val financialImpact: BigDecimal,
    val supportingSourceReferences: List<String> = emptyList(),
    val recommendedActionCode: String? = null,
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * Provenance Lineage Record for Forward-Looking Profitability.
 */
data class ProfitabilityForecastProvenance(
    val provenanceId: String,
    val forecastId: String,
    val tenantId: String,
    val projectId: String,
    val sourceModule: String,
    val sourceEntityType: String,
    val sourceEntityId: String,
    val sourceSnapshotId: String? = null,
    val sourcePeriodId: String? = null,
    val metricType: String,
    val amount: BigDecimal,
    val fingerprint: String
)

/**
 * Mathematical Assertion for Non-Mutating Forecast Reconciliation.
 */
data class ForecastReconciliationAssertion(
    val assertionName: String,
    val isPassed: Boolean,
    val expectedAmount: BigDecimal,
    val actualAmount: BigDecimal,
    val discrepancyAmount: BigDecimal,
    val details: String
)

/**
 * Non-Mutating Forecast Reconciliation Event.
 */
data class ProfitabilityForecastReconciliationEvent(
    val eventId: String,
    val tenantId: String,
    val projectId: String,
    val forecastId: String,
    val isBalanced: Boolean,
    val revenueDifference: BigDecimal,
    val costDifference: BigDecimal,
    val profitDifference: BigDecimal,
    val marginDifference: BigDecimal,
    val componentDifference: BigDecimal,
    val scenarioDifference: BigDecimal,
    val assertions: List<ForecastReconciliationAssertion> = emptyList(),
    val errorDetails: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Non-Mutating Analytical Comparison of Projected Forecast vs Subsequently Observed Actual Results.
 */
data class ForecastActualComparison(
    val comparisonId: String,
    val tenantId: String,
    val projectId: String,
    val forecastId: String,
    val actualPeriodId: String,
    val targetScope: ProfitabilityForecastScope,
    val targetEntityId: String,
    val targetEntityLabel: String,

    // Projected vs Actual Pairs
    val forecastRevenue: BigDecimal,
    val actualRevenue: BigDecimal,
    val revenueVariance: BigDecimal,
    val revenueVariancePercentage: BigDecimal?,

    val forecastCost: BigDecimal,
    val actualCost: BigDecimal,
    val costVariance: BigDecimal,
    val costVariancePercentage: BigDecimal?,

    val forecastGrossProfit: BigDecimal,
    val actualGrossProfit: BigDecimal,
    val profitVariance: BigDecimal,
    val profitVariancePercentage: BigDecimal?,

    val forecastMarginPercentage: BigDecimal?,
    val actualMarginPercentage: BigDecimal?,
    val marginVariancePercentage: BigDecimal?,

    val forecastUnits: Long,
    val actualUnits: Long,
    val unitsVariance: Long,

    val isDirectionallyAccurate: Boolean,
    val meanAbsolutePercentageError: BigDecimal?,
    val evaluationNotes: String? = null,
    val comparedAt: Long = System.currentTimeMillis()
)

/**
 * Immutable Audit Event for Forecast Activities.
 */
data class ProfitabilityForecastAuditEvent(
    val auditId: String,
    val tenantId: String,
    val projectId: String,
    val forecastId: String,
    val actionType: String,
    val actorId: String,
    val actorRole: String,
    val details: String,
    val previousStateHash: String? = null,
    val newStateHash: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Query Filter for listing Profitability Forecasts.
 */
data class ProfitabilityForecastFilter(
    val targetScope: ProfitabilityForecastScope? = null,
    val targetEntityId: String? = null,
    val forecastMethod: ProfitabilityForecastMethod? = null,
    val scenarioType: ProfitabilityScenarioType? = null,
    val forecastPeriod: String? = null,
    val status: ForecastStatus? = null,
    val limit: Int = 50,
    val offset: Int = 0
)

/**
 * Stable, Verified, Read-Only Contract for AI-Agent and Executive Handoff.
 * Module 16 Step 08.
 */
data class Module16Step08ProfitabilityForecastHandoffContract(
    val contractVersion: String = "MODULE16_STEP08_V1",
    val forecastId: String,
    val tenantId: String,
    val projectId: String,
    val targetScope: ProfitabilityForecastScope,
    val targetEntityId: String,
    val targetEntityLabel: String,
    val forecastPeriod: String,
    val horizon: String,
    val forecastMethod: String,
    val scenarioType: String,
    val projectedRevenue: BigDecimal,
    val projectedTotalCost: BigDecimal,
    val projectedGrossProfit: BigDecimal,
    val projectedGrossMarginPercentage: BigDecimal?,
    val projectedUnits: Long,
    val confidenceScore: BigDecimal,
    val confidenceLevel: String,
    val riskLevel: String,
    val breakEvenRevenue: BigDecimal?,
    val majorDrivers: List<String>,
    val majorRisks: List<String>,
    val topManagementInsights: List<String>,
    val scenarioSummaryDeltas: Map<String, BigDecimal>,
    val isReconciled: Boolean,
    val integrityHash: String,
    val generatedAt: Long = System.currentTimeMillis()
)

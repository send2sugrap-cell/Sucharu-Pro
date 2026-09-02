package com.sucharu.sucharupro.domain.model.profitability

import java.math.BigDecimal

/**
 * Executive KPI Health Classification.
 */
enum class KpiHealthClassification {
    EXCELLENT,
    HEALTHY,
    STABLE,
    WATCH,
    WARNING,
    CRITICAL,
    INSUFFICIENT_DATA
}

/**
 * KPI Variance Direction.
 */
enum class KpiDirection {
    IMPROVING,
    STABLE,
    DETERIORATING,
    NEUTRAL
}

/**
 * Scorecard Health Dimension.
 */
enum class ScorecardDimension {
    REVENUE_HEALTH,
    MARGIN_HEALTH,
    COST_STABILITY,
    CUSTOMER_PROFITABILITY,
    PRODUCT_PROFITABILITY,
    VENDOR_COST_EFFICIENCY,
    FORECAST_CONFIDENCE,
    ALERT_RISK,
    CONCENTRATION_RISK,
    DATA_INTEGRITY
}

/**
 * Executive Report Section Category.
 */
enum class ExecutiveReportSectionKey {
    EXECUTIVE_SUMMARY,
    KPI_SCORECARD,
    REVENUE_PERFORMANCE,
    COST_PERFORMANCE,
    GROSS_PROFIT_MARGIN,
    CONTRIBUTION_PERFORMANCE,
    JOB_PROFITABILITY,
    PRODUCT_PROFITABILITY,
    CUSTOMER_PROFITABILITY,
    VENDOR_ECONOMICS,
    PERIOD_PERFORMANCE,
    CROSS_DIMENSIONAL_DRIVERS,
    PROFITABILITY_LEAKAGE,
    FORECAST_SCENARIO,
    ALERTS_EARLY_WARNING,
    MANAGEMENT_ACTIONS,
    CONCENTRATION_RISKS,
    RECONCILIATION_INTEGRITY,
    MANAGEMENT_PRIORITIES,
    PROVENANCE_AUDIT
}

/**
 * Canonical Executive KPI Entry.
 */
data class ExecutiveKpi(
    val kpiKey: String,
    val kpiName: String,
    val category: String, // Revenue, Cost, Profit, Operational, Forecast, Alerts, Integrity
    val currentValue: BigDecimal,
    val previousValue: BigDecimal? = null,
    val varianceAmount: BigDecimal? = null,
    val variancePercentage: BigDecimal? = null,
    val unit: String = "BDT", // BDT, %, Count, Score
    val direction: KpiDirection = KpiDirection.NEUTRAL,
    val health: KpiHealthClassification = KpiHealthClassification.HEALTHY,
    val confidenceScore: BigDecimal = BigDecimal("100.0000"),
    val explanation: String,
    val sourceLineage: String
)

/**
 * Management Scorecard Item.
 */
data class ExecutiveScorecardItem(
    val dimension: ScorecardDimension,
    val dimensionName: String,
    val weight: BigDecimal, // 0.0000 to 1.0000
    val rawScore: BigDecimal, // 0.0000 to 100.0000
    val weightedScore: BigDecimal, // 0.0000 to 100.0000
    val classification: KpiHealthClassification,
    val trend: KpiDirection,
    val keyFindings: List<String>,
    val primaryMetric: String,
    val primaryMetricValue: BigDecimal
)

/**
 * Overall Management Scorecard.
 */
data class ExecutiveManagementScorecard(
    val overallScore: BigDecimal, // 0.0000 to 100.0000
    val classification: KpiHealthClassification,
    val overallTrend: KpiDirection,
    val items: List<ExecutiveScorecardItem>,
    val executiveSummary: String,
    val calculationTimestamp: Long = System.currentTimeMillis()
)

/**
 * Executive Top/Bottom Ranking Item.
 */
data class ExecutiveRankingItem(
    val rank: Int,
    val dimension: ProfitabilityAlertDimension, // JOB, PRODUCT, CUSTOMER, VENDOR
    val entityId: String,
    val entityCode: String,
    val entityName: String,
    val revenue: BigDecimal,
    val cost: BigDecimal,
    val grossProfit: BigDecimal,
    val marginPercentage: BigDecimal,
    val contributionMarginPercentage: BigDecimal? = null,
    val score: BigDecimal,
    val highlightReason: String
)

/**
 * Set of Executive Rankings across Dimensions.
 */
data class ExecutiveRankingsPayload(
    val topProfitableJobs: List<ExecutiveRankingItem>,
    val lossMakingJobs: List<ExecutiveRankingItem>,
    val topProfitableProducts: List<ExecutiveRankingItem>,
    val leastProfitableProducts: List<ExecutiveRankingItem>,
    val topContributingCustomers: List<ExecutiveRankingItem>,
    val lowestMarginCustomers: List<ExecutiveRankingItem>,
    val highestSpendVendors: List<ExecutiveRankingItem>,
    val highestRiskVendors: List<ExecutiveRankingItem>
)

/**
 * Profitability Concentration Metric.
 */
data class ConcentrationMetric(
    val dimension: ProfitabilityAlertDimension,
    val top1SharePercentage: BigDecimal,
    val top5SharePercentage: BigDecimal,
    val top10SharePercentage: BigDecimal,
    val totalEntitiesCount: Int,
    val riskLevel: ForecastRiskLevel,
    val explanation: String
)

/**
 * Comprehensive Executive Concentration Summary.
 */
data class ExecutiveConcentrationSummary(
    val customerRevenueConcentration: ConcentrationMetric,
    val customerProfitConcentration: ConcentrationMetric,
    val productRevenueConcentration: ConcentrationMetric,
    val vendorSpendConcentration: ConcentrationMetric,
    val overallConcentrationRisk: ForecastRiskLevel
)

/**
 * Executive Profitability Driver.
 */
data class ExecutiveProfitabilityDriver(
    val driverId: String,
    val driverName: String,
    val category: String, // Revenue, Material, Labour, Machine, Wastage, Overhead, Outsourcing, CustomerMix, ProductMix
    val impactAmount: BigDecimal,
    val impactPercentage: BigDecimal,
    val direction: ProfitabilityAlertDirection,
    val severity: ProfitabilityAlertSeverity,
    val affectedEntitiesCount: Int,
    val description: String,
    val sourceLineage: String
)

/**
 * Executive Profitability Leakage Summary.
 */
data class ExecutiveLeakageSummary(
    val totalLeakageAmount: BigDecimal,
    val leakagePercentageOfRevenue: BigDecimal,
    val directMaterialWastageLeakage: BigDecimal,
    val reworkCostLeakage: BigDecimal,
    val unallocatedOverheadLeakage: BigDecimal,
    val pricingErosionLeakage: BigDecimal,
    val vendorCostSurgeLeakage: BigDecimal,
    val topLeakageItems: List<ProfitLeakageItem> = emptyList(),
    val primaryMitigationRecommendation: String
)

/**
 * Unified Executive Management Priority Item.
 */
data class ExecutivePriorityItem(
    val priorityRank: Int,
    val priorityId: String,
    val title: String,
    val category: String, // Alert, ForecastRisk, Leakage, Concentration, Discrepancy, Strategy
    val dimension: ProfitabilityAlertDimension,
    val entityId: String?,
    val entityLabel: String?,
    val financialImpact: BigDecimal,
    val priorityScore: BigDecimal, // 0.0000 to 100.0000
    val severity: ProfitabilityAlertSeverity,
    val urgencyLevel: AlertEscalationLevel,
    val recommendedActionCode: ManagementActionCode?,
    val recommendedActionTitle: String,
    val sourceModule: String,
    val sourceStep: String,
    val sourceReferenceId: String,
    val currentStatus: String
)

/**
 * Complete Executive Report Section.
 */
data class ExecutiveReportSection(
    val sectionKey: ExecutiveReportSectionKey,
    val sectionTitle: String,
    val orderIndex: Int,
    val summaryNarrative: String,
    val keyMetrics: Map<String, String>,
    val highlights: List<String>,
    val warnings: List<String>
)

/**
 * Complete Executive Profitability Report.
 */
data class ExecutiveProfitabilityReport(
    val reportId: String,
    val tenantId: String,
    val projectId: String,
    val periodId: String?,
    val generatedAt: Long,
    val executiveSummary: String,
    val scorecard: ExecutiveManagementScorecard,
    val kpis: List<ExecutiveKpi>,
    val sections: List<ExecutiveReportSection>,
    val priorities: List<ExecutivePriorityItem>,
    val reportIntegrityHash: String,
    val contractVersion: String = "1.0.0"
)

/**
 * Executive Profitability Snapshot Entity.
 */
data class ExecutiveProfitabilitySnapshot(
    val snapshotId: String,
    val tenantId: String,
    val projectId: String,
    val periodId: String?,
    val generatedAt: Long,
    val totalGrossRevenue: BigDecimal,
    val totalNetRevenue: BigDecimal,
    val totalActualCost: BigDecimal,
    val totalGrossProfit: BigDecimal,
    val grossMarginPercentage: BigDecimal,
    val totalContributionAmount: BigDecimal,
    val contributionMarginPercentage: BigDecimal,
    val forecastRevenue: BigDecimal?,
    val forecastGrossProfit: BigDecimal?,
    val forecastGrossMargin: BigDecimal?,
    val activeAlertsCount: Int,
    val criticalAlertsCount: Int,
    val pendingActionsCount: Int,
    val overallHealth: KpiHealthClassification,
    val overallScore: BigDecimal,
    val scorecardJson: String,
    val kpisJson: String,
    val rankingsJson: String,
    val prioritiesJson: String,
    val concentrationJson: String,
    val driversJson: String,
    val leakageJson: String,
    val reconciliationJson: String,
    val sourceFingerprint: String,
    val integrityHash: String,
    val calculationVersion: String = "1.0.0"
)

/**
 * Executive Lineage & Provenance Record.
 */
data class ExecutiveProvenanceRecord(
    val provenanceId: String,
    val snapshotId: String,
    val tenantId: String,
    val kpiOrSectionKey: String,
    val sourceModule: String,
    val sourceStep: String,
    val sourceEntityType: String,
    val sourceEntityId: String,
    val sourceSnapshotId: String?,
    val metricKey: String,
    val metricValue: BigDecimal,
    val calculationTimestamp: Long,
    val provenanceHash: String
)

/**
 * Non-Mutating Executive Reconciliation Result.
 */
data class ExecutiveReconciliationResult(
    val reconciliationId: String,
    val tenantId: String,
    val projectId: String,
    val periodId: String?,
    val snapshotId: String,
    val checkedAt: Long,
    val isBalanced: Boolean,
    val revenueMatches: Boolean,
    val costMatches: Boolean,
    val profitMatches: Boolean,
    val forecastMatches: Boolean,
    val alertCountsMatch: Boolean,
    val discrepancies: List<String>,
    val integrityHash: String
)

/**
 * Module 16 Step 10 Cryptographic AI-Agent & Management Handoff Contract.
 */
data class Module16Step10ExecutiveProfitabilityHandoffContract(
    val handoffId: String,
    val tenantId: String,
    val projectId: String,
    val periodId: String?,
    val generatedAt: Long,
    val contractVersion: String = "1.0.0",
    val overallHealth: KpiHealthClassification,
    val overallScorecardScore: BigDecimal,
    val keyExecutiveKpis: List<ExecutiveKpi>,
    val topProfitabilityDrivers: List<ExecutiveProfitabilityDriver>,
    val leakageSummary: ExecutiveLeakageSummary,
    val concentrationRisks: ExecutiveConcentrationSummary,
    val topPriorityDecisions: List<ExecutivePriorityItem>,
    val forecastSummary: ProfitabilityForecastSnapshot?,
    val alertMonitoringSummary: ProfitabilityMonitoringSnapshot?,
    val reconciliationStatus: ExecutiveReconciliationResult,
    val sourceSnapshotReferences: List<String>,
    val isReadOnly: Boolean = true,
    val handoffIntegrityHash: String
)

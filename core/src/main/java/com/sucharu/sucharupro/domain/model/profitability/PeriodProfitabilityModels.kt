package com.sucharu.sucharupro.domain.model.profitability

import java.math.BigDecimal

/**
 * Domain models, enums and value objects for Period-Wise Profitability, Business Performance
 * & Financial Trend Intelligence Engine.
 * Module 16 Step 06.
 */

enum class PeriodType {
    DAY,
    WEEK,
    MONTH,
    QUARTER,
    HALF_YEAR,
    YEAR,
    CUSTOM
}

enum class PeriodStatus {
    OPEN,
    IN_PROGRESS,
    CLOSED,
    CERTIFIED,
    SUPERSEDED
}

enum class ProfitabilityClassification {
    HIGHLY_PROFITABLE,
    PROFITABLE,
    LOW_MARGIN,
    BREAK_EVEN,
    LOSS_MAKING,
    NO_REVENUE,
    INSUFFICIENT_DATA
}

enum class PeriodTrendDirection {
    STRONGLY_IMPROVING,
    IMPROVING,
    STABLE,
    DECLINING,
    STRONGLY_DECLINING,
    INSUFFICIENT_DATA
}

enum class PeriodSourceReadiness {
    READY,
    PARTIAL,
    DEGRADED,
    CONFLICT,
    UNAVAILABLE
}

enum class PeriodRankingCriteria {
    GROSS_PROFIT,
    GROSS_MARGIN,
    REVENUE,
    TOTAL_COST,
    CONTRIBUTION_AMOUNT,
    CONTRIBUTION_MARGIN,
    PROFIT_PER_JOB,
    PROFIT_PER_UNIT
}

data class PeriodProfitabilitySnapshot(
    val snapshotId: String,
    val tenantId: String,
    val projectId: String,
    val periodId: String,
    val periodType: PeriodType,
    val periodStart: Long,
    val periodEnd: Long,
    val timezone: String = "Asia/Dhaka",
    val periodKey: String = "",
    val fiscalPeriodId: String? = null,
    val periodStatus: PeriodStatus = PeriodStatus.OPEN,
    val currency: String = "BDT",
    val calculationVersion: String = "MODULE16_PERIOD_PROFITABILITY_V1",
    val snapshotVersion: Int = 1,
    val supersedesSnapshotId: String? = null,
    val supersededBySnapshotId: String? = null,
    val generatedAt: Long = System.currentTimeMillis(),
    val generatedBy: String = "SYSTEM",
    val sourceAsOf: Long = System.currentTimeMillis(),

    // Financial Core
    val revenue: BigDecimal = BigDecimal.ZERO,
    val totalActualCost: BigDecimal = BigDecimal.ZERO,
    val grossProfit: BigDecimal = BigDecimal.ZERO,
    val grossMarginPercentage: BigDecimal? = null,
    val costToRevenuePercentage: BigDecimal? = null,
    val directCost: BigDecimal = BigDecimal.ZERO,
    val indirectCost: BigDecimal = BigDecimal.ZERO,
    val contributionAmount: BigDecimal = BigDecimal.ZERO,
    val contributionMarginPercentage: BigDecimal? = null,

    // Baseline & Variance
    val baselineRevenue: BigDecimal? = null,
    val baselineCost: BigDecimal? = null,
    val revenueVariance: BigDecimal? = null,
    val revenueVariancePercentage: BigDecimal? = null,
    val costVariance: BigDecimal? = null,
    val costVariancePercentage: BigDecimal? = null,
    val profitVariance: BigDecimal? = null,
    val profitVariancePercentage: BigDecimal? = null,

    // Volume & Unit Economics
    val jobCount: Int = 0,
    val completedJobCount: Int = 0,
    val productCount: Int = 0,
    val customerCount: Int = 0,
    val vendorCount: Int = 0,
    val totalUnits: Long = 0L,
    val averageRevenuePerJob: BigDecimal? = null,
    val averageProfitPerJob: BigDecimal? = null,
    val averageRevenuePerUnit: BigDecimal? = null,
    val averageCostPerUnit: BigDecimal? = null,
    val averageProfitPerUnit: BigDecimal? = null,

    // Classifications
    val profitabilityClassification: ProfitabilityClassification = ProfitabilityClassification.INSUFFICIENT_DATA,
    val trendDirection: PeriodTrendDirection = PeriodTrendDirection.INSUFFICIENT_DATA,
    val sourceReadiness: PeriodSourceReadiness = PeriodSourceReadiness.READY,

    // Sub-breakdowns
    val costBreakdown: List<PeriodCostBreakdownItem> = emptyList(),
    val revenueAttributions: List<PeriodRevenueAttributionItem> = emptyList(),
    val provenanceFingerprints: List<String> = emptyList(),
    val integrityHash: String = "",
    val isCertified: Boolean = false,
    val certifiedAt: Long? = null,
    val certificateId: String? = null,
    val warnings: List<String> = emptyList()
)

data class PeriodCostBreakdownItem(
    val componentType: JobCostComponentType,
    val amount: BigDecimal,
    val percentageOfTotalCost: BigDecimal,
    val percentageOfRevenue: BigDecimal?,
    val sourceAttributionCount: Int = 1
)

data class PeriodRevenueAttributionItem(
    val attributionDimension: String, // e.g. "CUSTOMER", "PRODUCT", "JOB", "INVOICE"
    val dimensionId: String,
    val dimensionName: String,
    val amount: BigDecimal,
    val percentageOfTotalRevenue: BigDecimal,
    val sourceModule: String = "MODULE_14",
    val sourceEntityType: String = "INVOICE",
    val sourceEntityId: String = ""
)

data class PeriodRankingItem(
    val rank: Int,
    val periodId: String,
    val periodKey: String,
    val periodType: PeriodType,
    val periodStart: Long,
    val periodEnd: Long,
    val metricValue: BigDecimal,
    val metricLabel: String,
    val revenue: BigDecimal,
    val grossProfit: BigDecimal,
    val grossMarginPercentage: BigDecimal?,
    val profitabilityClassification: ProfitabilityClassification
)

data class PeriodConcentrationAnalysis(
    val tenantId: String,
    val projectId: String,
    val periodType: PeriodType,
    val scopeLabel: String,
    val totalRevenue: BigDecimal,
    val totalProfit: BigDecimal,
    val totalPeriodsEvaluated: Int,
    val top1PeriodId: String?,
    val top1PeriodKey: String?,
    val top1Profit: BigDecimal,
    val top1ProfitSharePercentage: BigDecimal,
    val top3Profit: BigDecimal,
    val top3ProfitSharePercentage: BigDecimal,
    val top5Profit: BigDecimal,
    val top5ProfitSharePercentage: BigDecimal,
    val evaluatedAt: Long = System.currentTimeMillis()
)

data class PeriodComparisonResult(
    val currentPeriodId: String,
    val currentPeriodKey: String,
    val comparisonPeriodId: String,
    val comparisonPeriodKey: String,
    val periodType: PeriodType,
    val currentRevenue: BigDecimal,
    val previousRevenue: BigDecimal,
    val revenueDelta: BigDecimal,
    val revenueDeltaPercentage: BigDecimal?,
    val currentCost: BigDecimal,
    val previousCost: BigDecimal,
    val costDelta: BigDecimal,
    val costDeltaPercentage: BigDecimal?,
    val currentGrossProfit: BigDecimal,
    val previousGrossProfit: BigDecimal,
    val grossProfitDelta: BigDecimal,
    val grossProfitDeltaPercentage: BigDecimal?,
    val currentGrossMarginPercentage: BigDecimal?,
    val previousGrossMarginPercentage: BigDecimal?,
    val grossMarginPercentageDelta: BigDecimal?,
    val currentContributionAmount: BigDecimal,
    val previousContributionAmount: BigDecimal,
    val contributionDelta: BigDecimal,
    val currentUnits: Long,
    val previousUnits: Long,
    val currentJobCount: Int,
    val previousJobCount: Int,
    val trendDirection: PeriodTrendDirection,
    val trendReason: String
)

data class PeriodReconciliationAssertion(
    val assertionName: String,
    val isPassed: Boolean,
    val expectedAmount: BigDecimal,
    val actualAmount: BigDecimal,
    val discrepancyAmount: BigDecimal,
    val details: String
)

data class PeriodProfitabilityReconciliationEvent(
    val eventId: String,
    val tenantId: String,
    val projectId: String,
    val periodId: String,
    val snapshotId: String?,
    val isBalanced: Boolean,
    val revenueDifference: BigDecimal,
    val costDifference: BigDecimal,
    val profitDifference: BigDecimal,
    val marginDifference: BigDecimal,
    val contributionDifference: BigDecimal,
    val childAggregationDifference: BigDecimal,
    val crossDimensionalDifference: BigDecimal,
    val assertions: List<PeriodReconciliationAssertion> = emptyList(),
    val errorDetails: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

data class PeriodProfitabilityProvenanceRecord(
    val provenanceId: String,
    val tenantId: String,
    val projectId: String,
    val periodId: String,
    val sourceModule: String,
    val sourceEntityType: String,
    val sourceEntityId: String,
    val sourceTransactionId: String? = null,
    val sourceSnapshotId: String? = null,
    val amount: BigDecimal,
    val componentType: JobCostComponentType? = null,
    val attributionDimension: String,
    val fingerprint: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class PeriodProfitabilityAuditEvent(
    val auditId: String,
    val tenantId: String,
    val projectId: String,
    val periodId: String,
    val action: String,
    val actorId: String,
    val actorRole: String,
    val snapshotId: String? = null,
    val calculationVersion: String = "MODULE16_PERIOD_PROFITABILITY_V1",
    val previousState: String? = null,
    val resultingState: String? = null,
    val details: String? = null,
    val integrityHash: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class PeriodUnattributedItem(
    val unattributedId: String,
    val tenantId: String,
    val projectId: String,
    val periodId: String,
    val itemType: String, // "UNATTRIBUTED_REVENUE" or "UNATTRIBUTED_COST"
    val sourceModule: String,
    val sourceEntityType: String,
    val sourceEntityId: String,
    val amount: BigDecimal,
    val reason: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class PeriodSourceCollectionResult(
    val recognizedRevenue: BigDecimal,
    val directCost: BigDecimal,
    val indirectCost: BigDecimal,
    val totalCost: BigDecimal,
    val costBreakdown: List<PeriodCostBreakdownItem>,
    val revenueAttributions: List<PeriodRevenueAttributionItem>,
    val provenanceRecords: List<PeriodProfitabilityProvenanceRecord>,
    val unattributedItems: List<PeriodUnattributedItem>,
    val jobCount: Int,
    val completedJobCount: Int,
    val productCount: Int,
    val customerCount: Int,
    val vendorCount: Int,
    val totalUnits: Long,
    val sourceReadiness: PeriodSourceReadiness,
    val warnings: List<String>
)

data class PeriodProfitabilityFilter(
    val periodType: PeriodType? = null,
    val status: PeriodStatus? = null,
    val periodStartFrom: Long? = null,
    val periodEndTo: Long? = null,
    val limit: Int = 50,
    val offset: Int = 0
)

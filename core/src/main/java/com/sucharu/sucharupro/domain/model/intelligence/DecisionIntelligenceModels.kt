package com.sucharu.sucharupro.domain.model.intelligence

import java.math.BigDecimal

/**
 * BI-08 Trend Directions.
 */
enum class DecisionKpiTrendDirection {
    UPWARD,
    DOWNWARD,
    STABLE
}

/**
 * BI-08 Operational Attention Severity.
 */
enum class OperationalAttentionSeverity {
    LOW_ATTENTION,
    NORMAL_ATTENTION,
    URGENT_ATTENTION,
    CRITICAL_ATTENTION
}

/**
 * Measurable Business Decision KPI Item.
 */
data class DecisionKpiItem(
    val kpiCode: String,
    val title: String,
    val currentValue: String,
    val baselineValue: String,
    val trendPercentage: Double,
    val trendDirection: DecisionKpiTrendDirection = DecisionKpiTrendDirection.UPWARD,
    val statusMessage: String
)

/**
 * Operational Exception Item for Executive Action Center.
 */
data class DecisionExceptionItem(
    val exceptionId: String,
    val category: String,
    val severity: OperationalAttentionSeverity,
    val title: String,
    val description: String,
    val affectedRecordId: String? = null,
    val targetRoute: String? = null
)

/**
 * BI-08 Master Decision Intelligence & Executive Operations Read Model.
 */
data class DecisionIntelligenceSummary(
    val totalRevenueYtd: BigDecimal,
    val activeOrdersCount: Int,
    val quotationConversionRatePercentage: BigDecimal,
    val activeProductionJobsCount: Int,
    val totalOutstandingReceivable: BigDecimal,
    val overdueReceivableAmount: BigDecimal,
    val averageGrossMarginPercentage: BigDecimal,
    val totalProcurementCommitment: BigDecimal,

    val kpiList: List<DecisionKpiItem> = emptyList(),
    val exceptionQueue: List<DecisionExceptionItem> = emptyList(),

    val isShadowAnalyticsDatabaseCreated: Boolean = false, // Critical Invariant: Always false!
    val generatedAt: String
)

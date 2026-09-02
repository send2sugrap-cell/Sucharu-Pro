package com.sucharu.sucharupro.domain.model.qc.analytics

/**
 * Severity levels for deterministic operational insights.
 */
enum class QcInsightSeverity {
    INFO,
    WARNING,
    CRITICAL
}

/**
 * Categorization of deterministic operational observations.
 */
enum class QcInsightType(val defaultLabel: String) {
    HIGH_QC_COST("High QC Cost"),
    HIGH_QC_TIME("High QC Time"),
    HIGH_DEFECT_RATE("High Defect Rate"),
    HIGH_REWORK_RATE("High Rework Rate"),
    HIGH_RE_QC_RATE("High Re-QC Rate"),
    COST_OVERRUN("Cost Overrun"),
    TIME_OVERRUN("Time Overrun"),
    REPEATED_FAILURE("Repeated Quality Failure"),
    STAGE_QUALITY_RISK("Stage Quality Risk"),
    IMPROVEMENT_OPPORTUNITY("Improvement Opportunity")
}

/**
 * Represents a deterministic, rule-evaluated operational insight.
 */
data class QcOperationalInsight(
    val id: String,
    val productionJobId: String? = null,
    val projectId: String? = null,
    val severity: QcInsightSeverity,
    val type: QcInsightType,
    val title: String,
    val description: String,
    val metricValue: Double,
    val thresholdValue: Double,
    val generatedAt: String
)

package com.sucharu.sucharupro.domain.model.qc.governance

/**
 * Result entity representing the deterministic evaluation of a KPI against target boundaries.
 */
data class QcKpiThreshold(
    val kpiType: QcGovernanceKpi,
    val currentValue: Double,
    val targetValue: Double,
    val minimumAcceptableValue: Double? = null,
    val maximumAcceptableValue: Double? = null,
    val warningThreshold: Double? = null,
    val criticalThreshold: Double? = null,
    val status: QcThresholdStatus,
    val severity: QcThresholdSeverity,
    val varianceFromTarget: Double = currentValue - targetValue,
    val message: String
)

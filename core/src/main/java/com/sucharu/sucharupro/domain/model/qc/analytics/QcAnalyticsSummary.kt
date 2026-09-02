package com.sucharu.sucharupro.domain.model.qc.analytics

/**
 * High-level aggregated QC operational KPI summary across jobs within an analytical period.
 */
data class QcAnalyticsSummary(
    val period: QcAnalyticsPeriod,
    val projectId: String? = null,
    val totalJobs: Int = 0,
    val totalQcCost: Double = 0.0,
    val totalQcTimeMinutes: Long = 0L,
    val averageQcCostPerJob: Double = 0.0,
    val averageQcTimeMinutesPerJob: Double = 0.0,
    val totalDefects: Int = 0,
    val averageDefectsPerJob: Double = 0.0,
    val totalReworks: Int = 0,
    val totalReQcCycles: Int = 0,
    val firstPassQcRate: Double = 0.0,
    val reworkRate: Double = 0.0,
    val reQcRate: Double = 0.0,
    val finalQcPassRate: Double = 0.0,
    val totalCostVariance: Double = 0.0,
    val totalTimeVariance: Long = 0L,
    val openDefectCount: Int = 0,
    val activeReworkCount: Int = 0,
    val failedReQcCount: Int = 0,
    val releasedJobCount: Int = 0
)

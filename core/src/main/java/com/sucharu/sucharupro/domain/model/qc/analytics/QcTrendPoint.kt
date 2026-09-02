package com.sucharu.sucharupro.domain.model.qc.analytics

/**
 * Time-series data point representing QC metrics over a specific time window.
 */
data class QcTrendPoint(
    val periodStart: String,
    val periodEnd: String,
    val totalQcCost: Double = 0.0,
    val totalQcTimeMinutes: Long = 0L,
    val defectCount: Int = 0,
    val reworkCount: Int = 0,
    val reQcCycleCount: Int = 0,
    val costVariance: Double = 0.0,
    val timeVariance: Long = 0L
)

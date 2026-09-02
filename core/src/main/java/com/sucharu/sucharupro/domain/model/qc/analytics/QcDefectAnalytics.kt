package com.sucharu.sucharupro.domain.model.qc.analytics

import com.sucharu.sucharupro.domain.model.qc.DefectCategory

/**
 * Analytical breakdown grouped by DefectCategory.
 */
data class QcDefectAnalytics(
    val defectCategory: DefectCategory,
    val defectCount: Int = 0,
    val affectedQuantity: Int = 0,
    val totalQcCost: Double = 0.0,
    val totalQcTimeMinutes: Long = 0L,
    val reworkCount: Int = 0,
    val reQcCycleCount: Int = 0,
    val averageResolutionTimeMinutes: Double = 0.0,
    val percentageOfTotalDefects: Double = 0.0
)

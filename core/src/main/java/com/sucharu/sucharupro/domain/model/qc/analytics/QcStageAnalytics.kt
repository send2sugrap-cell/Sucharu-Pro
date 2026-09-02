package com.sucharu.sucharupro.domain.model.qc.analytics

import com.sucharu.sucharupro.domain.model.production.ProductionStageType

/**
 * Analytical breakdown grouped by canonical ProductionStageType.
 */
data class QcStageAnalytics(
    val productionStage: ProductionStageType,
    val inspectionCount: Int = 0,
    val defectCount: Int = 0,
    val reworkCount: Int = 0,
    val reQcCount: Int = 0,
    val totalQcCost: Double = 0.0,
    val totalQcTimeMinutes: Long = 0L,
    val defectRate: Double = 0.0,
    val reworkRate: Double = 0.0,
    val averageQcTimeMinutes: Double = 0.0
)

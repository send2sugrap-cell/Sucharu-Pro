package com.sucharu.sucharupro.domain.model.job

import com.sucharu.sucharupro.domain.model.production.ProductionStageType

/**
 * Aggregated execution performance metrics for a specific canonical production stage type.
 */
data class ProductionStagePerformanceItem(
    val stageType: ProductionStageType,
    val totalExecutions: Int = 0,
    val completedCount: Int = 0,
    val skippedCount: Int = 0,
    val averageDurationSeconds: Long = 0L,
    val formattedAverageDuration: String = "0m",
    val totalOutputQuantity: Int = 0
)

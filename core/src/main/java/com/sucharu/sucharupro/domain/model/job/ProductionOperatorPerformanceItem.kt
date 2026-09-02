package com.sucharu.sucharupro.domain.model.job

/**
 * Derived production performance metrics for an operator across executed jobs and stages.
 */
data class ProductionOperatorPerformanceItem(
    val operatorId: String,
    val operatorName: String,
    val completedStageCount: Int = 0,
    val activeStageCount: Int = 0,
    val assignedStageCount: Int = 0,
    val totalExecutionSeconds: Long = 0L,
    val formattedTotalDuration: String = "0m",
    val averageExecutionSeconds: Long = 0L,
    val formattedAverageDuration: String = "0m",
    val outputQuantity: Int = 0,
    val urgentStageCount: Int = 0,
    val completedJobCount: Int = 0
)

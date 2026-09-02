package com.sucharu.sucharupro.domain.model.job

/**
 * Aggregated KPIs and statistical performance metrics for historical production execution.
 */
data class ProductionPerformanceMetrics(
    val totalHistoricalJobs: Int = 0,
    val completedJobs: Int = 0,
    val deliveredJobs: Int = 0,
    val cancelledJobs: Int = 0,
    val currentlyActiveJobs: Int = 0,
    val completionRate: Double = 0.0,
    val totalStageExecutions: Int = 0,
    val completedStages: Int = 0,
    val skippedStages: Int = 0,
    val averageStageDurationSeconds: Long = 0L,
    val formattedAverageStageDuration: String = "0m",
    val longestStageDurationSeconds: Long = 0L,
    val formattedLongestStageDuration: String = "0m",
    val shortestStageDurationSeconds: Long = 0L,
    val formattedShortestStageDuration: String = "0m",
    val plannedQuantity: Int = 0,
    val recordedOutput: Int = 0,
    val remainingQuantity: Int = 0,
    val outputCompletionRate: Double = 0.0,
    val operatorsInvolvedCount: Int = 0,
    val completedAssignments: Int = 0,
    val activeAssignments: Int = 0,
    val averageExecutionDurationSeconds: Long = 0L,
    val formattedAverageExecutionDuration: String = "0m"
)

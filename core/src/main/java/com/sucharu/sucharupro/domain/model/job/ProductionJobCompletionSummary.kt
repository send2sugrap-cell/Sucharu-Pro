package com.sucharu.sucharupro.domain.model.job

import com.sucharu.sucharupro.domain.model.order.OrderPriority

/**
 * Consolidated read-only completion and lifecycle summary for a single production job.
 */
data class ProductionJobCompletionSummary(
    val jobId: String,
    val jobNumber: String,
    val jobTitle: String,
    val orderId: String,
    val orderNumber: String,
    val customerId: String,
    val plannedQuantity: Int,
    val unit: String,
    val totalRecordedOutput: Int,
    val remainingQuantity: Int,
    val overallProgressFraction: Float,
    val finalStatus: ProductionJobStatus,
    val priority: OrderPriority,
    val createdAt: String,
    val completedAt: String? = null,
    val totalDurationSeconds: Long = 0L,
    val formattedTotalDuration: String = "0m",
    val operatorCount: Int = 0,
    val completedStageCount: Int = 0,
    val skippedStageCount: Int = 0,
    val executionCount: Int = 0,
    val outputRecordCount: Int = 0,
    val stageHistory: List<ProductionStageHistoryItem> = emptyList(),
    val recentActivities: List<ProductionActivityEvent> = emptyList()
)

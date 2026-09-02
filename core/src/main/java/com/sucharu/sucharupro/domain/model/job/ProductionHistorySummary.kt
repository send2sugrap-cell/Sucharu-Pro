package com.sucharu.sucharupro.domain.model.job

import com.sucharu.sucharupro.domain.model.order.OrderPriority

/**
 * Immutable historical summary record for a production job (Module 04 Step 08).
 */
data class ProductionHistorySummary(
    val jobId: String,
    val jobNumber: String,
    val orderId: String,
    val orderNumber: String,
    val customerId: String,
    val title: String,
    val quantity: Int,
    val unit: String,
    val priority: OrderPriority,
    val finalStatus: ProductionJobStatus,
    val createdAt: String,
    val updatedAt: String,
    val completedAt: String? = null,
    val totalDurationSeconds: Long = 0L,
    val formattedDuration: String = "0m",
    val completedStageCount: Int = 0,
    val skippedStageCount: Int = 0,
    val totalStageCount: Int = 13,
    val totalRecordedOutput: Int = 0,
    val remainingQuantity: Int = quantity,
    val overallProgressFraction: Float = 0f,
    val operatorCount: Int = 0,
    val executionCount: Int = 0,
    val outputRecordCount: Int = 0
)

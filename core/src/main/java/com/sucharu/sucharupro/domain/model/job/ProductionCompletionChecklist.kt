package com.sucharu.sucharupro.domain.model.job

import com.sucharu.sucharupro.domain.model.production.ProductionStageType

/**
 * Represents an individual check within the production completion readiness checklist.
 */
data class ProductionCompletionChecklistItem(
    val key: String,
    val title: String,
    val isPassed: Boolean,
    val message: String
)

/**
 * Pure derived model representing overall production completion readiness for a Job Card.
 */
data class ProductionCompletionChecklist(
    val jobId: String,
    val jobNumber: String,
    val isEligible: Boolean,
    val items: List<ProductionCompletionChecklistItem>,
    val blockingReasons: List<String> = emptyList(),
    val isOverProduced: Boolean = false,
    val overProductionQuantity: Int = 0
) {
    val passedCount: Int get() = items.count { it.isPassed }
    val totalCount: Int get() = items.size
}

/**
 * Immutable production-side handoff snapshot representing completed production work.
 */
data class ProductionReadyHandoff(
    val productionJobId: String,
    val jobNumber: String,
    val orderId: String,
    val orderNumber: String,
    val customerId: String,
    val title: String,
    val plannedQuantity: Int,
    val recordedQuantity: Int,
    val remainingQuantity: Int,
    val overProductionQuantity: Int,
    val unit: String,
    val completionPercentage: Double,
    val completedStageCount: Int,
    val skippedStageCount: Int,
    val totalStageCount: Int,
    val totalDurationSeconds: Long,
    val operatorCount: Int,
    val confirmedAt: String,
    val confirmedBy: String,
    val confirmedByName: String,
    val remarks: String? = null,
    val productionStatus: ProductionJobStatus = ProductionJobStatus.READY,
    val items: List<ProductionJobItem> = emptyList()
)

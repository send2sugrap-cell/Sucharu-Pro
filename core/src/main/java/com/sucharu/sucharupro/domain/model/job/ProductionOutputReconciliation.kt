package com.sucharu.sucharupro.domain.model.job

import com.sucharu.sucharupro.domain.model.production.ProductionStageType

/**
 * Immutable domain model representing item-level production output reconciliation.
 */
data class ProductionItemOutputReconciliation(
    val itemId: String,
    val description: String,
    val plannedQuantity: Int,
    val recordedQuantity: Int,
    val remainingQuantity: Int,
    val overProductionQuantity: Int,
    val underProductionQuantity: Int,
    val completionPercentage: Double,
    val unit: String
) {
    val isFullyProduced: Boolean get() = recordedQuantity >= plannedQuantity
    val isOverProduced: Boolean get() = overProductionQuantity > 0
}

/**
 * Immutable domain model representing stage-level production output reconciliation.
 */
data class ProductionStageOutputReconciliation(
    val stageId: String,
    val stageType: ProductionStageType,
    val plannedQuantity: Int,
    val recordedQuantity: Int,
    val remainingQuantity: Int,
    val overProductionQuantity: Int,
    val completionPercentage: Double,
    val outputCount: Int,
    val unit: String
) {
    val isFullyProduced: Boolean get() = recordedQuantity >= plannedQuantity
    val isOverProduced: Boolean get() = overProductionQuantity > 0
}

/**
 * Immutable domain model representing comprehensive production output reconciliation for a Job Card.
 */
data class ProductionOutputReconciliation(
    val jobId: String,
    val jobNumber: String,
    val plannedQuantity: Int,
    val recordedQuantity: Int,
    val remainingQuantity: Int,
    val overProductionQuantity: Int,
    val underProductionQuantity: Int,
    val completionPercentage: Double,
    val unit: String,
    val outputRecordCount: Int,
    val stageReconciliations: List<ProductionStageOutputReconciliation> = emptyList(),
    val itemReconciliations: List<ProductionItemOutputReconciliation> = emptyList()
) {
    val isFullyProduced: Boolean get() = recordedQuantity >= plannedQuantity
    val isOverProduced: Boolean get() = overProductionQuantity > 0

    val formattedCompletionPercentage: String
        get() = String.format(java.util.Locale.US, "%.1f%%", completionPercentage)
}

package com.sucharu.sucharupro.domain.model.job

import com.sucharu.sucharupro.domain.model.production.ProductionStageType

/**
 * Immutable domain model representing an operational production output record for a specific stage.
 */
data class ProductionStageOutput(
    val outputId: String,
    val jobId: String,
    val stageId: String,
    val stageType: ProductionStageType,
    val quantity: Int,
    val unit: String,
    val recordedAt: String,
    val operatorId: String? = null,
    val operatorName: String? = null,
    val recordedBy: String? = null,
    val recordedByName: String? = null,
    val executionId: String? = null,
    val remarks: String? = null
) {
    init {
        require(outputId.isNotBlank()) { "Output ID cannot be blank." }
        require(jobId.isNotBlank()) { "Job ID cannot be blank." }
        require(stageId.isNotBlank()) { "Stage ID cannot be blank." }
        require(quantity > 0) { "Output quantity must be greater than 0." }
        require(unit.isNotBlank()) { "Output unit cannot be blank." }
        require(recordedAt.isNotBlank()) { "Recorded timestamp cannot be blank." }
    }
}

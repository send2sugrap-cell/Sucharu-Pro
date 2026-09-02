package com.sucharu.sucharupro.domain.model.job

import com.sucharu.sucharupro.domain.model.production.ProductionStageStatus
import com.sucharu.sucharupro.domain.model.production.ProductionStageType

/**
 * Derived historical record of a single production stage execution.
 */
data class ProductionStageHistoryItem(
    val jobId: String,
    val jobNumber: String,
    val stageId: String,
    val stageType: ProductionStageType,
    val sequence: Int,
    val status: ProductionStageStatus,
    val operatorId: String? = null,
    val operatorName: String? = null,
    val assignmentStartedAt: String? = null,
    val executionStartedAt: String? = null,
    val executionCompletedAt: String? = null,
    val durationSeconds: Long = 0L,
    val formattedDuration: String? = null,
    val recordedOutputQuantity: Int = 0,
    val plannedQuantity: Int = 0,
    val progressFraction: Float = 0f,
    val remarks: String? = null
)

package com.sucharu.sucharupro.domain.model.job

import com.sucharu.sucharupro.domain.model.production.ProductionStageStatus
import com.sucharu.sucharupro.domain.model.production.ProductionStageType

/**
 * Represents the state and tracking metadata for a single production stage of a [ProductionJob].
 *
 * Each Job Card maintains the canonical 13 stages in strict sequential order.
 */
data class ProductionJobStage(
    val stageId: String,
    val jobId: String,
    val stageType: ProductionStageType,
    val sequence: Int,
    val status: ProductionStageStatus = ProductionStageStatus.PENDING,
    val startedAt: String? = null,
    val completedAt: String? = null,
    val assignedUserId: String? = null,
    val assignedUserName: String? = null,
    val notes: String? = null
) {
    val isPending: Boolean get() = status == ProductionStageStatus.PENDING
    val isInProgress: Boolean get() = status == ProductionStageStatus.IN_PROGRESS
    val isCompleted: Boolean get() = status == ProductionStageStatus.COMPLETED
    val isSkipped: Boolean get() = status == ProductionStageStatus.SKIPPED
    val isRework: Boolean get() = status == ProductionStageStatus.REWORK
    val isOnHold: Boolean get() = status == ProductionStageStatus.ON_HOLD

    companion object {
        /**
         * Generates the canonical list of 13 initial production stages in strict sequence for a [jobId],
         * with each stage initially in [ProductionStageStatus.PENDING].
         */
        fun createInitialStages(jobId: String): List<ProductionJobStage> {
            return ProductionStageType.orderedStages.map { stageType ->
                ProductionJobStage(
                    stageId = "${jobId}-stg-${stageType.displayOrder}",
                    jobId = jobId,
                    stageType = stageType,
                    sequence = stageType.displayOrder,
                    status = ProductionStageStatus.PENDING
                )
            }
        }
    }
}

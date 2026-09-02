package com.sucharu.sucharupro.domain.model.job

import com.sucharu.sucharupro.domain.model.production.ProductionStageStatus
import com.sucharu.sucharupro.domain.model.production.ProductionStageType

/**
 * Immutable domain model recording stage execution runtime history, duration, operator attribution, and remarks.
 */
data class ProductionStageExecution(
    val executionId: String,
    val jobId: String,
    val stageId: String,
    val stageType: ProductionStageType,
    val operatorId: String? = null,
    val operatorName: String? = null,
    val startedAt: String? = null,
    val completedAt: String? = null,
    val durationSeconds: Long? = null,
    val startRemarks: String? = null,
    val completionRemarks: String? = null,
    val status: ProductionStageStatus = ProductionStageStatus.PENDING,
    val createdAt: String
) {
    init {
        require(executionId.isNotBlank()) { "Execution ID cannot be blank." }
        require(jobId.isNotBlank()) { "Job ID cannot be blank." }
        require(stageId.isNotBlank()) { "Stage ID cannot be blank." }
        require(createdAt.isNotBlank()) { "Created timestamp cannot be blank." }
    }

    /**
     * Formatted duration string for UI display (e.g. "45 sec", "4 min 12 sec", "1 hr 25 min").
     */
    val formattedDuration: String?
        get() {
            val seconds = durationSeconds ?: return null
            if (seconds < 0) return null
            val hrs = seconds / 3600
            val mins = (seconds % 3600) / 60
            val secs = seconds % 60

            return when {
                hrs > 0 && mins > 0 -> "${hrs} hr ${mins} min"
                hrs > 0 -> "${hrs} hr"
                mins > 0 && secs > 0 -> "${mins} min ${secs} sec"
                mins > 0 -> "${mins} min"
                else -> "${secs} sec"
            }
        }
}

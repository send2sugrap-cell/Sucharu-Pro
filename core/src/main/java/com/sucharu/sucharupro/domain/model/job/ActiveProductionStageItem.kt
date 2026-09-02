package com.sucharu.sucharupro.domain.model.job

import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.production.ProductionStageStatus
import com.sucharu.sucharupro.domain.model.production.ProductionStageType

/**
 * Derived item representing an active, in-progress, or executable stage across all production jobs.
 */
data class ActiveProductionStageItem(
    val jobId: String,
    val jobNumber: String,
    val jobTitle: String,
    val orderNumber: String,
    val customerReference: String?,
    val stageId: String,
    val stageType: ProductionStageType,
    val sequence: Int,
    val stageStatus: ProductionStageStatus,
    val assignedOperatorId: String?,
    val assignedOperatorName: String?,
    val startedAt: String?,
    val durationFormatted: String?,
    val priority: OrderPriority,
    val progressFraction: Float = 0f
)

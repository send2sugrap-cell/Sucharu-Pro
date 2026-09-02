package com.sucharu.sucharupro.domain.model.job

import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.production.ProductionStageType

/**
 * Categorization of conditions requiring supervisor or production manager attention.
 */
enum class AttentionReasonType(val defaultLabel: String) {
    UNASSIGNED_ELIGIBLE_STAGE("Unassigned Stage"),
    WAITING_TO_START("Ready to Start"),
    ON_HOLD_JOB("Job On Hold"),
    URGENT_ACTIVE_JOB("Urgent Active Job"),
    READY_FOR_DELIVERY("Ready for Delivery"),
    ACTIVE_STAGE("In Progress")
}

/**
 * Derived item identifying an actionable exception or priority task requiring supervisor oversight.
 */
data class ProductionAttentionItem(
    val itemId: String,
    val reasonType: AttentionReasonType,
    val title: String,
    val description: String,
    val jobId: String,
    val jobNumber: String,
    val jobTitle: String? = null,
    val stageId: String? = null,
    val stageType: ProductionStageType? = null,
    val priority: OrderPriority = OrderPriority.NORMAL,
    val operatorName: String? = null
)

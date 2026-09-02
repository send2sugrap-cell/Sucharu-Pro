package com.sucharu.sucharupro.domain.model.job

import com.sucharu.sucharupro.domain.model.handoff.OrderJobHandoff
import com.sucharu.sucharupro.domain.model.order.DeliveryRequirement
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.production.ProductionStageStatus

/**
 * Master entity representing a Production Job Card in Sucharu Pro Printing ERP (Module 04).
 *
 * Represents an authoritative production instruction originating from a commercially sealed
 * [OrderJobHandoff]. Holds manufacturing-relevant item details, specifications, delivery requirements,
 * and tracks the canonical 13 production stages.
 */
data class ProductionJob(
    val jobId: String,
    val jobNumber: String,
    val orderId: String,
    val orderNumber: String,
    val customerId: String,
    val handoffId: String,
    val quotationId: String? = null,
    val approvedRevisionId: String? = null,
    val title: String,
    val description: String? = null,
    val priority: OrderPriority = OrderPriority.NORMAL,
    val status: ProductionJobStatus = ProductionJobStatus.READY_FOR_PRODUCTION,
    val quantity: Int,
    val unit: String = "Pcs",
    val specification: String? = null,
    val deliveryRequirement: DeliveryRequirement? = null,
    val items: List<ProductionJobItem> = emptyList(),
    val stages: List<ProductionJobStage> = ProductionJobStage.createInitialStages(jobId),
    val notes: String? = null,
    val createdAt: String,
    val createdBy: String? = null,
    val updatedAt: String,
    val updatedBy: String? = null
) {
    /**
     * Active current stage being worked on, or next pending stage in the sequence.
     */
    val currentStage: ProductionJobStage?
        get() = stages.find { it.status == ProductionStageStatus.IN_PROGRESS }
            ?: stages.find { it.status == ProductionStageStatus.PENDING }

    /**
     * Total count of stages that have finished execution (COMPLETED or SKIPPED).
     */
    val completedStagesCount: Int
        get() = stages.count { it.status == ProductionStageStatus.COMPLETED || it.status == ProductionStageStatus.SKIPPED }

    /**
     * Overall manufacturing progress fraction between 0.0f and 1.0f.
     */
    val progressFraction: Float
        get() = if (stages.isEmpty()) 0f else completedStagesCount.toFloat() / stages.size.toFloat()

    companion object {
        /**
         * Factory function to create an isolated [ProductionJob] from a validated [OrderJobHandoff].
         */
        fun fromHandoff(
            jobId: String,
            jobNumber: String,
            handoff: OrderJobHandoff,
            title: String? = null,
            description: String? = null,
            createdBy: String? = null,
            timestamp: String
        ): ProductionJob {
            val jobItems = handoff.items.map { ProductionJobItem.fromHandoffItem(it) }
            val derivedTitle = title
                ?: handoff.items.firstOrNull()?.description
                ?: "Job for Order ${handoff.orderNumber}"
            val derivedQuantity = if (handoff.items.isNotEmpty()) handoff.totalQuantity else 1
            val derivedUnit = handoff.items.firstOrNull()?.unit ?: "Pcs"
            val derivedSpecification = handoff.items.firstOrNull()?.specification

            return ProductionJob(
                jobId = jobId,
                jobNumber = jobNumber,
                orderId = handoff.orderId,
                orderNumber = handoff.orderNumber,
                customerId = handoff.customerId,
                handoffId = handoff.handoffId,
                quotationId = handoff.quotationId,
                approvedRevisionId = handoff.approvedRevisionId,
                title = derivedTitle,
                description = description,
                priority = handoff.priority,
                status = ProductionJobStatus.READY_FOR_PRODUCTION,
                quantity = derivedQuantity,
                unit = derivedUnit,
                specification = derivedSpecification,
                deliveryRequirement = handoff.deliveryRequirement,
                items = jobItems,
                stages = ProductionJobStage.createInitialStages(jobId),
                notes = handoff.notes,
                createdAt = timestamp,
                createdBy = createdBy,
                updatedAt = timestamp,
                updatedBy = createdBy
            )
        }
    }
}

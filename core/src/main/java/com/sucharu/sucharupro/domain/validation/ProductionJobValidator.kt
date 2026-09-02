package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.handoff.OrderJobHandoff
import com.sucharu.sucharupro.domain.model.handoff.OrderJobHandoffStatus
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.production.ProductionStageType

/**
 * Domain validator for [ProductionJob] and handoff-to-job conversion integrity in Sucharu Pro ERP.
 */
object ProductionJobValidator {

    /**
     * Validates domain integrity of a [ProductionJob] entity.
     */
    fun validateJob(job: ProductionJob): DomainResult<Unit> {
        if (job.jobId.isBlank()) {
            return DomainResult.Error(message = "Job ID cannot be blank.")
        }
        if (job.jobNumber.isBlank()) {
            return DomainResult.Error(message = "Job Number cannot be blank.")
        }
        if (job.orderId.isBlank()) {
            return DomainResult.Error(message = "Order ID cannot be blank.")
        }
        if (job.orderNumber.isBlank()) {
            return DomainResult.Error(message = "Order Number cannot be blank.")
        }
        if (job.customerId.isBlank()) {
            return DomainResult.Error(message = "Customer ID cannot be blank.")
        }
        if (job.handoffId.isBlank()) {
            return DomainResult.Error(message = "Handoff ID cannot be blank.")
        }
        if (job.title.isBlank()) {
            return DomainResult.Error(message = "Job title cannot be blank.")
        }
        if (job.quantity <= 0) {
            return DomainResult.Error(message = "Job quantity must be greater than zero (was ${job.quantity}).")
        }
        if (job.unit.isBlank()) {
            return DomainResult.Error(message = "Job unit cannot be blank.")
        }
        if (job.createdAt.isBlank()) {
            return DomainResult.Error(message = "Created timestamp cannot be blank.")
        }
        if (job.updatedAt.isBlank()) {
            return DomainResult.Error(message = "Updated timestamp cannot be blank.")
        }

        // Canonical stage integrity validation
        val stages = job.stages
        if (stages.size != ProductionStageType.TOTAL_STAGES) {
            return DomainResult.Error(
                message = "Job must contain exactly ${ProductionStageType.TOTAL_STAGES} canonical stages (found ${stages.size})."
            )
        }

        val stageTypes = stages.map { it.stageType }
        if (stageTypes.distinct().size != stages.size) {
            return DomainResult.Error(message = "Job stages must not contain duplicate stage types.")
        }

        val sequences = stages.map { it.sequence }
        val expectedSequences = (1..ProductionStageType.TOTAL_STAGES).toList()
        if (sequences != expectedSequences) {
            return DomainResult.Error(
                message = "Job stage sequences must be contiguous from 1 to ${ProductionStageType.TOTAL_STAGES}."
            )
        }

        for (stage in stages) {
            if (stage.sequence != stage.stageType.displayOrder) {
                return DomainResult.Error(
                    message = "Stage '${stage.stageType.defaultLabel}' has sequence ${stage.sequence}, expected ${stage.stageType.displayOrder}."
                )
            }
        }

        // Validate line items if present
        for (item in job.items) {
            if (item.itemId.isBlank()) {
                return DomainResult.Error(message = "Job item ID cannot be blank.")
            }
            if (item.description.isBlank()) {
                return DomainResult.Error(message = "Job item description cannot be blank.")
            }
            if (item.quantity <= 0) {
                return DomainResult.Error(message = "Job item quantity must be greater than zero.")
            }
            if (item.unit.isBlank()) {
                return DomainResult.Error(message = "Job item unit cannot be blank.")
            }
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates that an [OrderJobHandoff] is eligible for conversion to a [ProductionJob].
     */
    fun validateHandoffSource(handoff: OrderJobHandoff): DomainResult<Unit> {
        if (handoff.handoffId.isBlank()) {
            return DomainResult.Error(message = "Source Handoff ID cannot be blank.")
        }
        if (handoff.orderId.isBlank()) {
            return DomainResult.Error(message = "Source Order ID cannot be blank.")
        }
        if (handoff.customerId.isBlank()) {
            return DomainResult.Error(message = "Source Customer ID cannot be blank.")
        }
        if (handoff.handoffStatus == OrderJobHandoffStatus.CANCELLED) {
            return DomainResult.Error(message = "Cannot create Production Job from a cancelled handoff.")
        }
        if (handoff.items.isEmpty()) {
            return DomainResult.Error(message = "Source handoff must contain at least one item.")
        }
        return DomainResult.Success(Unit)
    }
}

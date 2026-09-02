package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStage
import com.sucharu.sucharupro.domain.model.job.ProductionStageOutput
import com.sucharu.sucharupro.domain.model.production.ProductionStageStatus

/**
 * Authoritative validator for operational production stage output recording (Module 04 Step 06).
 */
object ProductionStageOutputValidator {

    /**
     * Validates whether a new output record can be registered against [stageId] in [job].
     *
     * @param job Current [ProductionJob] entity.
     * @param stageId ID of the stage being processed.
     * @param existingOutputs All previously recorded valid outputs for this specific stage.
     * @param quantity New quantity to record.
     * @param unit Measurement unit for the output.
     */
    fun validateOutput(
        job: ProductionJob,
        stageId: String,
        existingOutputs: List<ProductionStageOutput>,
        quantity: Int,
        unit: String
    ): DomainResult<ProductionJobStage> {
        // Rule A & B: Locate the stage in Job
        if (stageId.isBlank()) {
            return DomainResult.Error(message = "Stage ID cannot be blank.")
        }
        val targetStage = job.stages.find { it.stageId == stageId }
            ?: return DomainResult.Error(message = "Stage '$stageId' does not exist on Job '${job.jobNumber}'.")

        // Rule F: Terminal Job Protection
        if (job.status.isTerminal) {
            return DomainResult.Error(
                message = "Cannot record output on ${job.status.defaultLabel} Job '${job.jobNumber}'."
            )
        }

        // Rule G: Stage Status Protection
        when (targetStage.status) {
            ProductionStageStatus.PENDING -> {
                return DomainResult.Error(
                    message = "Cannot record output for pending stage '${targetStage.stageType.defaultLabel}'. Start the stage first."
                )
            }
            ProductionStageStatus.SKIPPED -> {
                return DomainResult.Error(
                    message = "Cannot record output for skipped stage '${targetStage.stageType.defaultLabel}'."
                )
            }
            ProductionStageStatus.COMPLETED -> {
                return DomainResult.Error(
                    message = "Cannot record output for already completed stage '${targetStage.stageType.defaultLabel}'."
                )
            }
            ProductionStageStatus.IN_PROGRESS -> Unit
            else -> {
                return DomainResult.Error(
                    message = "Cannot record output for stage '${targetStage.stageType.defaultLabel}' in ${targetStage.status.defaultLabel} state."
                )
            }
        }

        // Rule C: Quantity must be positive
        if (quantity <= 0) {
            return DomainResult.Error(
                message = "Output quantity must be greater than 0. Provided: $quantity."
            )
        }

        // Rule D: Unit must be non-blank
        if (unit.isBlank()) {
            return DomainResult.Error(message = "Output unit cannot be blank.")
        }

        // Rule E: Planned Quantity constraint
        val currentAccumulated = existingOutputs
            .filter { it.jobId == job.jobId && it.stageId == stageId }
            .sumOf { it.quantity }
        val newTotal = currentAccumulated + quantity

        if (newTotal > job.quantity) {
            val remaining = (job.quantity - currentAccumulated).coerceAtLeast(0)
            return DomainResult.Error(
                message = "Accumulated output ($newTotal ${job.unit}) exceeds planned quantity (${job.quantity} ${job.unit}). Maximum remaining: $remaining ${job.unit}."
            )
        }

        return DomainResult.Success(targetStage)
    }

    /**
     * Calculates deterministic accumulated output quantity for a stage.
     */
    fun calculateTotalOutput(outputs: List<ProductionStageOutput>, jobId: String, stageId: String): Int {
        return outputs
            .filter { it.jobId == jobId && it.stageId == stageId }
            .sumOf { it.quantity }
    }

    /**
     * Calculates deterministic remaining output quantity for a stage.
     */
    fun calculateRemainingQuantity(plannedQuantity: Int, totalOutput: Int): Int {
        return (plannedQuantity - totalOutput).coerceAtLeast(0)
    }

    /**
     * Calculates deterministic stage output progress fraction in the range 0.0f..1.0f.
     */
    fun calculateProgressFraction(totalOutput: Int, plannedQuantity: Int): Float {
        if (plannedQuantity <= 0) return 0f
        val fraction = totalOutput.toFloat() / plannedQuantity.toFloat()
        return fraction.coerceIn(0f, 1f)
    }
}

package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStage
import com.sucharu.sucharupro.domain.model.job.ProductionStageExecution
import com.sucharu.sucharupro.domain.model.job.ProductionStageOutput
import com.sucharu.sucharupro.domain.model.production.ProductionStageStatus

/**
 * Authoritative validator for Production Output Recording (Module 04 Step 09).
 */
object ProductionOutputValidator {

    /**
     * Validates whether an operational output record can be registered against [stageId] in [job].
     *
     * @param job Target [ProductionJob] entity.
     * @param stageId ID of the stage being processed.
     * @param executions All recorded executions for the stage/job.
     * @param quantity Output quantity to record.
     * @param unit Output unit of measurement.
     */
    fun validateOutputRecord(
        job: ProductionJob?,
        stageId: String,
        executions: List<ProductionStageExecution> = emptyList(),
        quantity: Int,
        unit: String
    ): DomainResult<ProductionJobStage> {
        // Rule A: Target Production Job existence
        if (job == null) {
            return DomainResult.Error(message = "Target Production Job cannot be null.")
        }
        if (job.jobId.isBlank()) {
            return DomainResult.Error(message = "Job ID cannot be blank.")
        }

        // Rule B: Stage existence
        if (stageId.isBlank()) {
            return DomainResult.Error(message = "Stage ID cannot be blank.")
        }
        val targetStage = job.stages.find { it.stageId == stageId }
            ?: return DomainResult.Error(message = "Stage '$stageId' does not belong to Job '${job.jobNumber}'.")

        // Rule H: Terminal protection
        if (job.status.isTerminal) {
            return DomainResult.Error(
                message = "Cannot record output on ${job.status.defaultLabel} Job '${job.jobNumber}'."
            )
        }

        // Rule D: Stage Execution State
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

        // Rule E: Quantity must be > 0
        if (quantity <= 0) {
            return DomainResult.Error(
                message = "Output quantity must be greater than 0. Provided: $quantity."
            )
        }

        // Rule F: Unit must be non-blank
        if (unit.isBlank()) {
            return DomainResult.Error(message = "Output unit cannot be blank.")
        }

        return DomainResult.Success(targetStage)
    }
}

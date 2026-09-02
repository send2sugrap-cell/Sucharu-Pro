package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStage
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.production.ProductionStageStatus

/**
 * Authoritative validator for production stage progression, execution sequence,
 * start/completion prerequisites, and stage status transitions.
 */
object ProductionStageLifecycleValidator {

    /**
     * Validates that [stageId] in [job] is eligible to start execution.
     */
    fun validateStartStage(
        job: ProductionJob,
        stageId: String
    ): DomainResult<ProductionJobStage> {
        // 1. Check Job terminal & hold states
        if (job.status == ProductionJobStatus.DELIVERED || job.status == ProductionJobStatus.CANCELLED) {
            return DomainResult.Error(
                message = "Cannot start stage on a ${job.status.defaultLabel} job."
            )
        }
        if (job.status == ProductionJobStatus.ON_HOLD) {
            return DomainResult.Error(
                message = "Cannot start stage while job is On Hold. Resume the job first."
            )
        }

        // 2. Locate the stage
        val targetStage = job.stages.find { it.stageId == stageId }
            ?: return DomainResult.Error(message = "Stage with ID '$stageId' not found on Job '${job.jobNumber}'.")

        // 3. Check stage current status
        if (targetStage.status == ProductionStageStatus.IN_PROGRESS) {
            return DomainResult.Error(
                message = "Stage '${targetStage.stageType.defaultLabel}' is already in progress."
            )
        }
        if (targetStage.status == ProductionStageStatus.COMPLETED) {
            return DomainResult.Error(
                message = "Stage '${targetStage.stageType.defaultLabel}' is already completed."
            )
        }
        if (targetStage.status == ProductionStageStatus.SKIPPED) {
            return DomainResult.Error(
                message = "Stage '${targetStage.stageType.defaultLabel}' was skipped."
            )
        }

        // 4. Verify all predecessor stages are completed or skipped
        val predecessorIncomplete = job.stages
            .filter { it.sequence < targetStage.sequence }
            .firstOrNull { it.status != ProductionStageStatus.COMPLETED && it.status != ProductionStageStatus.SKIPPED }

        if (predecessorIncomplete != null) {
            return DomainResult.Error(
                message = "Cannot start '${targetStage.stageType.defaultLabel}'. Predecessor stage '${predecessorIncomplete.stageType.defaultLabel}' is ${predecessorIncomplete.status.defaultLabel}."
            )
        }

        return DomainResult.Success(targetStage)
    }

    /**
     * Validates that [stageId] in [job] can be marked as COMPLETED.
     */
    fun validateCompleteStage(
        job: ProductionJob,
        stageId: String
    ): DomainResult<ProductionJobStage> {
        // 1. Check Job terminal state
        if (job.status == ProductionJobStatus.DELIVERED || job.status == ProductionJobStatus.CANCELLED) {
            return DomainResult.Error(
                message = "Cannot complete stage on a ${job.status.defaultLabel} job."
            )
        }

        // 2. Locate the stage
        val targetStage = job.stages.find { it.stageId == stageId }
            ?: return DomainResult.Error(message = "Stage with ID '$stageId' not found on Job '${job.jobNumber}'.")

        // 3. Stage must be currently IN_PROGRESS
        if (targetStage.status != ProductionStageStatus.IN_PROGRESS) {
            return DomainResult.Error(
                message = "Cannot complete '${targetStage.stageType.defaultLabel}' because it is ${targetStage.status.defaultLabel} (Must be In Progress)."
            )
        }

        return DomainResult.Success(targetStage)
    }

    /**
     * Validates that [stageId] in [job] can be skipped if supported.
     */
    fun validateSkipStage(
        job: ProductionJob,
        stageId: String
    ): DomainResult<ProductionJobStage> {
        if (job.status.isTerminal) {
            return DomainResult.Error(message = "Cannot skip stage on a terminal job.")
        }

        val targetStage = job.stages.find { it.stageId == stageId }
            ?: return DomainResult.Error(message = "Stage not found.")

        if (!targetStage.stageType.canBeSkipped) {
            return DomainResult.Error(
                message = "Stage '${targetStage.stageType.defaultLabel}' is mandatory and cannot be skipped."
            )
        }

        if (targetStage.status == ProductionStageStatus.COMPLETED) {
            return DomainResult.Error(message = "Cannot skip already completed stage.")
        }

        val predecessorIncomplete = job.stages
            .filter { it.sequence < targetStage.sequence }
            .firstOrNull { it.status != ProductionStageStatus.COMPLETED && it.status != ProductionStageStatus.SKIPPED }

        if (predecessorIncomplete != null) {
            return DomainResult.Error(
                message = "Cannot skip stage until predecessor '${predecessorIncomplete.stageType.defaultLabel}' is finished."
            )
        }

        return DomainResult.Success(targetStage)
    }
}

package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStage
import com.sucharu.sucharupro.domain.model.job.ProductionStageAssignment
import com.sucharu.sucharupro.domain.model.production.ProductionStageStatus

/**
 * Authoritative validator for operator stage assignment, reassignment, and unassignment.
 */
object ProductionStageAssignmentValidator {

    /**
     * Validates operator assignment eligibility for a production stage.
     */
    fun validateAssignment(
        job: ProductionJob,
        stageId: String,
        operatorId: String,
        operatorName: String
    ): DomainResult<ProductionJobStage> {
        // 1. Terminal Job check
        if (job.status.isTerminal) {
            return DomainResult.Error(
                message = "Cannot assign operator to a ${job.status.defaultLabel} job."
            )
        }

        // 2. Stage existence check
        val stage = job.stages.find { it.stageId == stageId }
            ?: return DomainResult.Error(message = "Stage with ID '$stageId' not found on Job '${job.jobNumber}'.")

        // 3. Stage status check
        if (stage.status == ProductionStageStatus.COMPLETED) {
            return DomainResult.Error(
                message = "Cannot assign operator to completed stage '${stage.stageType.defaultLabel}'."
            )
        }
        if (stage.status == ProductionStageStatus.SKIPPED) {
            return DomainResult.Error(
                message = "Cannot assign operator to skipped stage '${stage.stageType.defaultLabel}'."
            )
        }

        // 4. Operator identity check
        if (operatorId.isBlank()) {
            return DomainResult.Error(message = "Operator ID cannot be blank.")
        }
        if (operatorName.isBlank()) {
            return DomainResult.Error(message = "Operator Name cannot be blank.")
        }

        return DomainResult.Success(stage)
    }

    /**
     * Validates operator reassignment eligibility.
     */
    fun validateReassignment(
        job: ProductionJob,
        stageId: String,
        currentAssignment: ProductionStageAssignment?,
        newOperatorId: String,
        newOperatorName: String
    ): DomainResult<ProductionJobStage> {
        val baseValidation = validateAssignment(job, stageId, newOperatorId, newOperatorName)
        if (baseValidation !is DomainResult.Success) {
            return baseValidation
        }

        if (currentAssignment == null || !currentAssignment.isActive) {
            return DomainResult.Error(
                message = "Cannot reassign: No active operator assignment found for stage '$stageId'."
            )
        }

        return baseValidation
    }

    /**
     * Validates unassignment eligibility.
     */
    fun validateUnassignment(
        job: ProductionJob,
        stageId: String,
        currentAssignment: ProductionStageAssignment?
    ): DomainResult<ProductionJobStage> {
        if (job.status.isTerminal) {
            return DomainResult.Error(
                message = "Cannot unassign operator from a ${job.status.defaultLabel} job."
            )
        }

        val stage = job.stages.find { it.stageId == stageId }
            ?: return DomainResult.Error(message = "Stage with ID '$stageId' not found.")

        if (stage.status == ProductionStageStatus.COMPLETED) {
            return DomainResult.Error(
                message = "Cannot unassign operator from completed stage '${stage.stageType.defaultLabel}'."
            )
        }
        if (stage.status == ProductionStageStatus.IN_PROGRESS) {
            return DomainResult.Error(
                message = "Cannot unassign operator while stage '${stage.stageType.defaultLabel}' is actively in progress."
            )
        }

        if (currentAssignment == null || !currentAssignment.isActive) {
            return DomainResult.Error(
                message = "No active operator assignment to remove for stage '$stageId'."
            )
        }

        return DomainResult.Success(stage)
    }
}

package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.production.ProductionStageStatus
import com.sucharu.sucharupro.domain.model.production.ProductionStageType

/**
 * Authoritative validator for [ProductionJob] lifecycle state transitions,
 * terminal state protection, hold/resume actions, and cancellation rules.
 */
object ProductionJobLifecycleValidator {

    /** Checks whether a job is in a terminal lifecycle state (DELIVERED or CANCELLED). */
    fun isTerminal(status: ProductionJobStatus): Boolean = status.isTerminal

    /** Checks whether a job is in a terminal lifecycle state. */
    fun isTerminal(job: ProductionJob): Boolean = isTerminal(job.status)

    /** Checks whether a job is operationally mutable. */
    fun isMutable(job: ProductionJob): Boolean = !isTerminal(job)

    /**
     * Validates whether [job] can transition to [targetStatus].
     */
    fun validateStatusTransition(
        job: ProductionJob,
        targetStatus: ProductionJobStatus
    ): DomainResult<Unit> {
        val currentStatus = job.status

        // 1. Reject self-transitions
        if (currentStatus == targetStatus) {
            return DomainResult.Error(
                message = "Job '${job.jobNumber}' is already in ${currentStatus.defaultLabel} state."
            )
        }

        // 2. Reject terminal state mutations
        if (currentStatus == ProductionJobStatus.DELIVERED) {
            return DomainResult.Error(
                message = "Delivered jobs cannot undergo status changes (Terminal state)."
            )
        }
        if (currentStatus == ProductionJobStatus.CANCELLED) {
            return DomainResult.Error(
                message = "Cancelled jobs cannot undergo status changes (Terminal state)."
            )
        }

        // 3. Validate transition matrix
        if (!currentStatus.canTransitionTo(targetStatus)) {
            return DomainResult.Error(
                message = "Cannot transition Job '${job.jobNumber}' from ${currentStatus.defaultLabel} to ${targetStatus.defaultLabel}."
            )
        }

        // 4. Special validation: IN_PROGRESS -> READY requires stages 1..11 to be completed/skipped
        if (targetStatus == ProductionJobStatus.READY) {
            val stagesBeforeReady = job.stages.filter { it.sequence < ProductionStageType.READY.displayOrder }
            val incompleteStages = stagesBeforeReady.filter {
                it.status != ProductionStageStatus.COMPLETED && it.status != ProductionStageStatus.SKIPPED
            }
            if (incompleteStages.isNotEmpty()) {
                val incompleteNames = incompleteStages.joinToString { it.stageType.defaultLabel }
                return DomainResult.Error(
                    message = "Cannot mark job as Ready. Incomplete stages: $incompleteNames."
                )
            }
        }

        // 5. Special validation: READY -> DELIVERED requires READY stage to be completed
        if (targetStatus == ProductionJobStatus.DELIVERED) {
            val readyStage = job.stages.find { it.stageType == ProductionStageType.READY }
            if (readyStage != null && readyStage.status != ProductionStageStatus.COMPLETED) {
                return DomainResult.Error(
                    message = "Cannot mark job as Delivered before Ready stage is completed."
                )
            }
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates that a job can be placed on hold.
     */
    fun validateHold(job: ProductionJob): DomainResult<Unit> {
        return validateStatusTransition(job, ProductionJobStatus.ON_HOLD)
    }

    /**
     * Validates that a job can be resumed from hold.
     */
    fun validateResume(job: ProductionJob): DomainResult<Unit> {
        if (job.status != ProductionJobStatus.ON_HOLD) {
            return DomainResult.Error(
                message = "Cannot resume Job '${job.jobNumber}' because it is not currently On Hold (Current: ${job.status.defaultLabel})."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates cancellation of a job, enforcing mandatory non-blank reason.
     */
    fun validateCancellation(
        job: ProductionJob,
        reason: String?
    ): DomainResult<Unit> {
        if (isTerminal(job)) {
            return DomainResult.Error(
                message = "Cannot cancel Job '${job.jobNumber}' because it is already in terminal state ${job.status.defaultLabel}."
            )
        }

        if (reason.isNullOrBlank()) {
            return DomainResult.Error(
                message = "Cancellation reason is required and cannot be blank."
            )
        }

        return DomainResult.Success(Unit)
    }
}

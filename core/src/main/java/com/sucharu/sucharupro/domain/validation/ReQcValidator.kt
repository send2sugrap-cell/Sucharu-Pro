package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.qc.ProductionDefect
import com.sucharu.sucharupro.domain.model.qc.ProductionQc
import com.sucharu.sucharupro.domain.model.qc.ProductionRework
import com.sucharu.sucharupro.domain.model.qc.QcInspectionChecklist
import com.sucharu.sucharupro.domain.model.qc.ReQcDecision
import com.sucharu.sucharupro.domain.model.qc.ReQcFailureReason
import com.sucharu.sucharupro.domain.model.qc.ReQcInspection
import com.sucharu.sucharupro.domain.model.qc.ReworkStatus
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Domain validator for [ReQcInspection] creation, mandatory fields, cross-job/project isolation,
 * quantity checks, and inspection outcome prerequisites (Module 06 Step 06).
 */
object ReQcValidator {

    val MANAGEMENT_ROLES = setOf(
        UserRole.ADMIN,
        UserRole.MANAGER
    )

    val OPERATIONAL_ROLES = setOf(
        UserRole.ADMIN,
        UserRole.MANAGER,
        UserRole.QC_INSPECTOR
    )

    /**
     * Validates RBAC permissions for creating or mutating Re-QC records.
     */
    fun validateMutationPermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole != null && callerRole !in OPERATIONAL_ROLES) {
            return DomainResult.Error(
                message = "User with role '${callerRole.defaultLabel}' is not authorized to manage Re-QC."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates Re-QC creation parameters before instantiating the model.
     */
    fun validateCreationParams(
        projectId: String,
        productionJobId: String,
        productionReworkId: String,
        cycleNumber: Int,
        createdBy: String,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<Unit> {
        val rbac = validateMutationPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        if (projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        if (productionJobId.isBlank()) {
            return DomainResult.Error(message = "Production Job ID cannot be blank.")
        }
        if (productionReworkId.isBlank()) {
            return DomainResult.Error(message = "Production Rework ID cannot be blank.")
        }
        if (cycleNumber < 1) {
            return DomainResult.Error(message = "Cycle number must be >= 1 (Received: $cycleNumber).")
        }
        if (createdBy.isBlank()) {
            return DomainResult.Error(message = "CreatedBy actor ID cannot be blank.")
        }
        if (timestamp.isBlank()) {
            return DomainResult.Error(message = "Timestamp cannot be blank.")
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates core field integrity of a [ReQcInspection].
     */
    fun validateReQc(reQc: ReQcInspection): DomainResult<Unit> {
        if (reQc.reQcId.isBlank()) {
            return DomainResult.Error(message = "Re-QC ID cannot be blank.")
        }
        if (reQc.projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        if (reQc.productionJobId.isBlank()) {
            return DomainResult.Error(message = "Production Job ID cannot be blank.")
        }
        if (reQc.productionReworkId.isBlank()) {
            return DomainResult.Error(message = "Production Rework ID cannot be blank.")
        }
        if (reQc.cycleNumber < 1) {
            return DomainResult.Error(message = "Cycle number must be >= 1 (${reQc.cycleNumber}).")
        }
        if (reQc.createdBy.isBlank()) {
            return DomainResult.Error(message = "CreatedBy actor ID cannot be blank.")
        }
        if (reQc.createdAt.isBlank()) {
            return DomainResult.Error(message = "CreatedAt timestamp cannot be blank.")
        }
        if (reQc.updatedAt.isBlank()) {
            return DomainResult.Error(message = "UpdatedAt timestamp cannot be blank.")
        }

        if (reQc.affectedQuantity != null && reQc.affectedQuantity < 0) {
            return DomainResult.Error(message = "Affected quantity cannot be negative (${reQc.affectedQuantity}).")
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates that source rework belongs to the same job and is in RETURNED_TO_QC state.
     */
    fun validateSourceRework(
        reQcJobId: String,
        rework: ProductionRework
    ): DomainResult<Unit> {
        if (rework.productionJobId != reQcJobId) {
            return DomainResult.Error(
                message = "Cross-job reference violation: Source rework '${rework.reworkId}' belongs to job '${rework.productionJobId}', not '$reQcJobId'."
            )
        }
        if (rework.status != ReworkStatus.RETURNED_TO_QC) {
            return DomainResult.Error(
                message = "Source rework '${rework.reworkId}' must be in 'RETURNED_TO_QC' status to initiate Re-QC (Current: ${rework.status.defaultLabel})."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates cross-job isolation for linked defect.
     */
    fun validateDefectCrossJobIsolation(
        reQcJobId: String,
        defect: ProductionDefect
    ): DomainResult<Unit> {
        if (defect.productionJobId != reQcJobId) {
            return DomainResult.Error(
                message = "Cross-job reference violation: Defect '${defect.defectId}' belongs to job '${defect.productionJobId}', not '$reQcJobId'."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates cross-job isolation for linked QC record.
     */
    fun validateQcCrossJobIsolation(
        reQcJobId: String,
        qc: ProductionQc
    ): DomainResult<Unit> {
        if (qc.productionJobId != reQcJobId) {
            return DomainResult.Error(
                message = "Cross-job reference violation: QC record '${qc.qcId}' belongs to job '${qc.productionJobId}', not '$reQcJobId'."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates cross-job isolation for linked checklist.
     */
    fun validateChecklistCrossJobIsolation(
        reQcJobId: String,
        checklist: QcInspectionChecklist
    ): DomainResult<Unit> {
        if (checklist.productionJobId != reQcJobId) {
            return DomainResult.Error(
                message = "Cross-job reference violation: Checklist '${checklist.inspectionChecklistId}' belongs to job '${checklist.productionJobId}', not '$reQcJobId'."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates cross-project isolation for job.
     */
    fun validateJobCrossProjectIsolation(
        reQcProjectId: String,
        job: ProductionJob
    ): DomainResult<Unit> {
        val jobMatchesProject = job.orderId == reQcProjectId ||
                job.customerId == reQcProjectId ||
                job.handoffId == reQcProjectId ||
                job.jobId == reQcProjectId

        if (!jobMatchesProject && reQcProjectId.isNotBlank() && job.orderId.isNotBlank()) {
            return DomainResult.Error(
                message = "Cross-project reference violation: Job '${job.jobId}' belongs to project/order '${job.orderId}', not '$reQcProjectId'."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates cross-project isolation for rework.
     */
    fun validateReworkCrossProjectIsolation(
        reQcProjectId: String,
        rework: ProductionRework
    ): DomainResult<Unit> {
        if (rework.projectId != reQcProjectId) {
            return DomainResult.Error(
                message = "Cross-project reference violation: Rework '${rework.reworkId}' belongs to project '${rework.projectId}', not '$reQcProjectId'."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates prerequisites for passing a Re-QC inspection.
     */
    fun validatePassPrerequisites(
        reQc: ReQcInspection,
        inspectorId: String?,
        callerRole: UserRole? = null
    ): DomainResult<Unit> {
        val rbac = validateMutationPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        if (reQc.startedAt.isNullOrBlank()) {
            return DomainResult.Error(
                message = "Cannot pass Re-QC '${reQc.reQcId}' because inspection has not been started."
            )
        }
        if (inspectorId.isNullOrBlank() && reQc.assignedInspectorId.isNullOrBlank()) {
            return DomainResult.Error(
                message = "Inspector identification is mandatory to pass Re-QC."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates prerequisites for failing a Re-QC inspection.
     */
    fun validateFailPrerequisites(
        reQc: ReQcInspection,
        failureReason: ReQcFailureReason?,
        failureNotes: String?,
        affectedQuantity: Int?,
        inspectorId: String?,
        callerRole: UserRole? = null
    ): DomainResult<Unit> {
        val rbac = validateMutationPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        if (reQc.startedAt.isNullOrBlank()) {
            return DomainResult.Error(
                message = "Cannot fail Re-QC '${reQc.reQcId}' because inspection has not been started."
            )
        }
        if (failureReason == null) {
            return DomainResult.Error(
                message = "Failure reason is mandatory when marking Re-QC as FAIL."
            )
        }
        if (failureNotes.isNullOrBlank()) {
            return DomainResult.Error(
                message = "Failure notes are mandatory when marking Re-QC as FAIL."
            )
        }
        if (affectedQuantity != null && affectedQuantity <= 0) {
            return DomainResult.Error(
                message = "Affected quantity must be greater than 0 when specified for failure ($affectedQuantity)."
            )
        }
        if (inspectorId.isNullOrBlank() && reQc.assignedInspectorId.isNullOrBlank()) {
            return DomainResult.Error(
                message = "Inspector identification is mandatory to record Re-QC failure."
            )
        }
        return DomainResult.Success(Unit)
    }
}

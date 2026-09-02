package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.FileReference
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.qc.ProductionDefect
import com.sucharu.sucharupro.domain.model.qc.ProductionQc
import com.sucharu.sucharupro.domain.model.qc.ProductionRework
import com.sucharu.sucharupro.domain.model.qc.QcInspectionChecklist
import com.sucharu.sucharupro.domain.model.qc.ReworkEvidence
import com.sucharu.sucharupro.domain.model.qc.ReworkStatus
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Domain validator for [ProductionRework] creation, integrity, RBAC, quantities,
 * cross-job/project isolation, completion, and evidence validation (Module 06 Step 05).
 */
object ProductionReworkValidator {

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
     * Validates RBAC permissions for creating or updating rework records.
     */
    fun validateMutationPermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole != null && callerRole !in OPERATIONAL_ROLES) {
            return DomainResult.Error(
                message = "User with role '${callerRole.defaultLabel}' is not authorized to manage QC rework."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates RBAC permissions for management review, approval, and rejection.
     * Enforces separation of duties (QC Inspector cannot approve management rework).
     */
    fun validateApprovalPermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole != null && callerRole !in MANAGEMENT_ROLES) {
            return DomainResult.Error(
                message = "User with role '${callerRole.defaultLabel}' is not authorized to approve or reject rework (Requires Admin or Manager)."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates rework creation parameters before instantiating the model.
     */
    fun validateCreationParams(
        projectId: String,
        productionJobId: String,
        affectedQuantity: Int,
        quantityUnit: String,
        description: String,
        requestedBy: String,
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
        if (affectedQuantity <= 0) {
            return DomainResult.Error(message = "Affected quantity must be greater than 0 ($affectedQuantity).")
        }
        if (quantityUnit.isBlank()) {
            return DomainResult.Error(message = "Quantity unit cannot be blank.")
        }
        if (description.isBlank()) {
            return DomainResult.Error(message = "Rework description cannot be blank.")
        }
        if (requestedBy.isBlank()) {
            return DomainResult.Error(message = "RequestedBy actor ID cannot be blank.")
        }
        if (timestamp.isBlank()) {
            return DomainResult.Error(message = "Timestamp cannot be blank.")
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates the core field integrity of a [ProductionRework].
     */
    fun validateRework(rework: ProductionRework): DomainResult<Unit> {
        if (rework.reworkId.isBlank()) {
            return DomainResult.Error(message = "Rework ID cannot be blank.")
        }
        if (rework.projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        if (rework.productionJobId.isBlank()) {
            return DomainResult.Error(message = "Production Job ID cannot be blank.")
        }
        if (rework.affectedQuantity <= 0) {
            return DomainResult.Error(message = "Affected quantity must be greater than 0 (${rework.affectedQuantity}).")
        }
        if (rework.quantityUnit.isBlank()) {
            return DomainResult.Error(message = "Quantity unit cannot be blank.")
        }
        if (rework.description.isBlank()) {
            return DomainResult.Error(message = "Rework description cannot be blank.")
        }
        if (rework.requestedBy.isBlank()) {
            return DomainResult.Error(message = "RequestedBy actor ID cannot be blank.")
        }
        if (rework.requestedAt.isBlank()) {
            return DomainResult.Error(message = "RequestedAt timestamp cannot be blank.")
        }
        if (rework.createdAt.isBlank()) {
            return DomainResult.Error(message = "CreatedAt timestamp cannot be blank.")
        }
        if (rework.updatedAt.isBlank()) {
            return DomainResult.Error(message = "UpdatedAt timestamp cannot be blank.")
        }

        if (rework.actualReworkedQuantity != null) {
            val qtyVal = validateQuantityBounds(rework.affectedQuantity, rework.actualReworkedQuantity)
            if (qtyVal is DomainResult.Error) return qtyVal
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates quantity rules:
     * 1. affectedQuantity must be > 0.
     * 2. actualReworkedQuantity cannot be negative.
     * 3. actualReworkedQuantity cannot exceed affectedQuantity.
     */
    fun validateQuantityBounds(affectedQuantity: Int, actualReworkedQuantity: Int): DomainResult<Unit> {
        if (affectedQuantity <= 0) {
            return DomainResult.Error(message = "Affected quantity must be greater than 0 ($affectedQuantity).")
        }
        if (actualReworkedQuantity < 0) {
            return DomainResult.Error(message = "Actual reworked quantity cannot be negative ($actualReworkedQuantity).")
        }
        if (actualReworkedQuantity > affectedQuantity) {
            return DomainResult.Error(
                message = "Actual reworked quantity ($actualReworkedQuantity) cannot exceed affected quantity ($affectedQuantity)."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates cross-job isolation when creating rework from a defect.
     */
    fun validateDefectCrossJobIsolation(
        reworkJobId: String,
        defect: ProductionDefect
    ): DomainResult<Unit> {
        if (defect.productionJobId != reworkJobId) {
            return DomainResult.Error(
                message = "Cross-job reference violation: Defect '${defect.defectId}' belongs to job '${defect.productionJobId}', not '$reworkJobId'."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates cross-project isolation when creating rework for a ProductionJob.
     */
    fun validateJobCrossProjectIsolation(
        reworkProjectId: String,
        job: ProductionJob
    ): DomainResult<Unit> {
        val jobMatchesProject = job.orderId == reworkProjectId ||
                job.customerId == reworkProjectId ||
                job.handoffId == reworkProjectId ||
                job.jobId == reworkProjectId

        if (!jobMatchesProject && reworkProjectId.isNotBlank() && job.orderId.isNotBlank()) {
            return DomainResult.Error(
                message = "Cross-project reference violation: Job '${job.jobId}' belongs to project/order '${job.orderId}', not '$reworkProjectId'."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates cross-job isolation when creating rework from a QC record.
     */
    fun validateQcCrossJobIsolation(
        reworkJobId: String,
        qc: ProductionQc
    ): DomainResult<Unit> {
        if (qc.productionJobId != reworkJobId) {
            return DomainResult.Error(
                message = "Cross-job reference violation: QC record '${qc.qcId}' belongs to job '${qc.productionJobId}', not '$reworkJobId'."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates cross-job isolation when creating rework from a checklist.
     */
    fun validateChecklistCrossJobIsolation(
        reworkJobId: String,
        checklist: QcInspectionChecklist
    ): DomainResult<Unit> {
        if (checklist.productionJobId != reworkJobId) {
            return DomainResult.Error(
                message = "Cross-job reference violation: Checklist '${checklist.inspectionChecklistId}' belongs to job '${checklist.productionJobId}', not '$reworkJobId'."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates prerequisites for completing rework.
     */
    fun validateCompletion(
        rework: ProductionRework,
        correctiveAction: String?,
        actualReworkedQuantity: Int?,
        completedBy: String?,
        callerRole: UserRole? = null
    ): DomainResult<Unit> {
        val rbac = validateMutationPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        if (rework.isTerminal) {
            return DomainResult.Error(
                message = "Cannot complete terminal rework '${rework.reworkId}' (Status: ${rework.status.defaultLabel})."
            )
        }

        if (rework.status != ReworkStatus.IN_PROGRESS) {
            return DomainResult.Error(
                message = "Cannot complete rework '${rework.reworkId}' because it is not in IN_PROGRESS status (Current: ${rework.status.defaultLabel})."
            )
        }

        if (correctiveAction.isNullOrBlank()) {
            return DomainResult.Error(message = "Corrective action description is mandatory when completing rework.")
        }

        if (actualReworkedQuantity == null) {
            return DomainResult.Error(message = "Actual reworked quantity is mandatory when completing rework.")
        }

        val qtyVal = validateQuantityBounds(rework.affectedQuantity, actualReworkedQuantity)
        if (qtyVal is DomainResult.Error) return qtyVal

        if (completedBy.isNullOrBlank()) {
            return DomainResult.Error(message = "CompletedBy identifier is mandatory when completing rework.")
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates prerequisites for returning rework to QC (handoff boundary).
     */
    fun validateReturnToQc(
        rework: ProductionRework,
        actorId: String?,
        callerRole: UserRole? = null
    ): DomainResult<Unit> {
        val rbac = validateMutationPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        if (rework.isTerminal) {
            return DomainResult.Error(
                message = "Cannot return terminal rework '${rework.reworkId}' to QC (Status: ${rework.status.defaultLabel})."
            )
        }

        if (rework.status != ReworkStatus.COMPLETED) {
            return DomainResult.Error(
                message = "Cannot return rework '${rework.reworkId}' to QC because it is not in COMPLETED status (Current: ${rework.status.defaultLabel})."
            )
        }

        if (actorId.isNullOrBlank()) {
            return DomainResult.Error(message = "Actor identifier is mandatory when returning rework to QC.")
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates attached evidence.
     */
    fun validateEvidence(evidence: ReworkEvidence, reworkId: String): DomainResult<Unit> {
        if (evidence.evidenceId.isBlank()) {
            return DomainResult.Error(message = "Evidence ID cannot be blank.")
        }
        if (evidence.reworkId != reworkId) {
            return DomainResult.Error(
                message = "Evidence rework ID '${evidence.reworkId}' does not match target rework '$reworkId'."
            )
        }
        if (evidence.createdBy.isBlank()) {
            return DomainResult.Error(message = "Evidence createdBy cannot be blank.")
        }
        if (evidence.createdAt.isBlank()) {
            return DomainResult.Error(message = "Evidence createdAt cannot be blank.")
        }
        if (evidence.fileReferenceId.isNullOrBlank() && evidence.fileReference == null && evidence.description.isNullOrBlank()) {
            return DomainResult.Error(message = "Evidence must contain a file reference or a description.")
        }

        evidence.fileReference?.let { file ->
            val fileVal = validateFileReference(file)
            if (fileVal is DomainResult.Error) return fileVal
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates supporting FileReference integrity.
     */
    fun validateFileReference(file: FileReference): DomainResult<Unit> {
        if (file.fileId.isBlank()) {
            return DomainResult.Error(message = "File ID cannot be blank.")
        }
        if (file.fileName.isBlank()) {
            return DomainResult.Error(message = "File name cannot be blank.")
        }
        if (file.mimeType.isBlank()) {
            return DomainResult.Error(message = "File MIME type cannot be blank.")
        }
        if (file.storagePath.isBlank()) {
            return DomainResult.Error(message = "File storage path cannot be blank.")
        }
        if (file.fileSize <= 0) {
            return DomainResult.Error(message = "File size must be greater than 0 (${file.fileSize}).")
        }
        return DomainResult.Success(Unit)
    }
}

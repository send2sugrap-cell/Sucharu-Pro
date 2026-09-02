package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.DefectEvidence
import com.sucharu.sucharupro.domain.model.qc.DefectStatus
import com.sucharu.sucharupro.domain.model.qc.ProductionDefect
import com.sucharu.sucharupro.domain.model.qc.ProductionQc
import com.sucharu.sucharupro.domain.model.qc.QcInspectionChecklist
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Domain validator for [ProductionDefect] creation, integrity, cross-job isolation, RBAC, and resolution/closure rules (Module 06 Step 04).
 */
object ProductionDefectValidator {

    val AUTHORIZED_DEFECT_ROLES = setOf(
        UserRole.ADMIN,
        UserRole.MANAGER,
        UserRole.QC_INSPECTOR
    )

    val AUTHORIZED_CLOSURE_ROLES = setOf(
        UserRole.ADMIN,
        UserRole.MANAGER
    )

    /**
     * Validates RBAC permissions for defect logging and updating.
     */
    fun validateDefectPermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole != null && callerRole !in AUTHORIZED_DEFECT_ROLES) {
            return DomainResult.Error(
                message = "User with role '${callerRole.defaultLabel}' is not authorized to manage QC defects."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates RBAC permissions for final defect closure.
     */
    fun validateClosurePermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole != null && callerRole !in AUTHORIZED_CLOSURE_ROLES) {
            return DomainResult.Error(
                message = "User with role '${callerRole.defaultLabel}' is not authorized to close QC defects (Requires Admin or Manager)."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates defect creation parameters before instantiating the model.
     */
    fun validateCreationParams(
        productionJobId: String,
        title: String,
        description: String,
        affectedQuantity: Int,
        affectedUnit: String,
        detectedBy: String,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<Unit> {
        val rbac = validateDefectPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        if (productionJobId.isBlank()) {
            return DomainResult.Error(message = "Production Job ID cannot be blank.")
        }
        if (title.isBlank()) {
            return DomainResult.Error(message = "Defect Title cannot be blank.")
        }
        if (description.isBlank()) {
            return DomainResult.Error(message = "Defect Description cannot be blank.")
        }
        if (affectedQuantity < 0) {
            return DomainResult.Error(message = "Affected quantity cannot be negative ($affectedQuantity).")
        }
        if (affectedUnit.isBlank()) {
            return DomainResult.Error(message = "Affected unit cannot be blank.")
        }
        if (detectedBy.isBlank()) {
            return DomainResult.Error(message = "DetectedBy actor ID cannot be blank.")
        }
        if (timestamp.isBlank()) {
            return DomainResult.Error(message = "Timestamp cannot be blank.")
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates the core field integrity of a [ProductionDefect].
     */
    fun validateDefect(defect: ProductionDefect): DomainResult<Unit> {
        if (defect.defectId.isBlank()) {
            return DomainResult.Error(message = "Defect ID cannot be blank.")
        }
        if (defect.productionJobId.isBlank()) {
            return DomainResult.Error(message = "Production Job ID cannot be blank.")
        }
        if (defect.title.isBlank()) {
            return DomainResult.Error(message = "Defect Title cannot be blank.")
        }
        if (defect.description.isBlank()) {
            return DomainResult.Error(message = "Defect Description cannot be blank.")
        }
        if (defect.affectedQuantity < 0) {
            return DomainResult.Error(message = "Affected quantity cannot be negative (${defect.affectedQuantity}).")
        }
        if (defect.affectedUnit.isBlank()) {
            return DomainResult.Error(message = "Affected unit cannot be blank.")
        }
        if (defect.detectedAt.isBlank()) {
            return DomainResult.Error(message = "DetectedAt timestamp cannot be blank.")
        }
        if (defect.detectedBy.isBlank()) {
            return DomainResult.Error(message = "DetectedBy actor ID cannot be blank.")
        }
        if (defect.createdAt.isBlank()) {
            return DomainResult.Error(message = "CreatedAt timestamp cannot be blank.")
        }
        if (defect.updatedAt.isBlank()) {
            return DomainResult.Error(message = "UpdatedAt timestamp cannot be blank.")
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates that attached evidence is valid and belongs to the given defect.
     */
    fun validateEvidence(evidence: DefectEvidence, defectId: String): DomainResult<Unit> {
        if (evidence.evidenceId.isBlank()) {
            return DomainResult.Error(message = "Evidence ID cannot be blank.")
        }
        if (evidence.defectId != defectId) {
            return DomainResult.Error(
                message = "Evidence defect ID '${evidence.defectId}' does not match target defect '$defectId'."
            )
        }
        if (evidence.createdBy.isBlank()) {
            return DomainResult.Error(message = "Evidence createdBy cannot be blank.")
        }
        if (evidence.createdAt.isBlank()) {
            return DomainResult.Error(message = "Evidence createdAt cannot be blank.")
        }
        if (evidence.fileReferenceId.isNullOrBlank() && evidence.description.isNullOrBlank()) {
            return DomainResult.Error(message = "Evidence must have either a file reference ID or a description.")
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates cross-job and cross-QC isolation when creating a defect from a QC record.
     */
    fun validateQcCrossJobIsolation(
        defectJobId: String,
        qc: ProductionQc
    ): DomainResult<Unit> {
        if (qc.productionJobId != defectJobId) {
            return DomainResult.Error(
                message = "Cross-job reference violation: QC record '${qc.qcId}' belongs to job '${qc.productionJobId}', not '$defectJobId'."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates cross-job and cross-checklist isolation when creating a defect from an inspection checklist.
     */
    fun validateChecklistCrossJobIsolation(
        defectJobId: String,
        checklist: QcInspectionChecklist
    ): DomainResult<Unit> {
        if (checklist.productionJobId != defectJobId) {
            return DomainResult.Error(
                message = "Cross-job reference violation: Checklist '${checklist.inspectionChecklistId}' belongs to job '${checklist.productionJobId}', not '$defectJobId'."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates prerequisites for marking a defect as RESOLVED.
     */
    fun validateResolution(
        defect: ProductionDefect,
        resolutionNotes: String?,
        resolvedBy: String?,
        callerRole: UserRole? = null
    ): DomainResult<Unit> {
        val rbac = validateDefectPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        if (defect.isTerminal) {
            return DomainResult.Error(
                message = "Cannot resolve terminal defect '${defect.defectId}' (Status: ${defect.status.defaultLabel})."
            )
        }

        if (resolutionNotes.isNullOrBlank()) {
            return DomainResult.Error(message = "Resolution notes are mandatory when resolving a defect.")
        }

        if (resolvedBy.isNullOrBlank()) {
            return DomainResult.Error(message = "ResolvedBy identifier is mandatory when resolving a defect.")
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates prerequisites for closing a defect.
     */
    fun validateClosure(
        defect: ProductionDefect,
        closedBy: String?,
        callerRole: UserRole? = null
    ): DomainResult<Unit> {
        val rbac = validateClosurePermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        if (defect.isTerminal) {
            return DomainResult.Error(
                message = "Defect '${defect.defectId}' is already in terminal status: ${defect.status.defaultLabel}."
            )
        }

        if (defect.status != DefectStatus.RESOLVED) {
            return DomainResult.Error(
                message = "Cannot close defect '${defect.defectId}' because it is not in RESOLVED status (Current: ${defect.status.defaultLabel})."
            )
        }

        if (closedBy.isNullOrBlank()) {
            return DomainResult.Error(message = "ClosedBy identifier is mandatory when closing a defect.")
        }

        return DomainResult.Success(Unit)
    }
}

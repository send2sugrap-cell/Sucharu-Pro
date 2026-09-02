package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ProductionQc
import com.sucharu.sucharupro.domain.model.qc.QcAssignment
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Authoritative validator for QC Inspector assignments, reassignments, and RBAC permissions (Module 06 Step 01).
 */
object QcAssignmentValidator {

    val AUTHORIZED_MANAGEMENT_ROLES = setOf(UserRole.ADMIN, UserRole.MANAGER)
    val AUTHORIZED_INSPECTION_ROLES = setOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.QC_INSPECTOR)

    /**
     * Validates whether a caller can manage QC assignments (ADMIN, MANAGER).
     */
    fun validateAssignmentPermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole != null && callerRole !in AUTHORIZED_MANAGEMENT_ROLES) {
            return DomainResult.Error(
                message = "User with role '${callerRole.defaultLabel}' is not authorized to assign or reassign QC inspectors."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates whether a caller can perform QC inspections (ADMIN, MANAGER, QC_INSPECTOR).
     */
    fun validateInspectionPermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole != null && callerRole !in AUTHORIZED_INSPECTION_ROLES) {
            return DomainResult.Error(
                message = "User with role '${callerRole.defaultLabel}' is not authorized to perform QC inspections."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates initial inspector assignment on a QC aggregate.
     */
    fun validateAssignment(
        qc: ProductionQc,
        inspectorId: String,
        inspectorName: String,
        callerRole: UserRole? = null
    ): DomainResult<Unit> {
        val rbacResult = validateAssignmentPermission(callerRole)
        if (rbacResult is DomainResult.Error) {
            return rbacResult
        }

        if (qc.isTerminal) {
            return DomainResult.Error(
                message = "Cannot assign inspector to terminal QC record '${qc.qcId}'."
            )
        }

        if (inspectorId.isBlank() || inspectorName.isBlank()) {
            return DomainResult.Error(message = "Inspector ID and Name cannot be blank.")
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates reassignment to a new inspector.
     */
    fun validateReassignment(
        qc: ProductionQc,
        currentAssignment: QcAssignment?,
        newInspectorId: String,
        newInspectorName: String,
        callerRole: UserRole? = null
    ): DomainResult<Unit> {
        val rbacResult = validateAssignmentPermission(callerRole)
        if (rbacResult is DomainResult.Error) {
            return rbacResult
        }

        if (qc.isTerminal) {
            return DomainResult.Error(
                message = "Cannot reassign inspector on terminal QC record '${qc.qcId}'."
            )
        }

        if (newInspectorId.isBlank() || newInspectorName.isBlank()) {
            return DomainResult.Error(message = "New Inspector ID and Name cannot be blank.")
        }

        if (currentAssignment != null && currentAssignment.inspectorId == newInspectorId) {
            return DomainResult.Error(
                message = "QC record is already assigned to inspector '$newInspectorName' ($newInspectorId)."
            )
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates unassignment of the active inspector.
     */
    fun validateUnassignment(
        qc: ProductionQc,
        callerRole: UserRole? = null
    ): DomainResult<Unit> {
        val rbacResult = validateAssignmentPermission(callerRole)
        if (rbacResult is DomainResult.Error) {
            return rbacResult
        }

        if (qc.isTerminal) {
            return DomainResult.Error(
                message = "Cannot unassign inspector from terminal QC record '${qc.qcId}'."
            )
        }

        if (!qc.isAssigned) {
            return DomainResult.Error(
                message = "QC record '${qc.qcId}' does not have an active inspector assignment to unassign."
            )
        }

        return DomainResult.Success(Unit)
    }
}

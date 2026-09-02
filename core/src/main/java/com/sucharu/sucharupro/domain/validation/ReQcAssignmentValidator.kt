package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ReQcInspection
import com.sucharu.sucharupro.domain.model.qc.ReQcStatus
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Domain validator for [ReQcInspection] assignment, reassignment, and unassignment (Module 06 Step 06).
 */
object ReQcAssignmentValidator {

    val AUTHORIZED_ASSIGNMENT_ROLES = setOf(
        UserRole.ADMIN,
        UserRole.MANAGER
    )

    val AUTHORIZED_EXECUTION_ROLES = setOf(
        UserRole.ADMIN,
        UserRole.MANAGER,
        UserRole.QC_INSPECTOR
    )

    /**
     * Validates management authorization for assigning or unassigning Re-QC.
     */
    fun validateAssignmentPermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole != null && callerRole !in AUTHORIZED_ASSIGNMENT_ROLES) {
            return DomainResult.Error(
                message = "User with role '${callerRole.defaultLabel}' is not authorized to assign Re-QC (Requires Admin or Manager)."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates role permissions for executing inspection (starting, recording results, passing, failing).
     */
    fun validateExecutionPermission(
        reQc: ReQcInspection,
        actorId: String?,
        callerRole: UserRole?
    ): DomainResult<Unit> {
        if (callerRole != null && callerRole !in AUTHORIZED_EXECUTION_ROLES) {
            return DomainResult.Error(
                message = "User with role '${callerRole.defaultLabel}' is not authorized to execute Re-QC."
            )
        }

        // If inspector role, check if they are the assigned inspector (if one is assigned)
        if (callerRole == UserRole.QC_INSPECTOR && !actorId.isNullOrBlank() && !reQc.assignedInspectorId.isNullOrBlank()) {
            if (reQc.assignedInspectorId != actorId) {
                return DomainResult.Error(
                    message = "QC Inspector '$actorId' cannot execute Re-QC '${reQc.reQcId}' assigned to inspector '${reQc.assignedInspectorId}'."
                )
            }
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates assignment parameters and state prerequisites.
     */
    fun validateAssignment(
        reQc: ReQcInspection,
        inspectorId: String,
        inspectorName: String,
        callerRole: UserRole? = null
    ): DomainResult<Unit> {
        val rbac = validateAssignmentPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        if (reQc.isTerminal) {
            return DomainResult.Error(
                message = "Cannot assign terminal Re-QC '${reQc.reQcId}' (Status: ${reQc.status.defaultLabel})."
            )
        }

        if (reQc.status == ReQcStatus.RETURNED_TO_REWORK) {
            return DomainResult.Error(
                message = "Cannot assign Re-QC '${reQc.reQcId}' after it has been returned to rework."
            )
        }

        if (reQc.status != ReQcStatus.PENDING && reQc.status != ReQcStatus.ASSIGNED) {
            return DomainResult.Error(
                message = "Cannot assign Re-QC '${reQc.reQcId}' in status '${reQc.status.defaultLabel}' (Must be PENDING or ASSIGNED)."
            )
        }

        if (inspectorId.isBlank()) {
            return DomainResult.Error(message = "Inspector ID cannot be blank.")
        }
        if (inspectorName.isBlank()) {
            return DomainResult.Error(message = "Inspector Name cannot be blank.")
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates unassignment prerequisites.
     */
    fun validateUnassignment(
        reQc: ReQcInspection,
        callerRole: UserRole? = null
    ): DomainResult<Unit> {
        val rbac = validateAssignmentPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        if (reQc.isTerminal) {
            return DomainResult.Error(
                message = "Cannot unassign terminal Re-QC '${reQc.reQcId}' (Status: ${reQc.status.defaultLabel})."
            )
        }

        if (reQc.status == ReQcStatus.RETURNED_TO_REWORK) {
            return DomainResult.Error(
                message = "Cannot unassign Re-QC '${reQc.reQcId}' after it has been returned to rework."
            )
        }

        if (!reQc.isAssigned) {
            return DomainResult.Error(
                message = "Re-QC '${reQc.reQcId}' does not have an active assignment to unassign."
            )
        }

        if (reQc.status == ReQcStatus.IN_INSPECTION || reQc.status == ReQcStatus.FAILED) {
            return DomainResult.Error(
                message = "Cannot unassign Re-QC '${reQc.reQcId}' while in status '${reQc.status.defaultLabel}'."
            )
        }

        return DomainResult.Success(Unit)
    }
}

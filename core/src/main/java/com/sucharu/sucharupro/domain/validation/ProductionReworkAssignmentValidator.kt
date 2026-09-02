package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ProductionRework
import com.sucharu.sucharupro.domain.model.qc.ReworkStatus
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Domain validator for [ProductionRework] assignment, reassignment, and unassignment (Module 06 Step 05).
 */
object ProductionReworkAssignmentValidator {

    val AUTHORIZED_ASSIGNMENT_ROLES = setOf(
        UserRole.ADMIN,
        UserRole.MANAGER
    )

    /**
     * Validates management authorization for assigning rework.
     */
    fun validateAssignmentPermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole != null && callerRole !in AUTHORIZED_ASSIGNMENT_ROLES) {
            return DomainResult.Error(
                message = "User with role '${callerRole.defaultLabel}' is not authorized to assign rework (Requires Admin or Manager)."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates assignment parameters and state prerequisites.
     */
    fun validateAssignment(
        rework: ProductionRework,
        assigneeId: String,
        assigneeName: String,
        callerRole: UserRole? = null
    ): DomainResult<Unit> {
        val rbac = validateAssignmentPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        if (rework.isTerminal) {
            return DomainResult.Error(
                message = "Cannot assign terminal rework '${rework.reworkId}' (Status: ${rework.status.defaultLabel})."
            )
        }

        if (rework.isReturnedToQc) {
            return DomainResult.Error(
                message = "Cannot assign rework '${rework.reworkId}' after it has been returned to QC."
            )
        }

        if (rework.status != ReworkStatus.APPROVED && rework.status != ReworkStatus.ASSIGNED) {
            return DomainResult.Error(
                message = "Cannot assign rework '${rework.reworkId}' in status '${rework.status.defaultLabel}' (Must be APPROVED or ASSIGNED)."
            )
        }

        if (assigneeId.isBlank()) {
            return DomainResult.Error(message = "Assignee ID cannot be blank.")
        }
        if (assigneeName.isBlank()) {
            return DomainResult.Error(message = "Assignee Name cannot be blank.")
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates unassignment prerequisites.
     */
    fun validateUnassignment(
        rework: ProductionRework,
        callerRole: UserRole? = null
    ): DomainResult<Unit> {
        val rbac = validateAssignmentPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        if (rework.isTerminal) {
            return DomainResult.Error(
                message = "Cannot unassign terminal rework '${rework.reworkId}' (Status: ${rework.status.defaultLabel})."
            )
        }

        if (rework.isReturnedToQc) {
            return DomainResult.Error(
                message = "Cannot unassign rework '${rework.reworkId}' after it has been returned to QC."
            )
        }

        if (!rework.isAssigned) {
            return DomainResult.Error(
                message = "Rework '${rework.reworkId}' does not have an active assignment to unassign."
            )
        }

        if (rework.status == ReworkStatus.IN_PROGRESS || rework.status == ReworkStatus.COMPLETED) {
            return DomainResult.Error(
                message = "Cannot unassign rework '${rework.reworkId}' while in status '${rework.status.defaultLabel}'."
            )
        }

        return DomainResult.Success(Unit)
    }
}

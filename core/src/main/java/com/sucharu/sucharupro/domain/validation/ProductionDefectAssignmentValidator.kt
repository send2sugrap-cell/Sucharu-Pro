package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ProductionDefect
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Domain validator for [ProductionDefect] assignment, reassignment, and unassignment (Module 06 Step 04).
 */
object ProductionDefectAssignmentValidator {

    val AUTHORIZED_ASSIGNMENT_ROLES = setOf(
        UserRole.ADMIN,
        UserRole.MANAGER,
        UserRole.QC_INSPECTOR
    )

    fun validateAssignmentPermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole != null && callerRole !in AUTHORIZED_ASSIGNMENT_ROLES) {
            return DomainResult.Error(
                message = "User with role '${callerRole.defaultLabel}' is not authorized to assign QC defects."
            )
        }
        return DomainResult.Success(Unit)
    }

    fun validateAssignment(
        defect: ProductionDefect,
        assigneeId: String,
        assigneeName: String,
        callerRole: UserRole? = null
    ): DomainResult<Unit> {
        val rbac = validateAssignmentPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        if (defect.isTerminal) {
            return DomainResult.Error(
                message = "Cannot assign terminal defect '${defect.defectId}' (Status: ${defect.status.defaultLabel})."
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

    fun validateUnassignment(
        defect: ProductionDefect,
        callerRole: UserRole? = null
    ): DomainResult<Unit> {
        val rbac = validateAssignmentPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        if (defect.isTerminal) {
            return DomainResult.Error(
                message = "Cannot unassign terminal defect '${defect.defectId}' (Status: ${defect.status.defaultLabel})."
            )
        }

        if (!defect.isAssigned) {
            return DomainResult.Error(
                message = "Defect '${defect.defectId}' does not have an active assignment to unassign."
            )
        }

        return DomainResult.Success(Unit)
    }
}

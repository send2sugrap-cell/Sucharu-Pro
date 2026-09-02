package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.design.DesignAssignment
import com.sucharu.sucharupro.domain.model.design.DesignProject
import com.sucharu.sucharupro.domain.model.design.DesignStatus
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Authoritative validator for Designer assignment, reassignment, and unassignment.
 */
object DesignAssignmentValidator {

    /** Roles authorized to assign/reassign/unassign designers. */
    private val AUTHORIZED_ASSIGNMENT_ROLES = setOf(UserRole.ADMIN, UserRole.MANAGER)

    /**
     * Validates whether a caller with [callerRole] is authorized to perform designer assignments.
     */
    fun validateAssignmentPermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole != null && callerRole !in AUTHORIZED_ASSIGNMENT_ROLES) {
            return DomainResult.Error(
                message = "User with role '${callerRole.defaultLabel}' is not authorized to manage designer assignments."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates designer assignment eligibility for a [DesignProject].
     */
    fun validateAssignment(
        project: DesignProject,
        designerId: String,
        designerName: String,
        callerRole: UserRole? = null
    ): DomainResult<Unit> {
        // 1. RBAC Check
        val rbacResult = validateAssignmentPermission(callerRole)
        if (rbacResult !is DomainResult.Success) {
            return rbacResult
        }

        // 2. Terminal Project Check
        if (project.isTerminal) {
            return DomainResult.Error(
                message = "Cannot assign designer to a ${project.status.defaultLabel} design project."
            )
        }

        // 3. Status eligibility check
        if (project.status != DesignStatus.NOT_STARTED &&
            project.status != DesignStatus.ASSIGNED &&
            project.status != DesignStatus.REVISION_REQUIRED &&
            project.status != DesignStatus.IN_DESIGN
        ) {
            return DomainResult.Error(
                message = "Cannot assign designer when project is in '${project.status.defaultLabel}' state."
            )
        }

        // 4. Designer identity check
        if (designerId.isBlank()) {
            return DomainResult.Error(message = "Designer ID cannot be blank.")
        }
        if (designerName.isBlank()) {
            return DomainResult.Error(message = "Designer Name cannot be blank.")
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates designer reassignment eligibility.
     */
    fun validateReassignment(
        project: DesignProject,
        currentAssignment: DesignAssignment?,
        newDesignerId: String,
        newDesignerName: String,
        callerRole: UserRole? = null
    ): DomainResult<Unit> {
        val baseValidation = validateAssignment(project, newDesignerId, newDesignerName, callerRole)
        if (baseValidation !is DomainResult.Success) {
            return baseValidation
        }

        if (currentAssignment == null || !currentAssignment.isActive) {
            return DomainResult.Error(
                message = "Cannot reassign: No active designer assignment found for project '${project.projectNumber}'."
            )
        }

        if (currentAssignment.designerId == newDesignerId) {
            return DomainResult.Error(
                message = "Designer '${newDesignerName}' is already actively assigned to this project."
            )
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates designer unassignment eligibility.
     */
    fun validateUnassignment(
        project: DesignProject,
        currentAssignment: DesignAssignment?,
        callerRole: UserRole? = null
    ): DomainResult<Unit> {
        // 1. RBAC Check
        val rbacResult = validateAssignmentPermission(callerRole)
        if (rbacResult !is DomainResult.Success) {
            return rbacResult
        }

        // 2. Terminal Project Check
        if (project.isTerminal) {
            return DomainResult.Error(
                message = "Cannot unassign designer from a ${project.status.defaultLabel} design project."
            )
        }

        // 3. In-progress check: Cannot unassign while actively in design without holding/changing state
        if (project.status == DesignStatus.IN_DESIGN) {
            return DomainResult.Error(
                message = "Cannot unassign designer while project is actively in design. Change status first or reassign directly."
            )
        }

        // 4. Active assignment existence check
        if (currentAssignment == null || !currentAssignment.isActive) {
            return DomainResult.Error(
                message = "No active designer assignment to remove for project '${project.projectNumber}'."
            )
        }

        return DomainResult.Success(Unit)
    }
}

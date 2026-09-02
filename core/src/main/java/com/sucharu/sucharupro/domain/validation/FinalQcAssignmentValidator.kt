package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.FinalQcInspection
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Domain validator enforcing Role-Based Access Control (RBAC) and Separation of Duties for Final QC & Production Release (Module 06 Step 07).
 */
object FinalQcAssignmentValidator {

    /**
     * Validates permission to create a Final QC inspection record.
     */
    fun validateCreatePermission(role: UserRole?): DomainResult<Unit> {
        if (role == null) return DomainResult.Success(Unit)
        return when (role) {
            UserRole.ADMIN,
            UserRole.MANAGER,
            UserRole.QC_INSPECTOR -> DomainResult.Success(Unit)
            else -> DomainResult.Error(message = "Role '$role' is not authorized to create Final QC inspection records.")
        }
    }

    /**
     * Validates permission to assign, reassign, or unassign inspectors for Final QC.
     */
    fun validateAssignmentPermission(role: UserRole?): DomainResult<Unit> {
        if (role == null) return DomainResult.Success(Unit)
        return when (role) {
            UserRole.ADMIN,
            UserRole.MANAGER -> DomainResult.Success(Unit)
            else -> DomainResult.Error(message = "Role '$role' is not authorized to assign or manage Final QC inspector assignments.")
        }
    }

    /**
     * Validates permission to start or execute a Final QC inspection.
     */
    fun validateInspectionExecutionPermission(
        role: UserRole?,
        inspection: FinalQcInspection,
        actorId: String
    ): DomainResult<Unit> {
        if (role == null) return DomainResult.Success(Unit)
        when (role) {
            UserRole.ADMIN,
            UserRole.MANAGER -> return DomainResult.Success(Unit)
            UserRole.QC_INSPECTOR -> {
                if (inspection.isAssigned && inspection.assignedInspectorId != actorId) {
                    return DomainResult.Error(
                        message = "Final QC is assigned to inspector ${inspection.assignedInspectorId}. Inspector $actorId is not authorized to execute it."
                    )
                }
                return DomainResult.Success(Unit)
            }
            else -> return DomainResult.Error(message = "Role '$role' is not authorized to perform Final QC inspections.")
        }
    }

    /**
     * Validates permission to formally authorize production release (Separation of Duties).
     *
     * Notice: [UserRole.QC_INSPECTOR] is intentionally EXCLUDED from release authorization.
     * Only [UserRole.ADMIN] and [UserRole.MANAGER] hold release authority.
     */
    fun validateReleaseAuthorizationPermission(role: UserRole?): DomainResult<Unit> {
        if (role == null) return DomainResult.Success(Unit)
        return when (role) {
            UserRole.ADMIN,
            UserRole.MANAGER -> DomainResult.Success(Unit)
            UserRole.QC_INSPECTOR -> DomainResult.Error(
                message = "Separation of duties violation: QC Inspectors cannot authorize production release. Management approval (ADMIN or MANAGER) is required."
            )
            else -> DomainResult.Error(message = "Role '$role' is not authorized to authorize production release.")
        }
    }

    /**
     * Validates permission to cancel a Final QC inspection.
     */
    fun validateCancelPermission(role: UserRole?): DomainResult<Unit> {
        if (role == null) return DomainResult.Success(Unit)
        return when (role) {
            UserRole.ADMIN,
            UserRole.MANAGER -> DomainResult.Success(Unit)
            else -> DomainResult.Error(message = "Role '$role' is not authorized to cancel Final QC records.")
        }
    }
}

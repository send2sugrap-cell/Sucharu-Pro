package com.sucharu.sucharupro.domain.validation.task

import com.sucharu.sucharupro.data.model.task.Task
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * RBAC authorization validator for task management operations in Sucharu Pro ERP.
 */
object TaskAuthorizationValidator {

    /**
     * Validates that caller role is an internal staff user authorized to interact with tasks.
     * Prohibits external actors (CUSTOMER, VENDOR) unconditionally.
     */
    fun validateInternalUser(callerRole: UserRole): Result<Unit> {
        if (!callerRole.isInternal) {
            return Result.failure(
                SecurityException("External actor role '${callerRole.name}' is prohibited from accessing staff tasks.")
            )
        }
        return Result.success(Unit)
    }

    /**
     * Validates task creation permissions.
     */
    fun validateCreateTask(callerRole: UserRole): Result<Unit> {
        val userCheck = validateInternalUser(callerRole)
        if (userCheck.isFailure) return userCheck

        // Internal staff roles can create tasks
        return Result.success(Unit)
    }

    /**
     * Validates task assignment / reassignment permissions.
     */
    fun validateAssignTask(callerRole: UserRole, isReassignment: Boolean = false): Result<Unit> {
        val userCheck = validateInternalUser(callerRole)
        if (userCheck.isFailure) return userCheck

        // Only ADMIN, MANAGER, and STAFF can assign/reassign
        if (callerRole != UserRole.ADMIN && callerRole != UserRole.MANAGER && callerRole != UserRole.STAFF) {
            val action = if (isReassignment) "reassign" else "assign"
            return Result.failure(
                SecurityException("Role '${callerRole.name}' is not authorized to $action tasks.")
            )
        }
        return Result.success(Unit)
    }

    /**
     * Validates task verification & closure permissions (Separation of duties).
     * Creator cannot verify their own task unless caller is ADMIN.
     */
    fun validateVerifyOrCloseTask(
        task: Task,
        actorUserId: String,
        callerRole: UserRole
    ): Result<Unit> {
        val userCheck = validateInternalUser(callerRole)
        if (userCheck.isFailure) return userCheck

        if (callerRole == UserRole.ADMIN) return Result.success(Unit)

        // Manager or QC inspector can verify/close
        if (callerRole != UserRole.MANAGER && callerRole != UserRole.QC_INSPECTOR) {
            return Result.failure(
                SecurityException("Role '${callerRole.name}' is not authorized to verify or close tasks.")
            )
        }

        // Separation of duties: creator != verifier
        if (task.createdBy == actorUserId) {
            return Result.failure(
                SecurityException("Separation of duties violation: Creator '$actorUserId' cannot verify their own task without Admin privilege.")
            )
        }

        return Result.success(Unit)
    }

    /**
     * Validates task view access visibility based on role and assignee isolation.
     */
    fun validateViewTask(
        task: Task,
        requestProjectId: String,
        actorUserId: String,
        callerRole: UserRole
    ): Result<Unit> {
        // Enforce strict project isolation
        if (task.projectId != requestProjectId) {
            return Result.failure(
                SecurityException("Cross-project isolation error: Task project '${task.projectId}' does not match request project '$requestProjectId'.")
            )
        }

        val userCheck = validateInternalUser(callerRole)
        if (userCheck.isFailure) return userCheck

        // ADMIN and MANAGER can view all tasks in project
        if (callerRole == UserRole.ADMIN || callerRole == UserRole.MANAGER) {
            return Result.success(Unit)
        }

        // STAFF/DESIGNER/QC/ACCOUNTS/WAREHOUSE can view if assigned to self, created by self, or public team task
        val isAssignedToSelf = task.assignedTo == actorUserId
        val isCreatedBySelf = task.createdBy == actorUserId
        val isTeamTask = task.assignedTo == null && task.teamId != null

        if (!isAssignedToSelf && !isCreatedBySelf && !isTeamTask) {
            return Result.failure(
                SecurityException("Assignee isolation violation: User '$actorUserId' with role '${callerRole.name}' is not authorized to view task '${task.taskId}'.")
            )
        }

        return Result.success(Unit)
    }
}

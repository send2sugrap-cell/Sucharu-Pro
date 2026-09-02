package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Role-Based Access Control (RBAC) validator for Dispatch Execution operations (Module 08 Step 03).
 */
object DispatchExecutionAuthorizationValidator {

    fun validateOperation(
        callerRole: UserRole?,
        operation: DispatchExecutionOperation,
        targetProjectId: String,
        callerProjectId: String? = null
    ): DomainResult<Unit> {
        if (callerRole == null) {
            return DomainResult.Error(message = "Caller role must be provided for authorization.")
        }

        // 1. Project Scoping Enforcement
        if (callerProjectId != null && callerProjectId != targetProjectId) {
            return DomainResult.Error(
                message = "Unauthorized: Access denied for project '$targetProjectId'."
            )
        }

        // 2. Role Permissions
        return when (operation) {
            DispatchExecutionOperation.VIEW -> {
                if (callerRole.isInternal) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "Role '$callerRole' is not authorized to view dispatch executions.")
                }
            }

            DispatchExecutionOperation.CREATE,
            DispatchExecutionOperation.EDIT,
            DispatchExecutionOperation.SUBMIT,
            DispatchExecutionOperation.APPROVE,
            DispatchExecutionOperation.CANCEL -> {
                when (callerRole) {
                    UserRole.ADMIN,
                    UserRole.MANAGER -> DomainResult.Success(Unit)
                    else -> DomainResult.Error(
                        message = "Role '$callerRole' is not authorized to perform '$operation' on dispatch executions."
                    )
                }
            }

            DispatchExecutionOperation.PREPARE,
            DispatchExecutionOperation.EXECUTE_DISPATCH -> {
                when (callerRole) {
                    UserRole.ADMIN,
                    UserRole.MANAGER,
                    UserRole.WAREHOUSE -> DomainResult.Success(Unit)
                    else -> DomainResult.Error(
                        message = "Role '$callerRole' is not authorized to perform '$operation' on dispatch executions."
                    )
                }
            }
        }
    }
}

/**
 * Supported Dispatch Execution operations.
 */
enum class DispatchExecutionOperation {
    VIEW,
    CREATE,
    EDIT,
    SUBMIT,
    APPROVE,
    PREPARE,
    EXECUTE_DISPATCH,
    CANCEL
}

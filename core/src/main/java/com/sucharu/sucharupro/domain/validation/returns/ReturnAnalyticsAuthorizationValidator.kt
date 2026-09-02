package com.sucharu.sucharupro.domain.validation.returns

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Validates RBAC and multi-tenant project isolation for Return Analytics & Governance (Module 11 Step 06).
 */
object ReturnAnalyticsAuthorizationValidator {

    private val authorizedRoles = setOf(
        UserRole.ADMIN,
        UserRole.MANAGER,
        UserRole.ACCOUNTS
    )

    /**
     * Validates that [callerRole] is permitted to access return analytics and governance operations.
     */
    fun validateRole(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole == null) return DomainResult.Success(Unit)
        return if (callerRole in authorizedRoles) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(
                message = "Access denied: Role '$callerRole' is unauthorized for Return Analytics & Governance. Requires ADMIN, MANAGER, or ACCOUNTS."
            )
        }
    }

    /**
     * Enforces strict multi-tenant boundary between caller and target project.
     */
    fun validateProjectIsolation(
        callerProjectId: String?,
        targetProjectId: String
    ): DomainResult<Unit> {
        if (callerProjectId == null) return DomainResult.Success(Unit)
        return if (callerProjectId == targetProjectId) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(
                message = "Access denied: Caller project '$callerProjectId' cannot access data for project '$targetProjectId'."
            )
        }
    }
}

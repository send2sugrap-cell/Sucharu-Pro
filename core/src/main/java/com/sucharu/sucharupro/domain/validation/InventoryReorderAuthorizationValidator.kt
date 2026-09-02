package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * RBAC authorization validator for Reorder Alert & Stock Level Management (Module 07 Step 08).
 *
 * Permission matrix:
 *
 * | Role      | Configure Policy | View Alert | Acknowledge Alert | Manage Alerts |
 * |-----------|------------------|------------|-------------------|---------------|
 * | ADMIN     | ✓                | ✓          | ✓                 | ✓             |
 * | MANAGER   | ✓                | ✓          | ✓                 | ✓             |
 * | WAREHOUSE | —                | ✓          | ✓                 | —             |
 * | Others    | —                | —          | —                 | —             |
 */
object InventoryReorderAuthorizationValidator {

    /**
     * Validates that the caller can configure (Create/Edit/Delete) stock level policies.
     */
    fun validateConfigurePolicyPermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole == null) {
            return DomainResult.Error(message = "Caller role must be provided for authorization.")
        }
        return when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER -> DomainResult.Success(Unit)
            else -> DomainResult.Error(
                message = "Role '${callerRole.defaultLabel}' is not authorized to configure stock level policies."
            )
        }
    }

    /**
     * Validates that the caller can view reorder alerts and policies.
     */
    fun validateViewPermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole == null) {
            return DomainResult.Error(message = "Caller role must be provided for authorization.")
        }
        return when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER,
            UserRole.WAREHOUSE -> DomainResult.Success(Unit)
            else -> DomainResult.Error(
                message = "Role '${callerRole.defaultLabel}' is not authorized to view reorder alerts and policies."
            )
        }
    }

    /**
     * Validates that the caller can acknowledge a reorder alert.
     */
    fun validateAcknowledgePermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole == null) {
            return DomainResult.Error(message = "Caller role must be provided for authorization.")
        }
        return when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER,
            UserRole.WAREHOUSE -> DomainResult.Success(Unit)
            else -> DomainResult.Error(
                message = "Role '${callerRole.defaultLabel}' is not authorized to acknowledge alerts."
            )
        }
    }

    /**
     * Validates that the caller can manage alerts (e.g. force resolution or bulk operations).
     */
    fun validateManageAlertsPermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole == null) {
            return DomainResult.Error(message = "Caller role must be provided for authorization.")
        }
        return when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER -> DomainResult.Success(Unit)
            else -> DomainResult.Error(
                message = "Role '${callerRole.defaultLabel}' is not authorized to manage reorder alerts."
            )
        }
    }
}

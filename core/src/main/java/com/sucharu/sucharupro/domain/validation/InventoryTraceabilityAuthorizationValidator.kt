package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Domain validator for Inventory Traceability RBAC (Module 07 Step 07).
 *
 * Roles authorized for Registering, Viewing, and Status Changes (HOLD, CLOSE):
 * - ADMIN
 * - MANAGER
 * - WAREHOUSE
 */
object InventoryTraceabilityAuthorizationValidator {

    /**
     * Validates permission to register batches, lots, and traceability records.
     * Allowed: ADMIN, MANAGER, WAREHOUSE.
     */
    fun validateRegisterPermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole == null) {
            return DomainResult.Error(message = "Caller role must be provided for authorization.")
        }
        return when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER,
            UserRole.WAREHOUSE -> DomainResult.Success(Unit)
            else -> DomainResult.Error(
                message = "Role '$callerRole' is not authorized to register traceability data."
            )
        }
    }

    /**
     * Validates permission to view traceability records and activity logs.
     * Allowed: ADMIN, MANAGER, WAREHOUSE.
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
                message = "Role '$callerRole' is not authorized to view traceability data."
            )
        }
    }

    /**
     * Validates permission to change lifecycle status (HOLD, CLOSE, ACTIVE) for batches and lots.
     * Allowed: ADMIN, MANAGER, WAREHOUSE.
     */
    fun validateStatusChangePermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole == null) {
            return DomainResult.Error(message = "Caller role must be provided for authorization.")
        }
        return when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER,
            UserRole.WAREHOUSE -> DomainResult.Success(Unit)
            else -> DomainResult.Error(
                message = "Role '$callerRole' is not authorized to change traceability status."
            )
        }
    }
}

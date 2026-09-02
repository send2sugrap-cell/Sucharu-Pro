package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * RBAC authorization validator for stock adjustment operations (Module 07 Step 06).
 *
 * Permission matrix:
 *
 * | Role         | Create | Edit | Approve | Process | Cancel | View |
 * |--------------|--------|------|---------|---------|--------|------|
 * | ADMIN        | ✓      | ✓    | ✓       | ✓       | ✓      | ✓    |
 * | MANAGER      | ✓      | ✓    | ✓       | ✓       | ✓      | ✓    |
 * | WAREHOUSE    | ✓      | ✓    | —       | ✓       | —      | ✓    |
 * | STAFF        | —      | —    | —       | —       | —      | ✓    |
 * | ACCOUNTS     | —      | —    | —       | —       | —      | ✓    |
 * | QC_INSPECTOR | —      | —    | —       | —       | —      | —    |
 * | DESIGNER     | —      | —    | —       | —       | —      | —    |
 *
 * All methods are pure and side-effect-free.
 */
object InventoryStockAdjustmentAuthorizationValidator {

    /**
     * Validates that the caller can view adjustment records.
     * Allowed: ADMIN, MANAGER, WAREHOUSE, STAFF, ACCOUNTS
     */
    fun validateViewPermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole == null) {
            return DomainResult.Error(message = "Caller role must be provided for authorization.")
        }
        return when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER,
            UserRole.WAREHOUSE,
            UserRole.STAFF,
            UserRole.ACCOUNTS -> DomainResult.Success(Unit)

            UserRole.QC_INSPECTOR,
            UserRole.DESIGNER,
            UserRole.CUSTOMER,
            UserRole.VENDOR,
            UserRole.AFFILIATE -> DomainResult.Error(
                message = "Role '${callerRole.defaultLabel}' is not authorized to view stock adjustments."
            )
        }
    }

    /**
     * Validates that the caller can create or edit a stock adjustment.
     * Allowed: ADMIN, MANAGER, WAREHOUSE
     */
    fun validateCreateEditPermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole == null) {
            return DomainResult.Error(message = "Caller role must be provided for authorization.")
        }
        return when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER,
            UserRole.WAREHOUSE -> DomainResult.Success(Unit)

            else -> DomainResult.Error(
                message = "Role '${callerRole.defaultLabel}' is not authorized to create or edit stock adjustments."
            )
        }
    }

    /**
     * Validates that the caller can approve a stock adjustment.
     * Allowed: ADMIN, MANAGER
     */
    fun validateApprovePermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole == null) {
            return DomainResult.Error(message = "Caller role must be provided for authorization.")
        }
        return when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER -> DomainResult.Success(Unit)

            else -> DomainResult.Error(
                message = "Role '${callerRole.defaultLabel}' is not authorized to approve stock adjustments."
            )
        }
    }

    /**
     * Validates that the caller can process/complete a stock adjustment.
     * Allowed: ADMIN, MANAGER, WAREHOUSE
     */
    fun validateProcessPermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole == null) {
            return DomainResult.Error(message = "Caller role must be provided for authorization.")
        }
        return when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER,
            UserRole.WAREHOUSE -> DomainResult.Success(Unit)

            else -> DomainResult.Error(
                message = "Role '${callerRole.defaultLabel}' is not authorized to process stock adjustments."
            )
        }
    }

    /**
     * Validates that the caller can cancel a stock adjustment.
     * Allowed: ADMIN, MANAGER
     */
    fun validateCancelPermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole == null) {
            return DomainResult.Error(message = "Caller role must be provided for authorization.")
        }
        return when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER -> DomainResult.Success(Unit)

            else -> DomainResult.Error(
                message = "Role '${callerRole.defaultLabel}' is not authorized to cancel stock adjustments."
            )
        }
    }
}

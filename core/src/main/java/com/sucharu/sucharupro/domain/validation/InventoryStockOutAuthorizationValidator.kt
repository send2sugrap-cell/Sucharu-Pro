package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * RBAC authorization validator for stock out / issue operations (Module 07 Step 04).
 *
 * Permission matrix:
 *
 * | Role         | Create | Edit | Approve | Issue | Complete | Cancel | View |
 * |--------------|--------|------|---------|-------|----------|--------|------|
 * | ADMIN        | ✓      | ✓    | ✓       | ✓     | ✓        | ✓      | ✓    |
 * | MANAGER      | ✓      | ✓    | ✓       | ✓     | ✓        | ✓      | ✓    |
 * | WAREHOUSE    | ✓      | ✓    | —       | ✓     | —        | —      | ✓    |
 * | STAFF        | —      | —    | —       | —     | —        | —      | ✓    |
 * | QC_INSPECTOR | —      | —    | —       | —     | —        | —      | ✓    |
 * | ACCOUNTS     | —      | —    | —       | —     | —        | —      | ✓    |
 *
 * All methods are pure and side-effect-free.
 */
object InventoryStockOutAuthorizationValidator {

    /**
     * Validates that the caller can view stock-out records.
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
            UserRole.QC_INSPECTOR,
            UserRole.ACCOUNTS -> DomainResult.Success(Unit)

            else -> DomainResult.Error(
                message = "Role '${callerRole.defaultLabel}' is not authorized to view stock-out records."
            )
        }
    }

    /**
     * Validates that the caller can create or edit a stock-out.
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
                message = "Role '${callerRole.defaultLabel}' is not authorized to create or edit stock-out records."
            )
        }
    }

    /**
     * Validates that the caller can approve a stock-out (DRAFT -> PENDING).
     */
    fun validateApprovePermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole == null) {
            return DomainResult.Error(message = "Caller role must be provided for authorization.")
        }
        return when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER -> DomainResult.Success(Unit)

            else -> DomainResult.Error(
                message = "Role '${callerRole.defaultLabel}' is not authorized to approve stock-out records."
            )
        }
    }

    /**
     * Validates that the caller can record issuing of stock.
     */
    fun validateIssuePermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole == null) {
            return DomainResult.Error(message = "Caller role must be provided for authorization.")
        }
        return when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER,
            UserRole.WAREHOUSE -> DomainResult.Success(Unit)

            else -> DomainResult.Error(
                message = "Role '${callerRole.defaultLabel}' is not authorized to record stock issuance."
            )
        }
    }

    /**
     * Validates that the caller can complete a stock-out.
     */
    fun validateCompletePermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole == null) {
            return DomainResult.Error(message = "Caller role must be provided for authorization.")
        }
        return when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER -> DomainResult.Success(Unit)

            else -> DomainResult.Error(
                message = "Role '${callerRole.defaultLabel}' is not authorized to complete a stock-out operation."
            )
        }
    }

    /**
     * Validates that the caller can cancel a stock-out.
     */
    fun validateCancelPermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole == null) {
            return DomainResult.Error(message = "Caller role must be provided for authorization.")
        }
        return when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER -> DomainResult.Success(Unit)

            else -> DomainResult.Error(
                message = "Role '${callerRole.defaultLabel}' is not authorized to cancel a stock-out operation."
            )
        }
    }
}

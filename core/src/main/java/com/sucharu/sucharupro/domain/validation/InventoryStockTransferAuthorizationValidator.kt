package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * RBAC authorization validator for stock transfer operations (Module 07 Step 05).
 *
 * Permission matrix:
 *
 * | Role         | Create | Edit | Approve | Transfer | Complete | Cancel | View |
 * |--------------|--------|------|---------|----------|----------|--------|------|
 * | ADMIN        | ✓      | ✓    | ✓       | ✓        | ✓        | ✓      | ✓    |
 * | MANAGER      | ✓      | ✓    | ✓       | ✓        | ✓        | ✓      | ✓    |
 * | WAREHOUSE    | ✓      | ✓    | —       | ✓        | —        | —      | ✓    |
 * | STAFF        | —      | —    | —       | —        | —        | —      | ✓    |
 * | QC_INSPECTOR | —      | —    | —       | —        | —        | —      | ✓    |
 * | ACCOUNTS     | —      | —    | —       | —        | —        | —      | ✓    |
 *
 * All methods are pure and side-effect-free.
 */
object InventoryStockTransferAuthorizationValidator {

    /**
     * Validates that the caller can view stock transfer records.
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
                message = "Role '${callerRole.defaultLabel}' is not authorized to view stock transfer records."
            )
        }
    }

    /**
     * Validates that the caller can create or edit a stock transfer.
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
                message = "Role '${callerRole.defaultLabel}' is not authorized to create or edit stock transfer records."
            )
        }
    }

    /**
     * Validates that the caller can approve a stock transfer (PENDING -> APPROVED).
     */
    fun validateApprovePermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole == null) {
            return DomainResult.Error(message = "Caller role must be provided for authorization.")
        }
        return when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER -> DomainResult.Success(Unit)

            else -> DomainResult.Error(
                message = "Role '${callerRole.defaultLabel}' is not authorized to approve stock transfer records."
            )
        }
    }

    /**
     * Validates that the caller can record execution of a transfer (APPROVED -> TRANSFERRING).
     */
    fun validateTransferPermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole == null) {
            return DomainResult.Error(message = "Caller role must be provided for authorization.")
        }
        return when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER,
            UserRole.WAREHOUSE -> DomainResult.Success(Unit)

            else -> DomainResult.Error(
                message = "Role '${callerRole.defaultLabel}' is not authorized to record stock transfer execution."
            )
        }
    }

    /**
     * Validates that the caller can complete a stock transfer.
     */
    fun validateCompletePermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole == null) {
            return DomainResult.Error(message = "Caller role must be provided for authorization.")
        }
        return when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER -> DomainResult.Success(Unit)

            else -> DomainResult.Error(
                message = "Role '${callerRole.defaultLabel}' is not authorized to complete a stock transfer operation."
            )
        }
    }

    /**
     * Validates that the caller can cancel a stock transfer.
     */
    fun validateCancelPermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole == null) {
            return DomainResult.Error(message = "Caller role must be provided for authorization.")
        }
        return when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER -> DomainResult.Success(Unit)

            else -> DomainResult.Error(
                message = "Role '${callerRole.defaultLabel}' is not authorized to cancel a stock transfer operation."
            )
        }
    }
}

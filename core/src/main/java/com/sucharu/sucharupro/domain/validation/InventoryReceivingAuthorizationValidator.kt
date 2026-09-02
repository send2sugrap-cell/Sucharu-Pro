package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * RBAC authorization validator for stock receiving operations (Module 07 Step 03).
 *
 * Permission matrix:
 *
 * | Role         | Create | Edit | Verify | Accept/Reject | Complete | Cancel | View |
 * |--------------|--------|------|--------|---------------|----------|--------|------|
 * | ADMIN        | ✓      | ✓    | ✓      | ✓             | ✓        | ✓      | ✓    |
 * | MANAGER      | ✓      | ✓    | ✓      | ✓             | ✓        | ✓      | ✓    |
 * | WAREHOUSE    | ✓      | ✓    | ✓      | —             | —        | —      | ✓    |
 * | STAFF        | —      | —    | —      | —             | —        | —      | ✓    |
 * | QC_INSPECTOR | —      | —    | —      | —             | —        | —      | ✓    |
 * | ACCOUNTS     | —      | —    | —      | —             | —        | —      | ✓    |
 * | DESIGNER     | —      | —    | —      | —             | —        | —      | —    |
 * | CUSTOMER     | —      | —    | —      | —             | —        | —      | —    |
 * | VENDOR       | —      | —    | —      | —             | —        | —      | —    |
 * | AFFILIATE    | —      | —    | —      | —             | —        | —      | —    |
 *
 * All methods are pure and side-effect-free.
 */
object InventoryReceivingAuthorizationValidator {

    /**
     * Validates that the caller can view receiving records.
     * Allowed: ADMIN, MANAGER, WAREHOUSE, STAFF, QC_INSPECTOR, ACCOUNTS
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

            UserRole.DESIGNER,
            UserRole.CUSTOMER,
            UserRole.VENDOR,
            UserRole.AFFILIATE -> DomainResult.Error(
                message = "Role '${callerRole.defaultLabel}' is not authorized to view receiving records."
            )
        }
    }

    /**
     * Validates that the caller can create or edit a receiving.
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
                message = "Role '${callerRole.defaultLabel}' is not authorized to create or edit receiving records."
            )
        }
    }

    /**
     * Validates that the caller can verify a receiving line.
     * Allowed: ADMIN, MANAGER, WAREHOUSE
     */
    fun validateVerifyPermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole == null) {
            return DomainResult.Error(message = "Caller role must be provided for authorization.")
        }
        return when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER,
            UserRole.WAREHOUSE -> DomainResult.Success(Unit)

            else -> DomainResult.Error(
                message = "Role '${callerRole.defaultLabel}' is not authorized to verify receiving lines."
            )
        }
    }

    /**
     * Validates that the caller can accept or reject quantities.
     * Only privileged management roles may authorize stock quantity acceptance.
     * WAREHOUSE cannot independently accept/reject (must be ADMIN or MANAGER).
     * Allowed: ADMIN, MANAGER
     */
    fun validateAcceptRejectPermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole == null) {
            return DomainResult.Error(message = "Caller role must be provided for authorization.")
        }
        return when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER -> DomainResult.Success(Unit)

            else -> DomainResult.Error(
                message = "Role '${callerRole.defaultLabel}' is not authorized to accept or reject quantities. " +
                    "Quantity acceptance requires ADMIN or MANAGER role."
            )
        }
    }

    /**
     * Validates that the caller can complete a receiving.
     * Allowed: ADMIN, MANAGER
     */
    fun validateCompletePermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole == null) {
            return DomainResult.Error(message = "Caller role must be provided for authorization.")
        }
        return when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER -> DomainResult.Success(Unit)

            else -> DomainResult.Error(
                message = "Role '${callerRole.defaultLabel}' is not authorized to complete a receiving operation."
            )
        }
    }

    /**
     * Validates that the caller can cancel a receiving.
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
                message = "Role '${callerRole.defaultLabel}' is not authorized to cancel a receiving operation."
            )
        }
    }
}

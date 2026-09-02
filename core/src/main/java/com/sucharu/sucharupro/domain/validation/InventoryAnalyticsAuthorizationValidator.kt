package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * RBAC authorization validator for Inventory Analytics and Governance (Module 07 Step 10).
 *
 * Permission matrix:
 * | Role      | View Analytics | Governance Mutation | Financial Valuation |
 * |-----------|----------------|---------------------|---------------------|
 * | ADMIN     | ✓              | ✓                   | ✓                   |
 * | MANAGER   | ✓              | ✓                   | ✓                   |
 * | WAREHOUSE | ✓              | —                   | —                   |
 */
object InventoryAnalyticsAuthorizationValidator {

    /**
     * Validates that the caller can view analytics.
     */
    fun validateViewAnalyticsPermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole == null) {
            return DomainResult.Error(message = "Caller role must be provided for authorization.")
        }

        return when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER,
            UserRole.WAREHOUSE -> DomainResult.Success(Unit)
            else -> DomainResult.Error(
                message = "Role '${callerRole.defaultLabel}' is not authorized to view inventory analytics."
            )
        }
    }

    /**
     * Validates that the caller can perform governance mutations (e.g., acknowledging exceptions).
     */
    fun validateGovernanceMutationPermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole == null) {
            return DomainResult.Error(message = "Caller role must be provided for authorization.")
        }

        return when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER -> DomainResult.Success(Unit)
            else -> DomainResult.Error(
                message = "Role '${callerRole.defaultLabel}' is not authorized to perform governance mutations."
            )
        }
    }

    /**
     * Validates that the caller can view financial valuations in analytics.
     */
    fun validateFinancialValuationPermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole == null) {
            return DomainResult.Error(message = "Caller role must be provided for authorization.")
        }

        return when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER -> DomainResult.Success(Unit)
            else -> DomainResult.Error(
                message = "Role '${callerRole.defaultLabel}' is not authorized to view financial valuations."
            )
        }
    }
}

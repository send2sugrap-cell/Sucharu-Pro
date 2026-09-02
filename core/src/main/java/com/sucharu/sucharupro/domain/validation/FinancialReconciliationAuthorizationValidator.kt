package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Validates role-based access control and separation of duties for financial reconciliation & control (Module 09 Step 08).
 */
object FinancialReconciliationAuthorizationValidator {

    fun validateView(
        callerRole: UserRole,
        targetCustomerId: String? = null,
        authenticatedCustomerId: String? = null,
        targetVendorId: String? = null,
        authenticatedVendorId: String? = null
    ): DomainResult<Unit> {
        return when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER,
            UserRole.ACCOUNTS,
            UserRole.STAFF -> DomainResult.Success(Unit)

            UserRole.CUSTOMER -> {
                if (authenticatedCustomerId != null && targetCustomerId == authenticatedCustomerId) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "Customer is only authorized to view their own financial records.")
                }
            }

            UserRole.VENDOR -> {
                if (authenticatedVendorId != null && targetVendorId == authenticatedVendorId) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "Vendor is only authorized to view their own financial records.")
                }
            }

            else -> DomainResult.Error(message = "Role '$callerRole' is not authorized to view financial records.")
        }
    }

    fun validateCreateReconciliation(callerRole: UserRole): DomainResult<Unit> {
        return when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER,
            UserRole.ACCOUNTS -> DomainResult.Success(Unit)
            else -> DomainResult.Error(message = "Role '$callerRole' is not authorized to create financial reconciliations.")
        }
    }

    fun validateApproveReconciliation(
        callerRole: UserRole,
        creatorId: String?,
        approverId: String
    ): DomainResult<Unit> {
        if (callerRole != UserRole.ADMIN && callerRole != UserRole.MANAGER && callerRole != UserRole.ACCOUNTS) {
            return DomainResult.Error(message = "Role '$callerRole' is not authorized to approve reconciliations.")
        }

        // Separation of duties
        if (callerRole != UserRole.ADMIN && creatorId != null && creatorId == approverId) {
            return DomainResult.Error(message = "Separation of duties violation: Creator cannot approve their own reconciliation.")
        }

        return DomainResult.Success(Unit)
    }

    fun validateClosePeriod(
        callerRole: UserRole,
        initiatorId: String?,
        closerId: String
    ): DomainResult<Unit> {
        if (callerRole != UserRole.ADMIN && callerRole != UserRole.ACCOUNTS) {
            return DomainResult.Error(message = "Role '$callerRole' is not authorized to close accounting periods.")
        }

        // Separation of duties
        if (callerRole != UserRole.ADMIN && initiatorId != null && initiatorId == closerId) {
            return DomainResult.Error(message = "Separation of duties violation: Period closing initiator cannot approve the final close.")
        }

        return DomainResult.Success(Unit)
    }

    fun validateReopenPeriod(callerRole: UserRole): DomainResult<Unit> {
        return if (callerRole == UserRole.ADMIN) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Only ADMIN is authorized to approve accounting period reopening.")
        }
    }

    fun validateWaiveCriticalDiscrepancy(callerRole: UserRole): DomainResult<Unit> {
        return if (callerRole == UserRole.ADMIN) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Only ADMIN is authorized to waive critical financial discrepancies.")
        }
    }
}

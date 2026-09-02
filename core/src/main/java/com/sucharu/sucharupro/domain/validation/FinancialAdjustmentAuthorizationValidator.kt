package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * RBAC and separation-of-duties authorization validator for Financial Adjustments (Module 09 Step 07).
 */
object FinancialAdjustmentAuthorizationValidator {

    fun validateCreateDraft(callerRole: UserRole): DomainResult<Unit> {
        val allowed = callerRole == UserRole.ADMIN ||
                callerRole == UserRole.MANAGER ||
                callerRole == UserRole.ACCOUNTS ||
                callerRole == UserRole.STAFF

        return if (allowed) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Unauthorized: Role '$callerRole' cannot create financial adjustments.")
        }
    }

    fun validateUpdateDraft(callerRole: UserRole): DomainResult<Unit> {
        val allowed = callerRole == UserRole.ADMIN ||
                callerRole == UserRole.MANAGER ||
                callerRole == UserRole.ACCOUNTS ||
                callerRole == UserRole.STAFF

        return if (allowed) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Unauthorized: Role '$callerRole' cannot update draft financial adjustments.")
        }
    }

    fun validateSubmit(callerRole: UserRole): DomainResult<Unit> {
        val allowed = callerRole == UserRole.ADMIN ||
                callerRole == UserRole.MANAGER ||
                callerRole == UserRole.ACCOUNTS ||
                callerRole == UserRole.STAFF

        return if (allowed) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Unauthorized: Role '$callerRole' cannot submit financial adjustments.")
        }
    }

    fun validateApprove(
        callerRole: UserRole,
        creatorId: String,
        approverId: String
    ): DomainResult<Unit> {
        val allowed = callerRole == UserRole.ADMIN ||
                callerRole == UserRole.MANAGER ||
                callerRole == UserRole.ACCOUNTS

        if (!allowed) {
            return DomainResult.Error(message = "Unauthorized: Role '$callerRole' cannot approve financial adjustments.")
        }

        if (callerRole != UserRole.ADMIN && creatorId.isNotBlank() && creatorId == approverId) {
            return DomainResult.Error(
                message = "Separation of duties violation: Creator '$creatorId' cannot approve their own financial adjustment."
            )
        }

        return DomainResult.Success(Unit)
    }

    fun validatePost(
        callerRole: UserRole,
        creatorId: String,
        posterId: String
    ): DomainResult<Unit> {
        val allowed = callerRole == UserRole.ADMIN ||
                callerRole == UserRole.MANAGER ||
                callerRole == UserRole.ACCOUNTS

        if (!allowed) {
            return DomainResult.Error(message = "Unauthorized: Role '$callerRole' cannot post financial adjustments.")
        }

        if (callerRole != UserRole.ADMIN && creatorId.isNotBlank() && creatorId == posterId) {
            return DomainResult.Error(
                message = "Separation of duties violation: Creator '$creatorId' cannot post their own financial adjustment."
            )
        }

        return DomainResult.Success(Unit)
    }

    fun validateReject(callerRole: UserRole): DomainResult<Unit> {
        val allowed = callerRole == UserRole.ADMIN ||
                callerRole == UserRole.MANAGER ||
                callerRole == UserRole.ACCOUNTS

        return if (allowed) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Unauthorized: Role '$callerRole' cannot reject financial adjustments.")
        }
    }

    fun validateCancel(callerRole: UserRole): DomainResult<Unit> {
        val allowed = callerRole == UserRole.ADMIN ||
                callerRole == UserRole.MANAGER ||
                callerRole == UserRole.ACCOUNTS ||
                callerRole == UserRole.STAFF

        return if (allowed) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Unauthorized: Role '$callerRole' cannot cancel financial adjustments.")
        }
    }

    fun validateView(
        callerRole: UserRole,
        targetCustomerId: String? = null,
        authenticatedCustomerId: String? = null,
        targetVendorId: String? = null,
        authenticatedVendorId: String? = null
    ): DomainResult<Unit> {
        if (callerRole.isInternal) {
            return DomainResult.Success(Unit)
        }

        if (callerRole == UserRole.CUSTOMER) {
            if (authenticatedCustomerId.isNullOrBlank() || targetCustomerId != authenticatedCustomerId) {
                return DomainResult.Error(
                    message = "Customer access violation: Cannot view financial adjustments belonging to another customer."
                )
            }
            return DomainResult.Success(Unit)
        }

        if (callerRole == UserRole.VENDOR) {
            if (authenticatedVendorId.isNullOrBlank() || targetVendorId != authenticatedVendorId) {
                return DomainResult.Error(
                    message = "Vendor access violation: Cannot view financial adjustments belonging to another vendor."
                )
            }
            return DomainResult.Success(Unit)
        }

        return DomainResult.Error(message = "Unauthorized: Role '$callerRole' cannot access financial adjustments.")
    }
}

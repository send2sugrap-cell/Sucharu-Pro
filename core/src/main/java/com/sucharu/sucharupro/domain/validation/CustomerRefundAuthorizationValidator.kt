package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * RBAC and separation-of-duties authorization validator for Customer Refunds (Module 09 Step 07).
 */
object CustomerRefundAuthorizationValidator {

    fun validateCreateDraft(callerRole: UserRole): DomainResult<Unit> {
        val allowed = callerRole == UserRole.ADMIN ||
                callerRole == UserRole.MANAGER ||
                callerRole == UserRole.ACCOUNTS ||
                callerRole == UserRole.STAFF

        return if (allowed) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Unauthorized: Role '$callerRole' cannot create customer refunds.")
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
            DomainResult.Error(message = "Unauthorized: Role '$callerRole' cannot update draft customer refunds.")
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
            DomainResult.Error(message = "Unauthorized: Role '$callerRole' cannot submit customer refunds.")
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
            return DomainResult.Error(message = "Unauthorized: Role '$callerRole' cannot approve customer refunds.")
        }

        if (callerRole != UserRole.ADMIN && creatorId.isNotBlank() && creatorId == approverId) {
            return DomainResult.Error(
                message = "Separation of duties violation: Creator '$creatorId' cannot approve their own refund."
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
            return DomainResult.Error(message = "Unauthorized: Role '$callerRole' cannot post customer refunds to ledger.")
        }

        if (callerRole != UserRole.ADMIN && creatorId.isNotBlank() && creatorId == posterId) {
            return DomainResult.Error(
                message = "Separation of duties violation: Creator '$creatorId' cannot post their own refund to ledger."
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
            DomainResult.Error(message = "Unauthorized: Role '$callerRole' cannot reject customer refunds.")
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
            DomainResult.Error(message = "Unauthorized: Role '$callerRole' cannot cancel customer refunds.")
        }
    }

    fun validateView(
        callerRole: UserRole,
        targetCustomerId: String? = null,
        authenticatedCustomerId: String? = null
    ): DomainResult<Unit> {
        if (callerRole.isInternal) {
            return DomainResult.Success(Unit)
        }

        if (callerRole == UserRole.CUSTOMER) {
            if (authenticatedCustomerId.isNullOrBlank() || targetCustomerId != authenticatedCustomerId) {
                return DomainResult.Error(
                    message = "Customer access violation: Cannot view refunds belonging to another customer."
                )
            }
            return DomainResult.Success(Unit)
        }

        return DomainResult.Error(message = "Unauthorized: Role '$callerRole' cannot access customer refunds.")
    }
}

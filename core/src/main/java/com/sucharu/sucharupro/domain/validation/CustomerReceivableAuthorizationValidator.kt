package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * RBAC authorization validator for Customer Receivable operations (Module 09 Step 02).
 */
object CustomerReceivableAuthorizationValidator {

    fun validateViewReceivables(
        callerRole: UserRole,
        requestedCustomerId: String? = null,
        authenticatedCustomerId: String? = null
    ): DomainResult<Unit> {
        if (callerRole.isInternal) {
            return DomainResult.Success(Unit)
        }

        if (callerRole == UserRole.CUSTOMER) {
            if (authenticatedCustomerId == null || requestedCustomerId == null || authenticatedCustomerId != requestedCustomerId) {
                return DomainResult.Error(
                    message = "Unauthorized: Customer users may only view their own receivable obligations."
                )
            }
            return DomainResult.Success(Unit)
        }

        return DomainResult.Error(
            message = "Unauthorized: External role '$callerRole' cannot view customer receivables."
        )
    }

    fun validateCreateReceivable(callerRole: UserRole): DomainResult<Unit> {
        val allowed = callerRole == UserRole.ADMIN ||
                callerRole == UserRole.MANAGER ||
                callerRole == UserRole.ACCOUNTS

        return if (allowed) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(
                message = "Unauthorized: Role '$callerRole' is not permitted to create customer receivables."
            )
        }
    }

    fun validateUpdateReceivable(callerRole: UserRole): DomainResult<Unit> {
        val allowed = callerRole == UserRole.ADMIN ||
                callerRole == UserRole.MANAGER ||
                callerRole == UserRole.ACCOUNTS

        return if (allowed) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(
                message = "Unauthorized: Role '$callerRole' is not permitted to update customer receivables."
            )
        }
    }

    fun validateCancelReceivable(callerRole: UserRole): DomainResult<Unit> {
        val allowed = callerRole == UserRole.ADMIN ||
                callerRole == UserRole.MANAGER ||
                callerRole == UserRole.ACCOUNTS

        return if (allowed) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(
                message = "Unauthorized: Role '$callerRole' is not permitted to cancel customer receivables."
            )
        }
    }

    fun validateRecordSettlement(callerRole: UserRole): DomainResult<Unit> {
        val allowed = callerRole == UserRole.ADMIN ||
                callerRole == UserRole.MANAGER ||
                callerRole == UserRole.ACCOUNTS

        return if (allowed) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(
                message = "Unauthorized: Role '$callerRole' is not permitted to record receivable settlements."
            )
        }
    }
}

package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * RBAC and Separation of Duties validator for Customer Payment operations (Module 09 Step 03).
 */
object CustomerPaymentAuthorizationValidator {

    fun validateViewPayments(
        callerRole: UserRole,
        requestedCustomerId: String? = null,
        authenticatedCustomerId: String? = null
    ): DomainResult<Unit> {
        if (callerRole.isInternal) return DomainResult.Success(Unit)

        if (callerRole == UserRole.CUSTOMER) {
            if (authenticatedCustomerId == null || requestedCustomerId == null || authenticatedCustomerId != requestedCustomerId) {
                return DomainResult.Error(
                    message = "Unauthorized: Customer users may only view their own payment and receipt records."
                )
            }
            return DomainResult.Success(Unit)
        }

        return DomainResult.Error(
            message = "Unauthorized: External role '$callerRole' cannot view customer payments."
        )
    }

    fun validateCreateDraftPayment(callerRole: UserRole): DomainResult<Unit> {
        val allowed = callerRole == UserRole.ADMIN ||
                callerRole == UserRole.MANAGER ||
                callerRole == UserRole.ACCOUNTS ||
                callerRole == UserRole.STAFF

        return if (allowed) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(
                message = "Unauthorized: Role '$callerRole' is not permitted to create customer payments."
            )
        }
    }

    fun validateUpdatePayment(callerRole: UserRole): DomainResult<Unit> {
        val allowed = callerRole == UserRole.ADMIN ||
                callerRole == UserRole.MANAGER ||
                callerRole == UserRole.ACCOUNTS ||
                callerRole == UserRole.STAFF

        return if (allowed) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(
                message = "Unauthorized: Role '$callerRole' is not permitted to update customer payments."
            )
        }
    }

    fun validatePostPayment(
        callerRole: UserRole,
        creatorId: String,
        posterId: String
    ): DomainResult<Unit> {
        val allowedRole = callerRole == UserRole.ADMIN ||
                callerRole == UserRole.MANAGER ||
                callerRole == UserRole.ACCOUNTS

        if (!allowedRole) {
            return DomainResult.Error(
                message = "Unauthorized: Role '$callerRole' is not permitted to post customer payments."
            )
        }

        // Separation of duties: Creator cannot post own payment unless ADMIN
        if (creatorId == posterId && callerRole != UserRole.ADMIN) {
            return DomainResult.Error(
                message = "Separation of duties violation: Payment creator '$creatorId' cannot approve and post their own payment. Requires an independent Accounts officer or Admin."
            )
        }

        return DomainResult.Success(Unit)
    }

    fun validateRejectPayment(callerRole: UserRole): DomainResult<Unit> {
        val allowed = callerRole == UserRole.ADMIN ||
                callerRole == UserRole.MANAGER ||
                callerRole == UserRole.ACCOUNTS

        return if (allowed) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(
                message = "Unauthorized: Role '$callerRole' is not permitted to reject customer payments."
            )
        }
    }

    fun validateCancelPayment(callerRole: UserRole): DomainResult<Unit> {
        val allowed = callerRole == UserRole.ADMIN ||
                callerRole == UserRole.MANAGER ||
                callerRole == UserRole.ACCOUNTS

        return if (allowed) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(
                message = "Unauthorized: Role '$callerRole' is not permitted to cancel customer payments."
            )
        }
    }
}



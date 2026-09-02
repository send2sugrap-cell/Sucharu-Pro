package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * RBAC authorization validator for Financial Transactions & Ledger operations (Module 09 Step 01).
 *
 * Rules:
 * - View transactions: ADMIN, MANAGER, ACCOUNTS, STAFF
 * - Create transaction: ADMIN, MANAGER, ACCOUNTS, STAFF
 * - Update draft: ADMIN, MANAGER, ACCOUNTS, STAFF
 * - Submit transaction: ADMIN, MANAGER, ACCOUNTS, STAFF
 * - Post transaction: ADMIN, MANAGER, ACCOUNTS (Separation of duties: creator cannot post own transaction unless ADMIN)
 * - Reject transaction: ADMIN, MANAGER, ACCOUNTS
 * - Cancel transaction: ADMIN, MANAGER, ACCOUNTS
 * - View ledger: ADMIN, MANAGER, ACCOUNTS
 */
object FinancialAuthorizationValidator {

    fun validateViewTransactions(callerRole: UserRole): DomainResult<Unit> {
        return if (callerRole.isInternal) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Unauthorized: External role '$callerRole' cannot view internal financial transactions.")
        }
    }

    fun validateCreateTransaction(callerRole: UserRole): DomainResult<Unit> {
        val allowed = callerRole == UserRole.ADMIN ||
                callerRole == UserRole.MANAGER ||
                callerRole == UserRole.ACCOUNTS ||
                callerRole == UserRole.STAFF

        return if (allowed) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Unauthorized: Role '$callerRole' is not permitted to create financial transactions.")
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
            DomainResult.Error(message = "Unauthorized: Role '$callerRole' is not permitted to update draft financial transactions.")
        }
    }

    fun validateSubmitTransaction(callerRole: UserRole): DomainResult<Unit> {
        val allowed = callerRole == UserRole.ADMIN ||
                callerRole == UserRole.MANAGER ||
                callerRole == UserRole.ACCOUNTS ||
                callerRole == UserRole.STAFF

        return if (allowed) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Unauthorized: Role '$callerRole' is not permitted to submit financial transactions for approval.")
        }
    }

    fun validatePostTransaction(
        callerRole: UserRole,
        creatorId: String,
        actorId: String
    ): DomainResult<Unit> {
        val allowedRole = callerRole == UserRole.ADMIN ||
                callerRole == UserRole.MANAGER ||
                callerRole == UserRole.ACCOUNTS

        if (!allowedRole) {
            return DomainResult.Error(message = "Unauthorized: Role '$callerRole' is not permitted to post financial transactions.")
        }

        // Separation of duties: creator cannot post own transaction unless ADMIN
        if (callerRole != UserRole.ADMIN && creatorId.isNotBlank() && creatorId == actorId) {
            return DomainResult.Error(
                message = "Separation of duties violation: Creator '$creatorId' cannot approve/post their own financial transaction."
            )
        }

        return DomainResult.Success(Unit)
    }

    fun validateRejectTransaction(callerRole: UserRole): DomainResult<Unit> {
        val allowed = callerRole == UserRole.ADMIN ||
                callerRole == UserRole.MANAGER ||
                callerRole == UserRole.ACCOUNTS

        return if (allowed) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Unauthorized: Role '$callerRole' is not permitted to reject financial transactions.")
        }
    }

    fun validateCancelTransaction(callerRole: UserRole): DomainResult<Unit> {
        val allowed = callerRole == UserRole.ADMIN ||
                callerRole == UserRole.MANAGER ||
                callerRole == UserRole.ACCOUNTS ||
                callerRole == UserRole.STAFF

        return if (allowed) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Unauthorized: Role '$callerRole' is not permitted to cancel financial transactions.")
        }
    }

    fun validateViewLedger(callerRole: UserRole): DomainResult<Unit> {
        val allowed = callerRole.hasFinancialAccess || callerRole == UserRole.ADMIN || callerRole == UserRole.MANAGER || callerRole == UserRole.ACCOUNTS
        return if (allowed) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Unauthorized: Role '$callerRole' does not have financial ledger access.")
        }
    }
}

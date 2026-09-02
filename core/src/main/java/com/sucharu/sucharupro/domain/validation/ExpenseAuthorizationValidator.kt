package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * RBAC and separation-of-duties authorization validator for Expenses and Expense Categories (Module 09 Step 06).
 */
object ExpenseAuthorizationValidator {

    fun validateCreateCategory(callerRole: UserRole): DomainResult<Unit> {
        val allowed = callerRole == UserRole.ADMIN ||
                callerRole == UserRole.MANAGER ||
                callerRole == UserRole.ACCOUNTS

        return if (allowed) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Unauthorized: Role '$callerRole' cannot create expense categories.")
        }
    }

    fun validateUpdateCategory(callerRole: UserRole): DomainResult<Unit> {
        val allowed = callerRole == UserRole.ADMIN ||
                callerRole == UserRole.MANAGER ||
                callerRole == UserRole.ACCOUNTS

        return if (allowed) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Unauthorized: Role '$callerRole' cannot update expense categories.")
        }
    }

    fun validateCreateDraftExpense(callerRole: UserRole): DomainResult<Unit> {
        val allowed = callerRole == UserRole.ADMIN ||
                callerRole == UserRole.MANAGER ||
                callerRole == UserRole.ACCOUNTS ||
                callerRole == UserRole.STAFF

        return if (allowed) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Unauthorized: Role '$callerRole' cannot create expenses.")
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
            DomainResult.Error(message = "Unauthorized: Role '$callerRole' cannot update draft expenses.")
        }
    }

    fun validateSubmitExpense(callerRole: UserRole): DomainResult<Unit> {
        val allowed = callerRole == UserRole.ADMIN ||
                callerRole == UserRole.MANAGER ||
                callerRole == UserRole.ACCOUNTS ||
                callerRole == UserRole.STAFF

        return if (allowed) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Unauthorized: Role '$callerRole' cannot submit expenses for approval.")
        }
    }

    fun validateApproveExpense(
        callerRole: UserRole,
        creatorId: String,
        approverId: String
    ): DomainResult<Unit> {
        val allowed = callerRole == UserRole.ADMIN ||
                callerRole == UserRole.MANAGER ||
                callerRole == UserRole.ACCOUNTS

        if (!allowed) {
            return DomainResult.Error(message = "Unauthorized: Role '$callerRole' cannot approve expenses.")
        }

        // Separation of duties: creator cannot approve own expense unless ADMIN
        if (callerRole != UserRole.ADMIN && creatorId.isNotBlank() && creatorId == approverId) {
            return DomainResult.Error(
                message = "Separation of duties violation: Creator '$creatorId' cannot approve their own expense."
            )
        }

        return DomainResult.Success(Unit)
    }

    fun validatePostExpense(
        callerRole: UserRole,
        creatorId: String,
        posterId: String
    ): DomainResult<Unit> {
        val allowed = callerRole == UserRole.ADMIN ||
                callerRole == UserRole.MANAGER ||
                callerRole == UserRole.ACCOUNTS

        if (!allowed) {
            return DomainResult.Error(message = "Unauthorized: Role '$callerRole' cannot post expenses to financial ledger.")
        }

        // Separation of duties: creator cannot post own expense unless ADMIN
        if (callerRole != UserRole.ADMIN && creatorId.isNotBlank() && creatorId == posterId) {
            return DomainResult.Error(
                message = "Separation of duties violation: Creator '$creatorId' cannot post their own expense to financial ledger."
            )
        }

        return DomainResult.Success(Unit)
    }

    fun validateRejectExpense(callerRole: UserRole): DomainResult<Unit> {
        val allowed = callerRole == UserRole.ADMIN ||
                callerRole == UserRole.MANAGER ||
                callerRole == UserRole.ACCOUNTS

        return if (allowed) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Unauthorized: Role '$callerRole' cannot reject expenses.")
        }
    }

    fun validateCancelExpense(callerRole: UserRole): DomainResult<Unit> {
        val allowed = callerRole == UserRole.ADMIN ||
                callerRole == UserRole.MANAGER ||
                callerRole == UserRole.ACCOUNTS ||
                callerRole == UserRole.STAFF

        return if (allowed) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Unauthorized: Role '$callerRole' cannot cancel expenses.")
        }
    }

    fun validateViewExpenses(callerRole: UserRole): DomainResult<Unit> {
        if (callerRole.isInternal) {
            return DomainResult.Success(Unit)
        }

        return DomainResult.Error(message = "Unauthorized: External role '$callerRole' cannot access internal business expenses.")
    }
}

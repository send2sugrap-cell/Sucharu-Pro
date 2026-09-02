package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * RBAC and separation-of-duties authorization validator for Supplier Payments (Module 09 Step 05).
 */
object SupplierPaymentAuthorizationValidator {

    fun validateCreateDraftPayment(callerRole: UserRole): DomainResult<Unit> {
        val allowed = callerRole == UserRole.ADMIN ||
                callerRole == UserRole.MANAGER ||
                callerRole == UserRole.ACCOUNTS ||
                callerRole == UserRole.STAFF

        return if (allowed) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Unauthorized: Role '$callerRole' cannot create supplier payments.")
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
            DomainResult.Error(message = "Unauthorized: Role '$callerRole' cannot update draft supplier payments.")
        }
    }

    fun validateSubmitPayment(callerRole: UserRole): DomainResult<Unit> {
        val allowed = callerRole == UserRole.ADMIN ||
                callerRole == UserRole.MANAGER ||
                callerRole == UserRole.ACCOUNTS ||
                callerRole == UserRole.STAFF

        return if (allowed) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Unauthorized: Role '$callerRole' cannot submit supplier payments.")
        }
    }

    fun validateApprovePayment(
        callerRole: UserRole,
        creatorId: String,
        approverId: String
    ): DomainResult<Unit> {
        val allowed = callerRole == UserRole.ADMIN ||
                callerRole == UserRole.MANAGER ||
                callerRole == UserRole.ACCOUNTS

        if (!allowed) {
            return DomainResult.Error(message = "Unauthorized: Role '$callerRole' cannot approve supplier payments.")
        }

        // Separation of duties: creator cannot approve own payment unless ADMIN
        if (callerRole != UserRole.ADMIN && creatorId.isNotBlank() && creatorId == approverId) {
            return DomainResult.Error(
                message = "Separation of duties violation: Creator '$creatorId' cannot approve their own supplier payment."
            )
        }

        return DomainResult.Success(Unit)
    }

    fun validatePostPayment(
        callerRole: UserRole,
        creatorId: String,
        posterId: String
    ): DomainResult<Unit> {
        val allowed = callerRole == UserRole.ADMIN ||
                callerRole == UserRole.MANAGER ||
                callerRole == UserRole.ACCOUNTS

        if (!allowed) {
            return DomainResult.Error(message = "Unauthorized: Role '$callerRole' cannot post supplier payments to ledger.")
        }

        // Separation of duties: creator cannot post own payment unless ADMIN
        if (callerRole != UserRole.ADMIN && creatorId.isNotBlank() && creatorId == posterId) {
            return DomainResult.Error(
                message = "Separation of duties violation: Creator '$creatorId' cannot post their own supplier payment to ledger."
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
            DomainResult.Error(message = "Unauthorized: Role '$callerRole' cannot reject supplier payments.")
        }
    }

    fun validateCancelPayment(callerRole: UserRole): DomainResult<Unit> {
        val allowed = callerRole == UserRole.ADMIN ||
                callerRole == UserRole.MANAGER ||
                callerRole == UserRole.ACCOUNTS ||
                callerRole == UserRole.STAFF

        return if (allowed) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Unauthorized: Role '$callerRole' cannot cancel supplier payments.")
        }
    }

    fun validateViewPayments(
        callerRole: UserRole,
        requestedVendorId: String? = null,
        authenticatedVendorId: String? = null
    ): DomainResult<Unit> {
        if (callerRole.isInternal) {
            return DomainResult.Success(Unit)
        }

        if (callerRole == UserRole.VENDOR) {
            if (authenticatedVendorId.isNullOrBlank()) {
                return DomainResult.Error(message = "Unauthorized: Vendor session is not bound to a vendor account.")
            }
            if (requestedVendorId != null && requestedVendorId != authenticatedVendorId) {
                return DomainResult.Error(message = "Unauthorized: Vendors can only access their own payment history.")
            }
            return DomainResult.Success(Unit)
        }

        return DomainResult.Error(message = "Unauthorized: Role '$callerRole' cannot access supplier payment records.")
    }
}

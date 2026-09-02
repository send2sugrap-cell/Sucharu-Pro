package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * RBAC and separation-of-duties authorization validator for Vendor Payables (Module 09 Step 04).
 */
object VendorPayableAuthorizationValidator {

    fun validateCreatePayable(callerRole: UserRole): DomainResult<Unit> {
        val allowed = callerRole == UserRole.ADMIN ||
                callerRole == UserRole.MANAGER ||
                callerRole == UserRole.ACCOUNTS ||
                callerRole == UserRole.STAFF

        return if (allowed) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Unauthorized: Role '$callerRole' cannot create supplier payables.")
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
            DomainResult.Error(message = "Unauthorized: Role '$callerRole' cannot update draft supplier payables.")
        }
    }

    fun validateSubmitPayable(callerRole: UserRole): DomainResult<Unit> {
        val allowed = callerRole == UserRole.ADMIN ||
                callerRole == UserRole.MANAGER ||
                callerRole == UserRole.ACCOUNTS ||
                callerRole == UserRole.STAFF

        return if (allowed) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Unauthorized: Role '$callerRole' cannot submit payables for approval.")
        }
    }

    fun validateApprovePayable(
        callerRole: UserRole,
        creatorId: String,
        approverId: String
    ): DomainResult<Unit> {
        val allowed = callerRole == UserRole.ADMIN ||
                callerRole == UserRole.MANAGER ||
                callerRole == UserRole.ACCOUNTS

        if (!allowed) {
            return DomainResult.Error(message = "Unauthorized: Role '$callerRole' cannot approve supplier payables.")
        }

        // Separation of duties: creator cannot approve own payable unless ADMIN
        if (callerRole != UserRole.ADMIN && creatorId.isNotBlank() && creatorId == approverId) {
            return DomainResult.Error(
                message = "Separation of duties violation: Creator '$creatorId' cannot approve their own supplier payable."
            )
        }

        return DomainResult.Success(Unit)
    }

    fun validateCancelPayable(callerRole: UserRole): DomainResult<Unit> {
        val allowed = callerRole == UserRole.ADMIN ||
                callerRole == UserRole.MANAGER ||
                callerRole == UserRole.ACCOUNTS

        return if (allowed) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Unauthorized: Role '$callerRole' cannot cancel supplier payables.")
        }
    }

    fun validateRecordSettlement(callerRole: UserRole): DomainResult<Unit> {
        val allowed = callerRole == UserRole.ADMIN ||
                callerRole == UserRole.MANAGER ||
                callerRole == UserRole.ACCOUNTS

        return if (allowed) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Unauthorized: Role '$callerRole' cannot record settlements on payables.")
        }
    }

    fun validateViewPayables(
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
                return DomainResult.Error(message = "Unauthorized: Vendors can only access their own payables.")
            }
            return DomainResult.Success(Unit)
        }

        return DomainResult.Error(message = "Unauthorized: Role '$callerRole' cannot access supplier payable records.")
    }
}

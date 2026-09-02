package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole

enum class DeliveryReturnOperation {
    VIEW,
    CREATE,
    UPDATE,
    SUBMIT,
    APPROVE,
    REJECT,
    RECEIVE,
    INSPECT,
    SET_DISPOSITION,
    PROCESS_RESTOCK,
    COMPLETE,
    CANCEL
}

/**
 * RBAC authorization validator for Delivery Return operations (Module 08 Step 07).
 */
object DeliveryReturnAuthorizationValidator {

    fun validateOperation(
        callerRole: UserRole,
        operation: DeliveryReturnOperation,
        targetProjectId: String,
        callerProjectId: String? = null
    ): DomainResult<Unit> {
        if (callerProjectId != null && callerProjectId != targetProjectId) {
            return DomainResult.Error(
                message = "Access denied: Caller project '$callerProjectId' cannot operate on return in project '$targetProjectId'."
            )
        }

        val isAuthorized = when (callerRole) {
            UserRole.ADMIN, UserRole.MANAGER -> true
            UserRole.WAREHOUSE -> when (operation) {
                DeliveryReturnOperation.VIEW,
                DeliveryReturnOperation.CREATE,
                DeliveryReturnOperation.UPDATE,
                DeliveryReturnOperation.SUBMIT,
                DeliveryReturnOperation.RECEIVE,
                DeliveryReturnOperation.INSPECT,
                DeliveryReturnOperation.SET_DISPOSITION,
                DeliveryReturnOperation.PROCESS_RESTOCK,
                DeliveryReturnOperation.COMPLETE,
                DeliveryReturnOperation.CANCEL -> true
                DeliveryReturnOperation.APPROVE,
                DeliveryReturnOperation.REJECT -> false
            }
            UserRole.QC_INSPECTOR -> when (operation) {
                DeliveryReturnOperation.VIEW,
                DeliveryReturnOperation.INSPECT,
                DeliveryReturnOperation.SET_DISPOSITION -> true
                else -> false
            }
            UserRole.STAFF -> when (operation) {
                DeliveryReturnOperation.VIEW,
                DeliveryReturnOperation.CREATE,
                DeliveryReturnOperation.UPDATE,
                DeliveryReturnOperation.SUBMIT -> true
                else -> false
            }
            UserRole.ACCOUNTS, UserRole.DESIGNER -> operation == DeliveryReturnOperation.VIEW
            UserRole.CUSTOMER, UserRole.VENDOR, UserRole.AFFILIATE -> false
        }

        return if (isAuthorized) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(
                message = "Role '$callerRole' is unauthorized to perform '$operation' on Delivery Returns."
            )
        }
    }
}

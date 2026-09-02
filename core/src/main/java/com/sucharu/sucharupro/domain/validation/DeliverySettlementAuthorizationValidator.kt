package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole

enum class DeliverySettlementOperation {
    CREATE,
    EDIT,
    RECALCULATE,
    RECORD_PARTIAL,
    CREATE_SPLIT,
    FINALIZE_SETTLEMENT,
    DISPUTE_SETTLEMENT,
    CANCEL,
    VIEW
}

/**
 * RBAC and Multi-tenant authorization validator for Settlement Management (Module 08 Step 06).
 */
object DeliverySettlementAuthorizationValidator {

    fun validateOperation(
        callerRole: UserRole,
        operation: DeliverySettlementOperation,
        targetProjectId: String,
        callerProjectId: String? = null
    ): DomainResult<Unit> {
        // Multi-tenant project boundary check
        if (callerProjectId != null && callerProjectId != targetProjectId) {
            return DomainResult.Error(
                message = "Access denied: Caller project '$callerProjectId' does not match target project '$targetProjectId'."
            )
        }

        val isAuthorized = when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER -> true

            UserRole.WAREHOUSE -> operation in listOf(
                DeliverySettlementOperation.VIEW,
                DeliverySettlementOperation.CREATE_SPLIT,
                DeliverySettlementOperation.RECORD_PARTIAL,
                DeliverySettlementOperation.RECALCULATE
            )

            UserRole.QC_INSPECTOR,
            UserRole.ACCOUNTS,
            UserRole.STAFF,
            UserRole.CUSTOMER,
            UserRole.DESIGNER,
            UserRole.AFFILIATE -> operation == DeliverySettlementOperation.VIEW

            UserRole.VENDOR -> false
        }

        return if (isAuthorized) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(
                message = "User role '${callerRole.name}' is not authorized to perform operation '$operation' on settlements."
            )
        }
    }
}

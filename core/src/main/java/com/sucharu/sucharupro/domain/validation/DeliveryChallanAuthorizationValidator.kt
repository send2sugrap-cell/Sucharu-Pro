package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanStatus
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Role-Based Access Control (RBAC) validator for Delivery Challan operations (Module 08 Step 02).
 */
object DeliveryChallanAuthorizationValidator {

    fun validateOperation(
        callerRole: UserRole?,
        operation: DeliveryChallanOperation,
        targetProjectId: String,
        callerProjectId: String? = null
    ): DomainResult<Unit> {
        if (callerRole == null) {
            return DomainResult.Error(message = "Caller role must be provided for authorization.")
        }

        // 1. Project Scoping Enforcement
        if (callerProjectId != null && callerProjectId != targetProjectId) {
            return DomainResult.Error(
                message = "Unauthorized: Access denied for project '$targetProjectId'."
            )
        }

        // 2. Role-Based Permission Check
        return when (operation) {
            DeliveryChallanOperation.VIEW -> {
                if (callerRole.isInternal) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "Role '$callerRole' is not authorized to view delivery challans.")
                }
            }

            DeliveryChallanOperation.CREATE,
            DeliveryChallanOperation.EDIT,
            DeliveryChallanOperation.SUBMIT,
            DeliveryChallanOperation.APPROVE,
            DeliveryChallanOperation.CANCEL -> {
                when (callerRole) {
                    UserRole.ADMIN,
                    UserRole.MANAGER -> DomainResult.Success(Unit)
                    else -> DomainResult.Error(
                        message = "Role '$callerRole' is not authorized to perform '$operation' on delivery challans."
                    )
                }
            }

            DeliveryChallanOperation.READY_FOR_DISPATCH -> {
                when (callerRole) {
                    UserRole.ADMIN,
                    UserRole.MANAGER,
                    UserRole.WAREHOUSE -> DomainResult.Success(Unit)
                    else -> DomainResult.Error(
                        message = "Role '$callerRole' is not authorized to perform '$operation' on delivery challans."
                    )
                }
            }
        }
    }
}

/**
 * Operations on Delivery Challans.
 */
enum class DeliveryChallanOperation {
    VIEW,
    CREATE,
    EDIT,
    SUBMIT,
    APPROVE,
    READY_FOR_DISPATCH,
    CANCEL
}

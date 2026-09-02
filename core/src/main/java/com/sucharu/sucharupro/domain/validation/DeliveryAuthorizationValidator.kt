package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Role-Based Access Control (RBAC) validator for Delivery Order operations (Module 08 Step 01).
 *
 * Permission matrix:
 * | Operation               | ADMIN | MANAGER | WAREHOUSE | STAFF | QC_INSPECTOR | ACCOUNTS | External |
 * |-------------------------|-------|---------|-----------|-------|--------------|----------|----------|
 * | VIEW                    | ✓     | ✓       | ✓         | ✓     | ✓            | ✓        | —        |
 * | CREATE                  | ✓     | ✓       | —         | —     | —            | —        | —        |
 * | EDIT                    | ✓     | ✓       | —         | —     | —            | —        | —        |
 * | SUBMIT                  | ✓     | ✓       | —         | —     | —            | —        | —        |
 * | APPROVE                 | ✓     | ✓       | —         | —     | —            | —        | —        |
 * | READY_FOR_DISPATCH      | ✓     | ✓       | ✓         | —     | —            | —        | —        |
 * | CANCEL                  | ✓     | ✓       | —         | —     | —            | —        | —        |
 * | CREATE_DISPATCH_REQUEST | ✓     | ✓       | ✓         | —     | —            | —        | —        |
 */
object DeliveryAuthorizationValidator {

    /**
     * Validates if the caller is authorized to perform a specific delivery operation.
     */
    fun validateOperation(
        callerRole: UserRole?,
        operation: DeliveryOperation,
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
            DeliveryOperation.VIEW -> {
                if (callerRole.isInternal) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "Role '$callerRole' is not authorized to view delivery orders.")
                }
            }

            DeliveryOperation.CREATE,
            DeliveryOperation.EDIT,
            DeliveryOperation.SUBMIT,
            DeliveryOperation.APPROVE,
            DeliveryOperation.CANCEL -> {
                when (callerRole) {
                    UserRole.ADMIN,
                    UserRole.MANAGER -> DomainResult.Success(Unit)
                    else -> DomainResult.Error(
                        message = "Role '$callerRole' is not authorized to perform '$operation' on delivery orders."
                    )
                }
            }

            DeliveryOperation.READY_FOR_DISPATCH,
            DeliveryOperation.CREATE_DISPATCH_REQUEST -> {
                when (callerRole) {
                    UserRole.ADMIN,
                    UserRole.MANAGER,
                    UserRole.WAREHOUSE -> DomainResult.Success(Unit)
                    else -> DomainResult.Error(
                        message = "Role '$callerRole' is not authorized to perform '$operation' on delivery orders."
                    )
                }
            }
        }
    }

    /**
     * Validates visibility of delivery orders based on status.
     */
    fun validateStatusVisibility(
        callerRole: UserRole?,
        status: DeliveryOrderStatus
    ): DomainResult<Unit> {
        if (callerRole == null) {
            return DomainResult.Error(message = "Caller role must be provided for authorization.")
        }

        return when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER -> DomainResult.Success(Unit)

            UserRole.WAREHOUSE -> {
                val allowedStatuses = listOf(
                    DeliveryOrderStatus.APPROVED,
                    DeliveryOrderStatus.READY_FOR_DISPATCH,
                    DeliveryOrderStatus.DISPATCHED,
                    DeliveryOrderStatus.DELIVERED
                )
                if (status in allowedStatuses) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "Warehouse staff cannot view orders in '$status' status.")
                }
            }

            else -> {
                if (callerRole.isInternal) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(message = "External role '$callerRole' is not authorized to view delivery order status.")
                }
            }
        }
    }
}

/**
 * Defines supported operations on Delivery Orders.
 */
enum class DeliveryOperation {
    VIEW,
    CREATE,
    EDIT,
    SUBMIT,
    APPROVE,
    READY_FOR_DISPATCH,
    CANCEL,
    CREATE_DISPATCH_REQUEST
}

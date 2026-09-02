package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole

enum class DeliveryShipmentOperation {
    CREATE,
    EDIT,
    MARK_READY,
    MARK_DISPATCHED,
    UPDATE_STATUS,
    RECORD_ATTEMPT,
    ADD_EVENT,
    CANCEL,
    VIEW
}

/**
 * Validates RBAC and multi-tenant project isolation for Delivery Shipments (Module 08 Step 05).
 */
object DeliveryShipmentAuthorizationValidator {

    fun validateOperation(
        callerRole: UserRole,
        operation: DeliveryShipmentOperation,
        targetProjectId: String,
        callerProjectId: String? = null
    ): DomainResult<Unit> {
        // Multi-tenant project boundary check
        if (callerProjectId != null && callerProjectId != targetProjectId) {
            return DomainResult.Error(
                message = "Access denied: Cross-project access from '$callerProjectId' to '$targetProjectId' is prohibited."
            )
        }

        return when (callerRole) {
            UserRole.ADMIN, UserRole.MANAGER -> DomainResult.Success(Unit)

            UserRole.WAREHOUSE -> {
                when (operation) {
                    DeliveryShipmentOperation.VIEW,
                    DeliveryShipmentOperation.CREATE,
                    DeliveryShipmentOperation.MARK_READY,
                    DeliveryShipmentOperation.MARK_DISPATCHED,
                    DeliveryShipmentOperation.UPDATE_STATUS,
                    DeliveryShipmentOperation.RECORD_ATTEMPT,
                    DeliveryShipmentOperation.ADD_EVENT -> DomainResult.Success(Unit)
                    else -> DomainResult.Error(
                        message = "Role '$callerRole' is not authorized to perform '$operation' on shipments."
                    )
                }
            }

            UserRole.STAFF, UserRole.QC_INSPECTOR, UserRole.ACCOUNTS, UserRole.DESIGNER, UserRole.AFFILIATE, UserRole.CUSTOMER -> {
                if (operation == DeliveryShipmentOperation.VIEW) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(
                        message = "Role '$callerRole' is not authorized to perform '$operation' on shipments."
                    )
                }
            }

            UserRole.VENDOR -> {
                DomainResult.Error(
                    message = "External role '$callerRole' is not authorized to access internal shipments."
                )
            }
        }
    }
}

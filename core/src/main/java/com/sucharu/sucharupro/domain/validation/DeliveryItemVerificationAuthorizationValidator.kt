package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole

enum class DeliveryItemVerificationOperation {
    CREATE,
    EDIT,
    SUBMIT,
    START_VERIFICATION,
    VERIFY_LINE,
    COMPLETE_VERIFICATION,
    CLOSE,
    CANCEL,
    VIEW,
    RESOLVE_ISSUES
}

/**
 * Validates RBAC and multi-tenant project isolation for Delivery Item Verifications (Module 08 Step 04).
 */
object DeliveryItemVerificationAuthorizationValidator {

    fun validateOperation(
        callerRole: UserRole,
        operation: DeliveryItemVerificationOperation,
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
                    DeliveryItemVerificationOperation.VIEW,
                    DeliveryItemVerificationOperation.CREATE,
                    DeliveryItemVerificationOperation.START_VERIFICATION,
                    DeliveryItemVerificationOperation.VERIFY_LINE,
                    DeliveryItemVerificationOperation.COMPLETE_VERIFICATION -> DomainResult.Success(Unit)
                    else -> DomainResult.Error(
                        message = "Role '$callerRole' is not authorized to perform '$operation' on delivery verifications."
                    )
                }
            }

            UserRole.QC_INSPECTOR -> {
                when (operation) {
                    DeliveryItemVerificationOperation.VIEW,
                    DeliveryItemVerificationOperation.VERIFY_LINE -> DomainResult.Success(Unit)
                    else -> DomainResult.Error(
                        message = "Role '$callerRole' is not authorized to perform '$operation' on delivery verifications."
                    )
                }
            }

            UserRole.STAFF, UserRole.ACCOUNTS, UserRole.DESIGNER, UserRole.AFFILIATE -> {
                if (operation == DeliveryItemVerificationOperation.VIEW) {
                    DomainResult.Success(Unit)
                } else {
                    DomainResult.Error(
                        message = "Role '$callerRole' is not authorized to perform '$operation' on delivery verifications."
                    )
                }
            }

            UserRole.CUSTOMER, UserRole.VENDOR -> {
                DomainResult.Error(
                    message = "External role '$callerRole' is not authorized to access internal delivery verifications."
                )
            }
        }
    }
}

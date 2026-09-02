package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Operations subject to RBAC authorization for Proof of Delivery (Module 08 Step 08).
 */
enum class DeliveryProofOperation {
    VIEW,
    CREATE,
    UPDATE,
    SUBMIT,
    REVIEW,
    VERIFY,
    ACCEPT,
    REJECT,
    CANCEL,
    ADD_EVIDENCE,
    REMOVE_EVIDENCE,
    CONFIRM_RECIPIENT
}

/**
 * RBAC and project isolation validator for Proof of Delivery operations (Module 08 Step 08).
 */
object DeliveryProofAuthorizationValidator {

    fun validateOperation(
        callerRole: UserRole,
        operation: DeliveryProofOperation,
        targetProjectId: String,
        callerProjectId: String? = null,
        actorId: String? = null,
        creatorId: String? = null
    ): DomainResult<Unit> {
        // 1. Project boundary check
        if (callerProjectId != null && callerProjectId != targetProjectId) {
            return DomainResult.Error(
                message = "Project isolation violation: Caller from project '$callerProjectId' cannot access POD in project '$targetProjectId'."
            )
        }

        // 2. Separation of duties: Creator cannot approve/accept own record (unless ADMIN)
        if (operation == DeliveryProofOperation.ACCEPT && actorId != null && creatorId != null && actorId == creatorId) {
            if (callerRole != UserRole.ADMIN) {
                return DomainResult.Error(
                    message = "Separation of duties: User '$actorId' created this POD and cannot approve/accept it."
                )
            }
        }

        // 3. Role-based operation permission
        val isAllowed = when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER -> true

            UserRole.WAREHOUSE -> operation in listOf(
                DeliveryProofOperation.VIEW,
                DeliveryProofOperation.CREATE,
                DeliveryProofOperation.UPDATE,
                DeliveryProofOperation.SUBMIT,
                DeliveryProofOperation.CANCEL,
                DeliveryProofOperation.ADD_EVIDENCE,
                DeliveryProofOperation.REMOVE_EVIDENCE,
                DeliveryProofOperation.CONFIRM_RECIPIENT
            )

            UserRole.QC_INSPECTOR -> operation in listOf(
                DeliveryProofOperation.VIEW,
                DeliveryProofOperation.REVIEW,
                DeliveryProofOperation.VERIFY
            )

            UserRole.STAFF -> operation in listOf(
                DeliveryProofOperation.VIEW,
                DeliveryProofOperation.ADD_EVIDENCE,
                DeliveryProofOperation.CONFIRM_RECIPIENT
            )

            UserRole.CUSTOMER -> operation in listOf(
                DeliveryProofOperation.VIEW,
                DeliveryProofOperation.CONFIRM_RECIPIENT
            )

            UserRole.ACCOUNTS -> operation in listOf(
                DeliveryProofOperation.VIEW
            )

            UserRole.DESIGNER,
            UserRole.VENDOR,
            UserRole.AFFILIATE -> operation in listOf(
                DeliveryProofOperation.VIEW
            )
        }

        return if (isAllowed) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(
                message = "Role '$callerRole' is not authorized to perform operation '$operation' on DeliveryProof."
            )
        }
    }
}

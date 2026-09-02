package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole

enum class DeliveryReconciliationOperation {
    VIEW,
    CREATE,
    UPDATE,
    REFRESH,
    START_RECONCILIATION,
    RESOLVE_DISCREPANCY,
    MARK_RECONCILED,
    CLOSE
}

/**
 * RBAC, Separation of Duties and Project Isolation validator for Delivery Reconciliation (Module 08 Step 09).
 */
object DeliveryReconciliationAuthorizationValidator {

    fun validateOperation(
        callerRole: UserRole,
        operation: DeliveryReconciliationOperation,
        targetProjectId: String,
        callerProjectId: String? = null,
        actorId: String? = null,
        creatorId: String? = null
    ): DomainResult<Unit> {
        // 1. Project Boundary Check
        if (callerProjectId != null && callerProjectId != targetProjectId) {
            return DomainResult.Error(
                message = "Project isolation violation: Caller from project '$callerProjectId' cannot access reconciliation in project '$targetProjectId'."
            )
        }

        // 2. Separation of Duties: Creator cannot close reconciliation (unless ADMIN)
        if (operation == DeliveryReconciliationOperation.CLOSE && actorId != null && creatorId != null && actorId == creatorId) {
            if (callerRole != UserRole.ADMIN) {
                return DomainResult.Error(
                    message = "Separation of duties: User '$actorId' created this reconciliation and cannot close it."
                )
            }
        }

        // 3. Role-based Operation Check
        val isAllowed = when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER -> true

            UserRole.WAREHOUSE -> operation in listOf(
                DeliveryReconciliationOperation.VIEW,
                DeliveryReconciliationOperation.CREATE,
                DeliveryReconciliationOperation.UPDATE,
                DeliveryReconciliationOperation.REFRESH,
                DeliveryReconciliationOperation.START_RECONCILIATION,
                DeliveryReconciliationOperation.MARK_RECONCILED
            )

            UserRole.QC_INSPECTOR -> operation in listOf(
                DeliveryReconciliationOperation.VIEW,
                DeliveryReconciliationOperation.REFRESH,
                DeliveryReconciliationOperation.RESOLVE_DISCREPANCY
            )

            UserRole.STAFF -> operation in listOf(
                DeliveryReconciliationOperation.VIEW,
                DeliveryReconciliationOperation.CREATE,
                DeliveryReconciliationOperation.REFRESH
            )

            UserRole.ACCOUNTS -> operation in listOf(
                DeliveryReconciliationOperation.VIEW
            )

            UserRole.CUSTOMER,
            UserRole.DESIGNER,
            UserRole.VENDOR,
            UserRole.AFFILIATE -> operation in listOf(
                DeliveryReconciliationOperation.VIEW
            )
        }

        return if (isAllowed) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(
                message = "Role '$callerRole' is not authorized to perform operation '$operation' on DeliveryReconciliation."
            )
        }
    }
}

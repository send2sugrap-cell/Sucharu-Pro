package com.sucharu.sucharupro.domain.validation.returns

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * RBAC operations for the Return Request domain (Module 11 Step 01).
 *
 * Permission boundaries:
 *   CREATE_RETURN     — raise a new Return Request
 *   VIEW_RETURN       — read a Return Request and its items
 *   INSPECT_RETURN    — perform in-warehouse inspection of a returned item
 *   APPROVE_RETURN    — approve an inspected return
 *   REJECT_RETURN     — reject an inspected return
 *   RECEIVE_RETURN    — mark physical goods as received in warehouse
 *   PROCESS_RETURN    — close / process a received return
 *   CANCEL_RETURN     — cancel a Return Request
 */
enum class ReturnOperation {
    CREATE_RETURN,
    VIEW_RETURN,
    INSPECT_RETURN,
    APPROVE_RETURN,
    REJECT_RETURN,
    RECEIVE_RETURN,
    PROCESS_RETURN,
    CANCEL_RETURN,
    SETTLE_RETURN
}

/**
 * RBAC authorization validator for Return Request operations (Module 11 Step 01, Step 05).
 *
 * Uses the existing [UserRole] definitions and [DomainResult] pattern — no new roles
 * or new result types are introduced.
 *
 * Project isolation is enforced here: a caller from project A cannot operate on
 * a Return belonging to project B.
 */
object ReturnAuthorizationValidator {

    /**
     * Validates whether [callerRole] is allowed to perform [operation] on a Return
     * belonging to [targetProjectId], given that the caller operates in [callerProjectId].
     *
     * Returns [DomainResult.Success] when authorized, [DomainResult.Error] otherwise.
     */
    fun validateOperation(
        callerRole: UserRole,
        operation: ReturnOperation,
        targetProjectId: String,
        callerProjectId: String? = null
    ): DomainResult<Unit> {
        // Project isolation — enforced before role check
        if (callerProjectId != null && callerProjectId != targetProjectId) {
            return DomainResult.Error(
                message = "Access denied: Caller project '$callerProjectId' cannot operate " +
                    "on Return in project '$targetProjectId'."
            )
        }

        val isAuthorized = when (callerRole) {
            // Full authority over all Return operations
            UserRole.ADMIN,
            UserRole.MANAGER -> true

            // Accounts can view returns and execute financial settlement
            UserRole.ACCOUNTS -> when (operation) {
                ReturnOperation.VIEW_RETURN,
                ReturnOperation.SETTLE_RETURN -> true
                else -> false
            }

            // Warehouse staff handle the physical side of returns
            UserRole.WAREHOUSE -> when (operation) {
                ReturnOperation.VIEW_RETURN,
                ReturnOperation.RECEIVE_RETURN,
                ReturnOperation.INSPECT_RETURN,
                ReturnOperation.PROCESS_RETURN,
                ReturnOperation.CANCEL_RETURN -> true
                ReturnOperation.CREATE_RETURN,
                ReturnOperation.APPROVE_RETURN,
                ReturnOperation.REJECT_RETURN,
                ReturnOperation.SETTLE_RETURN -> false
            }

            // QC inspectors can view and perform inspection
            UserRole.QC_INSPECTOR -> when (operation) {
                ReturnOperation.VIEW_RETURN,
                ReturnOperation.INSPECT_RETURN -> true
                else -> false
            }

            // General staff can create and view return requests
            UserRole.STAFF -> when (operation) {
                ReturnOperation.VIEW_RETURN,
                ReturnOperation.CREATE_RETURN -> true
                else -> false
            }

            // Designer has no business involvement in returns
            UserRole.DESIGNER -> false

            // External actors — no return operations allowed
            UserRole.CUSTOMER,
            UserRole.VENDOR,
            UserRole.AFFILIATE -> false
        }

        return if (isAuthorized) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(
                message = "Role '$callerRole' is unauthorized to perform '$operation' on Return Requests."
            )
        }
    }
}

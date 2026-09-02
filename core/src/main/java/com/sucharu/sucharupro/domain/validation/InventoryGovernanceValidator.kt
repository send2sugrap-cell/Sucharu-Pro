package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.analytics.InventoryExceptionStatus

/**
 * Validator for Inventory Governance workflows (Module 07 Step 10).
 */
object InventoryGovernanceValidator {

    /**
     * Validates transitions for inventory exceptions.
     * Path: OPEN -> ACKNOWLEDGED -> RESOLVED.
     * Prevents transitions from terminal states (RESOLVED, DISMISSED).
     */
    fun validateExceptionTransition(
        from: InventoryExceptionStatus,
        to: InventoryExceptionStatus
    ): DomainResult<Unit> {
        if (from == to) return DomainResult.Success(Unit)

        // Terminal states
        if (from == InventoryExceptionStatus.RESOLVED || from == InventoryExceptionStatus.DISMISSED) {
            return DomainResult.Error(message = "Cannot transition from terminal state '${from.name}'.")
        }

        val isValid = when (from) {
            InventoryExceptionStatus.OPEN -> {
                to == InventoryExceptionStatus.ACKNOWLEDGED || to == InventoryExceptionStatus.DISMISSED
            }
            InventoryExceptionStatus.ACKNOWLEDGED -> {
                to == InventoryExceptionStatus.RESOLVED || to == InventoryExceptionStatus.DISMISSED
            }
            else -> false
        }

        return if (isValid) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Invalid status transition from '${from.name}' to '${to.name}'.")
        }
    }
}

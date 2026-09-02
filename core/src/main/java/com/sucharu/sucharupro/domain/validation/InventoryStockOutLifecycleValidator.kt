package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOutStatus

/**
 * Domain validator for stock out lifecycle state-machine transitions (Module 07 Step 04).
 *
 * State machine:
 *   DRAFT → PENDING → ISSUING → COMPLETED (terminal)
 *   DRAFT / PENDING → CANCELLED (terminal)
 *
 * All methods are pure and side-effect-free.
 */
object InventoryStockOutLifecycleValidator {

    /**
     * Validates a stock-out status transition.
     */
    fun validateTransition(
        current: InventoryStockOutStatus,
        target: InventoryStockOutStatus
    ): DomainResult<Unit> {
        if (current == target) return DomainResult.Success(Unit)

        if (current.isTerminal) {
            return DomainResult.Error(
                message = "Stock-out is in terminal state '${current.defaultLabel}' and cannot transition to '${target.defaultLabel}'."
            )
        }

        val allowed = when (current) {
            InventoryStockOutStatus.DRAFT -> setOf(
                InventoryStockOutStatus.PENDING,
                InventoryStockOutStatus.CANCELLED
            )
            InventoryStockOutStatus.PENDING -> setOf(
                InventoryStockOutStatus.ISSUING,
                InventoryStockOutStatus.CANCELLED
            )
            InventoryStockOutStatus.ISSUING -> setOf(
                InventoryStockOutStatus.COMPLETED
            )
            InventoryStockOutStatus.COMPLETED,
            InventoryStockOutStatus.CANCELLED -> emptySet()
        }

        return if (target in allowed) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(
                message = "Invalid stock-out transition: '${current.defaultLabel}' → '${target.defaultLabel}'. " +
                    "Allowed from '${current.defaultLabel}': ${allowed.map { it.defaultLabel }}."
            )
        }
    }

    /**
     * Checks if the stock-out can be mutated in its current status.
     * Terminal states (COMPLETED, CANCELLED) are immutable.
     */
    fun canMutate(status: InventoryStockOutStatus): Boolean {
        return !status.isTerminal
    }
}

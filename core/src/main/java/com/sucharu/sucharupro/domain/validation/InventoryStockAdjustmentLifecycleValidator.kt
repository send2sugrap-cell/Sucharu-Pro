package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryStockAdjustmentStatus

/**
 * Domain validator for stock adjustment lifecycle state-machine transitions (Module 07 Step 06).
 *
 * Enforces explicit, legal transitions to prevent arbitrary state jumps and
 * protect terminal states from mutation.
 *
 * All methods are pure and side-effect-free.
 */
object InventoryStockAdjustmentLifecycleValidator {

    /**
     * Validates an adjustment-level status transition.
     *
     * Legal transitions:
     *   DRAFT     → PENDING, CANCELLED
     *   PENDING   → APPROVED, CANCELLED
     *   APPROVED  → ADJUSTING, CANCELLED
     *   ADJUSTING → COMPLETED
     *   COMPLETED → (terminal, no transitions)
     *   CANCELLED → (terminal, no transitions)
     */
    fun validateAdjustmentTransition(
        current: InventoryStockAdjustmentStatus,
        target: InventoryStockAdjustmentStatus
    ): DomainResult<Unit> {
        if (current == target) return DomainResult.Success(Unit)

        if (current.isTerminal) {
            return DomainResult.Error(
                message = "Adjustment is in terminal state '${current.defaultLabel}' and cannot transition to '${target.defaultLabel}'."
            )
        }

        val allowed = when (current) {
            InventoryStockAdjustmentStatus.DRAFT -> setOf(
                InventoryStockAdjustmentStatus.PENDING,
                InventoryStockAdjustmentStatus.CANCELLED
            )
            InventoryStockAdjustmentStatus.PENDING -> setOf(
                InventoryStockAdjustmentStatus.APPROVED,
                InventoryStockAdjustmentStatus.CANCELLED
            )
            InventoryStockAdjustmentStatus.APPROVED -> setOf(
                InventoryStockAdjustmentStatus.ADJUSTING,
                InventoryStockAdjustmentStatus.CANCELLED
            )
            InventoryStockAdjustmentStatus.ADJUSTING -> setOf(
                InventoryStockAdjustmentStatus.COMPLETED
            )
            InventoryStockAdjustmentStatus.COMPLETED,
            InventoryStockAdjustmentStatus.CANCELLED -> emptySet()
        }

        return if (target in allowed) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(
                message = "Invalid adjustment transition: '${current.defaultLabel}' → '${target.defaultLabel}'. " +
                    "Allowed from '${current.defaultLabel}': ${allowed.map { it.defaultLabel }}."
            )
        }
    }
}

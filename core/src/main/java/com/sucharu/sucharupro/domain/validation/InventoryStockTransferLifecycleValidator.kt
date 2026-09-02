package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransferStatus

/**
 * Domain validator for stock transfer lifecycle state-machine transitions (Module 07 Step 05).
 *
 * State machine:
 *   DRAFT → PENDING → APPROVED → TRANSFERRING → COMPLETED (terminal)
 *   DRAFT / PENDING / APPROVED → CANCELLED (terminal)
 *
 * All methods are pure and side-effect-free.
 */
object InventoryStockTransferLifecycleValidator {

    /**
     * Validates a stock transfer status transition.
     */
    fun validateTransition(
        current: InventoryStockTransferStatus,
        target: InventoryStockTransferStatus
    ): DomainResult<Unit> {
        if (current == target) return DomainResult.Success(Unit)

        if (current.isTerminal) {
            return DomainResult.Error(
                message = "Stock transfer is in terminal state '${current.defaultLabel}' and cannot transition to '${target.defaultLabel}'."
            )
        }

        val allowed = when (current) {
            InventoryStockTransferStatus.DRAFT -> setOf(
                InventoryStockTransferStatus.PENDING,
                InventoryStockTransferStatus.CANCELLED
            )
            InventoryStockTransferStatus.PENDING -> setOf(
                InventoryStockTransferStatus.APPROVED,
                InventoryStockTransferStatus.CANCELLED
            )
            InventoryStockTransferStatus.APPROVED -> setOf(
                InventoryStockTransferStatus.TRANSFERRING,
                InventoryStockTransferStatus.CANCELLED
            )
            InventoryStockTransferStatus.TRANSFERRING -> setOf(
                InventoryStockTransferStatus.COMPLETED
            )
            InventoryStockTransferStatus.COMPLETED,
            InventoryStockTransferStatus.CANCELLED -> emptySet()
        }

        return if (target in allowed) {
            DomainResult.Success(Unit)
        } else {
            val allowedLabels = allowed.joinToString { it.defaultLabel }
            DomainResult.Error(
                message = "Invalid stock transfer transition: '${current.defaultLabel}' → '${target.defaultLabel}'. " +
                    "Allowed from '${current.defaultLabel}': $allowedLabels."
            )
        }
    }

    /**
     * Checks if the stock transfer can be mutated in its current status.
     * Terminal states (COMPLETED, CANCELLED) are immutable.
     */
    fun canMutate(status: InventoryStockTransferStatus): Boolean {
        return !status.isTerminal
    }
}

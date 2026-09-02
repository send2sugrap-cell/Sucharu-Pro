package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryAdjustmentType
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryStockAdjustmentLine

/**
 * Domain validator for adjustment line quantity and structural rules (Module 07 Step 06).
 *
 * All methods are pure and side-effect-free.
 */
object InventoryStockAdjustmentLineValidator {

    /**
     * Validates the structural invariants of an [InventoryStockAdjustmentLine].
     */
    fun validateLine(line: InventoryStockAdjustmentLine): DomainResult<Unit> {
        if (line.adjustmentLineId.isBlank()) {
            return DomainResult.Error(message = "Adjustment line ID cannot be blank.")
        }
        if (line.adjustmentId.isBlank()) {
            return DomainResult.Error(message = "Adjustment ID cannot be blank.")
        }
        if (line.projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        if (line.inventoryProductId.isBlank()) {
            return DomainResult.Error(message = "Inventory product ID cannot be blank.")
        }
        if (line.warehouseId.isBlank()) {
            return DomainResult.Error(message = "Warehouse ID cannot be blank.")
        }
        if (line.locationId.isBlank()) {
            return DomainResult.Error(message = "Location ID cannot be blank.")
        }
        if (line.createdAt.isBlank()) {
            return DomainResult.Error(message = "createdAt timestamp cannot be blank.")
        }
        if (line.updatedAt.isBlank()) {
            return DomainResult.Error(message = "updatedAt timestamp cannot be blank.")
        }
        if (line.updatedAt < line.createdAt) {
            return DomainResult.Error(
                message = "Line updatedAt (${line.updatedAt}) cannot precede createdAt (${line.createdAt})."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates quantity constraints for stock adjustment.
     *
     * Rules:
     *   quantityChange must be non-zero.
     *   DECREASE must not result in negative stock: currentQuantity + quantityChange >= 0.
     *   Structural check: quantityChange == adjustedQuantity - currentQuantity.
     */
    fun validateAdjustmentQuantity(
        type: InventoryAdjustmentType,
        currentQuantity: Int,
        adjustedQuantity: Int,
        quantityChange: Int
    ): DomainResult<Unit> {
        if (currentQuantity < 0) {
            return DomainResult.Error(message = "currentQuantity cannot be negative (was $currentQuantity).")
        }
        if (adjustedQuantity < 0) {
            return DomainResult.Error(message = "adjustedQuantity cannot be negative (was $adjustedQuantity).")
        }

        val calculatedChange = adjustedQuantity - currentQuantity
        if (quantityChange != calculatedChange) {
            return DomainResult.Error(
                message = "quantityChange ($quantityChange) must match adjustedQuantity - currentQuantity ($calculatedChange)."
            )
        }

        if (quantityChange == 0) {
            return DomainResult.Error(message = "quantityChange cannot be zero. No adjustment needed.")
        }

        if (type == InventoryAdjustmentType.INCREASE && quantityChange <= 0) {
            return DomainResult.Error(message = "INCREASE adjustment must have a positive quantityChange.")
        }

        if (type == InventoryAdjustmentType.DECREASE) {
            if (quantityChange >= 0) {
                return DomainResult.Error(message = "DECREASE adjustment must have a negative quantityChange.")
            }
            if (currentQuantity + quantityChange < 0) {
                return DomainResult.Error(
                    message = "Insufficient stock for DECREASE. currentQuantity ($currentQuantity) + quantityChange ($quantityChange) would be negative."
                )
            }
        }

        return DomainResult.Success(Unit)
    }
}

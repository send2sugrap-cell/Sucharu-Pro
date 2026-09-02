package com.sucharu.sucharupro.domain.model.inventory.adjustment

import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit

/**
 * Individual stock adjustment line representing a correction for one product
 * (Module 07 Step 06).
 */
data class InventoryStockAdjustmentLine(
    val adjustmentLineId: String,
    val adjustmentId: String,
    val projectId: String,
    val inventoryProductId: String,
    val warehouseId: String,
    val locationId: String,
    val adjustmentType: InventoryAdjustmentType,
    val adjustmentReason: InventoryAdjustmentReason,
    val currentQuantity: Int,
    val adjustedQuantity: Int,
    val quantityChange: Int,
    val unit: InventoryUnit = InventoryUnit.PCS,
    val notes: String? = null,
    val createdAt: String,
    val updatedAt: String
) {
    init {
        require(adjustmentLineId.isNotBlank()) { "Adjustment line ID cannot be blank." }
        require(adjustmentId.isNotBlank()) { "Adjustment ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(inventoryProductId.isNotBlank()) { "Inventory product ID cannot be blank." }
        require(warehouseId.isNotBlank()) { "Warehouse ID cannot be blank." }
        require(locationId.isNotBlank()) { "Location ID cannot be blank." }
        require(currentQuantity >= 0) { "currentQuantity cannot be negative." }
        require(adjustedQuantity >= 0) { "adjustedQuantity cannot be negative." }
        require(createdAt.isNotBlank()) { "createdAt timestamp cannot be blank." }
        require(updatedAt.isNotBlank()) { "updatedAt timestamp cannot be blank." }
        require(updatedAt >= createdAt) { "updatedAt ($updatedAt) cannot precede createdAt ($createdAt)." }

        val calculatedChange = adjustedQuantity - currentQuantity
        require(quantityChange == calculatedChange) {
            "quantityChange ($quantityChange) must match adjustedQuantity - currentQuantity ($calculatedChange)."
        }

        if (adjustmentType == InventoryAdjustmentType.INCREASE) {
            require(quantityChange > 0) { "INCREASE adjustment must have a positive quantityChange." }
        } else {
            require(quantityChange < 0) { "DECREASE adjustment must have a negative quantityChange." }
        }
    }
}

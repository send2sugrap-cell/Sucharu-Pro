package com.sucharu.sucharupro.domain.model.inventory.adjustment

import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit

/**
 * Immutable stock adjustment record generated when an adjustment line is processed
 * (Module 07 Step 06).
 *
 * This record represents the foundational adjustment event that updates inventory levels.
 */
data class InventoryStockAdjustmentRecord(
    val adjustmentRecordId: String,
    val adjustmentId: String,
    val adjustmentLineId: String,
    val projectId: String,
    val inventoryProductId: String,
    val warehouseId: String,
    val locationId: String,
    val adjustmentType: InventoryAdjustmentType,
    val adjustmentReason: InventoryAdjustmentReason,
    val quantity: Int,
    val unit: InventoryUnit,
    val createdBy: String,
    val createdAt: String,
    val sourceReference: String? = null
) {
    init {
        require(adjustmentRecordId.isNotBlank()) { "Adjustment record ID cannot be blank." }
        require(adjustmentId.isNotBlank()) { "Adjustment ID cannot be blank." }
        require(adjustmentLineId.isNotBlank()) { "Adjustment line ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(inventoryProductId.isNotBlank()) { "Inventory product ID cannot be blank." }
        require(warehouseId.isNotBlank()) { "Warehouse ID cannot be blank." }
        require(locationId.isNotBlank()) { "Location ID cannot be blank." }
        require(quantity > 0) { "Adjustment quantity must be greater than zero (was $quantity)." }
        require(createdBy.isNotBlank()) { "createdBy actor cannot be blank." }
        require(createdAt.isNotBlank()) { "createdAt timestamp cannot be blank." }
    }
}

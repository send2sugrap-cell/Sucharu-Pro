package com.sucharu.sucharupro.domain.model.inventory.stockout

import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit

/**
 * Individual stock-out line representing one product being issued in a stock operation
 * (Module 07 Step 04).
 *
 * Quantity invariants:
 *   expectedQuantity >= 0
 *   issuedQuantity >= 0
 *   issuedQuantity <= expectedQuantity (typically, though over-issuing might be allowed depending on business rules)
 */
data class InventoryStockOutLine(
    val stockOutLineId: String,
    val stockOutId: String,
    val projectId: String,
    val inventoryProductId: String,
    val warehouseId: String,
    val locationId: String,
    val expectedQuantity: Int = 0,
    val issuedQuantity: Int = 0,
    val unit: InventoryUnit = InventoryUnit.PCS,
    val notes: String? = null,
    val createdAt: String,
    val updatedAt: String
) {
    init {
        require(stockOutLineId.isNotBlank()) { "Stock-out line ID cannot be blank." }
        require(stockOutId.isNotBlank()) { "Stock-out ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(inventoryProductId.isNotBlank()) { "Inventory product ID cannot be blank." }
        require(warehouseId.isNotBlank()) { "Warehouse ID cannot be blank." }
        require(locationId.isNotBlank()) { "Location ID cannot be blank." }
        require(expectedQuantity >= 0) { "expectedQuantity cannot be negative." }
        require(issuedQuantity >= 0) { "issuedQuantity cannot be negative." }
        require(createdAt.isNotBlank()) { "createdAt timestamp cannot be blank." }
        require(updatedAt.isNotBlank()) { "updatedAt timestamp cannot be blank." }
        require(updatedAt >= createdAt) { "updatedAt ($updatedAt) cannot precede createdAt ($createdAt)." }
    }

    /**
     * True when issued quantity matches or exceeds expected quantity.
     */
    val isFulfilled: Boolean
        get() = issuedQuantity >= expectedQuantity && expectedQuantity > 0
}

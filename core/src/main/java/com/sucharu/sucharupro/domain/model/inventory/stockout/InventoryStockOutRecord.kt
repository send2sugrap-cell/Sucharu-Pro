package com.sucharu.sucharupro.domain.model.inventory.stockout

import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit

/**
 * Immutable stock-out record generated when a stock-out line is processed with issued quantity
 * (Module 07 Step 04).
 *
 * This record represents the foundational stock-out event that reduces inventory quantity
 * at a specific warehouse location. It is write-once.
 */
data class InventoryStockOutRecord(
    val stockOutRecordId: String,
    val stockOutId: String,
    val stockOutLineId: String,
    val projectId: String,
    val inventoryProductId: String,
    val warehouseId: String,
    val locationId: String,
    val quantity: Int,
    val unit: InventoryUnit,
    val createdBy: String,
    val createdAt: String,
    val sourceReference: String? = null
) {
    init {
        require(stockOutRecordId.isNotBlank()) { "Stock-out record ID cannot be blank." }
        require(stockOutId.isNotBlank()) { "Stock-out ID cannot be blank." }
        require(stockOutLineId.isNotBlank()) { "Stock-out line ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(inventoryProductId.isNotBlank()) { "Inventory product ID cannot be blank." }
        require(warehouseId.isNotBlank()) { "Warehouse ID cannot be blank." }
        require(locationId.isNotBlank()) { "Location ID cannot be blank." }
        require(quantity > 0) { "Stock-out quantity must be greater than zero (was $quantity)." }
        require(createdBy.isNotBlank()) { "createdBy actor cannot be blank." }
        require(createdAt.isNotBlank()) { "createdAt timestamp cannot be blank." }
    }
}

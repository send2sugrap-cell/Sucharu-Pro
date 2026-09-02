package com.sucharu.sucharupro.domain.model.inventory.receiving

import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit

/**
 * Immutable stock-in record generated when a receiving line is completed with accepted quantity
 * (Module 07 Step 03).
 *
 * This record represents the foundational stock-in event that establishes accepted inventory
 * quantity at a specific warehouse location. It is write-once: once created it must never
 * be mutated or deleted.
 *
 * Important:
 * - Exactly ONE StockInRecord is produced per finalized receiving line that has acceptedQuantity > 0.
 * - Receiving lines with acceptedQuantity == 0 produce NO stock-in record.
 * - This is NOT the full inventory movement ledger (that belongs to Step 09).
 *   It only establishes the first accepted quantity boundary.
 *
 * Duplicate protection:
 * - The combination (receivingId + receivingLineId) must be unique in the system.
 * - Concurrent completion calls must result in exactly one StockInRecord per line.
 */
data class InventoryStockInRecord(
    val stockInId: String,
    val receivingId: String,
    val receivingLineId: String,
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
        require(stockInId.isNotBlank()) { "Stock-in ID cannot be blank." }
        require(receivingId.isNotBlank()) { "Receiving ID cannot be blank." }
        require(receivingLineId.isNotBlank()) { "Receiving line ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(inventoryProductId.isNotBlank()) { "Inventory product ID cannot be blank." }
        require(warehouseId.isNotBlank()) { "Warehouse ID cannot be blank." }
        require(locationId.isNotBlank()) { "Location ID cannot be blank." }
        require(quantity > 0) { "Stock-in quantity must be greater than zero (was $quantity)." }
        require(createdBy.isNotBlank()) { "createdBy actor cannot be blank." }
        require(createdAt.isNotBlank()) { "createdAt timestamp cannot be blank." }
    }
}

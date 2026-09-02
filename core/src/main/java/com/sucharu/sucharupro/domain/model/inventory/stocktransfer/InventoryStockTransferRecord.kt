package com.sucharu.sucharupro.domain.model.inventory.stocktransfer

import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit

/**
 * Immutable stock transfer record generated when a transfer line is processed (Module 07 Step 05).
 *
 * This record represents the foundational transfer event that reduces inventory at the
 * source location and increases it at the destination location.
 */
data class InventoryStockTransferRecord(
    val transferRecordId: String,
    val transferId: String,
    val transferLineId: String,
    val projectId: String,
    val inventoryProductId: String,
    val fromWarehouseId: String,
    val fromLocationId: String,
    val toWarehouseId: String,
    val toLocationId: String,
    val quantity: Int,
    val unit: InventoryUnit,
    val createdBy: String,
    val createdAt: String,
    val sourceReference: String? = null
) {
    init {
        require(transferRecordId.isNotBlank()) { "Transfer record ID cannot be blank." }
        require(transferId.isNotBlank()) { "Transfer ID cannot be blank." }
        require(transferLineId.isNotBlank()) { "Transfer line ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(inventoryProductId.isNotBlank()) { "Inventory product ID cannot be blank." }
        require(fromWarehouseId.isNotBlank()) { "From Warehouse ID cannot be blank." }
        require(fromLocationId.isNotBlank()) { "From Location ID cannot be blank." }
        require(toWarehouseId.isNotBlank()) { "To Warehouse ID cannot be blank." }
        require(toLocationId.isNotBlank()) { "To Location ID cannot be blank." }
        require(quantity > 0) { "Transfer quantity must be greater than zero (was $quantity)." }
        require(createdBy.isNotBlank()) { "createdBy actor cannot be blank." }
        require(createdAt.isNotBlank()) { "createdAt timestamp cannot be blank." }
    }
}

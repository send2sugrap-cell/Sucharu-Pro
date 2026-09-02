package com.sucharu.sucharupro.domain.model.inventory.stocktransfer

import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit

/**
 * Individual stock transfer line representing one product being transferred between
 * warehouses (Module 07 Step 05).
 *
 * Quantity invariants:
 *   expectedQuantity >= 0
 *   transferredQuantity >= 0
 *   transferredQuantity <= expectedQuantity (typically)
 */
data class InventoryStockTransferLine(
    val transferLineId: String,
    val transferId: String,
    val projectId: String,
    val inventoryProductId: String,
    val fromWarehouseId: String,
    val fromLocationId: String,
    val toWarehouseId: String,
    val toLocationId: String,
    val expectedQuantity: Int = 0,
    val transferredQuantity: Int = 0,
    val unit: InventoryUnit = InventoryUnit.PCS,
    val notes: String? = null,
    val createdAt: String,
    val updatedAt: String
) {
    init {
        require(transferLineId.isNotBlank()) { "Transfer line ID cannot be blank." }
        require(transferId.isNotBlank()) { "Transfer ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(inventoryProductId.isNotBlank()) { "Inventory product ID cannot be blank." }
        require(fromWarehouseId.isNotBlank()) { "From Warehouse ID cannot be blank." }
        require(fromLocationId.isNotBlank()) { "From Location ID cannot be blank." }
        require(toWarehouseId.isNotBlank()) { "To Warehouse ID cannot be blank." }
        require(toLocationId.isNotBlank()) { "To Location ID cannot be blank." }
        require(expectedQuantity >= 0) { "expectedQuantity cannot be negative." }
        require(transferredQuantity >= 0) { "transferredQuantity cannot be negative." }
        require(createdAt.isNotBlank()) { "createdAt timestamp cannot be blank." }
        require(updatedAt.isNotBlank()) { "updatedAt timestamp cannot be blank." }
        require(updatedAt >= createdAt) { "updatedAt ($updatedAt) cannot precede createdAt ($createdAt)." }
    }

    /**
     * True when transferred quantity matches or exceeds expected quantity.
     */
    val isFulfilled: Boolean
        get() = transferredQuantity >= expectedQuantity && expectedQuantity > 0
}

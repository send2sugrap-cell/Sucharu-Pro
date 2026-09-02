package com.sucharu.sucharupro.domain.model.inventory.receiving

import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit

/**
 * Individual receiving line representing one product being received in a stock operation
 * (Module 07 Step 03).
 *
 * Quantity invariants (always enforced):
 *   expectedQuantity >= 0
 *   receivedQuantity > 0
 *   acceptedQuantity >= 0
 *   rejectedQuantity >= 0
 *   acceptedQuantity + rejectedQuantity <= receivedQuantity
 *
 * For a finalized line:
 *   acceptedQuantity + rejectedQuantity == receivedQuantity
 *
 * Accepted quantity (> 0) produces exactly one [InventoryStockInRecord].
 * Rejected quantity MUST NOT enter stock.
 */
data class InventoryReceivingLine(
    val receivingLineId: String,
    val receivingId: String,
    val projectId: String,
    val inventoryProductId: String,
    val warehouseId: String,
    val locationId: String,
    val expectedQuantity: Int = 0,
    val receivedQuantity: Int = 0,
    val acceptedQuantity: Int = 0,
    val rejectedQuantity: Int = 0,
    val unit: InventoryUnit = InventoryUnit.PCS,
    val lineStatus: InventoryReceivingLineStatus = InventoryReceivingLineStatus.PENDING,
    val rejectionReason: String? = null,
    val notes: String? = null,
    val createdAt: String,
    val updatedAt: String
) {
    init {
        require(receivingLineId.isNotBlank()) { "Receiving line ID cannot be blank." }
        require(receivingId.isNotBlank()) { "Receiving ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(inventoryProductId.isNotBlank()) { "Inventory product ID cannot be blank." }
        require(warehouseId.isNotBlank()) { "Warehouse ID cannot be blank." }
        require(locationId.isNotBlank()) { "Location ID cannot be blank." }
        require(expectedQuantity >= 0) { "expectedQuantity cannot be negative." }
        require(receivedQuantity >= 0) { "receivedQuantity cannot be negative." }
        require(acceptedQuantity >= 0) { "acceptedQuantity cannot be negative." }
        require(rejectedQuantity >= 0) { "rejectedQuantity cannot be negative." }
        require(acceptedQuantity + rejectedQuantity <= receivedQuantity) {
            "acceptedQuantity ($acceptedQuantity) + rejectedQuantity ($rejectedQuantity) " +
                "cannot exceed receivedQuantity ($receivedQuantity)."
        }
        require(createdAt.isNotBlank()) { "createdAt timestamp cannot be blank." }
        require(updatedAt.isNotBlank()) { "updatedAt timestamp cannot be blank." }
        require(updatedAt >= createdAt) { "updatedAt ($updatedAt) cannot precede createdAt ($createdAt)." }
    }

    /**
     * True when accepted + rejected equals received (quantities fully reconciled).
     */
    val isQuantityReconciled: Boolean
        get() = (acceptedQuantity + rejectedQuantity) == receivedQuantity

    /**
     * True for finalized lines that have a terminal line status and reconciled quantities.
     */
    val isFullyFinalized: Boolean
        get() = lineStatus.isFinalized && isQuantityReconciled
}

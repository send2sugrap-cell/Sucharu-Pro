package com.sucharu.sucharupro.domain.model.inventory.traceability

import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit

/**
 * Types of inventory movements recorded for traceability (Module 07 Step 07).
 */
enum class InventoryMovementType(val defaultLabel: String) {
    STOCK_IN("Stock In"),
    STOCK_OUT("Stock Out"),
    TRANSFER_OUT("Transfer Out"),
    TRANSFER_IN("Transfer In"),
    ADJUSTMENT_IN("Adjustment In"),
    ADJUSTMENT_OUT("Adjustment Out")
}

/**
 * Immutable record mapping batches and lots to inventory movements (Module 07 Step 07).
 */
data class InventoryTraceabilityRecord(
    val traceRecordId: String,
    val batchId: String?,
    val lotId: String?,
    val projectId: String,
    val productId: String,
    val locationId: String,
    val movementRecordId: String,
    val movementType: InventoryMovementType,
    val quantity: Double,
    val unit: InventoryUnit,
    val actorId: String,
    val timestamp: String
) {
    init {
        require(traceRecordId.isNotBlank()) { "Trace Record ID cannot be blank." }
        require(batchId != null || lotId != null) { "Either Batch ID or Lot ID must be provided." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(productId.isNotBlank()) { "Product ID cannot be blank." }
        require(locationId.isNotBlank()) { "Location ID cannot be blank." }
        require(movementRecordId.isNotBlank()) { "Movement Record ID cannot be blank." }
        require(quantity >= 0) { "Quantity cannot be negative." }
        require(actorId.isNotBlank()) { "Actor ID cannot be blank." }
        require(timestamp.isNotBlank()) { "Timestamp cannot be blank." }
    }
}

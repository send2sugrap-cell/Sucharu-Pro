package com.sucharu.sucharupro.domain.model.inventory.traceability

/**
 * Represents an inventory lot, optionally associated with a production batch (Module 07 Step 07).
 */
data class InventoryLot(
    val lotId: String,
    val lotNo: String,
    val projectId: String,
    val productId: String,
    val batchId: String? = null,
    val status: InventoryTraceabilityStatus,
    val createdAt: String,
    val notes: String? = null
) {
    init {
        require(lotId.isNotBlank()) { "Lot ID cannot be blank." }
        require(lotNo.isNotBlank()) { "Lot Number cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(productId.isNotBlank()) { "Product ID cannot be blank." }
        require(createdAt.isNotBlank()) { "Created timestamp cannot be blank." }
    }
}

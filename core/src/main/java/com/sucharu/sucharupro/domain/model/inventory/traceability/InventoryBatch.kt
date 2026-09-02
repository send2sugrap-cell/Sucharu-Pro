package com.sucharu.sucharupro.domain.model.inventory.traceability

/**
 * Represents a production batch for tracking and traceability (Module 07 Step 07).
 */
data class InventoryBatch(
    val batchId: String,
    val batchNo: String,
    val projectId: String,
    val productId: String,
    val productionReferenceId: String?,
    val productionReferenceType: String?,
    val status: InventoryTraceabilityStatus,
    val createdAt: String,
    val notes: String? = null
) {
    init {
        require(batchId.isNotBlank()) { "Batch ID cannot be blank." }
        require(batchNo.isNotBlank()) { "Batch Number cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(productId.isNotBlank()) { "Product ID cannot be blank." }
        require(createdAt.isNotBlank()) { "Created timestamp cannot be blank." }
    }
}

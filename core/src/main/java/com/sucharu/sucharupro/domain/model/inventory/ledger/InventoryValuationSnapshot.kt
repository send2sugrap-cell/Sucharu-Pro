package com.sucharu.sucharupro.domain.model.inventory.ledger

/**
 * Read-side valuation snapshot for reporting and financial integrity (Module 07 Step 09).
 */
data class InventoryValuationSnapshot(
    val snapshotId: String,
    val projectId: String,
    val productId: String,
    val locationId: String? = null,
    val quantity: Double,
    val unitCost: Double,
    val totalValue: Double,
    val valuationMethod: InventoryValuationMethod,
    val calculatedAt: String
) {
    init {
        require(snapshotId.isNotBlank()) { "Snapshot ID cannot be blank" }
        require(projectId.isNotBlank()) { "Project ID cannot be blank" }
        require(productId.isNotBlank()) { "Product ID cannot be blank" }
        require(quantity >= 0) { "Quantity cannot be negative" }
        require(unitCost >= 0) { "Unit cost cannot be negative" }
        require(totalValue >= 0) { "Total value cannot be negative" }
        require(calculatedAt.isNotBlank()) { "calculatedAt timestamp cannot be blank" }
    }
}

/**
 * Supported inventory valuation methodologies.
 */
enum class InventoryValuationMethod {
    FIFO,
    WEIGHTED_AVERAGE
}

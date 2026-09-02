package com.sucharu.sucharupro.domain.model.inventory.ledger

/**
 * Normalized immutable movement record for auditing and valuation (Module 07 Step 09).
 */
data class InventoryMovementLedgerEntry(
    val ledgerEntryId: String,
    val projectId: String,
    val productId: String,
    val locationId: String,
    val batchId: String? = null,
    val lotId: String? = null,
    val movementType: InventoryMovementLedgerType,
    val direction: InventoryMovementDirection,
    val quantity: Double,
    val unitCost: Double? = null,
    val totalCost: Double? = null,
    val referenceId: String,
    val referenceType: String,
    val movementAt: String,
    val sourceMovementId: String,
    val createdAt: String
) {
    init {
        require(ledgerEntryId.isNotBlank()) { "Ledger Entry ID cannot be blank" }
        require(projectId.isNotBlank()) { "Project ID cannot be blank" }
        require(productId.isNotBlank()) { "Product ID cannot be blank" }
        require(locationId.isNotBlank()) { "Location ID cannot be blank" }
        require(quantity != 0.0) { "Quantity cannot be zero" }
        
        if (direction == InventoryMovementDirection.IN) {
            require(quantity > 0) { "Inbound quantity must be positive" }
        } else {
            require(quantity < 0) { "Outbound quantity must be negative" }
        }
        
        unitCost?.let { require(it >= 0) { "Unit cost cannot be negative" } }
        totalCost?.let { require(it >= 0) { "Total cost cannot be negative" } }
        
        require(movementAt.isNotBlank()) { "movementAt timestamp cannot be blank" }
        require(createdAt.isNotBlank()) { "createdAt timestamp cannot be blank" }
    }
}

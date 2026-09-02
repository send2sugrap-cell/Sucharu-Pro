package com.sucharu.sucharupro.domain.model.inventory.ledger

/**
 * Result of comparing ledger totals against source-calculated quantities (Module 07 Step 09).
 */
data class InventoryLedgerReconciliationResult(
    val projectId: String,
    val productId: String,
    val locationId: String,
    val ledgerQuantity: Double,
    val sourceCalculatedQuantity: Double,
    val difference: Double,
    val status: InventoryReconciliationStatus,
    val checkedAt: String,
    val details: String? = null
) {
    init {
        require(projectId.isNotBlank()) { "Project ID cannot be blank" }
        require(productId.isNotBlank()) { "Product ID cannot be blank" }
        require(locationId.isNotBlank()) { "Location ID cannot be blank" }
        require(checkedAt.isNotBlank()) { "checkedAt timestamp cannot be blank" }
    }
}

/**
 * Status indicators for ledger reconciliation.
 */
enum class InventoryReconciliationStatus {
    MATCHED,
    MISMATCHED,
    INCOMPLETE,
    COST_DATA_MISSING
}

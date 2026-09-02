package com.sucharu.sucharupro.domain.model.inventory.stockout

/**
 * Categorization for the purpose of stock withdrawal / issue (Module 07 Step 04).
 */
enum class InventoryIssueType(val defaultLabel: String) {
    PRODUCTION("Production"),
    DELIVERY("Delivery"),
    REPLACEMENT("Replacement"),
    INTERNAL("Internal"),
    OTHER("Other")
}

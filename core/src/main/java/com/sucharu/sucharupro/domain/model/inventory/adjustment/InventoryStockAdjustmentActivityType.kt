package com.sucharu.sucharupro.domain.model.inventory.adjustment

/**
 * Enumeration of append-only audit event types for stock adjustment operations
 * (Module 07 Step 06).
 */
enum class InventoryStockAdjustmentActivityType(val defaultLabel: String) {
    CREATED("Adjustment Created"),
    UPDATED("Adjustment Updated"),
    SUBMITTED("Adjustment Submitted"),
    APPROVED("Adjustment Approved"),
    STARTED("Adjustment Started"),
    COMPLETED("Adjustment Completed"),
    CANCELLED("Adjustment Cancelled")
}

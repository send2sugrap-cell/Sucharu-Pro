package com.sucharu.sucharupro.domain.model.inventory.adjustment

/**
 * Business justification for a stock adjustment (Module 07 Step 06).
 */
enum class InventoryAdjustmentReason(val defaultLabel: String) {
    PHYSICAL_COUNT("Physical Count"),
    DAMAGED("Damaged"),
    LOST("Lost"),
    FOUND("Found"),
    EXPIRED("Expired"),
    DATA_CORRECTION("Data Correction"),
    OTHER("Other")
}

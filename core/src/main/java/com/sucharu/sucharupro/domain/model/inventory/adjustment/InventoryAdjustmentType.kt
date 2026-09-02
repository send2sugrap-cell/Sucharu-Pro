package com.sucharu.sucharupro.domain.model.inventory.adjustment

/**
 * Direction of a stock adjustment (Module 07 Step 06).
 */
enum class InventoryAdjustmentType(val defaultLabel: String) {
    INCREASE("Increase"),
    DECREASE("Decrease")
}

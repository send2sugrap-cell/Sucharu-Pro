package com.sucharu.sucharupro.domain.model.inventory.reorder

/**
 * Classification of reorder alerts based on severity and condition (Module 07 Step 08).
 */
enum class InventoryReorderAlertType(val defaultLabel: String, val priority: Int) {
    LOW_STOCK("Low Stock", 1),
    REORDER_REQUIRED("Reorder Required", 2),
    CRITICAL("Critical Level", 3),
    OUT_OF_STOCK("Out of Stock", 4)
}

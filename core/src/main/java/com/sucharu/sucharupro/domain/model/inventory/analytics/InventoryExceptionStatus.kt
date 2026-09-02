package com.sucharu.sucharupro.domain.model.inventory.analytics

/**
 * Workflow states for an inventory exception record (Module 07 Step 10).
 */
enum class InventoryExceptionStatus {
    OPEN,
    ACKNOWLEDGED,
    RESOLVED,
    DISMISSED
}

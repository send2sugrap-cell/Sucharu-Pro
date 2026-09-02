package com.sucharu.sucharupro.domain.model.inventory

/**
 * Lifecycle status of an inventory product master record (Module 07 Step 01).
 */
enum class InventoryProductStatus(val defaultLabel: String) {
    ACTIVE("Active"),
    INACTIVE("Inactive")
}

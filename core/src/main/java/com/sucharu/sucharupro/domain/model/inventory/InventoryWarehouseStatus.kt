package com.sucharu.sucharupro.domain.model.inventory

/**
 * Lifecycle status of an inventory warehouse (Module 07 Step 02).
 */
enum class InventoryWarehouseStatus(val defaultLabel: String) {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    ARCHIVED("Archived");

    val isTerminal: Boolean
        get() = this == ARCHIVED
}

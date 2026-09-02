package com.sucharu.sucharupro.domain.model.inventory

/**
 * Lifecycle status of a storage location (Module 07 Step 02).
 */
enum class InventoryLocationStatus(val defaultLabel: String) {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    ARCHIVED("Archived");

    val isTerminal: Boolean
        get() = this == ARCHIVED
}

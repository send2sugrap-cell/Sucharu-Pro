package com.sucharu.sucharupro.domain.model.inventory.reorder

/**
 * Lifecycle status of a reorder alert (Module 07 Step 08).
 */
enum class InventoryReorderAlertStatus(val defaultLabel: String) {
    OPEN("Open"),
    ACKNOWLEDGED("Acknowledged"),
    RESOLVED("Resolved"),
    DISMISSED("Dismissed");

    /**
     * True for terminal states.
     */
    val isTerminal: Boolean
        get() = this == RESOLVED || this == DISMISSED
}

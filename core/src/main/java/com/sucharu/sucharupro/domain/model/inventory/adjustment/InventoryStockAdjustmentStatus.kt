package com.sucharu.sucharupro.domain.model.inventory.adjustment

/**
 * Lifecycle status of a stock adjustment operation (Module 07 Step 06).
 *
 * State machine:
 *   DRAFT → PENDING → APPROVED → ADJUSTING → COMPLETED (terminal)
 *   DRAFT / PENDING / APPROVED → CANCELLED (terminal)
 */
enum class InventoryStockAdjustmentStatus(val defaultLabel: String) {
    DRAFT("Draft"),
    PENDING("Pending"),
    APPROVED("Approved"),
    ADJUSTING("Adjusting"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    /**
     * True for terminal states that must not be mutated.
     */
    val isTerminal: Boolean
        get() = this == COMPLETED || this == CANCELLED
}

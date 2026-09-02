package com.sucharu.sucharupro.domain.model.inventory.stockout

/**
 * Lifecycle status of a stock out / issue operation (Module 07 Step 04).
 *
 * State machine:
 *   DRAFT → PENDING → ISSUING → COMPLETED (terminal)
 *   DRAFT / PENDING → CANCELLED (terminal)
 */
enum class InventoryStockOutStatus(val defaultLabel: String) {
    DRAFT("Draft"),
    PENDING("Pending"),
    ISSUING("Issuing"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    /**
     * True for terminal states that must not be mutated.
     */
    val isTerminal: Boolean
        get() = this == COMPLETED || this == CANCELLED
}

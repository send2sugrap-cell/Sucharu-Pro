package com.sucharu.sucharupro.domain.model.inventory.receiving

/**
 * Lifecycle status of a stock receiving operation (Module 07 Step 03).
 *
 * State machine:
 *
 *   DRAFT → PENDING → RECEIVING ─┬─ ACCEPTED
 *                                 ├─ PARTIALLY_ACCEPTED
 *                                 ├─ PARTIALLY_REJECTED
 *                                 └─ REJECTED
 *
 *   ACCEPTED / PARTIALLY_ACCEPTED / PARTIALLY_REJECTED → COMPLETED (terminal)
 *   REJECTED → COMPLETED (terminal)
 *   DRAFT / PENDING → CANCELLED (terminal)
 *
 * Terminal states: COMPLETED, CANCELLED, REJECTED
 *
 * Terminal records must NOT be mutated after reaching a terminal state.
 */
enum class InventoryReceivingStatus(val defaultLabel: String) {
    DRAFT("Draft"),
    PENDING("Pending"),
    RECEIVING("Receiving"),
    PARTIALLY_ACCEPTED("Partially Accepted"),
    ACCEPTED("Accepted"),
    PARTIALLY_REJECTED("Partially Rejected"),
    REJECTED("Rejected"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    /**
     * True for terminal states that must not be mutated.
     */
    val isTerminal: Boolean
        get() = this == COMPLETED || this == CANCELLED || this == REJECTED
}

package com.sucharu.sucharupro.domain.model.inventory.receiving

/**
 * Lifecycle status of an individual receiving line item (Module 07 Step 03).
 *
 * State machine:
 *
 *   PENDING → VERIFIED → ACCEPTED
 *                      → PARTIALLY_ACCEPTED
 *                      → REJECTED
 *   PENDING / VERIFIED → CANCELLED (terminal)
 *
 * Terminal states: ACCEPTED, PARTIALLY_ACCEPTED, REJECTED, CANCELLED
 *
 * A finalized line (ACCEPTED, PARTIALLY_ACCEPTED, REJECTED) produces exactly one
 * stock-in record for its acceptedQuantity (if > 0). CANCELLED lines produce none.
 */
enum class InventoryReceivingLineStatus(val defaultLabel: String) {
    PENDING("Pending"),
    VERIFIED("Verified"),
    PARTIALLY_ACCEPTED("Partially Accepted"),
    ACCEPTED("Accepted"),
    REJECTED("Rejected"),
    CANCELLED("Cancelled");

    /**
     * True for terminal line statuses where no further mutation is allowed.
     */
    val isTerminal: Boolean
        get() = this == ACCEPTED || this == PARTIALLY_ACCEPTED || this == REJECTED || this == CANCELLED

    /**
     * True for finalized lines that have resolved accepted/rejected quantities.
     */
    val isFinalized: Boolean
        get() = this == ACCEPTED || this == PARTIALLY_ACCEPTED || this == REJECTED
}

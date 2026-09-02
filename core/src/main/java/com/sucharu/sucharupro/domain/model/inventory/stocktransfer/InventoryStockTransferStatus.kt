package com.sucharu.sucharupro.domain.model.inventory.stocktransfer

/**
 * Lifecycle status of a stock transfer operation (Module 07 Step 05).
 *
 * State machine:
 *   DRAFT → PENDING → APPROVED → TRANSFERRING → COMPLETED (terminal)
 *   DRAFT / PENDING / APPROVED → CANCELLED (terminal)
 */
enum class InventoryStockTransferStatus(val defaultLabel: String) {
    DRAFT("Draft"),
    PENDING("Pending"),
    APPROVED("Approved"),
    TRANSFERRING("Transferring"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    /**
     * True for terminal states that must not be mutated.
     */
    val isTerminal: Boolean
        get() = this == COMPLETED || this == CANCELLED
}

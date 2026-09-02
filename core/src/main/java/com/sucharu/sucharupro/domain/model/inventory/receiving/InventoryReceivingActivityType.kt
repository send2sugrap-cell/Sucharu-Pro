package com.sucharu.sucharupro.domain.model.inventory.receiving

/**
 * Enumeration of append-only audit event types for stock receiving operations
 * (Module 07 Step 03).
 */
enum class InventoryReceivingActivityType(val defaultLabel: String) {
    RECEIVING_CREATED("Receiving Created"),
    RECEIVING_UPDATED("Receiving Updated"),
    LINE_ADDED("Line Added"),
    LINE_UPDATED("Line Updated"),
    RECEIVING_STARTED("Receiving Started"),
    LINE_VERIFIED("Line Verified"),
    QUANTITY_ACCEPTED("Quantity Accepted"),
    QUANTITY_REJECTED("Quantity Rejected"),
    RECEIVING_COMPLETED("Receiving Completed"),
    STOCK_IN_CREATED("Stock-In Created"),
    RECEIVING_CANCELLED("Receiving Cancelled")
}

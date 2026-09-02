package com.sucharu.sucharupro.domain.model.inventory.stockout

/**
 * Enumeration of append-only audit event types for stock out / issue operations
 * (Module 07 Step 04).
 */
enum class InventoryStockOutActivityType(val defaultLabel: String) {
    STOCK_OUT_CREATED("Stock-Out Created"),
    STOCK_OUT_UPDATED("Stock-Out Updated"),
    LINE_ADDED("Line Added"),
    LINE_UPDATED("Line Updated"),
    ISSUING_STARTED("Issuing Started"),
    QUANTITY_ISSUED("Quantity Issued"),
    STOCK_OUT_COMPLETED("Stock-Out Completed"),
    STOCK_OUT_RECORD_CREATED("Stock-Out Record Created"),
    STOCK_OUT_CANCELLED("Stock-Out Cancelled")
}

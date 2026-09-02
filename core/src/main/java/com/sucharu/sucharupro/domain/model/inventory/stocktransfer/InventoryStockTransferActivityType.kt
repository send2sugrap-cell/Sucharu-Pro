package com.sucharu.sucharupro.domain.model.inventory.stocktransfer

/**
 * Enumeration of append-only audit event types for stock transfer operations
 * (Module 07 Step 05).
 */
enum class InventoryStockTransferActivityType(val defaultLabel: String) {
    CREATED("Transfer Created"),
    SUBMITTED("Transfer Submitted"),
    APPROVED("Transfer Approved"),
    STARTED("Transfer Started"),
    COMPLETED("Transfer Completed"),
    CANCELLED("Transfer Cancelled")
}

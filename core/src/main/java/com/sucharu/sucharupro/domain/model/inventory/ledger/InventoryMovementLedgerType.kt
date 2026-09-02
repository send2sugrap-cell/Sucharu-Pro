package com.sucharu.sucharupro.domain.model.inventory.ledger

/**
 * Categorization of physical or logical stock movements within the ledger (Module 07 Step 09).
 */
enum class InventoryMovementLedgerType {
    STOCK_IN,
    STOCK_OUT,
    TRANSFER_IN,
    TRANSFER_OUT,
    ADJUSTMENT_IN,
    ADJUSTMENT_OUT
}

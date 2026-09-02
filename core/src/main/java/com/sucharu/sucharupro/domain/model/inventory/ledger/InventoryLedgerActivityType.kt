package com.sucharu.sucharupro.domain.model.inventory.ledger

/**
 * Audit event types for movement ledger operations (Module 07 Step 09).
 */
enum class InventoryLedgerActivityType {
    LEDGER_BUILT,
    RECONCILIATION_EXECUTED,
    VALUATION_SNAPSHOT_CREATED,
    ENTRY_ADDED,
    ENTRY_CORRECTED
}

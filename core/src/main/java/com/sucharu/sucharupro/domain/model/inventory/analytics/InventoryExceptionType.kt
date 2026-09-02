package com.sucharu.sucharupro.domain.model.inventory.analytics

/**
 * Categories of inventory data anomalies and governance exceptions (Module 07 Step 10).
 */
enum class InventoryExceptionType {
    NEGATIVE_BALANCE,
    RECONCILIATION_MISMATCH,
    COST_DATA_MISSING,
    STALE_INVENTORY,
    DATA_INCONSISTENCY
}

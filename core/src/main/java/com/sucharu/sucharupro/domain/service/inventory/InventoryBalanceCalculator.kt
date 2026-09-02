package com.sucharu.sucharupro.domain.service.inventory

import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementLedgerEntry

/**
 * Filter criteria for balance calculations.
 */
data class InventoryBalanceFilter(
    val productId: String? = null,
    val locationId: String? = null,
    val batchId: String? = null,
    val lotId: String? = null,
    val fromDate: String? = null,
    val toDate: String? = null
)

/**
 * Domain service to calculate stock balance from ledger entries (Module 07 Step 09).
 * Implements the logic: Opening Balance + Total IN - Total OUT = Closing Balance.
 */
object InventoryBalanceCalculator {

    /**
     * Calculates the closing balance based on ledger entries and an optional opening balance.
     * Note: [InventoryMovementLedgerEntry.quantity] is assumed to be already signed 
     * (positive for IN, negative for OUT).
     */
    fun calculateBalance(
        entries: List<InventoryMovementLedgerEntry>,
        openingBalance: Double = 0.0,
        filters: InventoryBalanceFilter? = null
    ): Double {
        val filteredEntries = if (filters == null) {
            entries
        } else {
            entries.filter { entry ->
                (filters.productId == null || entry.productId == filters.productId) &&
                (filters.locationId == null || entry.locationId == filters.locationId) &&
                (filters.batchId == null || entry.batchId == filters.batchId) &&
                (filters.lotId == null || entry.lotId == filters.lotId) &&
                (filters.fromDate == null || entry.movementAt >= filters.fromDate) &&
                (filters.toDate == null || entry.movementAt <= filters.toDate)
            }
        }

        return openingBalance + filteredEntries.sumOf { it.quantity }
    }
}

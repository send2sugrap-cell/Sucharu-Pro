package com.sucharu.sucharupro.domain.service.inventory

import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementDirection
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementLedgerEntry
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementLedgerType
import kotlin.math.abs

/**
 * Domain service for inventory analytics calculations (Module 07 Step 10).
 */
class InventoryAnalyticsEvaluator {

    /**
     * Groups ledger entries by date (YYYY-MM-DD).
     */
    fun groupByDate(entries: List<InventoryMovementLedgerEntry>): Map<String, List<InventoryMovementLedgerEntry>> {
        return entries.groupBy { it.movementAt.substringBefore("T") }
    }

    /**
     * Calculates net inbound, outbound, and adjustment quantities.
     */
    fun calculateNetQuantities(entries: List<InventoryMovementLedgerEntry>): InventorySummary {
        var inbound = 0.0
        var outbound = 0.0
        var adjustment = 0.0

        entries.forEach { entry ->
            when (entry.movementType) {
                InventoryMovementLedgerType.STOCK_IN,
                InventoryMovementLedgerType.TRANSFER_IN -> inbound += abs(entry.quantity)
                
                InventoryMovementLedgerType.STOCK_OUT,
                InventoryMovementLedgerType.TRANSFER_OUT -> outbound += abs(entry.quantity)
                
                InventoryMovementLedgerType.ADJUSTMENT_IN,
                InventoryMovementLedgerType.ADJUSTMENT_OUT -> adjustment += entry.quantity // Adjustments can be + or -
            }
        }

        return InventorySummary(
            netInbound = inbound,
            netOutbound = outbound,
            netAdjustment = adjustment
        )
    }

    /**
     * Calculates inventory turnover: Outbound Qty / Avg Inventory.
     * Avg Inventory = (Opening + Closing) / 2
     */
    fun calculateTurnover(
        outboundQty: Double,
        openingBalance: Double,
        closingBalance: Double
    ): Double {
        val avgInventory = (openingBalance + closingBalance) / 2.0
        if (avgInventory == 0.0) return 0.0
        return abs(outboundQty) / avgInventory
    }

    data class InventorySummary(
        val netInbound: Double,
        val netOutbound: Double,
        val netAdjustment: Double
    )
}

package com.sucharu.sucharupro.domain.service.inventory

import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementDirection
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementLedgerEntry
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryValuationMethod
import kotlin.math.abs

/**
 * Result of a valuation calculation.
 */
sealed class ValuationResult {
    data class Success(val unitCost: Double, val totalValue: Double) : ValuationResult()
    object DataMissing : ValuationResult()
    object InvalidData : ValuationResult()
}

/**
 * Domain service to calculate inventory valuation using FIFO or Weighted Average (Module 07 Step 09).
 */
object InventoryValuationCalculator {

    /**
     * Calculates the valuation of a set of ledger entries.
     */
    fun calculateValuation(
        entries: List<InventoryMovementLedgerEntry>,
        method: InventoryValuationMethod
    ): ValuationResult {
        if (entries.isEmpty()) return ValuationResult.Success(0.0, 0.0)
        
        // Return DataMissing if any IN movement has no unit cost defined.
        if (entries.any { it.direction == InventoryMovementDirection.IN && it.unitCost == null }) {
            return ValuationResult.DataMissing
        }

        return when (method) {
            InventoryValuationMethod.FIFO -> calculateFIFO(entries)
            InventoryValuationMethod.WEIGHTED_AVERAGE -> calculateWeightedAverage(entries)
        }
    }

    private fun calculateFIFO(entries: List<InventoryMovementLedgerEntry>): ValuationResult {
        val sortedEntries = entries.sortedBy { it.movementAt }
        val stockLayers = mutableListOf<Pair<Double, Double>>() // quantity, unitCost

        for (entry in sortedEntries) {
            if (entry.direction == InventoryMovementDirection.IN) {
                stockLayers.add(entry.quantity to (entry.unitCost ?: 0.0))
            } else {
                var remainingToDeduct = abs(entry.quantity)
                while (remainingToDeduct > 0 && stockLayers.isNotEmpty()) {
                    val (layerQty, layerCost) = stockLayers[0]
                    if (layerQty <= remainingToDeduct) {
                        remainingToDeduct -= layerQty
                        stockLayers.removeAt(0)
                    } else {
                        stockLayers[0] = (layerQty - remainingToDeduct) to layerCost
                        remainingToDeduct = 0.0
                    }
                }
            }
        }

        val totalQuantity = stockLayers.sumOf { it.first }
        val totalValue = stockLayers.sumOf { it.first * it.second }
        val avgUnitCost = if (totalQuantity > 0) totalValue / totalQuantity else 0.0

        return ValuationResult.Success(avgUnitCost, totalValue)
    }

    private fun calculateWeightedAverage(entries: List<InventoryMovementLedgerEntry>): ValuationResult {
        var totalQuantity = 0.0
        var totalValue = 0.0

        // Implements moving weighted average calculation
        for (entry in entries.sortedBy { it.movementAt }) {
            if (entry.direction == InventoryMovementDirection.IN) {
                totalQuantity += entry.quantity
                totalValue += entry.quantity * (entry.unitCost ?: 0.0)
            } else {
                val currentAvgCost = if (totalQuantity > 0) totalValue / totalQuantity else 0.0
                val deductQty = abs(entry.quantity)
                
                val actualDeduct = if (totalQuantity >= deductQty) deductQty else totalQuantity
                totalValue -= actualDeduct * currentAvgCost
                totalQuantity -= deductQty
            }
        }
        
        val finalQuantity = if (totalQuantity < 0) 0.0 else totalQuantity
        val finalValue = if (totalValue < 0) 0.0 else totalValue

        val finalUnitCost = if (finalQuantity > 0) finalValue / finalQuantity else 0.0
        return ValuationResult.Success(finalUnitCost, finalValue)
    }
}

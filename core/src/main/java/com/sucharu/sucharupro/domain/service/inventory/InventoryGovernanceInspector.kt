package com.sucharu.sucharupro.domain.service.inventory

import com.sucharu.sucharupro.domain.model.inventory.analytics.InventoryException
import com.sucharu.sucharupro.domain.model.inventory.analytics.InventoryExceptionStatus
import com.sucharu.sucharupro.domain.model.inventory.analytics.InventoryExceptionType
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementLedgerEntry
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementLedgerType
import java.util.UUID

/**
 * Domain service for inventory governance and exception detection (Module 07 Step 10).
 */
class InventoryGovernanceInspector {

    private val adjustmentFrequencyThreshold = 5

    /**
     * Scans ledger entries and identifies negative balances at any point in time.
     */
    fun detectNegativeBalances(
        projectId: String,
        entries: List<InventoryMovementLedgerEntry>,
        detectedAt: String
    ): List<InventoryException> {
        val exceptions = mutableListOf<InventoryException>()
        
        // Group by unique stock identifier (Product + Location + Batch + Lot)
        val groupedEntries = entries.groupBy { 
            "${it.productId}|${it.locationId}|${it.batchId ?: ""}|${it.lotId ?: ""}" 
        }

        groupedEntries.forEach { (key, productEntries) ->
            var runningBalance = 0.0
            val sortedEntries = productEntries.sortedBy { it.movementAt }
            
            for (entry in sortedEntries) {
                runningBalance += entry.quantity
                if (runningBalance < -0.000001) { // Floating point safety
                    val parts = key.split("|")
                    exceptions.add(
                        InventoryException(
                            exceptionId = UUID.randomUUID().toString(),
                            projectId = projectId,
                            type = InventoryExceptionType.NEGATIVE_BALANCE,
                            targetId = parts[0], // ProductId
                            targetType = InventoryException.TargetType.PRODUCT,
                            severity = InventoryException.Severity.CRITICAL,
                            status = InventoryExceptionStatus.OPEN,
                            detectedAt = detectedAt,
                            details = "Negative balance of $runningBalance detected at ${entry.movementAt} for Location ${parts[1]}"
                        )
                    )
                    // We only report the first instance of negative balance per group in this scan to avoid noise.
                    break 
                }
            }
        }

        return exceptions
    }

    /**
     * Identifies products with high adjustment frequency.
     */
    fun detectHighAdjustmentFrequency(
        projectId: String,
        entries: List<InventoryMovementLedgerEntry>,
        detectedAt: String
    ): List<InventoryException> {
        val adjustmentTypes = setOf(
            InventoryMovementLedgerType.ADJUSTMENT_IN,
            InventoryMovementLedgerType.ADJUSTMENT_OUT
        )

        return entries.filter { it.movementType in adjustmentTypes }
            .groupBy { it.productId }
            .filter { (_, productEntries) -> productEntries.size > adjustmentFrequencyThreshold }
            .map { (productId, productEntries) ->
                InventoryException(
                    exceptionId = UUID.randomUUID().toString(),
                    projectId = projectId,
                    type = InventoryExceptionType.DATA_INCONSISTENCY,
                    targetId = productId,
                    targetType = InventoryException.TargetType.PRODUCT,
                    severity = InventoryException.Severity.MEDIUM,
                    status = InventoryExceptionStatus.OPEN,
                    detectedAt = detectedAt,
                    details = "High adjustment frequency detected: ${productEntries.size} adjustments found in the current period."
                )
            }
    }
}

package com.sucharu.sucharupro.domain.service.inventory

import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryLedgerReconciliationResult
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryReconciliationStatus
import java.time.Instant

/**
 * Domain service to reconcile ledger totals against source datasource sums (Module 07 Step 09).
 */
object InventoryLedgerReconciliationService {

    /**
     * Reconciles the ledger quantity against a source-calculated quantity.
     * Returns a [InventoryLedgerReconciliationResult] with the status and difference.
     */
    fun reconcile(
        projectId: String,
        productId: String,
        locationId: String,
        ledgerQuantity: Double,
        sourceCalculatedQuantity: Double,
        details: String? = null
    ): InventoryLedgerReconciliationResult {
        val difference = ledgerQuantity - sourceCalculatedQuantity
        val status = if (difference == 0.0) {
            InventoryReconciliationStatus.MATCHED
        } else {
            InventoryReconciliationStatus.MISMATCHED
        }

        return InventoryLedgerReconciliationResult(
            projectId = projectId,
            productId = productId,
            locationId = locationId,
            ledgerQuantity = ledgerQuantity,
            sourceCalculatedQuantity = sourceCalculatedQuantity,
            difference = difference,
            status = status,
            checkedAt = Instant.now().toString(),
            details = details
        )
    }
}

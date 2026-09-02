package com.sucharu.sucharupro.domain.service

import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementDirection
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementLedgerEntry
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementLedgerType
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryValuationMethod
import com.sucharu.sucharupro.domain.service.inventory.InventoryValuationCalculator
import com.sucharu.sucharupro.domain.service.inventory.ValuationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies Weighted Average logic for inventory valuation (Module 07 Step 09).
 */
class InventoryValuationWAvg_Test {

    private fun createIn(qty: Double, cost: Double, at: String) = InventoryMovementLedgerEntry(
        ledgerEntryId = "L-IN-$at",
        projectId = "P1",
        productId = "PROD-01",
        locationId = "L1",
        movementType = InventoryMovementLedgerType.STOCK_IN,
        direction = InventoryMovementDirection.IN,
        quantity = qty,
        unitCost = cost,
        totalCost = qty * cost,
        referenceId = "R1",
        referenceType = "TEST",
        movementAt = at,
        sourceMovementId = "S-IN-$at",
        createdAt = at
    )

    private fun createOut(qty: Double, at: String) = InventoryMovementLedgerEntry(
        ledgerEntryId = "L-OUT-$at",
        projectId = "P1",
        productId = "PROD-01",
        locationId = "L1",
        movementType = InventoryMovementLedgerType.STOCK_OUT,
        direction = InventoryMovementDirection.OUT,
        quantity = -qty,
        referenceId = "R1",
        referenceType = "TEST",
        movementAt = at,
        sourceMovementId = "S-OUT-$at",
        createdAt = at
    )

    @Test
    fun `weighted average logic`() {
        val entries = listOf(
            createIn(10.0, 100.0, "2026-08-17T10:00:00Z"), // 10 @ 100, Total: 1000
            createIn(10.0, 200.0, "2026-08-17T11:00:00Z")  // +10 @ 200, Total: 3000. WAvg: 3000/20 = 150
        )
        
        val result = InventoryValuationCalculator.calculateValuation(entries, InventoryValuationMethod.WEIGHTED_AVERAGE)
        
        assertTrue(result is ValuationResult.Success)
        val success = result as ValuationResult.Success
        assertEquals(150.0, success.unitCost, 0.0)
        assertEquals(3000.0, success.totalValue, 0.0)
    }

    @Test
    fun `moving weighted average after consumption`() {
        val entries = listOf(
            createIn(10.0, 100.0, "10:00"), // 10 @ 100. Val: 1000
            createIn(10.0, 200.0, "11:00"), // 20 @ 150. Val: 3000
            createOut(10.0, "12:00"),       // -10 @ 150. Remaining: 10 @ 150. Val: 1500
            createIn(10.0, 300.0, "13:00")  // +10 @ 300. Total: 20 @ (1500+3000)/20 = 225. Val: 4500
        )

        val result = InventoryValuationCalculator.calculateValuation(entries, InventoryValuationMethod.WEIGHTED_AVERAGE)
        val success = result as ValuationResult.Success
        assertEquals(225.0, success.unitCost, 0.0)
        assertEquals(4500.0, success.totalValue, 0.0)
    }
}

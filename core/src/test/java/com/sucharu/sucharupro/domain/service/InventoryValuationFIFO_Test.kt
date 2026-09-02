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
 * Verifies FIFO consumption logic for inventory valuation (Module 07 Step 09).
 */
class InventoryValuationFIFO_Test {

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
    fun `FIFO consumption logic`() {
        val entries = listOf(
            createIn(10.0, 100.0, "2026-08-17T10:00:00Z"), // Layer 1: 10 @ 100
            createIn(10.0, 200.0, "2026-08-17T11:00:00Z"), // Layer 2: 10 @ 200
            createOut(15.0, "2026-08-17T12:00:00Z")        // Consume 10 from L1, 5 from L2. Remaining: 5 @ 200
        )
        
        val result = InventoryValuationCalculator.calculateValuation(entries, InventoryValuationMethod.FIFO)
        
        assertTrue(result is ValuationResult.Success)
        val success = result as ValuationResult.Success
        assertEquals(200.0, success.unitCost, 0.0)
        assertEquals(1000.0, success.totalValue, 0.0)
    }

    @Test
    fun `FIFO - multiple layers consumption`() {
        val entries = listOf(
            createIn(10.0, 10.0, "10:00"),
            createIn(10.0, 20.0, "11:00"),
            createIn(10.0, 30.0, "12:00"),
            createOut(25.0, "13:00") // 10@10 + 10@20 + 5@30 consumed. Remaining: 5 @ 30
        )

        val result = InventoryValuationCalculator.calculateValuation(entries, InventoryValuationMethod.FIFO)
        val success = result as ValuationResult.Success
        assertEquals(150.0, success.totalValue, 0.0)
    }
}

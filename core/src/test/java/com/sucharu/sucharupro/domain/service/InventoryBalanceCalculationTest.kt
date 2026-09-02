package com.sucharu.sucharupro.domain.service

import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementDirection
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementLedgerEntry
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementLedgerType
import com.sucharu.sucharupro.domain.service.inventory.InventoryBalanceCalculator
import com.sucharu.sucharupro.domain.service.inventory.InventoryBalanceFilter
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests summation rules, opening and closing balance calculation (Module 07 Step 09).
 */
class InventoryBalanceCalculationTest {

    private fun createEntry(productId: String, qty: Double, at: String) = InventoryMovementLedgerEntry(
        ledgerEntryId = "L-${qty}-${at}",
        projectId = "P1",
        productId = productId,
        locationId = "L1",
        movementType = if (qty > 0) InventoryMovementLedgerType.STOCK_IN else InventoryMovementLedgerType.STOCK_OUT,
        direction = if (qty > 0) InventoryMovementDirection.IN else InventoryMovementDirection.OUT,
        quantity = qty,
        referenceId = "R1",
        referenceType = "TEST",
        movementAt = at,
        sourceMovementId = "S1-${qty}-${at}",
        createdAt = at
    )

    @Test
    fun `summation rules - correct balance calculation`() {
        val entries = listOf(
            createEntry("P1", 100.0, "2026-08-17T10:00:00Z"),
            createEntry("P1", -20.0, "2026-08-17T11:00:00Z"),
            createEntry("P1", 50.0, "2026-08-17T12:00:00Z")
        )
        
        val balance = InventoryBalanceCalculator.calculateBalance(entries)
        assertEquals(130.0, balance, 0.0)
    }

    @Test
    fun `opening and closing balance integration`() {
        val entries = listOf(
            createEntry("P1", 50.0, "2026-08-17T10:00:00Z")
        )
        
        val balance = InventoryBalanceCalculator.calculateBalance(entries, openingBalance = 1000.0)
        assertEquals(1050.0, balance, 0.0)
    }

    @Test
    fun `filtering by product and date`() {
        val entries = listOf(
            createEntry("P1", 100.0, "2026-08-17T10:00:00Z"),
            createEntry("P2", 200.0, "2026-08-17T10:00:00Z"),
            createEntry("P1", -30.0, "2026-08-17T15:00:00Z")
        )
        
        val filter = InventoryBalanceFilter(
            productId = "P1",
            toDate = "2026-08-17T12:00:00Z"
        )
        
        val balance = InventoryBalanceCalculator.calculateBalance(entries, filters = filter)
        assertEquals(100.0, balance, 0.0)
    }
}

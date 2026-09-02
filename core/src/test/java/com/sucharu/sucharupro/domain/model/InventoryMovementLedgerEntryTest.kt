package com.sucharu.sucharupro.domain.model

import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementDirection
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementLedgerEntry
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementLedgerType
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests structural rules and immutability for InventoryMovementLedgerEntry (Module 07 Step 09).
 */
class InventoryMovementLedgerEntryTest {

    @Test
    fun `structural rules - valid inbound entry`() {
        val entry = InventoryMovementLedgerEntry(
            ledgerEntryId = "LED-001",
            projectId = "PROJ-01",
            productId = "PROD-01",
            locationId = "LOC-01",
            movementType = InventoryMovementLedgerType.STOCK_IN,
            direction = InventoryMovementDirection.IN,
            quantity = 10.0,
            unitCost = 50.0,
            totalCost = 500.0,
            referenceId = "REC-001",
            referenceType = "RECEIVING",
            movementAt = "2026-08-17T10:00:00Z",
            sourceMovementId = "SIN-001",
            createdAt = "2026-08-17T10:05:00Z"
        )
        
        assertEquals("LED-001", entry.ledgerEntryId)
        assertEquals(10.0, entry.quantity, 0.0)
        assertEquals(50.0, entry.unitCost!!, 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `structural rules - blank IDs should fail`() {
        InventoryMovementLedgerEntry(
            ledgerEntryId = "",
            projectId = "PROJ-01",
            productId = "PROD-01",
            locationId = "LOC-01",
            movementType = InventoryMovementLedgerType.STOCK_IN,
            direction = InventoryMovementDirection.IN,
            quantity = 10.0,
            referenceId = "REC-001",
            referenceType = "RECEIVING",
            movementAt = "2026-08-17T10:00:00Z",
            sourceMovementId = "SIN-001",
            createdAt = "2026-08-17T10:05:00Z"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `structural rules - zero quantity should fail`() {
        InventoryMovementLedgerEntry(
            ledgerEntryId = "LED-001",
            projectId = "PROJ-01",
            productId = "PROD-01",
            locationId = "LOC-01",
            movementType = InventoryMovementLedgerType.STOCK_IN,
            direction = InventoryMovementDirection.IN,
            quantity = 0.0,
            referenceId = "REC-001",
            referenceType = "RECEIVING",
            movementAt = "2026-08-17T10:00:00Z",
            sourceMovementId = "SIN-001",
            createdAt = "2026-08-17T10:05:00Z"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `structural rules - inbound quantity must be positive`() {
        InventoryMovementLedgerEntry(
            ledgerEntryId = "LED-001",
            projectId = "PROJ-01",
            productId = "PROD-01",
            locationId = "LOC-01",
            movementType = InventoryMovementLedgerType.STOCK_IN,
            direction = InventoryMovementDirection.IN,
            quantity = -10.0,
            referenceId = "REC-001",
            referenceType = "RECEIVING",
            movementAt = "2026-08-17T10:00:00Z",
            sourceMovementId = "SIN-001",
            createdAt = "2026-08-17T10:05:00Z"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `structural rules - outbound quantity must be negative`() {
        InventoryMovementLedgerEntry(
            ledgerEntryId = "LED-001",
            projectId = "PROJ-01",
            productId = "PROD-01",
            locationId = "LOC-01",
            movementType = InventoryMovementLedgerType.STOCK_OUT,
            direction = InventoryMovementDirection.OUT,
            quantity = 10.0,
            referenceId = "SO-001",
            referenceType = "STOCK_OUT",
            movementAt = "2026-08-17T10:00:00Z",
            sourceMovementId = "SOUT-001",
            createdAt = "2026-08-17T10:05:00Z"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `structural rules - negative unit cost should fail`() {
        InventoryMovementLedgerEntry(
            ledgerEntryId = "LED-001",
            projectId = "PROJ-01",
            productId = "PROD-01",
            locationId = "LOC-01",
            movementType = InventoryMovementLedgerType.STOCK_IN,
            direction = InventoryMovementDirection.IN,
            quantity = 10.0,
            unitCost = -1.0,
            referenceId = "REC-001",
            referenceType = "RECEIVING",
            movementAt = "2026-08-17T10:00:00Z",
            sourceMovementId = "SIN-001",
            createdAt = "2026-08-17T10:05:00Z"
        )
    }
}

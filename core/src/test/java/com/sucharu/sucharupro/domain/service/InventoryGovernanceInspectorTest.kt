package com.sucharu.sucharupro.domain.service

import com.sucharu.sucharupro.domain.model.inventory.analytics.InventoryExceptionType
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementDirection
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementLedgerEntry
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementLedgerType
import com.sucharu.sucharupro.domain.service.inventory.InventoryGovernanceInspector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/**
 * Unit tests for [InventoryGovernanceInspector] (Module 07 Step 10).
 * Covers detection of negative stock and high adjustment frequency.
 */
class InventoryGovernanceInspectorTest {

    private val inspector = InventoryGovernanceInspector()

    @Test
    fun `detectNegativeBalances identifies negative stock at point in time`() {
        val entries = listOf(
            createEntry("2026-08-17T10:00:00Z", 50.0),
            createEntry("2026-08-17T11:00:00Z", -60.0), // Balance becomes -10 here
            createEntry("2026-08-17T12:00:00Z", 20.0)   // Balance becomes +10
        )

        val exceptions = inspector.detectNegativeBalances("PRJ-01", entries, "2026-08-17T13:00:00Z")

        assertEquals(1, exceptions.size)
        assertEquals(InventoryExceptionType.NEGATIVE_BALANCE, exceptions[0].type)
        assertTrue(exceptions[0].details?.contains("Negative balance") == true)
    }

    @Test
    fun `detectNegativeBalances only reports the first instance per product-location group`() {
        val entries = listOf(
            createEntry("2026-08-17T10:00:00Z", -10.0), // First instance
            createEntry("2026-08-17T11:00:00Z", -5.0)   // Second instance (should be ignored to avoid noise)
        )

        val exceptions = inspector.detectNegativeBalances("PRJ-01", entries, "2026-08-17T12:00:00Z")

        assertEquals(1, exceptions.size)
    }

    @Test
    fun `detectHighAdjustmentFrequency flags products exceeding threshold of 5`() {
        val entries = mutableListOf<InventoryMovementLedgerEntry>()
        // 6 adjustments for the same product
        repeat(6) { i ->
            entries.add(
                createEntry(
                    "2026-08-17T10:0$i:00Z", 
                    1.0, 
                    InventoryMovementLedgerType.ADJUSTMENT_IN
                )
            )
        }

        val exceptions = inspector.detectHighAdjustmentFrequency("PRJ-01", entries, "2026-08-17T11:00:00Z")

        assertEquals(1, exceptions.size)
        assertEquals(InventoryExceptionType.DATA_INCONSISTENCY, exceptions[0].type)
        assertTrue(exceptions[0].details?.contains("High adjustment frequency") == true)
    }

    @Test
    fun `detectHighAdjustmentFrequency does not flag products below threshold`() {
        val entries = mutableListOf<InventoryMovementLedgerEntry>()
        repeat(5) { i ->
            entries.add(createEntry("2026-08-17T10:0$i:00Z", 1.0, InventoryMovementLedgerType.ADJUSTMENT_OUT))
        }

        val exceptions = inspector.detectHighAdjustmentFrequency("PRJ-01", entries, "2026-08-17T11:00:00Z")

        assertEquals(0, exceptions.size)
    }

    private fun createEntry(
        timestamp: String,
        quantity: Double,
        type: InventoryMovementLedgerType = InventoryMovementLedgerType.STOCK_IN
    ) = InventoryMovementLedgerEntry(
        ledgerEntryId = UUID.randomUUID().toString(),
        projectId = "PRJ-01",
        productId = "PRD-01",
        locationId = "LOC-01",
        movementType = type,
        direction = if (quantity > 0) InventoryMovementDirection.IN else InventoryMovementDirection.OUT,
        quantity = quantity,
        referenceId = "REF-01",
        referenceType = "TEST",
        movementAt = timestamp,
        sourceMovementId = "SRC-01",
        createdAt = timestamp
    )
}


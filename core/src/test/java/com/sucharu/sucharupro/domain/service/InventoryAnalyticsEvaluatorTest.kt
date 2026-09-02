package com.sucharu.sucharupro.domain.service

import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementDirection
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementLedgerEntry
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementLedgerType
import com.sucharu.sucharupro.domain.service.inventory.InventoryAnalyticsEvaluator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

/**
 * Unit tests for [InventoryAnalyticsEvaluator] (Module 07 Step 10).
 * Covers period aggregation and KPI math.
 */
class InventoryAnalyticsEvaluatorTest {

    private val evaluator = InventoryAnalyticsEvaluator()

    @Test
    fun `groupByDate correctly groups entries by YYYY-MM-DD`() {
        val entries = listOf(
            createEntry("2026-08-17T10:00:00Z"),
            createEntry("2026-08-17T15:00:00Z"),
            createEntry("2026-08-18T09:00:00Z"),
            createEntry("2026-08-19T00:00:00Z")
        )

        val groups = evaluator.groupByDate(entries)

        assertEquals(3, groups.size)
        assertEquals(2, groups["2026-08-17"]?.size)
        assertEquals(1, groups["2026-08-18"]?.size)
        assertEquals(1, groups["2026-08-19"]?.size)
    }

    @Test
    fun `calculateNetQuantities correctly sums inbound outbound and adjustments`() {
        val entries = listOf(
            // Inbound: 100 + 50 = 150
            createEntry("2026-08-17T10:00:00Z", InventoryMovementLedgerType.STOCK_IN, 100.0),
            createEntry("2026-08-17T14:00:00Z", InventoryMovementLedgerType.TRANSFER_IN, 50.0),
            
            // Outbound: abs(-20) + abs(-10) = 30
            createEntry("2026-08-17T11:00:00Z", InventoryMovementLedgerType.STOCK_OUT, -20.0),
            createEntry("2026-08-17T15:00:00Z", InventoryMovementLedgerType.TRANSFER_OUT, -10.0),
            
            // Adjustment: 5 - 3 = 2
            createEntry("2026-08-17T12:00:00Z", InventoryMovementLedgerType.ADJUSTMENT_IN, 5.0),
            createEntry("2026-08-17T13:00:00Z", InventoryMovementLedgerType.ADJUSTMENT_OUT, -3.0)
        )

        val summary = evaluator.calculateNetQuantities(entries)

        assertEquals("Net inbound should be 150", 150.0, summary.netInbound, 0.001)
        assertEquals("Net outbound should be 30", 30.0, summary.netOutbound, 0.001)
        assertEquals("Net adjustment should be 2", 2.0, summary.netAdjustment, 0.001)
    }

    @Test
    fun `calculateTurnover returns correct ratio for valid balances`() {
        // Avg Inventory = (Opening 10 + Closing 30) / 2 = 20
        // Turnover = Outbound 100 / Avg 20 = 5.0
        val turnover = evaluator.calculateTurnover(
            outboundQty = 100.0,
            openingBalance = 10.0,
            closingBalance = 30.0
        )
        assertEquals(5.0, turnover, 0.001)
    }

    @Test
    fun `calculateTurnover returns zero when average inventory is zero`() {
        val turnover = evaluator.calculateTurnover(
            outboundQty = 50.0,
            openingBalance = 0.0,
            closingBalance = 0.0
        )
        assertEquals(0.0, turnover, 0.001)
    }

    @Test
    fun `calculateTurnover handles negative outbound quantity using absolute value`() {
        val turnover = evaluator.calculateTurnover(
            outboundQty = -100.0,
            openingBalance = 10.0,
            closingBalance = 30.0
        )
        assertEquals(5.0, turnover, 0.001)
    }

    private fun createEntry(
        timestamp: String,
        type: InventoryMovementLedgerType = InventoryMovementLedgerType.STOCK_IN,
        quantity: Double = 1.0
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


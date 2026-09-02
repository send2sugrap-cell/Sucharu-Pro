package com.sucharu.sucharupro.domain.model

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceiving
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceivingLine
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceivingLineStatus
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceivingStatus
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryStockInRecord
import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [InventoryReceiving] and [InventoryReceivingLine] domain model invariants
 * (Module 07 Step 03).
 */
class InventoryReceivingModelTest {

    // ──────────────────────────────────────────────────────────────
    // InventoryReceiving
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `valid receiving constructs successfully`() {
        val receiving = buildReceiving()
        assertEquals("RCV-001", receiving.receivingId)
        assertEquals("PRJ-01", receiving.projectId)
        assertEquals("RCV-REF-001", receiving.receivingReference)
        assertEquals(InventoryReceivingStatus.DRAFT, receiving.status)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `receiving with blank receivingId fails`() {
        buildReceiving(receivingId = "  ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `receiving with blank projectId fails`() {
        buildReceiving(projectId = "")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `receiving with blank reference fails`() {
        buildReceiving(reference = "")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `receiving with blank warehouseId fails`() {
        buildReceiving(warehouseId = "")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `receiving with blank receivingDate fails`() {
        buildReceiving(receivingDate = "")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `receiving with updatedAt before createdAt fails`() {
        buildReceiving(createdAt = "2026-08-17T09:00:00Z", updatedAt = "2026-08-17T08:00:00Z")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `receiving with negative expectedTotalQuantity fails`() {
        buildReceiving(expectedTotalQuantity = -1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `COMPLETED receiving without completedAt fails`() {
        buildReceiving(status = InventoryReceivingStatus.COMPLETED, completedAt = null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `CANCELLED receiving without cancelledAt fails`() {
        buildReceiving(status = InventoryReceivingStatus.CANCELLED, cancelledAt = null)
    }

    @Test
    fun `normalizedReference is uppercase trimmed`() {
        val r = buildReceiving(reference = "  rcv-alpha  ")
        assertEquals("RCV-ALPHA", r.normalizedReference)
    }

    @Test
    fun `isTerminal is true for COMPLETED`() {
        val r = buildReceiving(status = InventoryReceivingStatus.COMPLETED, completedAt = "2026-08-17T12:00:00Z", completedBy = "admin")
        assertTrue(r.isTerminal)
    }

    @Test
    fun `isTerminal is true for CANCELLED`() {
        val r = buildReceiving(status = InventoryReceivingStatus.CANCELLED, cancelledAt = "2026-08-17T12:00:00Z", cancelledBy = "admin")
        assertTrue(r.isTerminal)
    }

    @Test
    fun `isTerminal is false for DRAFT`() {
        assertTrue(!buildReceiving(status = InventoryReceivingStatus.DRAFT).isTerminal)
    }

    // ──────────────────────────────────────────────────────────────
    // InventoryReceivingLine
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `valid line constructs successfully`() {
        val line = buildLine()
        assertEquals("LINE-001", line.receivingLineId)
        assertEquals(InventoryReceivingLineStatus.PENDING, line.lineStatus)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `line with blank receivingLineId fails`() {
        buildLine(lineId = "")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `line with negative receivedQuantity fails`() {
        buildLine(receivedQuantity = -1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `line where accepted + rejected exceeds received fails`() {
        buildLine(receivedQuantity = 5, acceptedQuantity = 4, rejectedQuantity = 3)
    }

    @Test
    fun `line isQuantityReconciled is true when accepted + rejected == received`() {
        val line = buildLine(receivedQuantity = 10, acceptedQuantity = 7, rejectedQuantity = 3)
        assertTrue(line.isQuantityReconciled)
    }

    @Test
    fun `line isQuantityReconciled is false when not fully split`() {
        val line = buildLine(receivedQuantity = 10, acceptedQuantity = 5, rejectedQuantity = 3)
        assertTrue(!line.isQuantityReconciled)
    }

    // ──────────────────────────────────────────────────────────────
    // InventoryStockInRecord
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `valid stock-in record constructs successfully`() {
        val record = InventoryStockInRecord(
            stockInId = "SI-001",
            receivingId = "RCV-001",
            receivingLineId = "LINE-001",
            projectId = "PRJ-01",
            inventoryProductId = "PROD-01",
            warehouseId = "WH-01",
            locationId = "LOC-01",
            quantity = 10,
            unit = InventoryUnit.PCS,
            createdBy = "admin-01",
            createdAt = "2026-08-17T12:00:00Z"
        )
        assertEquals(10, record.quantity)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `stock-in record with quantity zero fails`() {
        InventoryStockInRecord(
            stockInId = "SI-002",
            receivingId = "RCV-001",
            receivingLineId = "LINE-001",
            projectId = "PRJ-01",
            inventoryProductId = "PROD-01",
            warehouseId = "WH-01",
            locationId = "LOC-01",
            quantity = 0,
            unit = InventoryUnit.PCS,
            createdBy = "admin-01",
            createdAt = "2026-08-17T12:00:00Z"
        )
    }

    // ──────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────

    private fun buildReceiving(
        receivingId: String = "RCV-001",
        projectId: String = "PRJ-01",
        reference: String = "RCV-REF-001",
        warehouseId: String = "WH-01",
        receivingDate: String = "2026-08-17",
        status: InventoryReceivingStatus = InventoryReceivingStatus.DRAFT,
        expectedTotalQuantity: Int = 0,
        createdAt: String = "2026-08-17T08:00:00Z",
        updatedAt: String = "2026-08-17T08:00:00Z",
        completedAt: String? = null,
        completedBy: String? = null,
        cancelledAt: String? = null,
        cancelledBy: String? = null
    ) = InventoryReceiving(
        receivingId = receivingId,
        projectId = projectId,
        receivingReference = reference,
        warehouseId = warehouseId,
        receivingDate = receivingDate,
        status = status,
        expectedTotalQuantity = expectedTotalQuantity,
        createdBy = "admin-01",
        createdAt = createdAt,
        updatedAt = updatedAt,
        completedAt = completedAt,
        completedBy = completedBy,
        cancelledAt = cancelledAt,
        cancelledBy = cancelledBy
    )

    private fun buildLine(
        lineId: String = "LINE-001",
        receivingId: String = "RCV-001",
        receivedQuantity: Int = 0,
        acceptedQuantity: Int = 0,
        rejectedQuantity: Int = 0
    ) = InventoryReceivingLine(
        receivingLineId = lineId,
        receivingId = receivingId,
        projectId = "PRJ-01",
        inventoryProductId = "PROD-01",
        warehouseId = "WH-01",
        locationId = "LOC-01",
        receivedQuantity = receivedQuantity,
        acceptedQuantity = acceptedQuantity,
        rejectedQuantity = rejectedQuantity,
        createdAt = "2026-08-17T08:00:00Z",
        updatedAt = "2026-08-17T08:00:00Z"
    )
}

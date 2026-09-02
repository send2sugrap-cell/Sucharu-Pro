package com.sucharu.sucharupro.domain.model

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceivingLine
import com.sucharu.sucharupro.domain.validation.InventoryReceivingLineValidator
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [InventoryReceivingLineValidator] quantity and structural rules
 * (Module 07 Step 03).
 */
class InventoryReceivingLineValidationTest {

    @Test
    fun `valid received quantity passes`() {
        val result = InventoryReceivingLineValidator.validateReceivedQuantity(10, 10)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `zero received quantity fails`() {
        val result = InventoryReceivingLineValidator.validateReceivedQuantity(0, 10)
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun `negative received quantity fails`() {
        val result = InventoryReceivingLineValidator.validateReceivedQuantity(-1, 10)
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun `valid quantity split passes`() {
        val result = InventoryReceivingLineValidator.validateQuantitySplit(10, 7, 3)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `accepted + rejected exceeds received fails`() {
        val result = InventoryReceivingLineValidator.validateQuantitySplit(10, 7, 5)
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun `quantity split with full reconciliation required and partial split fails`() {
        val result = InventoryReceivingLineValidator.validateQuantitySplit(
            receivedQuantity = 10,
            acceptedQuantity = 5,
            rejectedQuantity = 3,
            requireFullReconciliation = true
        )
        assertTrue(result is DomainResult.Error)
        val msg = (result as DomainResult.Error).message
        assertTrue(msg.contains("must equal"))
    }

    @Test
    fun `full reconciliation with exact match passes`() {
        val result = InventoryReceivingLineValidator.validateQuantitySplit(
            receivedQuantity = 10,
            acceptedQuantity = 7,
            rejectedQuantity = 3,
            requireFullReconciliation = true
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `negative accepted quantity fails`() {
        val result = InventoryReceivingLineValidator.validateQuantitySplit(10, -1, 5)
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun `negative rejected quantity fails`() {
        val result = InventoryReceivingLineValidator.validateQuantitySplit(10, 5, -1)
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun `no duplicate line passes when no existing lines`() {
        val result = InventoryReceivingLineValidator.validateNoDuplicateLine(
            receivingId = "RCV-001",
            inventoryProductId = "PROD-01",
            locationId = "LOC-01",
            existingLines = emptyList()
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `duplicate product + location in same receiving fails`() {
        val existing = listOf(buildLine("LINE-001", "RCV-001", "PROD-01", "LOC-01"))
        val result = InventoryReceivingLineValidator.validateNoDuplicateLine(
            receivingId = "RCV-001",
            inventoryProductId = "PROD-01",
            locationId = "LOC-01",
            existingLines = existing
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun `same product at different location in same receiving passes`() {
        val existing = listOf(buildLine("LINE-001", "RCV-001", "PROD-01", "LOC-01"))
        val result = InventoryReceivingLineValidator.validateNoDuplicateLine(
            receivingId = "RCV-001",
            inventoryProductId = "PROD-01",
            locationId = "LOC-02",
            existingLines = existing
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `same product + location in different receiving passes`() {
        val existing = listOf(buildLine("LINE-001", "RCV-OTHER", "PROD-01", "LOC-01"))
        val result = InventoryReceivingLineValidator.validateNoDuplicateLine(
            receivingId = "RCV-001",
            inventoryProductId = "PROD-01",
            locationId = "LOC-01",
            existingLines = existing
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `update scenario - same line ID excluded from duplicate check`() {
        val existing = listOf(buildLine("LINE-001", "RCV-001", "PROD-01", "LOC-01"))
        val result = InventoryReceivingLineValidator.validateNoDuplicateLine(
            receivingId = "RCV-001",
            inventoryProductId = "PROD-01",
            locationId = "LOC-01",
            existingLines = existing,
            excludeLineId = "LINE-001"
        )
        assertTrue(result is DomainResult.Success)
    }

    private fun buildLine(
        lineId: String,
        receivingId: String,
        productId: String,
        locationId: String
    ) = InventoryReceivingLine(
        receivingLineId = lineId,
        receivingId = receivingId,
        projectId = "PRJ-01",
        inventoryProductId = productId,
        warehouseId = "WH-01",
        locationId = locationId,
        createdAt = "2026-08-17T08:00:00Z",
        updatedAt = "2026-08-17T08:00:00Z"
    )
}

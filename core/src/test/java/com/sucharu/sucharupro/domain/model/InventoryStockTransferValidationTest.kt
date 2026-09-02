package com.sucharu.sucharupro.domain.model

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransfer
import com.sucharu.sucharupro.domain.validation.InventoryStockTransferValidator
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Structural and quantity validation tests for [InventoryStockTransfer] (Module 07 Step 05).
 */
class InventoryStockTransferValidationTest {

    @Test
    fun `valid stock transfer passes validation`() {
        val transfer = buildTransfer()
        val result = InventoryStockTransferValidator.validateTransfer(transfer)
        assertTrue(result is DomainResult.Success)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank transfer ID fails`() {
        buildTransfer(transferId = " ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `same source and destination warehouse fails`() {
        buildTransfer(fromWarehouseId = "WH-01", toWarehouseId = "WH-01")
    }

    @Test
    fun `negative expected quantity fails`() {
        try {
            buildTransfer(expectedTotalQuantity = -1)
            assertTrue("Should have thrown IllegalArgumentException", false)
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("negative") == true)
        }
    }

    @Test
    fun `reference uniqueness validator detects duplicate`() {
        val existing = listOf(buildTransfer(transferReference = "ST-001"))
        val result = InventoryStockTransferValidator.validateReferenceUniqueness(
            reference = "st-001", // case insensitive
            transferId = "ST-NEW",
            projectId = "PRJ-01",
            existingTransfers = existing
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("already exists"))
    }

    @Test
    fun `reference uniqueness validator allows same reference in different project`() {
        val existing = listOf(buildTransfer(transferReference = "ST-001", projectId = "PRJ-01"))
        val result = InventoryStockTransferValidator.validateReferenceUniqueness(
            reference = "ST-001",
            transferId = "ST-NEW",
            projectId = "PRJ-02",
            existingTransfers = existing
        )
        assertTrue(result is DomainResult.Success)
    }

    private fun buildTransfer(
        transferId: String = "ST-01",
        projectId: String = "PRJ-01",
        transferReference: String = "ST-REF-01",
        fromWarehouseId: String = "WH-01",
        toWarehouseId: String = "WH-02",
        expectedTotalQuantity: Int = 10
    ) = InventoryStockTransfer(
        transferId = transferId,
        projectId = projectId,
        transferReference = transferReference,
        fromWarehouseId = fromWarehouseId,
        toWarehouseId = toWarehouseId,
        transferDate = "2026-08-17",
        expectedTotalQuantity = expectedTotalQuantity,
        createdBy = "admin",
        createdAt = "2026-08-17T10:00:00Z",
        updatedAt = "2026-08-17T10:00:00Z"
    )
}

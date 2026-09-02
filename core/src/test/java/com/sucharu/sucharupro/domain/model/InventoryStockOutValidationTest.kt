package com.sucharu.sucharupro.domain.model

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOut
import com.sucharu.sucharupro.domain.validation.InventoryStockOutValidator
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Structural and quantity validation tests for [InventoryStockOut] (Module 07 Step 04).
 */
class InventoryStockOutValidationTest {

    @Test
    fun `valid stock-out passes validation`() {
        val stockOut = buildStockOut()
        val result = InventoryStockOutValidator.validateStockOut(stockOut)
        assertTrue(result is DomainResult.Success)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank stock-out ID fails`() {
        buildStockOut(stockOutId = " ")
    }

    @Test
    fun `negative expected quantity fails`() {
        // Model init will throw, but let's test validator logic if it were possible to bypass init
        // For these models, require() is in init, so we test that they throw.
        try {
            buildStockOut(expectedTotalQuantity = -1)
            assertTrue("Should have thrown IllegalArgumentException", false)
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("negative") == true)
        }
    }

    @Test
    fun `reference uniqueness validator detects duplicate`() {
        val existing = listOf(buildStockOut(stockOutReference = "SO-001"))
        val result = InventoryStockOutValidator.validateReferenceUniqueness(
            reference = "so-001", // case insensitive
            stockOutId = "SO-NEW",
            projectId = "PRJ-01",
            existingStockOuts = existing
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("already exists"))
    }

    @Test
    fun `reference uniqueness validator allows same reference in different project`() {
        val existing = listOf(buildStockOut(stockOutReference = "SO-001", projectId = "PRJ-01"))
        val result = InventoryStockOutValidator.validateReferenceUniqueness(
            reference = "SO-001",
            stockOutId = "SO-NEW",
            projectId = "PRJ-02",
            existingStockOuts = existing
        )
        assertTrue(result is DomainResult.Success)
    }

    private fun buildStockOut(
        stockOutId: String = "SO-01",
        projectId: String = "PRJ-01",
        stockOutReference: String = "SO-REF-01",
        expectedTotalQuantity: Int = 10
    ) = InventoryStockOut(
        stockOutId = stockOutId,
        projectId = projectId,
        stockOutReference = stockOutReference,
        warehouseId = "WH-01",
        stockOutDate = "2026-08-17",
        expectedTotalQuantity = expectedTotalQuantity,
        createdBy = "admin",
        createdAt = "2026-08-17T10:00:00Z",
        updatedAt = "2026-08-17T10:00:00Z"
    )
}

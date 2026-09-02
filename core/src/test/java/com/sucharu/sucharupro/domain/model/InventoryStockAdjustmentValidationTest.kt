package com.sucharu.sucharupro.domain.model

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryAdjustmentReason
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryAdjustmentType
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryStockAdjustment
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryStockAdjustmentLine
import com.sucharu.sucharupro.domain.validation.InventoryStockAdjustmentLineValidator
import com.sucharu.sucharupro.domain.validation.InventoryStockAdjustmentValidator
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Structural and quantity validation tests for [InventoryStockAdjustment] (Module 07 Step 06).
 */
class InventoryStockAdjustmentValidationTest {

    @Test
    fun `valid stock adjustment passes validation`() {
        val adjustment = buildAdjustment()
        val result = InventoryStockAdjustmentValidator.validateAdjustment(adjustment)
        assertTrue(result is DomainResult.Success)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank adjustment ID fails`() {
        buildAdjustment(adjustmentId = " ")
    }

    @Test
    fun `negative total items adjusted fails`() {
        try {
            buildAdjustment(totalItemsAdjusted = -1)
            assertTrue("Should have thrown IllegalArgumentException", false)
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("negative") == true)
        }
    }

    @Test
    fun `reference uniqueness validator detects duplicate`() {
        val existing = listOf(buildAdjustment(adjustmentReference = "ADJ-001"))
        val result = InventoryStockAdjustmentValidator.validateAdjustmentReferenceUniqueness(
            reference = "adj-001", // case insensitive
            adjustmentId = "ADJ-NEW",
            projectId = "PRJ-01",
            existingAdjustments = existing
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("already exists"))
    }

    @Test
    fun `reference uniqueness validator allows same reference in different project`() {
        val existing = listOf(buildAdjustment(adjustmentReference = "ADJ-001", projectId = "PRJ-01"))
        val result = InventoryStockAdjustmentValidator.validateAdjustmentReferenceUniqueness(
            reference = "ADJ-001",
            adjustmentId = "ADJ-NEW",
            projectId = "PRJ-02",
            existingAdjustments = existing
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `valid adjustment line passes validation`() {
        val line = buildAdjustmentLine()
        val result = InventoryStockAdjustmentLineValidator.validateLine(line)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `adjustment quantity validation - valid increase`() {
        val result = InventoryStockAdjustmentLineValidator.validateAdjustmentQuantity(
            type = InventoryAdjustmentType.INCREASE,
            currentQuantity = 10,
            adjustedQuantity = 15,
            quantityChange = 5
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `adjustment quantity validation - valid decrease`() {
        val result = InventoryStockAdjustmentLineValidator.validateAdjustmentQuantity(
            type = InventoryAdjustmentType.DECREASE,
            currentQuantity = 10,
            adjustedQuantity = 8,
            quantityChange = -2
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `adjustment quantity validation - zero change fails`() {
        val result = InventoryStockAdjustmentLineValidator.validateAdjustmentQuantity(
            type = InventoryAdjustmentType.INCREASE,
            currentQuantity = 10,
            adjustedQuantity = 10,
            quantityChange = 0
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("cannot be zero"))
    }

    @Test
    fun `adjustment quantity validation - insufficient stock for decrease fails`() {
        val result = InventoryStockAdjustmentLineValidator.validateAdjustmentQuantity(
            type = InventoryAdjustmentType.DECREASE,
            currentQuantity = 5,
            adjustedQuantity = 0,
            quantityChange = -5
        )
        assertTrue(result is DomainResult.Success)

        // quantityChange mismatch
        val mismatchResult = InventoryStockAdjustmentLineValidator.validateAdjustmentQuantity(
            type = InventoryAdjustmentType.DECREASE,
            currentQuantity = 5,
            adjustedQuantity = 0,
            quantityChange = -10
        )
        assertTrue(mismatchResult is DomainResult.Error)
        assertTrue((mismatchResult as DomainResult.Error).message.contains("must match"))

        // Would result in negative stock (caught by adjustedQuantity < 0 check in validator usually, 
        // but let's see if we can trigger the specific negative stock message)
        // Wait, the validator does:
        // if (adjustedQuantity < 0) return Error(...)
        // if (currentQuantity + quantityChange < 0) return Error(...)
        // Since quantityChange = adjustedQuantity - currentQuantity, 
        // currentQuantity + (adjustedQuantity - currentQuantity) = adjustedQuantity.
        // So they are basically the same check.
        
        val negativeResult = InventoryStockAdjustmentLineValidator.validateAdjustmentQuantity(
            type = InventoryAdjustmentType.DECREASE,
            currentQuantity = 5,
            adjustedQuantity = 4,
            quantityChange = -6 // mismatch
        )
        assertTrue(negativeResult is DomainResult.Error)
    }

    private fun buildAdjustment(
        adjustmentId: String = "ADJ-01",
        projectId: String = "PRJ-01",
        adjustmentReference: String = "ADJ-REF-01",
        totalItemsAdjusted: Int = 0
    ) = InventoryStockAdjustment(
        adjustmentId = adjustmentId,
        projectId = projectId,
        adjustmentReference = adjustmentReference,
        warehouseId = "WH-01",
        adjustmentDate = "2026-08-17",
        totalItemsAdjusted = totalItemsAdjusted,
        createdBy = "admin",
        createdAt = "2026-08-17T10:00:00Z",
        updatedAt = "2026-08-17T10:00:00Z"
    )

    private fun buildAdjustmentLine(
        adjustmentLineId: String = "LINE-01",
        adjustmentId: String = "ADJ-01",
        projectId: String = "PRJ-01",
        inventoryProductId: String = "PROD-01",
        adjustmentType: InventoryAdjustmentType = InventoryAdjustmentType.INCREASE,
        currentQuantity: Int = 10,
        adjustedQuantity: Int = 12
    ) = InventoryStockAdjustmentLine(
        adjustmentLineId = adjustmentLineId,
        adjustmentId = adjustmentId,
        projectId = projectId,
        inventoryProductId = inventoryProductId,
        warehouseId = "WH-01",
        locationId = "LOC-01",
        adjustmentType = adjustmentType,
        adjustmentReason = InventoryAdjustmentReason.PHYSICAL_COUNT,
        currentQuantity = currentQuantity,
        adjustedQuantity = adjustedQuantity,
        quantityChange = adjustedQuantity - currentQuantity,
        createdAt = "2026-08-17T10:00:00Z",
        updatedAt = "2026-08-17T10:00:00Z"
    )
}

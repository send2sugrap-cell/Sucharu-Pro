package com.sucharu.sucharupro.domain.model

import com.sucharu.sucharupro.domain.model.inventory.InventoryProduct
import com.sucharu.sucharupro.domain.model.inventory.InventoryProductType
import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryProductCreationTest {

    @Test
    fun `valid product instantiates correctly with all metadata`() {
        val product = InventoryProduct(
            id = "PRD-01",
            sku = "QUR-STD-001",
            name = "Quran Sharif Standard",
            description = "Standard 15 lines Hafezi Quran",
            categoryId = "CAT-BOOKS",
            productType = InventoryProductType.BOOK,
            unitOfMeasure = InventoryUnit.PCS,
            isStockTracked = true,
            isFinishedProduct = true,
            isSaleable = true,
            isActive = true,
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z",
            createdBy = "admin-01"
        )
        assertEquals("PRD-01", product.id)
        assertEquals("QUR-STD-001", product.sku)
        assertEquals("QUR-STD-001", product.normalizedSku)
        assertEquals("Quran Sharif Standard", product.name)
        assertTrue(product.isActive)
        assertTrue(product.isStockTracked)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank product id throws exception`() {
        InventoryProduct(
            id = "",
            sku = "QUR-001",
            name = "Quran",
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z",
            createdBy = "admin-01"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank sku throws exception`() {
        InventoryProduct(
            id = "PRD-02",
            sku = "  ",
            name = "Quran",
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z",
            createdBy = "admin-01"
        )
    }
}

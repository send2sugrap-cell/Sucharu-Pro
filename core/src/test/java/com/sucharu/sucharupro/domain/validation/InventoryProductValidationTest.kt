package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryProduct
import com.sucharu.sucharupro.domain.model.inventory.InventoryProductType
import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryProductValidationTest {

    @Test
    fun `valid product passes validator`() {
        val product = InventoryProduct(
            id = "PRD-01",
            sku = "QAI-001",
            name = "Noorani Qaida",
            categoryId = "CAT-BOOKS",
            productType = InventoryProductType.BOOK,
            unitOfMeasure = InventoryUnit.PCS,
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z",
            createdBy = "admin-01"
        )
        val result = InventoryProductValidator.validateProduct(product)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `updatedAt before createdAt fails validation`() {
        try {
            val product = InventoryProduct(
                id = "PRD-02",
                sku = "QAI-002",
                name = "Noorani Qaida Color",
                createdAt = "2026-08-17T10:00:00Z",
                updatedAt = "2026-08-17T08:00:00Z",
                createdBy = "admin-01"
            )
            val result = InventoryProductValidator.validateProduct(product)
            assertTrue(result is DomainResult.Error)
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("cannot precede") == true)
        }
    }
}

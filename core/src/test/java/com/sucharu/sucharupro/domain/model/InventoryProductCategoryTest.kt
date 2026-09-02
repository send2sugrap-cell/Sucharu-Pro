package com.sucharu.sucharupro.domain.model

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryProductCategory
import com.sucharu.sucharupro.domain.validation.InventoryProductValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryProductCategoryTest {

    @Test
    fun `category instantiates and validates correctly`() {
        val category = InventoryProductCategory(
            id = "CAT-ISLAMIC",
            name = "Islamic Publications",
            description = "Quran, Hadith, and Islamic Learning Books",
            isActive = true,
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z"
        )
        assertEquals("CAT-ISLAMIC", category.id)
        assertTrue(category.isActive)

        val valResult = InventoryProductValidator.validateCategory(category)
        assertTrue(valResult is DomainResult.Success)
    }

    @Test
    fun `category duplicate name validation prevents duplicates`() {
        val existing = listOf(
            InventoryProductCategory(
                id = "CAT-01",
                name = "Gifts",
                createdAt = "2026-08-17T08:00:00Z",
                updatedAt = "2026-08-17T08:00:00Z"
            )
        )
        val dupRes = InventoryProductValidator.validateCategoryNameUniqueness("gifts", "CAT-02", existing)
        assertTrue(dupRes is DomainResult.Error)
    }
}

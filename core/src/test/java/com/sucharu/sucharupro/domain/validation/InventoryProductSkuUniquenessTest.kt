package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryProduct
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryProductSkuUniquenessTest {

    @Test
    fun `exact match and case-insensitive whitespace variants fail SKU uniqueness`() {
        val existing = listOf(
            InventoryProduct(
                id = "PRD-01",
                sku = "TAJ-001",
                name = "Tajwid Book",
                createdAt = "2026-08-17T08:00:00Z",
                updatedAt = "2026-08-17T08:00:00Z",
                createdBy = "admin-01"
            )
        )

        // 1. Exact match
        val exactRes = InventoryProductValidator.validateSkuUniqueness("TAJ-001", "PRD-NEW", existing)
        assertTrue(exactRes is DomainResult.Error)

        // 2. Case insensitive
        val caseRes = InventoryProductValidator.validateSkuUniqueness("taj-001", "PRD-NEW", existing)
        assertTrue(caseRes is DomainResult.Error)

        // 3. Whitespace variant
        val wsRes = InventoryProductValidator.validateSkuUniqueness("  TAJ-001  ", "PRD-NEW", existing)
        assertTrue(wsRes is DomainResult.Error)

        // 4. Same product updating own SKU passes
        val selfRes = InventoryProductValidator.validateSkuUniqueness("TAJ-001", "PRD-01", existing)
        assertTrue(selfRes is DomainResult.Success)

        // 5. Distinct SKU passes
        val distinctRes = InventoryProductValidator.validateSkuUniqueness("TAJ-002", "PRD-NEW", existing)
        assertTrue(distinctRes is DomainResult.Success)
    }
}

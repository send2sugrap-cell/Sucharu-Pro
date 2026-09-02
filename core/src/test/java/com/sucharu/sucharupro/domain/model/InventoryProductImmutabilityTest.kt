package com.sucharu.sucharupro.domain.model

import com.sucharu.sucharupro.domain.model.inventory.InventoryProduct
import com.sucharu.sucharupro.domain.model.inventory.InventoryStockIdentity
import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class InventoryProductImmutabilityTest {

    @Test
    fun `product data class copy preserves immutable fields and returns distinct instances`() {
        val prod1 = InventoryProduct(
            id = "PRD-IMMUTABLE",
            sku = "SKU-IMMUTABLE",
            name = "Base Product",
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z",
            createdBy = "admin-01"
        )
        val prod2 = prod1.copy(name = "Updated Product", updatedAt = "2026-08-17T09:00:00Z")

        assertEquals("PRD-IMMUTABLE", prod2.id)
        assertEquals("SKU-IMMUTABLE", prod2.sku)
        assertEquals("Base Product", prod1.name)
        assertEquals("Updated Product", prod2.name)

        val stockId = InventoryStockIdentity.fromProduct(prod1)
        assertEquals("PRD-IMMUTABLE", stockId.productId)
        assertEquals(InventoryUnit.PCS, stockId.unit)
    }
}

package com.sucharu.sucharupro.domain.model

import com.sucharu.sucharupro.domain.model.inventory.InventoryProductType
import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class InventoryProductUnitTest {

    @Test
    fun `inventory units and product types expose expected labels`() {
        assertEquals("Pcs", InventoryUnit.PCS.defaultLabel)
        assertEquals("Set", InventoryUnit.SET.defaultLabel)
        assertEquals("Box", InventoryUnit.BOX.defaultLabel)
        assertEquals("Pack", InventoryUnit.PACK.defaultLabel)
        assertEquals("Unit", InventoryUnit.UNIT.defaultLabel)

        assertEquals("Finished Product", InventoryProductType.FINISHED_PRODUCT.defaultLabel)
        assertEquals("Book", InventoryProductType.BOOK.defaultLabel)
        assertEquals("Gift Product", InventoryProductType.GIFT_PRODUCT.defaultLabel)
    }

    @Test
    fun `enum entries are non empty`() {
        assertNotNull(InventoryUnit.entries)
        assertNotNull(InventoryProductType.entries)
    }
}

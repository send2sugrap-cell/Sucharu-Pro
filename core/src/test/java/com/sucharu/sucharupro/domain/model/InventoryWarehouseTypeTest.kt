package com.sucharu.sucharupro.domain.model

import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class InventoryWarehouseTypeTest {

    @Test
    fun `warehouse type enum labels are populated`() {
        assertEquals("Main Warehouse", InventoryWarehouseType.MAIN.defaultLabel)
        assertEquals("Finished Goods Warehouse", InventoryWarehouseType.FINISHED_GOODS.defaultLabel)
        assertEquals("Book Storage Facility", InventoryWarehouseType.BOOK.defaultLabel)
        assertEquals("Gift & Merchandise Hub", InventoryWarehouseType.GIFT.defaultLabel)
        assertNotNull(InventoryWarehouseType.entries)
    }
}

package com.sucharu.sucharupro.domain.model

import com.sucharu.sucharupro.domain.model.inventory.InventoryLocationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class InventoryLocationTypeTest {

    @Test
    fun `location type enum labels are populated`() {
        assertEquals("Storage Area", InventoryLocationType.AREA.defaultLabel)
        assertEquals("Storage Room", InventoryLocationType.ROOM.defaultLabel)
        assertEquals("Zone / Bay", InventoryLocationType.ZONE.defaultLabel)
        assertEquals("Storage Rack", InventoryLocationType.RACK.defaultLabel)
        assertEquals("Shelf Tier", InventoryLocationType.SHELF.defaultLabel)
        assertEquals("Storage Bin", InventoryLocationType.BIN.defaultLabel)
        assertNotNull(InventoryLocationType.entries)
    }
}

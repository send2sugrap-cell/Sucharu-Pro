package com.sucharu.sucharupro.domain.model

import com.sucharu.sucharupro.domain.model.inventory.InventoryLocation
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocationStatus
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class InventoryLocationCreationTest {

    @Test
    fun `valid location instantiates with metadata and capacity`() {
        val location = InventoryLocation(
            id = "LOC-01",
            projectId = "PRJ-01",
            warehouseId = "WH-01",
            parentLocationId = null,
            code = "ZONE-A",
            name = "Zone A Storage",
            description = "High density storage bay",
            type = InventoryLocationType.ZONE,
            status = InventoryLocationStatus.ACTIVE,
            capacity = 1000.0,
            capacityUnit = "PCS",
            notes = "Climate controlled",
            createdBy = "admin-01",
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z"
        )
        assertEquals("LOC-01", location.id)
        assertEquals("ZONE-A", location.code)
        assertEquals("ZONE-A", location.normalizedCode)
        assertEquals(1000.0, location.capacity!!, 0.001)
        assertEquals("PCS", location.capacityUnit)
        assertFalse(location.isTerminal)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative capacity throws exception`() {
        InventoryLocation(
            id = "LOC-02",
            projectId = "PRJ-01",
            warehouseId = "WH-01",
            code = "RACK-01",
            name = "Rack 1",
            capacity = -10.0,
            createdBy = "admin-01",
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z"
        )
    }
}

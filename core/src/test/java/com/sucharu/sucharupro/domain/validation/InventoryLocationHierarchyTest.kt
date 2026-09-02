package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocation
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocationType
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryLocationHierarchyTest {

    @Test
    fun `valid multi-tier location hierarchy resolves successfully`() {
        val zone = InventoryLocation(
            id = "LOC-ZONE",
            projectId = "PRJ-01",
            warehouseId = "WH-01",
            code = "ZONE-A",
            name = "Zone A",
            type = InventoryLocationType.ZONE,
            createdBy = "admin-01",
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z"
        )
        val rack = InventoryLocation(
            id = "LOC-RACK",
            projectId = "PRJ-01",
            warehouseId = "WH-01",
            parentLocationId = "LOC-ZONE",
            code = "RACK-01",
            name = "Rack 1",
            type = InventoryLocationType.RACK,
            createdBy = "admin-01",
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z"
        )
        val shelf = InventoryLocation(
            id = "LOC-SHELF",
            projectId = "PRJ-01",
            warehouseId = "WH-01",
            parentLocationId = "LOC-RACK",
            code = "SHELF-01",
            name = "Shelf 1",
            type = InventoryLocationType.SHELF,
            createdBy = "admin-01",
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z"
        )

        val list = listOf(zone, rack, shelf)

        // Validate shelf parenting
        val result = InventoryLocationValidator.validateParentHierarchy(
            locationId = "LOC-SHELF",
            parentLocationId = "LOC-RACK",
            warehouseId = "WH-01",
            projectId = "PRJ-01",
            allLocations = list
        )
        assertTrue(result is DomainResult.Success)
    }
}

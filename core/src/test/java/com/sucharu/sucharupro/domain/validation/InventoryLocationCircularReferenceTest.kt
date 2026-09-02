package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocation
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryLocationCircularReferenceTest {

    @Test
    fun `self parenting is detected and forbidden`() {
        val list = emptyList<InventoryLocation>()
        val result = InventoryLocationValidator.validateParentHierarchy(
            locationId = "LOC-01",
            parentLocationId = "LOC-01",
            warehouseId = "WH-01",
            projectId = "PRJ-01",
            allLocations = list
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("cannot be its own parent", ignoreCase = true))
    }

    @Test
    fun `indirect circular hierarchy reference is detected and prevented`() {
        // LOC-A -> LOC-B -> LOC-C. Now trying to set LOC-A's parent to LOC-C creates a cycle!
        val locA = InventoryLocation(
            id = "LOC-A",
            projectId = "PRJ-01",
            warehouseId = "WH-01",
            parentLocationId = null,
            code = "LOC-A",
            name = "Location A",
            createdBy = "admin-01",
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z"
        )
        val locB = InventoryLocation(
            id = "LOC-B",
            projectId = "PRJ-01",
            warehouseId = "WH-01",
            parentLocationId = "LOC-A",
            code = "LOC-B",
            name = "Location B",
            createdBy = "admin-01",
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z"
        )
        val locC = InventoryLocation(
            id = "LOC-C",
            projectId = "PRJ-01",
            warehouseId = "WH-01",
            parentLocationId = "LOC-B",
            code = "LOC-C",
            name = "Location C",
            createdBy = "admin-01",
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z"
        )

        val list = listOf(locA, locB, locC)

        val result = InventoryLocationValidator.validateParentHierarchy(
            locationId = "LOC-A",
            parentLocationId = "LOC-C",
            warehouseId = "WH-01",
            projectId = "PRJ-01",
            allLocations = list
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Circular hierarchy reference", ignoreCase = true))
    }
}

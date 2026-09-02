package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocation
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocationStatus
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryLocationParentValidationTest {

    @Test
    fun `cannot assign an archived parent to a location`() {
        val archivedParent = InventoryLocation(
            id = "LOC-ARCH-PARENT",
            projectId = "PRJ-01",
            warehouseId = "WH-01",
            code = "P-ARCH",
            name = "Archived Parent",
            status = InventoryLocationStatus.ARCHIVED,
            createdBy = "admin-01",
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z",
            archivedAt = "2026-08-17T08:30:00Z"
        )
        val list = listOf(archivedParent)

        val result = InventoryLocationValidator.validateParentHierarchy(
            locationId = "LOC-CHILD",
            parentLocationId = "LOC-ARCH-PARENT",
            warehouseId = "WH-01",
            projectId = "PRJ-01",
            allLocations = list
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("ARCHIVED parent", ignoreCase = true))
    }
}

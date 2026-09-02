package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocation
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryLocationParentIsolationTest {

    @Test
    fun `parent from different project is rejected`() {
        val parentFromOtherPrj = InventoryLocation(
            id = "LOC-PRJ-B",
            projectId = "PRJ-B", // Different Project
            warehouseId = "WH-01",
            code = "LOC-B",
            name = "Project B Loc",
            createdBy = "admin-01",
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z"
        )
        val list = listOf(parentFromOtherPrj)

        val result = InventoryLocationValidator.validateParentHierarchy(
            locationId = "LOC-CHILD",
            parentLocationId = "LOC-PRJ-B",
            warehouseId = "WH-01",
            projectId = "PRJ-A",
            allLocations = list
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("project mismatch", ignoreCase = true) ||
                (result as DomainResult.Error).message.contains("belongs to project", ignoreCase = true))
    }
}

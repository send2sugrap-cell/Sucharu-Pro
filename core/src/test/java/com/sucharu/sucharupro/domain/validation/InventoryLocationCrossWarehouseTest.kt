package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocation
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryLocationCrossWarehouseTest {

    @Test
    fun `parent from different warehouse is rejected`() {
        val parentInWh2 = InventoryLocation(
            id = "LOC-WH-02",
            projectId = "PRJ-01",
            warehouseId = "WH-02", // Different warehouse
            code = "LOC-W2",
            name = "Wh 2 Loc",
            createdBy = "admin-01",
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z"
        )
        val list = listOf(parentInWh2)

        val result = InventoryLocationValidator.validateParentHierarchy(
            locationId = "LOC-WH-01-CHILD",
            parentLocationId = "LOC-WH-02",
            warehouseId = "WH-01",
            projectId = "PRJ-01",
            allLocations = list
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("belongs to warehouse", ignoreCase = true))
    }
}

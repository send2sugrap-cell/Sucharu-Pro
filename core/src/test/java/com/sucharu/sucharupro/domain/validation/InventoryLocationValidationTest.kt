package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocation
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryLocationValidationTest {

    @Test
    fun `valid location passes validation`() {
        val loc = InventoryLocation(
            id = "LOC-01",
            projectId = "PRJ-01",
            warehouseId = "WH-01",
            code = "BIN-01",
            name = "Bin 1",
            createdBy = "admin-01",
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z"
        )
        val result = InventoryLocationValidator.validateLocation(loc)
        assertTrue(result is DomainResult.Success)
    }
}

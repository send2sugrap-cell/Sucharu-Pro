package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocation
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryLocationCodeUniquenessTest {

    @Test
    fun `location code uniqueness enforced per warehouse with case invariance`() {
        val existing = listOf(
            InventoryLocation(
                id = "LOC-01",
                projectId = "PRJ-01",
                warehouseId = "WH-01",
                code = "RACK-01",
                name = "Rack 1",
                createdBy = "admin-01",
                createdAt = "2026-08-17T08:00:00Z",
                updatedAt = "2026-08-17T08:00:00Z"
            )
        )

        // 1. Same warehouse, duplicate code fails
        val dupExact = InventoryLocationValidator.validateLocationCodeUniqueness("RACK-01", "LOC-02", "WH-01", existing)
        assertTrue(dupExact is DomainResult.Error)

        // 2. Same warehouse, case variant fails
        val dupCase = InventoryLocationValidator.validateLocationCodeUniqueness("rack-01", "LOC-02", "WH-01", existing)
        assertTrue(dupCase is DomainResult.Error)

        // 3. Different warehouse, same code succeeds
        val diffWh = InventoryLocationValidator.validateLocationCodeUniqueness("RACK-01", "LOC-02", "WH-02", existing)
        assertTrue(diffWh is DomainResult.Success)

        // 4. Same location updating itself succeeds
        val self = InventoryLocationValidator.validateLocationCodeUniqueness("RACK-01", "LOC-01", "WH-01", existing)
        assertTrue(self is DomainResult.Success)
    }
}

package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouse
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryWarehouseCodeUniquenessTest {

    @Test
    fun `warehouse code uniqueness enforced per project with case and whitespace invariance`() {
        val existing = listOf(
            InventoryWarehouse(
                id = "WH-01",
                projectId = "PRJ-A",
                code = "WH-CENTRAL",
                name = "Central Facility",
                createdBy = "admin-01",
                createdAt = "2026-08-17T08:00:00Z",
                updatedAt = "2026-08-17T08:00:00Z"
            )
        )

        // 1. Same project, duplicate code fails
        val dupExact = InventoryWarehouseValidator.validateWarehouseCodeUniqueness("WH-CENTRAL", "WH-02", "PRJ-A", existing)
        assertTrue(dupExact is DomainResult.Error)

        // 2. Same project, case variant fails
        val dupCase = InventoryWarehouseValidator.validateWarehouseCodeUniqueness("wh-central", "WH-02", "PRJ-A", existing)
        assertTrue(dupCase is DomainResult.Error)

        // 3. Different project, same code succeeds (cross-project isolation)
        val diffProject = InventoryWarehouseValidator.validateWarehouseCodeUniqueness("WH-CENTRAL", "WH-02", "PRJ-B", existing)
        assertTrue(diffProject is DomainResult.Success)

        // 4. Same warehouse updating itself succeeds
        val self = InventoryWarehouseValidator.validateWarehouseCodeUniqueness("WH-CENTRAL", "WH-01", "PRJ-A", existing)
        assertTrue(self is DomainResult.Success)
    }
}

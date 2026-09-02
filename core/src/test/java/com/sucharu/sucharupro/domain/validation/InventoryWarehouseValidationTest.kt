package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouse
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouseStatus
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryWarehouseValidationTest {

    @Test
    fun `valid warehouse passes validation`() {
        val warehouse = InventoryWarehouse(
            id = "WH-01",
            projectId = "PRJ-01",
            code = "WH-MAIN",
            name = "Main Facility",
            createdBy = "admin-01",
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z"
        )
        val result = InventoryWarehouseValidator.validateWarehouse(warehouse)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `archived warehouse without archivedAt fails validation`() {
        try {
            val warehouse = InventoryWarehouse(
                id = "WH-02",
                projectId = "PRJ-01",
                code = "WH-ARCHIVED",
                name = "Archived Facility",
                status = InventoryWarehouseStatus.ARCHIVED,
                createdBy = "admin-01",
                createdAt = "2026-08-17T08:00:00Z",
                updatedAt = "2026-08-17T08:00:00Z",
                archivedAt = null
            )
            val result = InventoryWarehouseValidator.validateWarehouse(warehouse)
            assertTrue(result is DomainResult.Error)
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("archivedAt timestamp is required") == true)
        }
    }
}

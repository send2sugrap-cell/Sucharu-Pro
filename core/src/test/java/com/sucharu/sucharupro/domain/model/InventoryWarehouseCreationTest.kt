package com.sucharu.sucharupro.domain.model

import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouse
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouseStatus
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryWarehouseCreationTest {

    @Test
    fun `valid warehouse instantiates correctly with metadata`() {
        val warehouse = InventoryWarehouse(
            id = "WH-01",
            projectId = "PRJ-01",
            code = "WH-CENTRAL",
            name = "Central Finished Goods Warehouse",
            description = "Main warehouse facility for printed and finished inventory",
            type = InventoryWarehouseType.FINISHED_GOODS,
            status = InventoryWarehouseStatus.ACTIVE,
            address = "Dhaka, Bangladesh",
            contactPerson = "Rafiqul Islam",
            contactPhone = "+8801700000000",
            notes = "Primary facility",
            createdBy = "admin-01",
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z"
        )
        assertEquals("WH-01", warehouse.id)
        assertEquals("WH-CENTRAL", warehouse.code)
        assertEquals("WH-CENTRAL", warehouse.normalizedCode)
        assertEquals("Central Finished Goods Warehouse", warehouse.name)
        assertFalse(warehouse.isTerminal)
        assertEquals(InventoryWarehouseStatus.ACTIVE, warehouse.status)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank warehouse code throws exception`() {
        InventoryWarehouse(
            id = "WH-02",
            projectId = "PRJ-01",
            code = "  ",
            name = "Warehouse",
            createdBy = "admin-01",
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z"
        )
    }
}

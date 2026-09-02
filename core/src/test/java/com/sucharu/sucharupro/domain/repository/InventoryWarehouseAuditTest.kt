package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeInventoryWarehouseDataSource
import com.sucharu.sucharupro.data.repository.InventoryWarehouseRepositoryImpl
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouse
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouseActivityType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class InventoryWarehouseAuditTest {

    private lateinit var dataSource: FakeInventoryWarehouseDataSource
    private lateinit var repository: InventoryWarehouseRepository

    @Before
    fun setup() {
        dataSource = FakeInventoryWarehouseDataSource()
        repository = InventoryWarehouseRepositoryImpl(dataSource)
    }

    @Test
    fun `warehouse operations record append-only audit events`() = runBlocking {
        // 1. Create
        repository.createWarehouse(
            InventoryWarehouse(
                id = "WH-AUD",
                projectId = "PRJ-01",
                code = "WH-AUD",
                name = "Audit Warehouse",
                createdBy = "admin-01",
                createdAt = "2026-08-17T08:00:00Z",
                updatedAt = "2026-08-17T08:00:00Z"
            ),
            callerRole = UserRole.ADMIN
        )

        // 2. Deactivate
        repository.deactivateWarehouse("WH-AUD", "mgr-01", "2026-08-17T08:30:00Z", UserRole.MANAGER)

        // 3. Activate
        repository.activateWarehouse("WH-AUD", "mgr-01", "2026-08-17T09:00:00Z", UserRole.MANAGER)

        // 4. Archive
        repository.archiveWarehouse("WH-AUD", "admin-01", "2026-08-17T09:30:00Z", UserRole.ADMIN)

        val events = repository.observeActivityEvents("PRJ-01").first()
        assertEquals(4, events.size)

        assertEquals(InventoryWarehouseActivityType.WAREHOUSE_ARCHIVED, events[0].eventType)
        assertEquals(InventoryWarehouseActivityType.WAREHOUSE_ACTIVATED, events[1].eventType)
        assertEquals(InventoryWarehouseActivityType.WAREHOUSE_DEACTIVATED, events[2].eventType)
        assertEquals(InventoryWarehouseActivityType.WAREHOUSE_CREATED, events[3].eventType)
    }
}

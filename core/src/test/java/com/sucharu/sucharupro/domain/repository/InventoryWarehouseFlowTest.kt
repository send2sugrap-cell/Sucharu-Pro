package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeInventoryWarehouseDataSource
import com.sucharu.sucharupro.data.repository.InventoryWarehouseRepositoryImpl
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouse
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class InventoryWarehouseFlowTest {

    private lateinit var dataSource: FakeInventoryWarehouseDataSource
    private lateinit var repository: InventoryWarehouseRepository

    @Before
    fun setup() {
        dataSource = FakeInventoryWarehouseDataSource()
        repository = InventoryWarehouseRepositoryImpl(dataSource)
    }

    @Test
    fun `warehouse flow emits updated list on new warehouse creation`() = runBlocking {
        val initial = repository.observeWarehouses("PRJ-01").first()
        assertEquals(0, initial.size)

        repository.createWarehouse(
            InventoryWarehouse(
                id = "WH-FLW-01",
                projectId = "PRJ-01",
                code = "WH-F01",
                name = "Flow Facility",
                createdBy = "admin-01",
                createdAt = "2026-08-17T08:00:00Z",
                updatedAt = "2026-08-17T08:00:00Z"
            ),
            callerRole = UserRole.ADMIN
        )

        val updated = repository.observeWarehouses("PRJ-01").first()
        assertEquals(1, updated.size)
        assertEquals("Flow Facility", updated.first().name)
    }
}

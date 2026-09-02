package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouse
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InventoryWarehouseDataSourceTest {

    private lateinit var dataSource: FakeInventoryWarehouseDataSource

    @Before
    fun setup() {
        dataSource = FakeInventoryWarehouseDataSource()
    }

    @Test
    fun `data source prevents duplicate id and duplicate code within same project`() = runBlocking {
        val wh1 = InventoryWarehouse(
            id = "WH-01",
            projectId = "PRJ-01",
            code = "WH-CODE",
            name = "Warehouse 1",
            createdBy = "admin-01",
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z"
        )
        val whDupCode = InventoryWarehouse(
            id = "WH-02",
            projectId = "PRJ-01",
            code = "WH-CODE", // Duplicate code in same project
            name = "Warehouse 2",
            createdBy = "admin-01",
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z"
        )
        val whDupId = InventoryWarehouse(
            id = "WH-01", // Duplicate ID
            projectId = "PRJ-01",
            code = "WH-NEW-CODE",
            name = "Warehouse 3",
            createdBy = "admin-01",
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z"
        )

        assertTrue(dataSource.insertWarehouse(wh1) is DomainResult.Success)
        assertTrue(dataSource.insertWarehouse(whDupCode) is DomainResult.Error)
        assertTrue(dataSource.insertWarehouse(whDupId) is DomainResult.Error)

        val list = dataSource.observeWarehouses().first()
        assertEquals(1, list.size)
    }
}

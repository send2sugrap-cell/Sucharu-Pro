package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeInventoryWarehouseDataSource
import com.sucharu.sucharupro.data.repository.InventoryWarehouseRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouse
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InventoryWarehouseCrossProjectIsolationTest {

    private lateinit var dataSource: FakeInventoryWarehouseDataSource
    private lateinit var repository: InventoryWarehouseRepository

    @Before
    fun setup() {
        dataSource = FakeInventoryWarehouseDataSource()
        repository = InventoryWarehouseRepositoryImpl(dataSource)
    }

    @Test
    fun `project A warehouses never appear in project B queries`() = runBlocking {
        repository.createWarehouse(
            InventoryWarehouse(
                id = "WH-PRJ-A",
                projectId = "PRJ-A",
                code = "WH-SHARED-CODE",
                name = "Warehouse A",
                createdBy = "admin-01",
                createdAt = "2026-08-17T08:00:00Z",
                updatedAt = "2026-08-17T08:00:00Z"
            ),
            callerRole = UserRole.ADMIN
        )

        repository.createWarehouse(
            InventoryWarehouse(
                id = "WH-PRJ-B",
                projectId = "PRJ-B",
                code = "WH-SHARED-CODE", // Same code in different project allowed
                name = "Warehouse B",
                createdBy = "admin-01",
                createdAt = "2026-08-17T08:00:00Z",
                updatedAt = "2026-08-17T08:00:00Z"
            ),
            callerRole = UserRole.ADMIN
        )

        val prjAList = repository.observeWarehouses("PRJ-A").first()
        assertEquals(1, prjAList.size)
        assertEquals("WH-PRJ-A", prjAList.first().id)

        val prjBList = repository.observeWarehouses("PRJ-B").first()
        assertEquals(1, prjBList.size)
        assertEquals("WH-PRJ-B", prjBList.first().id)

        // Query by code within project
        val prjACodeRes = repository.getWarehouseByCode("WH-SHARED-CODE", "PRJ-A")
        assertTrue(prjACodeRes is DomainResult.Success)
        assertEquals("WH-PRJ-A", (prjACodeRes as DomainResult.Success).data.id)

        val prjBCodeRes = repository.getWarehouseByCode("WH-SHARED-CODE", "PRJ-B")
        assertTrue(prjBCodeRes is DomainResult.Success)
        assertEquals("WH-PRJ-B", (prjBCodeRes as DomainResult.Success).data.id)
    }
}

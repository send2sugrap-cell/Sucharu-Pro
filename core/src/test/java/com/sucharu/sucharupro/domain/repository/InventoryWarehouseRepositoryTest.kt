package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeInventoryWarehouseDataSource
import com.sucharu.sucharupro.data.repository.InventoryWarehouseRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouse
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouseType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InventoryWarehouseRepositoryTest {

    private lateinit var dataSource: FakeInventoryWarehouseDataSource
    private lateinit var repository: InventoryWarehouseRepository

    @Before
    fun setup() {
        dataSource = FakeInventoryWarehouseDataSource()
        repository = InventoryWarehouseRepositoryImpl(dataSource)
    }

    @Test
    fun `full warehouse CRUD and querying through repository`() = runBlocking {
        val wh = InventoryWarehouse(
            id = "WH-REPO-01",
            projectId = "PRJ-01",
            code = "WH-R01",
            name = "Repository Hub",
            description = "Main test facility",
            type = InventoryWarehouseType.FINISHED_GOODS,
            createdBy = "admin-01",
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z"
        )
        val createRes = repository.createWarehouse(wh, callerRole = UserRole.ADMIN)
        assertTrue(createRes is DomainResult.Success)

        // Query by Code
        val codeRes = repository.getWarehouseByCode("wh-r01", "PRJ-01", UserRole.MANAGER)
        assertTrue(codeRes is DomainResult.Success)
        assertEquals("WH-REPO-01", (codeRes as DomainResult.Success).data.id)

        // Update Metadata
        val updateRes = repository.updateWarehouseMetadata(
            warehouseId = "WH-REPO-01",
            name = "Repository Hub (Updated)",
            description = "Updated description",
            type = InventoryWarehouseType.BOOK,
            address = "Dhaka City",
            contactPerson = "Karim",
            contactPhone = "01800000000",
            notes = "Updated",
            timestamp = "2026-08-17T09:00:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(updateRes is DomainResult.Success)
        assertEquals("Repository Hub (Updated)", (updateRes as DomainResult.Success).data.name)

        val list = repository.observeWarehouses("PRJ-01").first()
        assertEquals(1, list.size)
        assertEquals("Repository Hub (Updated)", list.first().name)
    }
}

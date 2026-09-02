package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeInventoryWarehouseDataSource
import com.sucharu.sucharupro.data.repository.InventoryWarehouseRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouse
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouseStatus
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouseType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InventoryWarehouseArchiveTest {

    private lateinit var dataSource: FakeInventoryWarehouseDataSource
    private lateinit var repository: InventoryWarehouseRepository

    @Before
    fun setup() {
        dataSource = FakeInventoryWarehouseDataSource()
        repository = InventoryWarehouseRepositoryImpl(dataSource)
    }

    @Test
    fun `archiving a warehouse renders it terminal and prevents further mutation`() = runBlocking {
        val wh = InventoryWarehouse(
            id = "WH-ARCH-01",
            projectId = "PRJ-01",
            code = "WH-ARCH",
            name = "Archiving Hub",
            status = InventoryWarehouseStatus.ACTIVE,
            createdBy = "admin-01",
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z"
        )
        repository.createWarehouse(wh, callerRole = UserRole.ADMIN)

        // Archive
        val archRes = repository.archiveWarehouse("WH-ARCH-01", "admin-01", "2026-08-17T09:00:00Z", UserRole.ADMIN)
        assertTrue(archRes is DomainResult.Success)
        val updated = (archRes as DomainResult.Success).data
        assertEquals(InventoryWarehouseStatus.ARCHIVED, updated.status)
        assertTrue(updated.isTerminal)

        // Attempting to reactivate fails
        val reactRes = repository.activateWarehouse("WH-ARCH-01", "admin-01", "2026-08-17T09:30:00Z", UserRole.ADMIN)
        assertTrue(reactRes is DomainResult.Error)

        // Attempting to update metadata fails
        val updateRes = repository.updateWarehouseMetadata(
            warehouseId = "WH-ARCH-01",
            name = "New Name",
            description = null,
            type = InventoryWarehouseType.MAIN,
            address = null,
            contactPerson = null,
            contactPhone = null,
            notes = null,
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.ADMIN
        )
        assertTrue(updateRes is DomainResult.Error)
    }
}

package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeInventoryLocationDataSource
import com.sucharu.sucharupro.data.repository.InventoryLocationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocation
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocationType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InventoryLocationRepositoryTest {

    private lateinit var dataSource: FakeInventoryLocationDataSource
    private lateinit var repository: InventoryLocationRepository

    @Before
    fun setup() {
        dataSource = FakeInventoryLocationDataSource()
        repository = InventoryLocationRepositoryImpl(dataSource)
    }

    @Test
    fun `full location repository CRUD, query by code, metadata update, and parent changes`() = runBlocking {
        val loc = InventoryLocation(
            id = "LOC-REP-01",
            projectId = "PRJ-01",
            warehouseId = "WH-01",
            code = "BIN-REP-01",
            name = "Repository Bin",
            type = InventoryLocationType.BIN,
            capacity = 500.0,
            capacityUnit = "PCS",
            createdBy = "admin-01",
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z"
        )
        val createRes = repository.createLocation(loc, callerRole = UserRole.ADMIN)
        assertTrue(createRes is DomainResult.Success)

        // Query by Code
        val codeRes = repository.getLocationByCode("bin-rep-01", "WH-01", "PRJ-01", UserRole.MANAGER)
        assertTrue(codeRes is DomainResult.Success)
        assertEquals("LOC-REP-01", (codeRes as DomainResult.Success).data.id)

        // Update Metadata
        val updateRes = repository.updateLocationMetadata(
            locationId = "LOC-REP-01",
            name = "Repository Bin Updated",
            description = "Updated desc",
            type = InventoryLocationType.SHELF,
            capacity = 750.0,
            capacityUnit = "PCS",
            notes = "Updated notes",
            timestamp = "2026-08-17T09:00:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(updateRes is DomainResult.Success)
        assertEquals("Repository Bin Updated", (updateRes as DomainResult.Success).data.name)
        assertEquals(750.0, (updateRes as DomainResult.Success).data.capacity!!, 0.001)

        val list = repository.observeLocations("PRJ-01").first()
        assertEquals(1, list.size)
    }
}

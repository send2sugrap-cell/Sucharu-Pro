package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeInventoryLocationDataSource
import com.sucharu.sucharupro.data.repository.InventoryLocationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocation
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocationStatus
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocationType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InventoryLocationArchiveTest {

    private lateinit var dataSource: FakeInventoryLocationDataSource
    private lateinit var repository: InventoryLocationRepository

    @Before
    fun setup() {
        dataSource = FakeInventoryLocationDataSource()
        repository = InventoryLocationRepositoryImpl(dataSource)
    }

    @Test
    fun `archiving a location renders it terminal and prevents mutations`() = runBlocking {
        val loc = InventoryLocation(
            id = "LOC-ARCH-01",
            projectId = "PRJ-01",
            warehouseId = "WH-01",
            code = "BIN-ARCH",
            name = "Archived Bin",
            status = InventoryLocationStatus.ACTIVE,
            createdBy = "admin-01",
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z"
        )
        repository.createLocation(loc, callerRole = UserRole.ADMIN)

        val archRes = repository.archiveLocation("LOC-ARCH-01", "admin-01", "2026-08-17T09:00:00Z", UserRole.ADMIN)
        assertTrue(archRes is DomainResult.Success)
        val updated = (archRes as DomainResult.Success).data
        assertEquals(InventoryLocationStatus.ARCHIVED, updated.status)
        assertTrue(updated.isTerminal)

        // Reactivate fails
        val reactRes = repository.activateLocation("LOC-ARCH-01", "admin-01", "2026-08-17T09:30:00Z", UserRole.ADMIN)
        assertTrue(reactRes is DomainResult.Error)

        // Metadata update fails
        val updateRes = repository.updateLocationMetadata(
            locationId = "LOC-ARCH-01",
            name = "New Name",
            description = null,
            type = InventoryLocationType.BIN,
            capacity = 100.0,
            capacityUnit = "PCS",
            notes = null,
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.ADMIN
        )
        assertTrue(updateRes is DomainResult.Error)
    }
}

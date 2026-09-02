package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeInventoryLocationDataSource
import com.sucharu.sucharupro.data.repository.InventoryLocationRepositoryImpl
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocation
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocationActivityType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class InventoryLocationAuditTest {

    private lateinit var dataSource: FakeInventoryLocationDataSource
    private lateinit var repository: InventoryLocationRepository

    @Before
    fun setup() {
        dataSource = FakeInventoryLocationDataSource()
        repository = InventoryLocationRepositoryImpl(dataSource)
    }

    @Test
    fun `location mutations generate append-only audit events`() = runBlocking {
        // 1. Create
        repository.createLocation(
            InventoryLocation(
                id = "LOC-AUD",
                projectId = "PRJ-01",
                warehouseId = "WH-01",
                code = "BIN-AUD",
                name = "Audit Bin",
                createdBy = "admin-01",
                createdAt = "2026-08-17T08:00:00Z",
                updatedAt = "2026-08-17T08:00:00Z"
            ),
            callerRole = UserRole.ADMIN
        )

        // 2. Deactivate
        repository.deactivateLocation("LOC-AUD", "mgr-01", "2026-08-17T08:30:00Z", UserRole.MANAGER)

        // 3. Activate
        repository.activateLocation("LOC-AUD", "mgr-01", "2026-08-17T09:00:00Z", UserRole.MANAGER)

        // 4. Archive
        repository.archiveLocation("LOC-AUD", "admin-01", "2026-08-17T09:30:00Z", UserRole.ADMIN)

        val events = repository.observeActivityEvents("PRJ-01").first()
        assertEquals(4, events.size)
        assertEquals(InventoryLocationActivityType.LOCATION_ARCHIVED, events[0].eventType)
        assertEquals(InventoryLocationActivityType.LOCATION_ACTIVATED, events[1].eventType)
        assertEquals(InventoryLocationActivityType.LOCATION_DEACTIVATED, events[2].eventType)
        assertEquals(InventoryLocationActivityType.LOCATION_CREATED, events[3].eventType)
    }
}

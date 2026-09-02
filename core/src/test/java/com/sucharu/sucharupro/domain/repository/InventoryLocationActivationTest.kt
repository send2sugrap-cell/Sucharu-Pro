package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeInventoryLocationDataSource
import com.sucharu.sucharupro.data.repository.InventoryLocationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocation
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocationStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InventoryLocationActivationTest {

    private lateinit var dataSource: FakeInventoryLocationDataSource
    private lateinit var repository: InventoryLocationRepository

    @Before
    fun setup() {
        dataSource = FakeInventoryLocationDataSource()
        repository = InventoryLocationRepositoryImpl(dataSource)
    }

    @Test
    fun `activating an inactive location updates active flow`() = runBlocking {
        val loc = InventoryLocation(
            id = "LOC-ACT-01",
            projectId = "PRJ-01",
            warehouseId = "WH-01",
            code = "BIN-A1",
            name = "Bin A1",
            status = InventoryLocationStatus.INACTIVE,
            createdBy = "admin-01",
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z"
        )
        repository.createLocation(loc, callerRole = UserRole.ADMIN)

        val activeList1 = repository.observeActiveLocations("WH-01", "PRJ-01").first()
        assertEquals(0, activeList1.size)

        val actRes = repository.activateLocation("LOC-ACT-01", "mgr-01", "2026-08-17T09:00:00Z", UserRole.MANAGER)
        assertTrue(actRes is DomainResult.Success)

        val activeList2 = repository.observeActiveLocations("WH-01", "PRJ-01").first()
        assertEquals(1, activeList2.size)
        assertEquals("LOC-ACT-01", activeList2.first().id)
    }
}

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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InventoryLocationDeactivationTest {

    private lateinit var dataSource: FakeInventoryLocationDataSource
    private lateinit var repository: InventoryLocationRepository

    @Before
    fun setup() {
        dataSource = FakeInventoryLocationDataSource()
        repository = InventoryLocationRepositoryImpl(dataSource)
    }

    @Test
    fun `deactivating location removes from active stream but keeps in warehouse stream`() = runBlocking {
        val loc = InventoryLocation(
            id = "LOC-DEACT-01",
            projectId = "PRJ-01",
            warehouseId = "WH-01",
            code = "BIN-A2",
            name = "Bin A2",
            status = InventoryLocationStatus.ACTIVE,
            createdBy = "admin-01",
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z"
        )
        repository.createLocation(loc, callerRole = UserRole.ADMIN)

        val deactRes = repository.deactivateLocation("LOC-DEACT-01", "mgr-01", "2026-08-17T09:00:00Z", UserRole.MANAGER)
        assertTrue(deactRes is DomainResult.Success)
        val updated = (deactRes as DomainResult.Success).data
        assertFalse(updated.status == InventoryLocationStatus.ACTIVE)

        val activeList = repository.observeActiveLocations("WH-01", "PRJ-01").first()
        assertEquals(0, activeList.size)

        val allList = repository.observeLocationsByWarehouse("WH-01", "PRJ-01").first()
        assertEquals(1, allList.size)
    }
}

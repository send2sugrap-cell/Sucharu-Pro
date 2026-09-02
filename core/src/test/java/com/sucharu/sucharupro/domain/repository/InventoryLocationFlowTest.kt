package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeInventoryLocationDataSource
import com.sucharu.sucharupro.data.repository.InventoryLocationRepositoryImpl
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocation
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class InventoryLocationFlowTest {

    private lateinit var dataSource: FakeInventoryLocationDataSource
    private lateinit var repository: InventoryLocationRepository

    @Before
    fun setup() {
        dataSource = FakeInventoryLocationDataSource()
        repository = InventoryLocationRepositoryImpl(dataSource)
    }

    @Test
    fun `location flow updates reactively on inserting child location`() = runBlocking {
        val initial = repository.observeLocations("PRJ-01").first()
        assertEquals(0, initial.size)

        repository.createLocation(
            InventoryLocation(
                id = "LOC-FLW-01",
                projectId = "PRJ-01",
                warehouseId = "WH-01",
                code = "BIN-F01",
                name = "Flow Bin",
                createdBy = "admin-01",
                createdAt = "2026-08-17T08:00:00Z",
                updatedAt = "2026-08-17T08:00:00Z"
            ),
            callerRole = UserRole.ADMIN
        )

        val updated = repository.observeLocations("PRJ-01").first()
        assertEquals(1, updated.size)
        assertEquals("Flow Bin", updated.first().name)
    }
}

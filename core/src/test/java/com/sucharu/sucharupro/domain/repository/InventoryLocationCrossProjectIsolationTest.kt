package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeInventoryLocationDataSource
import com.sucharu.sucharupro.data.repository.InventoryLocationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocation
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InventoryLocationCrossProjectIsolationTest {

    private lateinit var dataSource: FakeInventoryLocationDataSource
    private lateinit var repository: InventoryLocationRepository

    @Before
    fun setup() {
        dataSource = FakeInventoryLocationDataSource()
        repository = InventoryLocationRepositoryImpl(dataSource)
    }

    @Test
    fun `project A locations are never emitted in project B location flows`() = runBlocking {
        repository.createLocation(
            InventoryLocation(
                id = "LOC-PRJ-A",
                projectId = "PRJ-A",
                warehouseId = "WH-01",
                code = "BIN-01",
                name = "Bin A",
                createdBy = "admin-01",
                createdAt = "2026-08-17T08:00:00Z",
                updatedAt = "2026-08-17T08:00:00Z"
            ),
            callerRole = UserRole.ADMIN
        )

        repository.createLocation(
            InventoryLocation(
                id = "LOC-PRJ-B",
                projectId = "PRJ-B",
                warehouseId = "WH-02",
                code = "BIN-01",
                name = "Bin B",
                createdBy = "admin-01",
                createdAt = "2026-08-17T08:00:00Z",
                updatedAt = "2026-08-17T08:00:00Z"
            ),
            callerRole = UserRole.ADMIN
        )

        val prjALocations = repository.observeLocations("PRJ-A").first()
        assertEquals(1, prjALocations.size)
        assertEquals("LOC-PRJ-A", prjALocations.first().id)

        val prjBLocations = repository.observeLocations("PRJ-B").first()
        assertEquals(1, prjBLocations.size)
        assertEquals("LOC-PRJ-B", prjBLocations.first().id)
    }
}

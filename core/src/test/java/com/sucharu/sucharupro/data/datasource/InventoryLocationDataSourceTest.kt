package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InventoryLocationDataSourceTest {

    private lateinit var dataSource: FakeInventoryLocationDataSource

    @Before
    fun setup() {
        dataSource = FakeInventoryLocationDataSource()
    }

    @Test
    fun `data source enforces id uniqueness and warehouse-scoped code uniqueness`() = runBlocking {
        val loc1 = InventoryLocation(
            id = "LOC-01",
            projectId = "PRJ-01",
            warehouseId = "WH-01",
            code = "BIN-01",
            name = "Bin 1",
            createdBy = "admin-01",
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z"
        )
        val locDupCodeSameWh = InventoryLocation(
            id = "LOC-02",
            projectId = "PRJ-01",
            warehouseId = "WH-01",
            code = "BIN-01", // Duplicate code in same warehouse
            name = "Bin 2",
            createdBy = "admin-01",
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z"
        )
        val locDupId = InventoryLocation(
            id = "LOC-01", // Duplicate ID
            projectId = "PRJ-01",
            warehouseId = "WH-01",
            code = "BIN-NEW",
            name = "Bin 3",
            createdBy = "admin-01",
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z"
        )

        assertTrue(dataSource.insertLocation(loc1) is DomainResult.Success)
        assertTrue(dataSource.insertLocation(locDupCodeSameWh) is DomainResult.Error)
        assertTrue(dataSource.insertLocation(locDupId) is DomainResult.Error)

        val list = dataSource.observeLocations().first()
        assertEquals(1, list.size)
    }
}

package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeInventoryLocationDataSource
import com.sucharu.sucharupro.data.repository.InventoryLocationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocation
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InventoryLocationConcurrencyTest {

    private lateinit var dataSource: FakeInventoryLocationDataSource
    private lateinit var repository: InventoryLocationRepository

    @Before
    fun setup() {
        dataSource = FakeInventoryLocationDataSource()
        repository = InventoryLocationRepositoryImpl(dataSource)
    }

    @Test
    fun `concurrent distinct location creations all succeed`() = runBlocking {
        val total = 25
        val results = (1..total).map { index ->
            async(Dispatchers.Default) {
                repository.createLocation(
                    InventoryLocation(
                        id = "LOC-CONC-$index",
                        projectId = "PRJ-CONC",
                        warehouseId = "WH-01",
                        code = "BIN-C-$index",
                        name = "Bin $index",
                        createdBy = "admin-01",
                        createdAt = "2026-08-17T08:00:00Z",
                        updatedAt = "2026-08-17T08:00:00Z"
                    ),
                    callerRole = UserRole.ADMIN
                )
            }
        }.awaitAll()

        assertEquals(total, results.size)
        assertTrue(results.all { it is DomainResult.Success })
    }

    @Test
    fun `concurrent duplicate location code race allows exactly one creation in warehouse`() = runBlocking {
        val total = 20
        val results = (1..total).map { index ->
            async(Dispatchers.Default) {
                repository.createLocation(
                    InventoryLocation(
                        id = "LOC-RACE-$index",
                        projectId = "PRJ-RACE",
                        warehouseId = "WH-01",
                        code = "SAME-BIN-CODE",
                        name = "Race Bin $index",
                        createdBy = "admin-01",
                        createdAt = "2026-08-17T08:00:00Z",
                        updatedAt = "2026-08-17T08:00:00Z"
                    ),
                    callerRole = UserRole.ADMIN
                )
            }
        }.awaitAll()

        val successCount = results.count { it is DomainResult.Success }
        val errorCount = results.count { it is DomainResult.Error }

        assertEquals(1, successCount)
        assertEquals(total - 1, errorCount)
    }
}

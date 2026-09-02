package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeInventoryWarehouseDataSource
import com.sucharu.sucharupro.data.repository.InventoryWarehouseRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouse
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InventoryWarehouseConcurrencyTest {

    private lateinit var dataSource: FakeInventoryWarehouseDataSource
    private lateinit var repository: InventoryWarehouseRepository

    @Before
    fun setup() {
        dataSource = FakeInventoryWarehouseDataSource()
        repository = InventoryWarehouseRepositoryImpl(dataSource)
    }

    @Test
    fun `concurrent warehouse creations with distinct codes all succeed`() = runBlocking {
        val total = 25
        val results = (1..total).map { index ->
            async(Dispatchers.Default) {
                repository.createWarehouse(
                    InventoryWarehouse(
                        id = "WH-CONC-$index",
                        projectId = "PRJ-CONC",
                        code = "WH-C-$index",
                        name = "Warehouse $index",
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
    fun `concurrent duplicate warehouse code race allows exactly one creation`() = runBlocking {
        val total = 20
        val results = (1..total).map { index ->
            async(Dispatchers.Default) {
                repository.createWarehouse(
                    InventoryWarehouse(
                        id = "WH-RACE-$index",
                        projectId = "PRJ-RACE",
                        code = "SAME-WH-CODE",
                        name = "Race Warehouse $index",
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

package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeInventoryProductDataSource
import com.sucharu.sucharupro.data.repository.InventoryProductRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryProduct
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InventoryProductConcurrencyTest {

    private lateinit var dataSource: FakeInventoryProductDataSource
    private lateinit var repository: InventoryProductRepository

    @Before
    fun setup() {
        dataSource = FakeInventoryProductDataSource()
        repository = InventoryProductRepositoryImpl(dataSource)
    }

    @Test
    fun `concurrent creation of products with distinct SKUs succeeds`() = runBlocking {
        val total = 30
        val results = (1..total).map { index ->
            async(Dispatchers.Default) {
                repository.createProduct(
                    InventoryProduct(
                        id = "PRD-CONC-$index",
                        sku = "SKU-CONC-$index",
                        name = "Product $index",
                        createdAt = "2026-08-17T08:00:00Z",
                        updatedAt = "2026-08-17T08:00:00Z",
                        createdBy = "admin-01"
                    ),
                    callerRole = UserRole.ADMIN
                )
            }
        }.awaitAll()

        assertEquals(total, results.size)
        assertTrue(results.all { it is DomainResult.Success })
    }

    @Test
    fun `concurrent creation race on same duplicate SKU allows exactly one success`() = runBlocking {
        val attempts = 20
        val results = (1..attempts).map { index ->
            async(Dispatchers.Default) {
                repository.createProduct(
                    InventoryProduct(
                        id = "PRD-DUP-$index",
                        sku = "SAME-SKU-RACE",
                        name = "Race Product $index",
                        createdAt = "2026-08-17T08:00:00Z",
                        updatedAt = "2026-08-17T08:00:00Z",
                        createdBy = "admin-01"
                    ),
                    callerRole = UserRole.ADMIN
                )
            }
        }.awaitAll()

        val successCount = results.count { it is DomainResult.Success }
        val failureCount = results.count { it is DomainResult.Error }

        assertEquals(1, successCount)
        assertEquals(attempts - 1, failureCount)
    }
}

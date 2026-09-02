package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryProduct
import com.sucharu.sucharupro.domain.model.inventory.InventoryProductCategory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InventoryProductDataSourceTest {

    private lateinit var dataSource: FakeInventoryProductDataSource

    @Before
    fun setup() {
        dataSource = FakeInventoryProductDataSource()
    }

    @Test
    fun `data source enforces unique product id and unique sku`() = runBlocking {
        val prod1 = InventoryProduct(
            id = "PRD-01",
            sku = "SKU-001",
            name = "Item 1",
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z",
            createdBy = "admin-01"
        )
        val prod2 = InventoryProduct(
            id = "PRD-02",
            sku = "SKU-001", // Duplicate SKU
            name = "Item 2",
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z",
            createdBy = "admin-01"
        )
        val prod3 = InventoryProduct(
            id = "PRD-01", // Duplicate ID
            sku = "SKU-002",
            name = "Item 3",
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z",
            createdBy = "admin-01"
        )

        assertTrue(dataSource.insertProduct(prod1) is DomainResult.Success)
        assertTrue(dataSource.insertProduct(prod2) is DomainResult.Error)
        assertTrue(dataSource.insertProduct(prod3) is DomainResult.Error)

        val products = dataSource.observeProducts().first()
        assertEquals(1, products.size)
    }
}

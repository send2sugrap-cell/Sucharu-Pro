package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeInventoryProductDataSource
import com.sucharu.sucharupro.data.repository.InventoryProductRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryProduct
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InventoryProductDuplicatePreventionTest {

    private lateinit var dataSource: FakeInventoryProductDataSource
    private lateinit var repository: InventoryProductRepository

    @Before
    fun setup() {
        dataSource = FakeInventoryProductDataSource()
        repository = InventoryProductRepositoryImpl(dataSource)
    }

    @Test
    fun `cannot create two products with duplicate ID or duplicate SKU`() = runBlocking {
        val prod1 = InventoryProduct(
            id = "PRD-ORIGINAL",
            sku = "ORIGINAL-SKU",
            name = "Original Product",
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z",
            createdBy = "admin-01"
        )
        assertTrue(repository.createProduct(prod1, callerRole = UserRole.ADMIN) is DomainResult.Success)

        // Duplicate SKU with different ID
        val dupSku = InventoryProduct(
            id = "PRD-NEW",
            sku = "original-sku",
            name = "New Product",
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z",
            createdBy = "admin-01"
        )
        val dupSkuRes = repository.createProduct(dupSku, callerRole = UserRole.ADMIN)
        assertTrue(dupSkuRes is DomainResult.Error)

        // Duplicate ID with different SKU
        val dupId = InventoryProduct(
            id = "PRD-ORIGINAL",
            sku = "ANOTHER-SKU",
            name = "Another Product",
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z",
            createdBy = "admin-01"
        )
        val dupIdRes = repository.createProduct(dupId, callerRole = UserRole.ADMIN)
        assertTrue(dupIdRes is DomainResult.Error)
    }
}

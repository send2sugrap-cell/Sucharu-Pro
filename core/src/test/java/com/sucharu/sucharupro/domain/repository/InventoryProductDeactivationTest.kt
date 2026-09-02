package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeInventoryProductDataSource
import com.sucharu.sucharupro.data.repository.InventoryProductRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryProduct
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InventoryProductDeactivationTest {

    private lateinit var dataSource: FakeInventoryProductDataSource
    private lateinit var repository: InventoryProductRepository

    @Before
    fun setup() {
        dataSource = FakeInventoryProductDataSource()
        repository = InventoryProductRepositoryImpl(dataSource)
    }

    @Test
    fun `deactivating an active product retains product in all list while removing from active list`() = runBlocking {
        val product = InventoryProduct(
            id = "PRD-01",
            sku = "AMP-002",
            name = "Ampara Para 30 Color",
            isActive = true,
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z",
            createdBy = "admin-01"
        )
        repository.createProduct(product, callerRole = UserRole.ADMIN)

        val deactRes = repository.deactivateProduct("PRD-01", "mgr-01", "2026-08-17T09:00:00Z", UserRole.MANAGER)
        assertTrue(deactRes is DomainResult.Success)
        val updated = (deactRes as DomainResult.Success).data
        assertFalse(updated.isActive)

        // All products still contains it for historical reference
        val allProducts = repository.observeProducts().first()
        assertEquals(1, allProducts.size)
        assertEquals("PRD-01", allProducts.first().id)

        // Active products is empty
        val activeProducts = repository.observeActiveProducts().first()
        assertEquals(0, activeProducts.size)
    }
}

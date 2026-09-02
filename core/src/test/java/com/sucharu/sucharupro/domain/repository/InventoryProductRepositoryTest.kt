package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeInventoryProductDataSource
import com.sucharu.sucharupro.data.repository.InventoryProductRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryProduct
import com.sucharu.sucharupro.domain.model.inventory.InventoryProductCategory
import com.sucharu.sucharupro.domain.model.inventory.InventoryProductType
import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InventoryProductRepositoryTest {

    private lateinit var dataSource: FakeInventoryProductDataSource
    private lateinit var repository: InventoryProductRepository

    @Before
    fun setup() {
        dataSource = FakeInventoryProductDataSource()
        repository = InventoryProductRepositoryImpl(dataSource)
    }

    @Test
    fun `full lifecycle of product and category management through repository`() = runBlocking {
        // 1. Create Category
        val cat = InventoryProductCategory(
            id = "CAT-01",
            name = "Corporate Gifts",
            description = "Branded items",
            isActive = true,
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z"
        )
        val catRes = repository.createCategory(cat, callerRole = UserRole.ADMIN)
        assertTrue(catRes is DomainResult.Success)

        // 2. Create Product
        val prod = InventoryProduct(
            id = "PRD-01",
            sku = "GFT-PEN-001",
            name = "Executive Metal Pen",
            description = "Laser engraved pen with gift box",
            categoryId = "CAT-01",
            productType = InventoryProductType.GIFT_PRODUCT,
            unitOfMeasure = InventoryUnit.SET,
            isStockTracked = true,
            isFinishedProduct = true,
            isSaleable = true,
            isActive = true,
            createdAt = "2026-08-17T08:30:00Z",
            updatedAt = "2026-08-17T08:30:00Z",
            createdBy = "admin-01"
        )
        val prodRes = repository.createProduct(prod, callerRole = UserRole.ADMIN)
        assertTrue(prodRes is DomainResult.Success)

        // 3. Query by SKU
        val skuRes = repository.getProductBySku("GFT-PEN-001", callerRole = UserRole.MANAGER)
        assertTrue(skuRes is DomainResult.Success)
        assertEquals("PRD-01", (skuRes as DomainResult.Success).data.id)

        // 4. Update Metadata
        val updateRes = repository.updateProductMetadata(
            productId = "PRD-01",
            name = "Executive Metal Pen (Matte Black)",
            description = "Laser engraved pen with luxury box",
            categoryId = "CAT-01",
            productType = InventoryProductType.GIFT_PRODUCT,
            unitOfMeasure = InventoryUnit.SET,
            isStockTracked = true,
            isFinishedProduct = true,
            isSaleable = true,
            updatedBy = "mgr-01",
            timestamp = "2026-08-17T09:00:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(updateRes is DomainResult.Success)
        assertEquals("Executive Metal Pen (Matte Black)", (updateRes as DomainResult.Success).data.name)

        // 5. Verify flow emission
        val list = repository.observeProducts().first()
        assertEquals(1, list.size)
        assertEquals("Executive Metal Pen (Matte Black)", list.first().name)
    }
}

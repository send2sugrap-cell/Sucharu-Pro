package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.FakeInventoryProductDataSource
import com.sucharu.sucharupro.data.repository.InventoryProductRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryActivityType
import com.sucharu.sucharupro.domain.model.inventory.InventoryProduct
import com.sucharu.sucharupro.domain.model.inventory.InventoryProductCategory
import com.sucharu.sucharupro.domain.model.inventory.InventoryProductType
import com.sucharu.sucharupro.domain.model.inventory.InventoryStockIdentity
import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.InventoryProductRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InventoryProductEndToEndTest {

    private lateinit var dataSource: FakeInventoryProductDataSource
    private lateinit var repository: InventoryProductRepository

    @Before
    fun setup() {
        dataSource = FakeInventoryProductDataSource()
        repository = InventoryProductRepositoryImpl(dataSource)
    }

    @Test
    fun `full end-to-end inventory domain foundation master workflow`() = runBlocking {
        // ==========================================
        // 1. Create Category
        // ==========================================
        val category = InventoryProductCategory(
            id = "CAT-ISLAMIC-BOOKS",
            name = "Islamic Books & Quran",
            description = "Holy Quran, Qaida, and Islamic texts",
            isActive = true,
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z"
        )
        val catRes = repository.createCategory(category, callerRole = UserRole.ADMIN)
        assertTrue(catRes is DomainResult.Success)

        // ==========================================
        // 2. Create Multiple Products (Quran Sharif, Qaida, Gift)
        // ==========================================
        val quran = InventoryProduct(
            id = "PRD-QURAN-01",
            sku = "QUR-HAF-15L",
            name = "Quran Sharif Hafezi 15 Lines",
            description = "Standard 15 lines hardbound Hafezi Quran",
            categoryId = "CAT-ISLAMIC-BOOKS",
            productType = InventoryProductType.BOOK,
            unitOfMeasure = InventoryUnit.PCS,
            isStockTracked = true,
            isFinishedProduct = true,
            isSaleable = true,
            isActive = true,
            createdAt = "2026-08-17T08:30:00Z",
            updatedAt = "2026-08-17T08:30:00Z",
            createdBy = "admin-01"
        )
        val quranRes = repository.createProduct(quran, callerRole = UserRole.ADMIN)
        assertTrue(quranRes is DomainResult.Success)

        val qaida = InventoryProduct(
            id = "PRD-QAIDA-01",
            sku = "QAI-BAG-001",
            name = "Baghdadi Qaida Color Edition",
            description = "4-color learning qaida",
            categoryId = "CAT-ISLAMIC-BOOKS",
            productType = InventoryProductType.BOOK,
            unitOfMeasure = InventoryUnit.PCS,
            isStockTracked = true,
            isFinishedProduct = true,
            isSaleable = true,
            isActive = true,
            createdAt = "2026-08-17T08:35:00Z",
            updatedAt = "2026-08-17T08:35:00Z",
            createdBy = "admin-01"
        )
        val qaidaRes = repository.createProduct(qaida, callerRole = UserRole.MANAGER)
        assertTrue(qaidaRes is DomainResult.Success)

        // ==========================================
        // 3. Query by SKU and ID
        // ==========================================
        val quranBySku = repository.getProductBySku("qur-haf-15l", callerRole = UserRole.WAREHOUSE)
        assertTrue(quranBySku is DomainResult.Success)
        assertEquals("PRD-QURAN-01", (quranBySku as DomainResult.Success).data.id)

        val qaidaById = repository.getProductById("PRD-QAIDA-01", callerRole = UserRole.STAFF)
        assertTrue(qaidaById is DomainResult.Success)
        assertEquals("QAI-BAG-001", (qaidaById as DomainResult.Success).data.sku)

        // ==========================================
        // 4. Update Product Metadata
        // ==========================================
        val updateRes = repository.updateProductMetadata(
            productId = "PRD-QURAN-01",
            name = "Quran Sharif Hafezi 15 Lines (Gold Embossed)",
            description = "Luxury gold foiled cover with silk marker",
            categoryId = "CAT-ISLAMIC-BOOKS",
            productType = InventoryProductType.BOOK,
            unitOfMeasure = InventoryUnit.PCS,
            isStockTracked = true,
            isFinishedProduct = true,
            isSaleable = true,
            updatedBy = "mgr-01",
            timestamp = "2026-08-17T09:00:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(updateRes is DomainResult.Success)
        assertEquals("Quran Sharif Hafezi 15 Lines (Gold Embossed)", (updateRes as DomainResult.Success).data.name)

        // ==========================================
        // 5. Stock Tracking Identity
        // ==========================================
        val stockIdentity = InventoryStockIdentity.fromProduct((updateRes as DomainResult.Success).data)
        assertEquals("PRD-QURAN-01", stockIdentity.productId)
        assertEquals("QUR-HAF-15L", stockIdentity.sku)
        assertEquals(InventoryUnit.PCS, stockIdentity.unit)
        assertTrue(stockIdentity.stockTracked)

        // ==========================================
        // 6. Deactivation & Active Product Flow Filter
        // ==========================================
        val deactRes = repository.deactivateProduct("PRD-QAIDA-01", "mgr-01", "2026-08-17T09:30:00Z", UserRole.MANAGER)
        assertTrue(deactRes is DomainResult.Success)
        assertFalse((deactRes as DomainResult.Success).data.isActive)

        val activeProducts = repository.observeActiveProducts().first()
        assertEquals(1, activeProducts.size)
        assertEquals("PRD-QURAN-01", activeProducts.first().id)

        val allProducts = repository.observeProducts().first()
        assertEquals(2, allProducts.size)

        // Reactivate Qaida
        val reactRes = repository.activateProduct("PRD-QAIDA-01", "mgr-01", "2026-08-17T10:00:00Z", UserRole.MANAGER)
        assertTrue(reactRes is DomainResult.Success)
        assertTrue((reactRes as DomainResult.Success).data.isActive)

        val restoredActive = repository.observeActiveProducts().first()
        assertEquals(2, restoredActive.size)

        // ==========================================
        // 7. Audit Trail Verification
        // ==========================================
        val events = repository.observeActivityEvents().first()
        assertEquals(6, events.size)

        assertEquals(InventoryActivityType.PRODUCT_ACTIVATED, events[0].eventType)
        assertEquals(InventoryActivityType.PRODUCT_DEACTIVATED, events[1].eventType)
        assertEquals(InventoryActivityType.PRODUCT_UPDATED, events[2].eventType)
        assertEquals(InventoryActivityType.PRODUCT_CREATED, events[3].eventType)
        assertEquals(InventoryActivityType.PRODUCT_CREATED, events[4].eventType)
        assertEquals(InventoryActivityType.CATEGORY_CREATED, events[5].eventType)
    }
}

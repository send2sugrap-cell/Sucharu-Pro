package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeInventoryProductDataSource
import com.sucharu.sucharupro.data.repository.InventoryProductRepositoryImpl
import com.sucharu.sucharupro.domain.model.inventory.InventoryActivityType
import com.sucharu.sucharupro.domain.model.inventory.InventoryProduct
import com.sucharu.sucharupro.domain.model.inventory.InventoryProductCategory
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class InventoryProductAuditTest {

    private lateinit var dataSource: FakeInventoryProductDataSource
    private lateinit var repository: InventoryProductRepository

    @Before
    fun setup() {
        dataSource = FakeInventoryProductDataSource()
        repository = InventoryProductRepositoryImpl(dataSource)
    }

    @Test
    fun `product and category operations append immutable audit records`() = runBlocking {
        // 1. Create Category
        repository.createCategory(
            InventoryProductCategory(
                id = "CAT-AUDIT",
                name = "Audit Category",
                createdAt = "2026-08-17T08:00:00Z",
                updatedAt = "2026-08-17T08:00:00Z"
            ),
            callerRole = UserRole.ADMIN
        )

        // 2. Create Product
        repository.createProduct(
            InventoryProduct(
                id = "PRD-AUDIT",
                sku = "AUD-001",
                name = "Audit Product",
                categoryId = "CAT-AUDIT",
                createdAt = "2026-08-17T08:30:00Z",
                updatedAt = "2026-08-17T08:30:00Z",
                createdBy = "admin-01"
            ),
            callerRole = UserRole.ADMIN
        )

        // 3. Deactivate Product
        repository.deactivateProduct("PRD-AUDIT", "mgr-01", "2026-08-17T09:00:00Z", UserRole.MANAGER)

        // 4. Activate Product
        repository.activateProduct("PRD-AUDIT", "mgr-01", "2026-08-17T09:30:00Z", UserRole.MANAGER)

        val events = repository.observeActivityEvents().first()
        assertEquals(4, events.size)

        assertEquals(InventoryActivityType.PRODUCT_ACTIVATED, events[0].eventType)
        assertEquals(InventoryActivityType.PRODUCT_DEACTIVATED, events[1].eventType)
        assertEquals(InventoryActivityType.PRODUCT_CREATED, events[2].eventType)
        assertEquals(InventoryActivityType.CATEGORY_CREATED, events[3].eventType)
    }
}

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

class InventoryProductActivationTest {

    private lateinit var dataSource: FakeInventoryProductDataSource
    private lateinit var repository: InventoryProductRepository

    @Before
    fun setup() {
        dataSource = FakeInventoryProductDataSource()
        repository = InventoryProductRepositoryImpl(dataSource)
    }

    @Test
    fun `activating an inactive product updates status and active flow emission`() = runBlocking {
        val product = InventoryProduct(
            id = "PRD-01",
            sku = "AMP-001",
            name = "Ampara Para 30",
            isActive = false,
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z",
            createdBy = "admin-01"
        )
        repository.createProduct(product, callerRole = UserRole.ADMIN)

        val initialActive = repository.observeActiveProducts().first()
        assertEquals(0, initialActive.size)

        val activateRes = repository.activateProduct("PRD-01", "mgr-01", "2026-08-17T09:00:00Z", UserRole.MANAGER)
        assertTrue(activateRes is DomainResult.Success)
        val updated = (activateRes as DomainResult.Success).data
        assertTrue(updated.isActive)

        val newActive = repository.observeActiveProducts().first()
        assertEquals(1, newActive.size)
        assertEquals("PRD-01", newActive.first().id)
    }
}

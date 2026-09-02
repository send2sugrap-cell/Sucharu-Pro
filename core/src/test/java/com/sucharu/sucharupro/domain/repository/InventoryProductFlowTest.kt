package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeInventoryProductDataSource
import com.sucharu.sucharupro.data.repository.InventoryProductRepositoryImpl
import com.sucharu.sucharupro.domain.model.inventory.InventoryProduct
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class InventoryProductFlowTest {

    private lateinit var dataSource: FakeInventoryProductDataSource
    private lateinit var repository: InventoryProductRepository

    @Before
    fun setup() {
        dataSource = FakeInventoryProductDataSource()
        repository = InventoryProductRepositoryImpl(dataSource)
    }

    @Test
    fun `reactive flows emit updated state upon product creation and metadata updates`() = runBlocking {
        val stream1 = repository.observeProducts().first()
        assertEquals(0, stream1.size)

        repository.createProduct(
            InventoryProduct(
                id = "PRD-FLOW",
                sku = "FLW-001",
                name = "Flow Product",
                createdAt = "2026-08-17T08:00:00Z",
                updatedAt = "2026-08-17T08:00:00Z",
                createdBy = "admin-01"
            ),
            callerRole = UserRole.ADMIN
        )

        val stream2 = repository.observeProducts().first()
        assertEquals(1, stream2.size)
        assertEquals("Flow Product", stream2.first().name)
    }
}

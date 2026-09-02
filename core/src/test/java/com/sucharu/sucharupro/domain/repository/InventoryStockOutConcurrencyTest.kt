package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeInventoryLocationDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryProductDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryReceivingDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryStockOutDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryWarehouseDataSource
import com.sucharu.sucharupro.data.repository.InventoryStockOutRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocation
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocationType
import com.sucharu.sucharupro.domain.model.inventory.InventoryProduct
import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouse
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouseType
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryStockInRecord
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOut
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOutLine
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Concurrency tests for [InventoryStockOutRepository] (Module 07 Step 04).
 * Ensures that simultaneous issue requests against limited stock are handled correctly.
 */
class InventoryStockOutConcurrencyTest {

    private lateinit var stockOutDataSource: FakeInventoryStockOutDataSource
    private lateinit var receivingDataSource: FakeInventoryReceivingDataSource
    private lateinit var repository: InventoryStockOutRepository

    @Before
    fun setup() {
        runBlocking {
            stockOutDataSource = FakeInventoryStockOutDataSource()
            receivingDataSource = FakeInventoryReceivingDataSource()
            val productDataSource = FakeInventoryProductDataSource()
            val warehouseDataSource = FakeInventoryWarehouseDataSource()
            val locationDataSource = FakeInventoryLocationDataSource()
            repository = InventoryStockOutRepositoryImpl(
                stockOutDataSource = stockOutDataSource,
                receivingDataSource = receivingDataSource,
                productDataSource = productDataSource,
                warehouseDataSource = warehouseDataSource,
                locationDataSource = locationDataSource
            )
            // Seed infrastructure
            warehouseDataSource.insertWarehouse(buildWarehouse())
            locationDataSource.insertLocation(buildLocation())
            productDataSource.insertProduct(buildProduct())
            
            // Seed stock: 10 units
            receivingDataSource.insertStockInRecord(buildStockIn(10))
        }
    }

    @Test
    fun `simultaneous completions against limited stock only allows subset to succeed`() = runBlocking {
        // Create 3 stock outs, each wanting 5 units (Total 15, but only 10 available)
        val soIds = listOf("SO-1", "SO-2", "SO-3")
        for (id in soIds) {
            repository.createStockOut(buildStockOut(id, "REF-$id"), UserRole.MANAGER)
            repository.addStockOutLine(buildLine("LINE-$id", id, 5), UserRole.MANAGER)
            repository.submitStockOut(id, "admin", "2026-08-17T11:00:00Z", UserRole.MANAGER)
            repository.approveStockOut(id, "admin", "2026-08-17T12:00:00Z", UserRole.MANAGER)
        }

        // Run completions in parallel
        val deferred = soIds.map { id ->
            async {
                repository.completeStockOut(id, "admin", "2026-08-17T13:00:00Z", UserRole.MANAGER)
            }
        }
        val results = deferred.awaitAll()

        val successes = results.count { it is DomainResult.Success }
        val failures = results.count { it is DomainResult.Error }

        // Exactly 2 should succeed (5 + 5 = 10)
        assertEquals("Exactly 2 completions should succeed", 2, successes)
        assertEquals("Exactly 1 completion should fail due to insufficient stock", 1, failures)
        
        // Verify remaining available stock is 0
        val available = repository.getAvailableQuantity("PRJ-01", "WH-01", "LOC-01", "PROD-01")
        assertEquals(0, available)
    }

    // Helpers
    private fun buildStockOut(id: String, ref: String) = InventoryStockOut(
        stockOutId = id, projectId = "PRJ-01", stockOutReference = ref,
        warehouseId = "WH-01", stockOutDate = "2026-08-17", createdBy = "admin",
        createdAt = "2026-08-17T10:00:00Z", updatedAt = "2026-08-17T10:00:00Z"
    )

    private fun buildLine(id: String, soId: String, expectedQuantity: Int) = InventoryStockOutLine(
        stockOutLineId = id, stockOutId = soId, projectId = "PRJ-01",
        inventoryProductId = "PROD-01", warehouseId = "WH-01", locationId = "LOC-01",
        expectedQuantity = expectedQuantity, createdAt = "2026-08-17T10:00:00Z", updatedAt = "2026-08-17T10:00:00Z"
    )

    private fun buildWarehouse() = InventoryWarehouse(
        id = "WH-01", projectId = "PRJ-01", code = "WH01", name = "Warehouse 01",
        type = InventoryWarehouseType.FINISHED_GOODS, createdBy = "admin",
        createdAt = "2026-08-17T08:00:00Z", updatedAt = "2026-08-17T08:00:00Z"
    )

    private fun buildLocation() = InventoryLocation(
        id = "LOC-01", projectId = "PRJ-01", warehouseId = "WH-01", code = "LOC01",
        name = "Location 01", type = InventoryLocationType.SHELF, createdBy = "admin",
        createdAt = "2026-08-17T08:00:00Z", updatedAt = "2026-08-17T08:00:00Z"
    )

    private fun buildProduct() = InventoryProduct(
        id = "PROD-01", sku = "SKU01", name = "Product 01", isActive = true, isStockTracked = true,
        createdBy = "admin", createdAt = "2026-08-17T08:00:00Z", updatedAt = "2026-08-17T08:00:00Z"
    )

    private fun buildStockIn(quantity: Int) = InventoryStockInRecord(
        stockInId = UUID.randomUUID().toString(), receivingId = "RCV-01", receivingLineId = "RCV-LINE-01",
        projectId = "PRJ-01", inventoryProductId = "PROD-01", warehouseId = "WH-01", locationId = "LOC-01",
        quantity = quantity, unit = InventoryUnit.PCS, createdBy = "admin", createdAt = "2026-08-17T09:00:00Z"
    )
}

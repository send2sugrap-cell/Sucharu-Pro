package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.repository.InventoryStockAdjustmentRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.*
import com.sucharu.sucharupro.domain.model.inventory.adjustment.*
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryStockInRecord
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Concurrency tests for [InventoryStockAdjustmentRepository] (Module 07 Step 06).
 * Ensures atomic execution and concurrent DECREASE prevention against limited stock.
 */
class InventoryStockAdjustmentConcurrencyTest {

    private lateinit var receivingDataSource: FakeInventoryReceivingDataSource
    private lateinit var repository: InventoryStockAdjustmentRepository

    @Before
    fun setup() {
        runBlocking {
            val adjustmentDataSource = FakeInventoryStockAdjustmentDataSource()
            val stockOutDataSource = FakeInventoryStockOutDataSource()
            val transferDataSource = FakeInventoryStockTransferDataSource()
            receivingDataSource = FakeInventoryReceivingDataSource()
            val productDataSource = FakeInventoryProductDataSource()
            val warehouseDataSource = FakeInventoryWarehouseDataSource()
            val locationDataSource = FakeInventoryLocationDataSource()
            
            repository = InventoryStockAdjustmentRepositoryImpl(
                adjustmentDataSource = adjustmentDataSource,
                stockOutDataSource = stockOutDataSource,
                transferDataSource = transferDataSource,
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
    fun `simultaneous DECREASE adjustments against limited stock only allows subset to succeed`() = runBlocking {
        // Create 3 adjustments, each wanting to DECREASE by 5 units (Total 15, but only 10 available)
        val adjIds = listOf("ADJ-1", "ADJ-2", "ADJ-3")
        for (id in adjIds) {
            repository.createStockAdjustment(buildAdjustment(id, "REF-$id"), UserRole.MANAGER)
            repository.addStockAdjustmentLine(buildLine("LINE-$id", id, InventoryAdjustmentType.DECREASE, 10, 5), UserRole.MANAGER)
            repository.submitStockAdjustment(id, "admin", "2026-08-17T11:00:00Z", UserRole.MANAGER)
            repository.approveStockAdjustment(id, "admin", "2026-08-17T12:00:00Z", UserRole.MANAGER)
            repository.startStockAdjustment(id, "admin", "2026-08-17T12:30:00Z", UserRole.WAREHOUSE)
        }

        // Run completions in parallel
        val deferred = adjIds.map { id ->
            async {
                repository.completeStockAdjustment(id, "admin", "2026-08-17T13:00:00Z", UserRole.MANAGER)
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
    private fun buildAdjustment(id: String, ref: String) = InventoryStockAdjustment(
        adjustmentId = id, projectId = "PRJ-01", adjustmentReference = ref,
        warehouseId = "WH-01", adjustmentDate = "2026-08-17", createdBy = "admin",
        createdAt = "2026-08-17T10:00:00Z", updatedAt = "2026-08-17T10:00:00Z"
    )

    private fun buildLine(id: String, adjId: String, type: InventoryAdjustmentType, current: Int, adjusted: Int) = InventoryStockAdjustmentLine(
        adjustmentLineId = id, adjustmentId = adjId, projectId = "PRJ-01",
        inventoryProductId = "PROD-01", warehouseId = "WH-01", locationId = "LOC-01",
        adjustmentType = type, adjustmentReason = InventoryAdjustmentReason.PHYSICAL_COUNT,
        currentQuantity = current, adjustedQuantity = adjusted, quantityChange = adjusted - current,
        createdAt = "2026-08-17T10:00:00Z", updatedAt = "2026-08-17T10:00:00Z"
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

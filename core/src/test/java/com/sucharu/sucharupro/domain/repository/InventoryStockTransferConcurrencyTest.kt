package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeInventoryLocationDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryProductDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryReceivingDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryStockOutDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryStockTransferDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryWarehouseDataSource
import com.sucharu.sucharupro.data.repository.InventoryStockTransferRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocation
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocationType
import com.sucharu.sucharupro.domain.model.inventory.InventoryProduct
import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouse
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouseType
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryStockInRecord
import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransfer
import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransferLine
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Concurrency tests for [InventoryStockTransferRepository] (Module 07 Step 05).
 * Ensures that simultaneous completion requests against limited stock are handled correctly.
 */
class InventoryStockTransferConcurrencyTest {

    private lateinit var transferDataSource: FakeInventoryStockTransferDataSource
    private lateinit var receivingDataSource: FakeInventoryReceivingDataSource
    private lateinit var repository: InventoryStockTransferRepository

    @Before
    fun setup() {
        runBlocking {
            transferDataSource = FakeInventoryStockTransferDataSource()
            receivingDataSource = FakeInventoryReceivingDataSource()
            val stockOutDataSource = FakeInventoryStockOutDataSource()
            val productDataSource = FakeInventoryProductDataSource()
            val warehouseDataSource = FakeInventoryWarehouseDataSource()
            val locationDataSource = FakeInventoryLocationDataSource()
            repository = InventoryStockTransferRepositoryImpl(
                transferDataSource = transferDataSource,
                stockOutDataSource = stockOutDataSource,
                receivingDataSource = receivingDataSource,
                productDataSource = productDataSource,
                warehouseDataSource = warehouseDataSource,
                locationDataSource = locationDataSource
            )
            // Seed infrastructure
            warehouseDataSource.insertWarehouse(buildWarehouse("WH-SRC", "WHSRC"))
            warehouseDataSource.insertWarehouse(buildWarehouse("WH-DEST", "WHDEST"))
            locationDataSource.insertLocation(buildLocation("LOC-SRC", "WH-SRC", "LOCSRC"))
            locationDataSource.insertLocation(buildLocation("LOC-DEST", "WH-DEST", "LOCDEST"))
            productDataSource.insertProduct(buildProduct())
            
            // Seed stock: 10 units at source
            receivingDataSource.insertStockInRecord(buildStockIn(10, "WH-SRC", "LOC-SRC"))
        }
    }

    @Test
    fun `simultaneous completions against limited stock only allows subset to succeed`() = runBlocking {
        // Create 3 transfers, each wanting 5 units (Total 15, but only 10 available)
        val stIds = listOf("ST-1", "ST-2", "ST-3")
        for (id in stIds) {
            repository.createStockTransfer(buildTransfer(id, "REF-$id"), UserRole.MANAGER)
            repository.addStockTransferLine(buildLine("LINE-$id", id, 5), UserRole.MANAGER)
            repository.submitStockTransfer(id, "admin", "2026-08-17T11:00:00Z", UserRole.MANAGER)
            repository.approveStockTransfer(id, "admin", "2026-08-17T12:00:00Z", UserRole.MANAGER)
            repository.startStockTransfer(id, "admin", "2026-08-17T12:30:00Z", UserRole.WAREHOUSE)
        }

        // Run completions in parallel
        val deferred = stIds.map { id ->
            async {
                repository.completeStockTransfer(id, "admin", "2026-08-17T13:00:00Z", UserRole.MANAGER)
            }
        }
        val results = deferred.awaitAll()

        val successes = results.count { it is DomainResult.Success }
        val failures = results.count { it is DomainResult.Error }

        // Exactly 2 should succeed (5 + 5 = 10)
        assertEquals("Exactly 2 completions should succeed", 2, successes)
        assertEquals("Exactly 1 completion should fail due to insufficient stock", 1, failures)
        
        // Verify remaining available stock at source is 0
        val availableAtSrc = repository.getAvailableQuantity("PRJ-01", "WH-SRC", "LOC-SRC", "PROD-01")
        assertEquals(0, availableAtSrc)

        // Verify destination has 10
        val availableAtDest = repository.getAvailableQuantity("PRJ-01", "WH-DEST", "LOC-DEST", "PROD-01")
        assertEquals(10, availableAtDest)
    }

    // Helpers
    private fun buildTransfer(id: String, ref: String) = InventoryStockTransfer(
        transferId = id, projectId = "PRJ-01", transferReference = ref,
        fromWarehouseId = "WH-SRC", toWarehouseId = "WH-DEST", transferDate = "2026-08-17",
        createdBy = "admin", createdAt = "2026-08-17T10:00:00Z", updatedAt = "2026-08-17T10:00:00Z"
    )

    private fun buildLine(id: String, stId: String, expectedQuantity: Int) = InventoryStockTransferLine(
        transferLineId = id, transferId = stId, projectId = "PRJ-01",
        inventoryProductId = "PROD-01", fromWarehouseId = "WH-SRC", fromLocationId = "LOC-SRC",
        toWarehouseId = "WH-DEST", toLocationId = "LOC-DEST",
        expectedQuantity = expectedQuantity, createdAt = "2026-08-17T10:00:00Z", updatedAt = "2026-08-17T10:00:00Z"
    )

    private fun buildWarehouse(id: String, code: String) = InventoryWarehouse(
        id = id, projectId = "PRJ-01", code = code, name = "Warehouse $code",
        type = InventoryWarehouseType.FINISHED_GOODS, createdBy = "admin",
        createdAt = "2026-08-17T08:00:00Z", updatedAt = "2026-08-17T08:00:00Z"
    )

    private fun buildLocation(id: String, whId: String, code: String) = InventoryLocation(
        id = id, projectId = "PRJ-01", warehouseId = whId, code = code,
        name = "Location $code", type = InventoryLocationType.SHELF, createdBy = "admin",
        createdAt = "2026-08-17T08:00:00Z", updatedAt = "2026-08-17T08:00:00Z"
    )

    private fun buildProduct() = InventoryProduct(
        id = "PROD-01", sku = "SKU01", name = "Product 01", isActive = true, isStockTracked = true,
        createdBy = "admin", createdAt = "2026-08-17T08:00:00Z", updatedAt = "2026-08-17T08:00:00Z"
    )

    private fun buildStockIn(quantity: Int, warehouseId: String, locationId: String) = InventoryStockInRecord(
        stockInId = UUID.randomUUID().toString(), receivingId = "RCV-01", receivingLineId = "RCV-LINE-01",
        projectId = "PRJ-01", inventoryProductId = "PROD-01", warehouseId = warehouseId, locationId = locationId,
        quantity = quantity, unit = InventoryUnit.PCS, createdBy = "admin", createdAt = "2026-08-17T09:00:00Z"
    )
}

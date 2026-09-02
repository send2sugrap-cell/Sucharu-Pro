package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.repository.InventoryStockAdjustmentRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.*
import com.sucharu.sucharupro.domain.model.inventory.adjustment.*
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryStockInRecord
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOutRecord
import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransferRecord
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Comprehensive repository test for [InventoryStockAdjustmentRepository] (Module 07 Step 06).
 */
class InventoryStockAdjustmentRepositoryTest {

    private lateinit var adjustmentDataSource: FakeInventoryStockAdjustmentDataSource
    private lateinit var stockOutDataSource: FakeInventoryStockOutDataSource
    private lateinit var transferDataSource: FakeInventoryStockTransferDataSource
    private lateinit var receivingDataSource: FakeInventoryReceivingDataSource
    private lateinit var productDataSource: FakeInventoryProductDataSource
    private lateinit var warehouseDataSource: FakeInventoryWarehouseDataSource
    private lateinit var locationDataSource: FakeInventoryLocationDataSource
    private lateinit var repository: InventoryStockAdjustmentRepository

    @Before
    fun setup() {
        runBlocking {
            adjustmentDataSource = FakeInventoryStockAdjustmentDataSource()
            stockOutDataSource = FakeInventoryStockOutDataSource()
            transferDataSource = FakeInventoryStockTransferDataSource()
            receivingDataSource = FakeInventoryReceivingDataSource()
            productDataSource = FakeInventoryProductDataSource()
            warehouseDataSource = FakeInventoryWarehouseDataSource()
            locationDataSource = FakeInventoryLocationDataSource()
            
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
        }
    }

    @Test
    fun `create stock adjustment succeeds with valid data`() = runBlocking {
        val adjustment = buildAdjustment()
        val result = repository.createStockAdjustment(adjustment, UserRole.MANAGER)
        assertTrue(result is DomainResult.Success)
        assertEquals("ADJ-01", (result as DomainResult.Success).data.adjustmentId)
    }

    @Test
    fun `getAvailableQuantity incorporates all movement types`() = runBlocking {
        val projectId = "PRJ-01"
        val whId = "WH-01"
        val locId = "LOC-01"
        val prodId = "PROD-01"

        // 1. Stock In: +100
        receivingDataSource.insertStockInRecord(buildStockIn(100))
        
        // 2. Stock Out: -20
        stockOutDataSource.insertStockOutRecord(buildStockOutRecord(20))
        
        // 3. Transfer Out: -10
        transferDataSource.insertStockTransferRecord(buildTransferRecord(10, fromWh = whId, fromLoc = locId, toWh = "WH-OTHER", toLoc = "LOC-OTHER"))
        
        // 4. Transfer In: +5
        transferDataSource.insertStockTransferRecord(buildTransferRecord(5, fromWh = "WH-OTHER", fromLoc = "LOC-OTHER", toWh = whId, toLoc = locId))
        
        // 5. Existing Adjustment Increase: +15
        adjustmentDataSource.insertStockAdjustmentRecord(buildAdjustmentRecord(15, InventoryAdjustmentType.INCREASE))
        
        // 6. Existing Adjustment Decrease: -7
        adjustmentDataSource.insertStockAdjustmentRecord(buildAdjustmentRecord(7, InventoryAdjustmentType.DECREASE))

        // Expected: 100 - 20 - 10 + 5 + 15 - 7 = 83
        val available = repository.getAvailableQuantity(projectId, whId, locId, prodId)
        assertEquals(83, available)
    }

    @Test
    fun `complete stock adjustment creates appropriate records`() = runBlocking {
        // Seed initial stock: 50
        receivingDataSource.insertStockInRecord(buildStockIn(50))
        
        val adjustment = buildAdjustment("ADJ-01")
        repository.createStockAdjustment(adjustment, UserRole.MANAGER)
        
        // Line 1: Increase by 10
        val line1 = buildLine("L1", "ADJ-01", InventoryAdjustmentType.INCREASE, 50, 60)
        repository.addStockAdjustmentLine(line1, UserRole.MANAGER)
        
        // Line 2: Decrease by 20
        val line2 = buildLine("L2", "ADJ-01", InventoryAdjustmentType.DECREASE, 50, 30)
        repository.addStockAdjustmentLine(line2, UserRole.MANAGER)
        
        // Process
        repository.submitStockAdjustment("ADJ-01", "admin", "2026-08-17T11:00:00Z", UserRole.MANAGER)
        repository.approveStockAdjustment("ADJ-01", "admin", "2026-08-17T12:00:00Z", UserRole.MANAGER)
        repository.startStockAdjustment("ADJ-01", "admin", "2026-08-17T12:30:00Z", UserRole.WAREHOUSE)
        
        val result = repository.completeStockAdjustment("ADJ-01", "admin", "2026-08-17T13:00:00Z", UserRole.MANAGER)
        assertTrue(result is DomainResult.Success)
        
        val records = adjustmentDataSource.observeStockAdjustmentRecords().first()
        assertEquals(2, records.size)
        
        val incRecord = records.find { it.adjustmentType == InventoryAdjustmentType.INCREASE }
        val decRecord = records.find { it.adjustmentType == InventoryAdjustmentType.DECREASE }
        
        assertEquals(10, incRecord?.quantity)
        assertEquals(20, decRecord?.quantity)
    }

    @Test
    fun `complete stock adjustment fails if DECREASE results in negative stock`() = runBlocking {
        // Actual system stock: 10
        receivingDataSource.insertStockInRecord(buildStockIn(10))
        
        repository.createStockAdjustment(buildAdjustment("ADJ-01"), UserRole.MANAGER)
        // Line says current is 20, adjusted is 5 -> Change is -15.
        // But actual system stock is only 10. 10 - 15 = -5 (Fail)
        repository.addStockAdjustmentLine(buildLine("L1", "ADJ-01", InventoryAdjustmentType.DECREASE, 20, 5), UserRole.MANAGER)
        
        repository.submitStockAdjustment("ADJ-01", "admin", "2026-08-17T11:00:00Z", UserRole.MANAGER)
        repository.approveStockAdjustment("ADJ-01", "admin", "2026-08-17T12:00:00Z", UserRole.MANAGER)
        repository.startStockAdjustment("ADJ-01", "admin", "2026-08-17T12:30:00Z", UserRole.WAREHOUSE)
        
        val result = repository.completeStockAdjustment("ADJ-01", "admin", "2026-08-17T13:00:00Z", UserRole.MANAGER)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Insufficient stock"))
    }

    // Helpers
    private fun buildAdjustment(id: String = "ADJ-01") = InventoryStockAdjustment(
        adjustmentId = id, projectId = "PRJ-01", adjustmentReference = "ADJ-REF-01",
        warehouseId = "WH-01", adjustmentDate = "2026-08-17", createdBy = "admin",
        createdAt = "2026-08-17T10:00:00Z", updatedAt = "2026-08-17T10:00:00Z"
    )

    private fun buildLine(id: String, adjId: String, type: InventoryAdjustmentType, current: Int, adjusted: Int) = InventoryStockAdjustmentLine(
        adjustmentLineId = id, adjustmentId = adjId, projectId = "PRJ-01",
        inventoryProductId = "PROD-01", warehouseId = "WH-01", locationId = "LOC-01",
        adjustmentType = type, adjustmentReason = InventoryAdjustmentReason.PHYSICAL_COUNT,
        currentQuantity = current, adjustedQuantity = if (adjusted < 0) 0 else adjusted, 
        quantityChange = (if (adjusted < 0) 0 else adjusted) - current,
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

    private fun buildStockOutRecord(quantity: Int) = InventoryStockOutRecord(
        stockOutRecordId = UUID.randomUUID().toString(), stockOutId = "SO-01", stockOutLineId = "SO-LINE-01",
        projectId = "PRJ-01", inventoryProductId = "PROD-01", warehouseId = "WH-01", locationId = "LOC-01",
        quantity = quantity, unit = InventoryUnit.PCS, createdBy = "admin", createdAt = "2026-08-17T09:30:00Z"
    )

    private fun buildTransferRecord(quantity: Int, fromWh: String, fromLoc: String, toWh: String, toLoc: String) = InventoryStockTransferRecord(
        transferRecordId = UUID.randomUUID().toString(), transferId = UUID.randomUUID().toString(), transferLineId = UUID.randomUUID().toString(),
        projectId = "PRJ-01", inventoryProductId = "PROD-01",
        fromWarehouseId = fromWh, fromLocationId = fromLoc,
        toWarehouseId = toWh, toLocationId = toLoc,
        quantity = quantity, unit = InventoryUnit.PCS, createdBy = "admin", createdAt = "2026-08-17T09:45:00Z"
    )

    private fun buildAdjustmentRecord(quantity: Int, type: InventoryAdjustmentType) = InventoryStockAdjustmentRecord(
        adjustmentRecordId = UUID.randomUUID().toString(), adjustmentId = UUID.randomUUID().toString(), adjustmentLineId = UUID.randomUUID().toString(),
        projectId = "PRJ-01", inventoryProductId = "PROD-01", warehouseId = "WH-01", locationId = "LOC-01",
        adjustmentType = type, adjustmentReason = InventoryAdjustmentReason.DAMAGED,
        quantity = quantity, unit = InventoryUnit.PCS, createdBy = "admin", createdAt = "2026-08-17T09:50:00Z"
    )
}

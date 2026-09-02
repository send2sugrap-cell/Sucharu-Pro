package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.repository.InventoryStockAdjustmentRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.*
import com.sucharu.sucharupro.domain.model.inventory.adjustment.*
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryStockInRecord
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.InventoryStockAdjustmentRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * End-to-End integration test for Stock Adjustment Management (Module 07 Step 06).
 */
class InventoryStockAdjustmentEndToEndTest {

    private lateinit var adjustmentDataSource: FakeInventoryStockAdjustmentDataSource
    private lateinit var receivingDataSource: FakeInventoryReceivingDataSource
    private lateinit var productDataSource: FakeInventoryProductDataSource
    private lateinit var warehouseDataSource: FakeInventoryWarehouseDataSource
    private lateinit var locationDataSource: FakeInventoryLocationDataSource
    private lateinit var repository: InventoryStockAdjustmentRepository

    @Before
    fun setup() {
        runBlocking {
            adjustmentDataSource = FakeInventoryStockAdjustmentDataSource()
            receivingDataSource = FakeInventoryReceivingDataSource()
            productDataSource = FakeInventoryProductDataSource()
            warehouseDataSource = FakeInventoryWarehouseDataSource()
            locationDataSource = FakeInventoryLocationDataSource()
            
            repository = InventoryStockAdjustmentRepositoryImpl(
                adjustmentDataSource = adjustmentDataSource,
                stockOutDataSource = FakeInventoryStockOutDataSource(),
                transferDataSource = FakeInventoryStockTransferDataSource(),
                receivingDataSource = receivingDataSource,
                productDataSource = productDataSource,
                warehouseDataSource = warehouseDataSource,
                locationDataSource = locationDataSource
            )
            
            // 1. Seed infrastructure
            productDataSource.insertProduct(buildProduct())
            warehouseDataSource.insertWarehouse(buildWarehouse())
            locationDataSource.insertLocation(buildLocation())
        }
    }

    @Test
    fun `full stock adjustment lifecycle with balance verification`() = runBlocking {
        val projectId = "PRJ-E2E"
        val warehouseId = "WH-E2E"
        val locationId = "LOC-E2E"
        val productId = "PROD-E2E"
        val actor = "admin-01"
        val timestamp = "2026-08-17T15:00:00Z"

        // 2. Seed initial stock: 100 units
        receivingDataSource.insertStockInRecord(buildStockIn(100, projectId, productId, warehouseId, locationId))
        
        var available = repository.getAvailableQuantity(projectId, warehouseId, locationId, productId)
        assertEquals(100, available)

        // 3. Create Adjustment request
        val adjId = "ADJ-E2E-001"
        val adj = buildAdjustment(adjId, "ADJ-REF-E2E", projectId, warehouseId)
        val createResult = repository.createStockAdjustment(adj, UserRole.MANAGER)
        assertTrue(createResult is DomainResult.Success)

        // 4. Add lines (Increase by 20, Decrease by 15)
        repository.addStockAdjustmentLine(buildLine("L1", adjId, projectId, productId, warehouseId, locationId, InventoryAdjustmentType.INCREASE, 100, 120), UserRole.MANAGER)
        repository.addStockAdjustmentLine(buildLine("L2", adjId, projectId, productId, warehouseId, locationId, InventoryAdjustmentType.DECREASE, 100, 85), UserRole.MANAGER)

        // 5. Submit
        repository.submitStockAdjustment(adjId, actor, timestamp, UserRole.MANAGER)
        
        // 6. Approve
        repository.approveStockAdjustment(adjId, actor, timestamp, UserRole.MANAGER)

        // 6.1 Start Adjusting
        repository.startStockAdjustment(adjId, actor, timestamp, UserRole.WAREHOUSE)

        // 7. Complete
        val completeResult = repository.completeStockAdjustment(adjId, actor, timestamp, UserRole.MANAGER)
        assertTrue(completeResult is DomainResult.Success)
        assertEquals(InventoryStockAdjustmentStatus.COMPLETED, (completeResult as DomainResult.Success).data.status)

        // 8. Verify
        // Final Quantity Change: +20 - 15 = +5
        assertEquals(5, completeResult.data.totalQuantityChange)

        // Check Stock Adjustment Records
        val records = repository.observeStockAdjustmentRecords(projectId).first()
        assertEquals(2, records.size)

        // Check final available stock: 100 + 20 - 15 = 105
        available = repository.getAvailableQuantity(projectId, warehouseId, locationId, productId)
        assertEquals(105, available)
    }

    private fun buildAdjustment(id: String, ref: String, projectId: String, warehouseId: String) = InventoryStockAdjustment(
        adjustmentId = id, projectId = projectId, adjustmentReference = ref,
        warehouseId = warehouseId, adjustmentDate = "2026-08-17", createdBy = "admin",
        createdAt = "2026-08-17T10:00:00Z", updatedAt = "2026-08-17T10:00:00Z"
    )

    private fun buildLine(id: String, adjId: String, projectId: String, productId: String, warehouseId: String, locationId: String, type: InventoryAdjustmentType, current: Int, adjusted: Int) = InventoryStockAdjustmentLine(
        adjustmentLineId = id, adjustmentId = adjId, projectId = projectId,
        inventoryProductId = productId, warehouseId = warehouseId, locationId = locationId,
        adjustmentType = type, adjustmentReason = InventoryAdjustmentReason.PHYSICAL_COUNT,
        currentQuantity = current, adjustedQuantity = adjusted, quantityChange = adjusted - current,
        createdAt = "2026-08-17T10:00:00Z", updatedAt = "2026-08-17T10:00:00Z"
    )

    private fun buildWarehouse() = InventoryWarehouse(
        id = "WH-E2E", projectId = "PRJ-E2E", code = "WHE2E", name = "E2E Warehouse",
        type = InventoryWarehouseType.FINISHED_GOODS, createdBy = "admin",
        createdAt = "2026-08-17T08:00:00Z", updatedAt = "2026-08-17T08:00:00Z"
    )

    private fun buildLocation() = InventoryLocation(
        id = "LOC-E2E", projectId = "PRJ-E2E", warehouseId = "WH-E2E", code = "LOCE2E",
        name = "E2E Location", type = InventoryLocationType.SHELF, createdBy = "admin",
        createdAt = "2026-08-17T08:00:00Z", updatedAt = "2026-08-17T08:00:00Z"
    )

    private fun buildProduct() = InventoryProduct(
        id = "PROD-E2E", sku = "SKUE2E", name = "E2E Product", isActive = true, isStockTracked = true,
        createdBy = "admin", createdAt = "2026-08-17T08:00:00Z", updatedAt = "2026-08-17T08:00:00Z"
    )

    private fun buildStockIn(quantity: Int, projectId: String, productId: String, warehouseId: String, locationId: String) = InventoryStockInRecord(
        stockInId = UUID.randomUUID().toString(), receivingId = "RCV-E2E", receivingLineId = "RCV-LINE-E2E",
        projectId = projectId, inventoryProductId = productId, warehouseId = warehouseId, locationId = locationId,
        quantity = quantity, unit = InventoryUnit.PCS, createdBy = "admin", createdAt = "2026-08-17T09:00:00Z"
    )
}

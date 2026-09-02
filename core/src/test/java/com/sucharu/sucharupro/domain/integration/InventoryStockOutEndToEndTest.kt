package com.sucharu.sucharupro.domain.integration

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
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOutStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.InventoryStockOutRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * End-to-End integration test for Stock Out & Issue Management (Module 07 Step 04).
 *
 * Simulates:
 * 1. Seeding infrastructure (Product, Warehouse, Location).
 * 2. Seeding initial stock via [InventoryStockInRecord].
 * 3. Creating Stock-Out request (DRAFT).
 * 4. Adding lines.
 * 5. Submitting (PENDING).
 * 6. Approving (ISSUING).
 * 7. Completing Stock-Out.
 * 8. Verifying [InventoryStockOutRecord] creation and final available stock balance.
 */
class InventoryStockOutEndToEndTest {

    private lateinit var stockOutDataSource: FakeInventoryStockOutDataSource
    private lateinit var receivingDataSource: FakeInventoryReceivingDataSource
    private lateinit var productDataSource: FakeInventoryProductDataSource
    private lateinit var warehouseDataSource: FakeInventoryWarehouseDataSource
    private lateinit var locationDataSource: FakeInventoryLocationDataSource
    private lateinit var repository: InventoryStockOutRepository

    @Before
    fun setup() {
        runBlocking {
            stockOutDataSource = FakeInventoryStockOutDataSource()
            receivingDataSource = FakeInventoryReceivingDataSource()
            productDataSource = FakeInventoryProductDataSource()
            warehouseDataSource = FakeInventoryWarehouseDataSource()
            locationDataSource = FakeInventoryLocationDataSource()
            repository = InventoryStockOutRepositoryImpl(
                stockOutDataSource = stockOutDataSource,
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
    fun `full stock-out lifecycle with balance verification`() = runBlocking {
        val projectId = "PRJ-E2E"
        val warehouseId = "WH-E2E"
        val locationId = "LOC-E2E"
        val productId = "PROD-E2E"
        val actor = "admin-01"
        val timestamp = "2026-08-17T15:00:00Z"

        // 2. Seed initial stock: 500 units
        receivingDataSource.insertStockInRecord(buildStockIn(500, projectId, productId, warehouseId, locationId))
        
        var available = repository.getAvailableQuantity(projectId, warehouseId, locationId, productId)
        assertEquals(500, available)

        // 3. Create Stock-Out request
        val soId = "SO-E2E-001"
        val so = buildStockOut(soId, "SO-REF-E2E", projectId, warehouseId)
        val createResult = repository.createStockOut(so, UserRole.MANAGER)
        assertTrue(createResult is DomainResult.Success)

        // 4. Add lines (Issue 200 units)
        val lineId = "LINE-E2E-001"
        val line = buildLine(lineId, soId, projectId, productId, warehouseId, locationId, 200)
        val addLineResult = repository.addStockOutLine(line, UserRole.MANAGER)
        assertTrue(addLineResult is DomainResult.Success)

        // 5. Submit
        val submitResult = repository.submitStockOut(soId, actor, timestamp, UserRole.MANAGER)
        assertTrue(submitResult is DomainResult.Success)
        assertEquals(InventoryStockOutStatus.PENDING, (submitResult as DomainResult.Success).data.status)

        // 6. Approve
        val approveResult = repository.approveStockOut(soId, actor, timestamp, UserRole.MANAGER)
        assertTrue(approveResult is DomainResult.Success)
        assertEquals(InventoryStockOutStatus.ISSUING, (approveResult as DomainResult.Success).data.status)

        // 7. Complete
        val completeResult = repository.completeStockOut(soId, actor, timestamp, UserRole.MANAGER)
        assertTrue(completeResult is DomainResult.Success)
        assertEquals(InventoryStockOutStatus.COMPLETED, (completeResult as DomainResult.Success).data.status)

        // 8. Verify
        // Check Stock-Out Record
        val records = repository.observeStockOutRecords(projectId).first()
        assertEquals(1, records.size)
        val record = records.first()
        assertEquals(200, record.quantity)
        assertEquals(soId, record.stockOutId)

        // Check final available stock: 500 - 200 = 300
        available = repository.getAvailableQuantity(projectId, warehouseId, locationId, productId)
        assertEquals(300, available)
    }

    private fun buildStockOut(id: String, ref: String, projectId: String, warehouseId: String) = InventoryStockOut(
        stockOutId = id, projectId = projectId, stockOutReference = ref,
        warehouseId = warehouseId, stockOutDate = "2026-08-17", createdBy = "admin",
        createdAt = "2026-08-17T10:00:00Z", updatedAt = "2026-08-17T10:00:00Z"
    )

    private fun buildLine(id: String, soId: String, projectId: String, productId: String, warehouseId: String, locationId: String, qty: Int) = InventoryStockOutLine(
        stockOutLineId = id, stockOutId = soId, projectId = projectId,
        inventoryProductId = productId, warehouseId = warehouseId, locationId = locationId,
        expectedQuantity = qty, createdAt = "2026-08-17T10:00:00Z", updatedAt = "2026-08-17T10:00:00Z"
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

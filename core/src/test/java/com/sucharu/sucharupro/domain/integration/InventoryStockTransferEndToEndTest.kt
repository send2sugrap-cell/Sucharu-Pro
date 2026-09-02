package com.sucharu.sucharupro.domain.integration

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
import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransferRecord
import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransferStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.InventoryStockTransferRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * End-to-End integration test for Stock Transfer Management (Module 07 Step 05).
 *
 * Simulates:
 * 1. Seeding infrastructure (Product, Warehouses, Locations).
 * 2. Seeding initial stock at source via [InventoryStockInRecord].
 * 3. Creating Stock Transfer request (DRAFT).
 * 4. Adding lines.
 * 5. Submitting (PENDING).
 * 6. Approving (APPROVED).
 * 7. Completing Transfer.
 * 8. Verifying [InventoryStockTransferRecord] creation and final available stock balances at both sites.
 */
class InventoryStockTransferEndToEndTest {

    private lateinit var transferDataSource: FakeInventoryStockTransferDataSource
    private lateinit var receivingDataSource: FakeInventoryReceivingDataSource
    private lateinit var productDataSource: FakeInventoryProductDataSource
    private lateinit var warehouseDataSource: FakeInventoryWarehouseDataSource
    private lateinit var locationDataSource: FakeInventoryLocationDataSource
    private lateinit var repository: InventoryStockTransferRepository

    @Before
    fun setup() {
        runBlocking {
            transferDataSource = FakeInventoryStockTransferDataSource()
            receivingDataSource = FakeInventoryReceivingDataSource()
            productDataSource = FakeInventoryProductDataSource()
            warehouseDataSource = FakeInventoryWarehouseDataSource()
            locationDataSource = FakeInventoryLocationDataSource()
            repository = InventoryStockTransferRepositoryImpl(
                transferDataSource = transferDataSource,
                stockOutDataSource = FakeInventoryStockOutDataSource(),
                receivingDataSource = receivingDataSource,
                productDataSource = productDataSource,
                warehouseDataSource = warehouseDataSource,
                locationDataSource = locationDataSource
            )
            // 1. Seed infrastructure
            productDataSource.insertProduct(buildProduct())
            warehouseDataSource.insertWarehouse(buildWarehouse("WH-SRC-E2E", "WHSRCE2E"))
            warehouseDataSource.insertWarehouse(buildWarehouse("WH-DEST-E2E", "WHDESTE2E"))
            locationDataSource.insertLocation(buildLocation("LOC-SRC-E2E", "WH-SRC-E2E", "LOCSRCE2E"))
            locationDataSource.insertLocation(buildLocation("LOC-DEST-E2E", "WH-DEST-E2E", "LOCDESTE2E"))
        }
    }

    @Test
    fun `full stock transfer lifecycle with balanced verification`() = runBlocking {
        val projectId = "PRJ-E2E"
        val srcWhId = "WH-SRC-E2E"
        val srcLocId = "LOC-SRC-E2E"
        val destWhId = "WH-DEST-E2E"
        val destLocId = "LOC-DEST-E2E"
        val productId = "PROD-E2E"
        val actor = "admin-01"
        val timestamp = "2026-08-17T15:00:00Z"

        // 2. Seed initial stock: 500 units at source
        receivingDataSource.insertStockInRecord(buildStockIn(500, projectId, productId, srcWhId, srcLocId))
        
        var srcAvailable = repository.getAvailableQuantity(projectId, srcWhId, srcLocId, productId)
        var destAvailable = repository.getAvailableQuantity(projectId, destWhId, destLocId, productId)
        assertEquals(500, srcAvailable)
        assertEquals(0, destAvailable)

        // 3. Create Stock Transfer request
        val stId = "ST-E2E-001"
        val st = buildTransfer(stId, "ST-REF-E2E", projectId, srcWhId, destWhId)
        val createResult = repository.createStockTransfer(st, UserRole.MANAGER)
        assertTrue(createResult is DomainResult.Success)

        // 4. Add lines (Transfer 200 units)
        val lineId = "LINE-E2E-001"
        val line = buildLine(lineId, stId, projectId, productId, srcWhId, srcLocId, destWhId, destLocId, 200)
        val addLineResult = repository.addStockTransferLine(line, UserRole.MANAGER)
        assertTrue(addLineResult is DomainResult.Success)

        // 5. Submit
        val submitResult = repository.submitStockTransfer(stId, actor, timestamp, UserRole.MANAGER)
        assertTrue(submitResult is DomainResult.Success)
        assertEquals(InventoryStockTransferStatus.PENDING, (submitResult as DomainResult.Success).data.status)

        // 6. Approve
        val approveResult = repository.approveStockTransfer(stId, actor, timestamp, UserRole.MANAGER)
        assertTrue(approveResult is DomainResult.Success)
        assertEquals(InventoryStockTransferStatus.APPROVED, (approveResult as DomainResult.Success).data.status)

        // 7. Start Transfer
        val startResult = repository.startStockTransfer(stId, actor, timestamp, UserRole.WAREHOUSE)
        assertTrue(startResult is DomainResult.Success)
        assertEquals(InventoryStockTransferStatus.TRANSFERRING, (startResult as DomainResult.Success).data.status)

        // 8. Complete
        val completeResult = repository.completeStockTransfer(stId, actor, timestamp, UserRole.MANAGER)
        assertTrue(completeResult is DomainResult.Success)
        assertEquals(InventoryStockTransferStatus.COMPLETED, (completeResult as DomainResult.Success).data.status)

        // 9. Verify
        // Check Stock Transfer Record
        val records = repository.observeStockTransferRecords(projectId).first()
        assertEquals(1, records.size)
        val record = records.first()
        assertEquals(200, record.quantity)
        assertEquals(stId, record.transferId)
        assertEquals(srcWhId, record.fromWarehouseId)
        assertEquals(destWhId, record.toWarehouseId)

        // Check final available stock: 
        // Source: 500 - 200 = 300
        // Dest: 0 + 200 = 200
        srcAvailable = repository.getAvailableQuantity(projectId, srcWhId, srcLocId, productId)
        destAvailable = repository.getAvailableQuantity(projectId, destWhId, destLocId, productId)
        assertEquals(300, srcAvailable)
        assertEquals(200, destAvailable)
    }

    private fun buildTransfer(id: String, ref: String, projectId: String, fromWh: String, toWh: String) = InventoryStockTransfer(
        transferId = id, projectId = projectId, transferReference = ref,
        fromWarehouseId = fromWh, toWarehouseId = toWh, transferDate = "2026-08-17", createdBy = "admin",
        createdAt = "2026-08-17T10:00:00Z", updatedAt = "2026-08-17T10:00:00Z"
    )

    private fun buildLine(id: String, stId: String, projectId: String, productId: String, fromWh: String, fromLoc: String, toWh: String, toLoc: String, qty: Int) = InventoryStockTransferLine(
        transferLineId = id, transferId = stId, projectId = projectId,
        inventoryProductId = productId, fromWarehouseId = fromWh, fromLocationId = fromLoc,
        toWarehouseId = toWh, toLocationId = toLoc,
        expectedQuantity = qty, createdAt = "2026-08-17T10:00:00Z", updatedAt = "2026-08-17T10:00:00Z"
    )

    private fun buildWarehouse(id: String, code: String) = InventoryWarehouse(
        id = id, projectId = "PRJ-E2E", code = code, name = "E2E Warehouse $code",
        type = InventoryWarehouseType.FINISHED_GOODS, createdBy = "admin",
        createdAt = "2026-08-17T08:00:00Z", updatedAt = "2026-08-17T08:00:00Z"
    )

    private fun buildLocation(id: String, whId: String, code: String) = InventoryLocation(
        id = id, projectId = "PRJ-E2E", warehouseId = whId, code = code,
        name = "E2E Location $code", type = InventoryLocationType.SHELF, createdBy = "admin",
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

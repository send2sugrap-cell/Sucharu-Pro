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
import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransferStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Comprehensive repository test for [InventoryStockTransferRepository] (Module 07 Step 05).
 */
class InventoryStockTransferRepositoryTest {

    private lateinit var transferDataSource: FakeInventoryStockTransferDataSource
    private lateinit var stockOutDataSource: FakeInventoryStockOutDataSource
    private lateinit var receivingDataSource: FakeInventoryReceivingDataSource
    private lateinit var productDataSource: FakeInventoryProductDataSource
    private lateinit var warehouseDataSource: FakeInventoryWarehouseDataSource
    private lateinit var locationDataSource: FakeInventoryLocationDataSource
    private lateinit var repository: InventoryStockTransferRepository

    @Before
    fun setup() {
        runBlocking {
            transferDataSource = FakeInventoryStockTransferDataSource()
            stockOutDataSource = FakeInventoryStockOutDataSource()
            receivingDataSource = FakeInventoryReceivingDataSource()
            productDataSource = FakeInventoryProductDataSource()
            warehouseDataSource = FakeInventoryWarehouseDataSource()
            locationDataSource = FakeInventoryLocationDataSource()
            repository = InventoryStockTransferRepositoryImpl(
                transferDataSource = transferDataSource,
                stockOutDataSource = stockOutDataSource,
                receivingDataSource = receivingDataSource,
                productDataSource = productDataSource,
                warehouseDataSource = warehouseDataSource,
                locationDataSource = locationDataSource
            )
            // Seed infrastructure
            warehouseDataSource.insertWarehouse(buildWarehouse("WH-SRC", "WHSRC", "Source Warehouse"))
            warehouseDataSource.insertWarehouse(buildWarehouse("WH-DEST", "WHDEST", "Dest Warehouse"))
            locationDataSource.insertLocation(buildLocation("LOC-SRC", "WH-SRC", "LOCSRC"))
            locationDataSource.insertLocation(buildLocation("LOC-DEST", "WH-DEST", "LOCDEST"))
            productDataSource.insertProduct(buildProduct())
        }
    }

    @Test
    fun `create stock transfer succeeds with valid data`() = runBlocking {
        val transfer = buildTransfer()
        val result = repository.createStockTransfer(transfer, UserRole.MANAGER)
        assertTrue(result is DomainResult.Success)
        assertEquals("ST-01", (result as DomainResult.Success).data.transferId)
    }

    @Test
    fun `add line to stock transfer succeeds`() = runBlocking {
        repository.createStockTransfer(buildTransfer(), UserRole.MANAGER)
        val line = buildLine("LINE-01", "ST-01")
        val result = repository.addStockTransferLine(line, UserRole.MANAGER)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `complete stock transfer with insufficient stock fails`() = runBlocking {
        repository.createStockTransfer(buildTransfer(), UserRole.MANAGER)
        repository.addStockTransferLine(buildLine("LINE-01", "ST-01", expectedQuantity = 100), UserRole.MANAGER)
        repository.submitStockTransfer("ST-01", "admin", "2026-08-17T11:00:00Z", UserRole.MANAGER)
        repository.approveStockTransfer("ST-01", "admin", "2026-08-17T12:00:00Z", UserRole.MANAGER)
        repository.startStockTransfer("ST-01", "admin", "2026-08-17T12:30:00Z", UserRole.WAREHOUSE)

        // Seed some stock but less than 100
        receivingDataSource.insertStockInRecord(buildStockIn(quantity = 50, warehouseId = "WH-SRC", locationId = "LOC-SRC"))

        val result = repository.completeStockTransfer("ST-01", "admin", "2026-08-17T13:00:00Z", UserRole.MANAGER)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Insufficient stock"))
    }

    @Test
    fun `complete stock transfer with sufficient stock succeeds and creates records`() = runBlocking {
        repository.createStockTransfer(buildTransfer(), UserRole.MANAGER)
        repository.addStockTransferLine(buildLine("LINE-01", "ST-01", expectedQuantity = 100), UserRole.MANAGER)
        repository.submitStockTransfer("ST-01", "admin", "2026-08-17T11:00:00Z", UserRole.MANAGER)
        repository.approveStockTransfer("ST-01", "admin", "2026-08-17T12:00:00Z", UserRole.MANAGER)
        repository.startStockTransfer("ST-01", "admin", "2026-08-17T12:30:00Z", UserRole.WAREHOUSE)

        // Seed sufficient stock
        receivingDataSource.insertStockInRecord(buildStockIn(quantity = 150, warehouseId = "WH-SRC", locationId = "LOC-SRC"))

        val result = repository.completeStockTransfer("ST-01", "admin", "2026-08-17T13:00:00Z", UserRole.MANAGER)
        assertTrue(result is DomainResult.Success)
        assertEquals(InventoryStockTransferStatus.COMPLETED, (result as DomainResult.Success).data.status)

        // Verify record creation
        val records = transferDataSource.observeStockTransferRecords().first()
        assertEquals(1, records.size)
        val record = records.first()
        assertEquals(100, record.quantity)
        assertEquals("WH-SRC", record.fromWarehouseId)
        assertEquals("WH-DEST", record.toWarehouseId)

        // Verify balanced availability
        val srcQty = repository.getAvailableQuantity("PRJ-01", "WH-SRC", "LOC-SRC", "PROD-01")
        val destQty = repository.getAvailableQuantity("PRJ-01", "WH-DEST", "LOC-DEST", "PROD-01")
        
        assertEquals(50, srcQty) // 150 - 100
        assertEquals(100, destQty) // 0 + 100
    }

    // Helpers
    private fun buildTransfer(id: String = "ST-01") = InventoryStockTransfer(
        transferId = id, projectId = "PRJ-01", transferReference = "ST-REF-01",
        fromWarehouseId = "WH-SRC", toWarehouseId = "WH-DEST", transferDate = "2026-08-17",
        createdBy = "admin", createdAt = "2026-08-17T10:00:00Z", updatedAt = "2026-08-17T10:00:00Z"
    )

    private fun buildLine(id: String, stId: String, expectedQuantity: Int = 10) = InventoryStockTransferLine(
        transferLineId = id, transferId = stId, projectId = "PRJ-01",
        inventoryProductId = "PROD-01", fromWarehouseId = "WH-SRC", fromLocationId = "LOC-SRC",
        toWarehouseId = "WH-DEST", toLocationId = "LOC-DEST",
        expectedQuantity = expectedQuantity, createdAt = "2026-08-17T10:00:00Z", updatedAt = "2026-08-17T10:00:00Z"
    )

    private fun buildWarehouse(id: String, code: String, name: String) = InventoryWarehouse(
        id = id, projectId = "PRJ-01", code = code, name = name,
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

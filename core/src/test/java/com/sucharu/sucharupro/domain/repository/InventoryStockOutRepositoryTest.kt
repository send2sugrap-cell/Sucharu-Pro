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
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOutStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Comprehensive repository test for [InventoryStockOutRepository] (Module 07 Step 04).
 */
class InventoryStockOutRepositoryTest {

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
            // Seed infrastructure
            warehouseDataSource.insertWarehouse(buildWarehouse())
            locationDataSource.insertLocation(buildLocation())
            productDataSource.insertProduct(buildProduct())
        }
    }

    @Test
    fun `create stock out succeeds with valid data`() = runBlocking {
        val stockOut = buildStockOut()
        val result = repository.createStockOut(stockOut, UserRole.MANAGER)
        assertTrue(result is DomainResult.Success)
        assertEquals("SO-01", (result as DomainResult.Success).data.stockOutId)
    }

    @Test
    fun `add line to stock out succeeds`() = runBlocking {
        repository.createStockOut(buildStockOut(), UserRole.MANAGER)
        val line = buildLine("LINE-01", "SO-01")
        val result = repository.addStockOutLine(line, UserRole.MANAGER)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `complete stock out with insufficient stock fails`() = runBlocking {
        repository.createStockOut(buildStockOut(), UserRole.MANAGER)
        repository.addStockOutLine(buildLine("LINE-01", "SO-01", expectedQuantity = 100), UserRole.MANAGER)
        repository.submitStockOut("SO-01", "admin", "2026-08-17T11:00:00Z", UserRole.MANAGER)
        repository.approveStockOut("SO-01", "admin", "2026-08-17T12:00:00Z", UserRole.MANAGER)

        // Seed some stock but less than 100
        receivingDataSource.insertStockInRecord(buildStockIn(quantity = 50))

        val result = repository.completeStockOut("SO-01", "admin", "2026-08-17T13:00:00Z", UserRole.MANAGER)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Insufficient stock"))
    }

    @Test
    fun `complete stock out with sufficient stock succeeds and creates records`() = runBlocking {
        repository.createStockOut(buildStockOut(), UserRole.MANAGER)
        repository.addStockOutLine(buildLine("LINE-01", "SO-01", expectedQuantity = 100), UserRole.MANAGER)
        repository.submitStockOut("SO-01", "admin", "2026-08-17T11:00:00Z", UserRole.MANAGER)
        repository.approveStockOut("SO-01", "admin", "2026-08-17T12:00:00Z", UserRole.MANAGER)

        // Seed sufficient stock
        receivingDataSource.insertStockInRecord(buildStockIn(quantity = 150))

        val result = repository.completeStockOut("SO-01", "admin", "2026-08-17T13:00:00Z", UserRole.MANAGER)
        assertTrue(result is DomainResult.Success)
        assertEquals(InventoryStockOutStatus.COMPLETED, (result as DomainResult.Success).data.status)

        val records = stockOutDataSource.observeStockOutRecords().first()
        assertEquals(1, records.size)
        assertEquals(100, records.first().quantity)
    }

    // Helpers
    private fun buildStockOut(id: String = "SO-01") = InventoryStockOut(
        stockOutId = id, projectId = "PRJ-01", stockOutReference = "SO-REF-01",
        warehouseId = "WH-01", stockOutDate = "2026-08-17", createdBy = "admin",
        createdAt = "2026-08-17T10:00:00Z", updatedAt = "2026-08-17T10:00:00Z"
    )

    private fun buildLine(id: String, soId: String, expectedQuantity: Int = 10) = InventoryStockOutLine(
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

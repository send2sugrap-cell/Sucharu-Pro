package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeInventoryLocationDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryProductDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryReceivingDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryWarehouseDataSource
import com.sucharu.sucharupro.data.repository.InventoryReceivingRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocation
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocationType
import com.sucharu.sucharupro.domain.model.inventory.InventoryProduct
import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouse
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouseType
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceiving
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceivingLine
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceivingStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for full receiving completion and atomic StockInRecord creation (Module 07 Step 03).
 *
 * Validates:
 * - Complete receiving creates exactly one StockInRecord per accepted line.
 * - Completion is idempotent.
 * - All lines must be finalized before completion.
 * - Zero-accepted lines produce no stock-in record.
 */
class InventoryReceivingCompletionTest {

    private lateinit var receivingDataSource: FakeInventoryReceivingDataSource
    private lateinit var productDataSource: FakeInventoryProductDataSource
    private lateinit var warehouseDataSource: FakeInventoryWarehouseDataSource
    private lateinit var locationDataSource: FakeInventoryLocationDataSource
    private lateinit var repository: InventoryReceivingRepository

    @Before
    fun setup() {
        runBlocking {
            receivingDataSource = FakeInventoryReceivingDataSource()
            productDataSource = FakeInventoryProductDataSource()
            warehouseDataSource = FakeInventoryWarehouseDataSource()
            locationDataSource = FakeInventoryLocationDataSource()
            repository = InventoryReceivingRepositoryImpl(
                receivingDataSource,
                productDataSource,
                warehouseDataSource,
                locationDataSource
            )
            warehouseDataSource.insertWarehouse(buildWarehouse())
            locationDataSource.insertLocation(buildLocation())
            productDataSource.insertProduct(buildProduct())
        }
    }

    @Test
    fun `completing receiving with all accepted lines creates stock-in records`() = runBlocking {
        createAndFinalizeReceiving(acceptedQty = 10, rejectedQty = 0)

        val completeResult = repository.completeReceiving("RCV-001", "manager-01", "2026-08-17T12:00:00Z", UserRole.MANAGER)
        assertTrue(completeResult is DomainResult.Success)
        assertEquals(InventoryReceivingStatus.COMPLETED, (completeResult as DomainResult.Success).data.status)

        val stockIns = receivingDataSource.observeStockInRecords().first()
            .filter { it.receivingId == "RCV-001" }
        assertEquals(1, stockIns.size)
        assertEquals(10, stockIns.first().quantity)
        assertEquals("PROD-01", stockIns.first().inventoryProductId)
        assertEquals("LOC-01", stockIns.first().locationId)
    }

    @Test
    fun `completing receiving with all rejected lines creates no stock-in records`() = runBlocking {
        createAndFinalizeReceiving(acceptedQty = 0, rejectedQty = 10)

        val completeResult = repository.completeReceiving("RCV-001", "manager-01", "2026-08-17T12:00:00Z", UserRole.MANAGER)
        assertTrue(completeResult is DomainResult.Success)

        val stockIns = receivingDataSource.observeStockInRecords().first()
            .filter { it.receivingId == "RCV-001" }
        assertEquals(0, stockIns.size)
    }

    @Test
    fun `completing receiving with non-finalized lines fails`() = runBlocking {
        repository.createReceiving(buildReceiving(), UserRole.MANAGER)
        repository.submitReceiving("RCV-001", "admin", "2026-08-17T09:00:00Z", UserRole.MANAGER)
        repository.startReceiving("RCV-001", "admin", "2026-08-17T09:00:00Z", UserRole.WAREHOUSE)
        // Add a line but don't verify/accept it
        repository.addReceivingLine(buildLine("LINE-001"), UserRole.MANAGER)

        val result = repository.completeReceiving("RCV-001", "manager-01", "2026-08-17T12:00:00Z", UserRole.MANAGER)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("non-finalized"))
    }

    @Test
    fun `completing receiving with no lines fails`() = runBlocking {
        repository.createReceiving(buildReceiving(), UserRole.MANAGER)
        repository.submitReceiving("RCV-001", "admin", "2026-08-17T09:00:00Z", UserRole.MANAGER)
        repository.startReceiving("RCV-001", "admin", "2026-08-17T09:00:00Z", UserRole.WAREHOUSE)

        val result = repository.completeReceiving("RCV-001", "manager-01", "2026-08-17T12:00:00Z", UserRole.MANAGER)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("no lines"))
    }

    @Test
    fun `completing already completed receiving is idempotent`() = runBlocking {
        createAndFinalizeReceiving(acceptedQty = 10, rejectedQty = 0)

        repository.completeReceiving("RCV-001", "manager-01", "2026-08-17T12:00:00Z", UserRole.MANAGER)
        val secondComplete = repository.completeReceiving("RCV-001", "manager-01", "2026-08-17T13:00:00Z", UserRole.MANAGER)

        assertTrue(secondComplete is DomainResult.Success)
        // Stock-in records should still be exactly 1
        val stockIns = receivingDataSource.observeStockInRecords().first()
            .filter { it.receivingId == "RCV-001" }
        assertEquals(1, stockIns.size)
    }

    @Test
    fun `STAFF cannot complete receiving`() = runBlocking {
        createAndFinalizeReceiving(acceptedQty = 10, rejectedQty = 0)
        val result = repository.completeReceiving("RCV-001", "staff-01", "2026-08-17T12:00:00Z", UserRole.STAFF)
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun `completing receiving emits STOCK_IN_CREATED and RECEIVING_COMPLETED audit events`() = runBlocking {
        createAndFinalizeReceiving(acceptedQty = 10, rejectedQty = 0)
        repository.completeReceiving("RCV-001", "manager-01", "2026-08-17T12:00:00Z", UserRole.MANAGER)

        val events = receivingDataSource.observeAuditEvents().first()
            .filter { it.receivingId == "RCV-001" }
        assertTrue(events.any { it.eventType.name == "STOCK_IN_CREATED" })
        assertTrue(events.any { it.eventType.name == "RECEIVING_COMPLETED" })
    }

    @Test
    fun `completed receiving total quantities are summed correctly`() = runBlocking {
        createAndFinalizeReceiving(acceptedQty = 8, rejectedQty = 2)
        val result = repository.completeReceiving("RCV-001", "manager-01", "2026-08-17T12:00:00Z", UserRole.MANAGER)
        val completed = (result as DomainResult.Success).data
        assertEquals(8, completed.acceptedTotalQuantity)
        assertEquals(2, completed.rejectedTotalQuantity)
    }

    // ──────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────

    private suspend fun createAndFinalizeReceiving(acceptedQty: Int, rejectedQty: Int) {
        val totalQty = acceptedQty + rejectedQty
        repository.createReceiving(buildReceiving(), UserRole.MANAGER)
        repository.submitReceiving("RCV-001", "admin", "2026-08-17T09:00:00Z", UserRole.MANAGER)
        repository.startReceiving("RCV-001", "admin", "2026-08-17T09:00:00Z", UserRole.WAREHOUSE)
        repository.addReceivingLine(buildLine("LINE-001"), UserRole.MANAGER)
        repository.recordReceivedQuantity("LINE-001", totalQty, "warehouse-01", "2026-08-17T10:00:00Z", UserRole.WAREHOUSE)
        repository.verifyReceivingLine("LINE-001", "verifier-01", acceptedQty, rejectedQty, null, "2026-08-17T11:00:00Z", UserRole.MANAGER)
        if (acceptedQty > 0 && rejectedQty == 0) {
            repository.acceptLine("LINE-001", "manager-01", "2026-08-17T11:30:00Z", UserRole.MANAGER)
        } else if (acceptedQty == 0 && rejectedQty > 0) {
            repository.rejectLine("LINE-001", "Quality failure", "manager-01", "2026-08-17T11:30:00Z", UserRole.MANAGER)
        } else if (acceptedQty > 0 && rejectedQty > 0) {
            repository.acceptLine("LINE-001", "manager-01", "2026-08-17T11:30:00Z", UserRole.MANAGER)
        }
    }

    private fun buildReceiving() = InventoryReceiving(
        receivingId = "RCV-001",
        projectId = "PRJ-01",
        receivingReference = "RCV-REF-001",
        warehouseId = "WH-01",
        receivingDate = "2026-08-17",
        createdBy = "admin-01",
        createdAt = "2026-08-17T08:00:00Z",
        updatedAt = "2026-08-17T08:00:00Z"
    )

    private fun buildLine(lineId: String) = InventoryReceivingLine(
        receivingLineId = lineId,
        receivingId = "RCV-001",
        projectId = "PRJ-01",
        inventoryProductId = "PROD-01",
        warehouseId = "WH-01",
        locationId = "LOC-01",
        expectedQuantity = 10,
        unit = InventoryUnit.PCS,
        createdAt = "2026-08-17T08:00:00Z",
        updatedAt = "2026-08-17T08:00:00Z"
    )

    private fun buildWarehouse() = InventoryWarehouse(
        id = "WH-01", projectId = "PRJ-01", code = "WH001", name = "Main WH",
        type = InventoryWarehouseType.FINISHED_GOODS, createdBy = "admin-01",
        createdAt = "2026-08-17T08:00:00Z", updatedAt = "2026-08-17T08:00:00Z"
    )

    private fun buildLocation() = InventoryLocation(
        id = "LOC-01", projectId = "PRJ-01", warehouseId = "WH-01", code = "LOC-A1",
        name = "Shelf A1", type = InventoryLocationType.SHELF, createdBy = "admin-01",
        createdAt = "2026-08-17T08:00:00Z", updatedAt = "2026-08-17T08:00:00Z"
    )

    private fun buildProduct() = InventoryProduct(
        id = "PROD-01", sku = "SKU-001", name = "Test Book", isActive = true, isStockTracked = true,
        createdAt = "2026-08-17T08:00:00Z", updatedAt = "2026-08-17T08:00:00Z", createdBy = "admin-01"
    )
}

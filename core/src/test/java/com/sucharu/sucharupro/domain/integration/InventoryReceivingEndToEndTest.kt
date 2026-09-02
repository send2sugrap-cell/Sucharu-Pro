package com.sucharu.sucharupro.domain.integration

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
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceivingLineStatus
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceivingStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.InventoryReceivingRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * End-to-End integration test for Module 07 Step 03: Stock In & Receiving Management.
 *
 * Simulates a complete real-world flow:
 * 1. Seed dependencies (Product, Warehouse, Location).
 * 2. Create Receiving record (DRAFT).
 * 3. Submit Receiving (PENDING).
 * 4. Start Receiving (RECEIVING).
 * 5. Add Line Item.
 * 6. Record Received Quantity.
 * 7. Verify Quantity (Accepted vs Rejected).
 * 8. Accept Line.
 * 9. Complete Receiving.
 * 10. Verify final state and immutable Stock-In Record creation.
 */
class InventoryReceivingEndToEndTest {

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
    fun `full stock receiving and stock-in lifecycle`() = runBlocking {
        val projectId = "PRJ-01"
        val rcvId = "RCV-E2E-001"
        val lineId = "LINE-E2E-001"
        val actor = "admin-01"
        val timestamp = "2026-08-17T15:00:00Z"

        // 2. Create Receiving record
        val rcv = buildReceiving(rcvId, projectId)
        val createResult = repository.createReceiving(rcv, UserRole.MANAGER)
        assertTrue("Create should succeed", createResult is DomainResult.Success)

        // 3. Submit Receiving
        val submitResult = repository.submitReceiving(rcvId, actor, timestamp, UserRole.MANAGER)
        assertTrue("Submit should succeed", submitResult is DomainResult.Success)
        assertEquals(InventoryReceivingStatus.PENDING, (submitResult as DomainResult.Success).data.status)

        // 4. Start Receiving
        val startResult = repository.startReceiving(rcvId, actor, timestamp, UserRole.WAREHOUSE)
        assertTrue("Start should succeed", startResult is DomainResult.Success)
        assertEquals(InventoryReceivingStatus.RECEIVING, (startResult as DomainResult.Success).data.status)

        // 5. Add Line Item
        val line = buildLine(lineId, rcvId, projectId)
        val addLineResult = repository.addReceivingLine(line, UserRole.MANAGER)
        assertTrue("Add line should succeed", addLineResult is DomainResult.Success)

        // 6. Record Received Quantity
        val recordQtyResult = repository.recordReceivedQuantity(lineId, 100, actor, timestamp, UserRole.WAREHOUSE)
        assertTrue("Record quantity should succeed", recordQtyResult is DomainResult.Success)
        assertEquals(100, (recordQtyResult as DomainResult.Success).data.receivedQuantity)

        // 7. Verify Quantity (95 Accepted, 5 Rejected)
        val verifyResult = repository.verifyReceivingLine(lineId, actor, 95, 5, "5 damaged", timestamp, UserRole.MANAGER)
        assertTrue("Verify should succeed", verifyResult is DomainResult.Success)
        
        val lineAfterVerify = repository.getReceivingLine(lineId, UserRole.MANAGER)
        assertEquals(InventoryReceivingLineStatus.VERIFIED, (lineAfterVerify as DomainResult.Success).data.lineStatus)

        // 8. Accept Line
        val acceptResult = repository.acceptLine(lineId, actor, timestamp, UserRole.MANAGER)
        assertTrue("Accept line should succeed", acceptResult is DomainResult.Success)
        assertEquals(InventoryReceivingLineStatus.ACCEPTED, (acceptResult as DomainResult.Success).data.lineStatus)

        // 9. Complete Receiving
        val completeResult = repository.completeReceiving(rcvId, actor, timestamp, UserRole.MANAGER)
        assertTrue("Complete should succeed", completeResult is DomainResult.Success)
        val finalRcv = (completeResult as DomainResult.Success).data
        assertEquals(InventoryReceivingStatus.COMPLETED, finalRcv.status)
        assertEquals(95, finalRcv.acceptedTotalQuantity)
        assertEquals(5, finalRcv.rejectedTotalQuantity)

        // 10. Verify final outcome
        // Check Stock-In record
        val stockIns = repository.observeStockInRecordsByReceiving(rcvId).first()
        assertEquals("Exactly one stock-in record should exist for the accepted quantity", 1, stockIns.size)
        val stockIn = stockIns.first()
        assertEquals(95, stockIn.quantity)
        assertEquals("PROD-E2E", stockIn.inventoryProductId)
        assertEquals("LOC-E2E", stockIn.locationId)
        assertEquals("RCV-REF-E2E", stockIn.sourceReference)

        // Check Audit history
        val audit = repository.getAuditHistory(rcvId, UserRole.ADMIN)
        assertTrue("Audit should contain STOCK_IN_CREATED", (audit as DomainResult.Success).data.any { it.eventType == com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceivingActivityType.STOCK_IN_CREATED })
        assertTrue("Audit should contain RECEIVING_COMPLETED", (audit as DomainResult.Success).data.any { it.eventType == com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceivingActivityType.RECEIVING_COMPLETED })
    }

    private fun buildReceiving(id: String, projectId: String) = InventoryReceiving(
        receivingId = id, projectId = projectId, receivingReference = "RCV-REF-E2E",
        warehouseId = "WH-E2E", receivingDate = "2026-08-17", createdBy = "admin-01",
        createdAt = "2026-08-17T08:00:00Z", updatedAt = "2026-08-17T08:00:00Z"
    )

    private fun buildLine(id: String, rcvId: String, projectId: String) = InventoryReceivingLine(
        receivingLineId = id, receivingId = rcvId, projectId = projectId,
        inventoryProductId = "PROD-E2E", warehouseId = "WH-E2E", locationId = "LOC-E2E",
        expectedQuantity = 100, unit = InventoryUnit.PCS,
        createdAt = "2026-08-17T08:00:00Z", updatedAt = "2026-08-17T08:00:00Z"
    )

    private fun buildWarehouse() = InventoryWarehouse(
        id = "WH-E2E", projectId = "PRJ-01", code = "WHE2E", name = "E2E Warehouse",
        type = InventoryWarehouseType.FINISHED_GOODS, createdBy = "admin-01",
        createdAt = "2026-08-17T08:00:00Z", updatedAt = "2026-08-17T08:00:00Z"
    )

    private fun buildLocation() = InventoryLocation(
        id = "LOC-E2E", projectId = "PRJ-01", warehouseId = "WH-E2E", code = "LOC-E2E",
        name = "E2E Location", type = InventoryLocationType.SHELF, createdBy = "admin-01",
        createdAt = "2026-08-17T08:00:00Z", updatedAt = "2026-08-17T08:00:00Z"
    )

    private fun buildProduct() = InventoryProduct(
        id = "PROD-E2E", sku = "SKU-E2E", name = "E2E Book", isActive = true, isStockTracked = true,
        createdAt = "2026-08-17T08:00:00Z", updatedAt = "2026-08-17T08:00:00Z", createdBy = "admin-01"
    )
}

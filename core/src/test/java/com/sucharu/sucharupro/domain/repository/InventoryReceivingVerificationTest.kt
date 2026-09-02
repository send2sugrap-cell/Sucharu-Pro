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
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceivingLineStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for line verification, acceptance, and rejection (Module 07 Step 03).
 */
class InventoryReceivingVerificationTest {

    private lateinit var receivingDataSource: FakeInventoryReceivingDataSource
    private lateinit var repository: InventoryReceivingRepository

    @Before
    fun setup() {
        runBlocking {
            receivingDataSource = FakeInventoryReceivingDataSource()
            val productDataSource = FakeInventoryProductDataSource()
            val warehouseDataSource = FakeInventoryWarehouseDataSource()
            val locationDataSource = FakeInventoryLocationDataSource()
            repository = InventoryReceivingRepositoryImpl(receivingDataSource, productDataSource, warehouseDataSource, locationDataSource)
            warehouseDataSource.insertWarehouse(buildWarehouse())
            locationDataSource.insertLocation(buildLocation())
            productDataSource.insertProduct(buildProduct())
            // Setup: create receiving → submit → start → add line → record quantity
            repository.createReceiving(buildReceiving(), UserRole.MANAGER)
            repository.submitReceiving("RCV-001", "admin", "2026-08-17T09:00:00Z", UserRole.MANAGER)
            repository.startReceiving("RCV-001", "admin", "2026-08-17T09:30:00Z", UserRole.WAREHOUSE)
            repository.addReceivingLine(buildLine(), UserRole.MANAGER)
            repository.recordReceivedQuantity("LINE-001", 10, "warehouse-01", "2026-08-17T10:00:00Z", UserRole.WAREHOUSE)
        }
    }

    @Test
    fun `verifying a line creates verification record and transitions to VERIFIED`() = runBlocking {
        val result = repository.verifyReceivingLine(
            receivingLineId = "LINE-001",
            verifiedBy = "qc-01",
            acceptedQuantity = 8,
            rejectedQuantity = 2,
            verificationNotes = "Minor defects",
            timestamp = "2026-08-17T11:00:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(result is DomainResult.Success)
        val verification = (result as DomainResult.Success).data
        assertEquals(8, verification.acceptedQuantity)
        assertEquals(2, verification.rejectedQuantity)

        // Line should be VERIFIED
        val lineResult = repository.getReceivingLine("LINE-001", UserRole.MANAGER)
        val line = (lineResult as DomainResult.Success).data
        assertEquals(InventoryReceivingLineStatus.VERIFIED, line.lineStatus)
        assertEquals(8, line.acceptedQuantity)
        assertEquals(2, line.rejectedQuantity)
    }

    @Test
    fun `verification with partial split (not full reconciliation) fails`() = runBlocking {
        val result = repository.verifyReceivingLine(
            receivingLineId = "LINE-001",
            verifiedBy = "qc-01",
            acceptedQuantity = 5,
            rejectedQuantity = 3, // 5+3=8 != 10 received
            verificationNotes = null,
            timestamp = "2026-08-17T11:00:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("must equal"))
    }

    @Test
    fun `verifying a line without recorded quantity fails`() = runBlocking {
        // LINE-001 already has quantity 10 recorded in @Before, so directly attempt
        // verify on a fresh receiving line that has 0 received quantity
        // by resetting with a direct datasource insert (simulating PENDING with no received qty)
        val pendingLine = InventoryReceivingLine(
            receivingLineId = "LINE-NOQTY", receivingId = "RCV-001", projectId = "PRJ-01",
            inventoryProductId = "PROD-01", warehouseId = "WH-01", locationId = "LOC-01",
            receivedQuantity = 0,
            createdAt = "2026-08-17T08:00:00Z", updatedAt = "2026-08-17T08:00:00Z"
        )
        receivingDataSource.insertReceivingLine(pendingLine)
        val result = repository.verifyReceivingLine(
            receivingLineId = "LINE-NOQTY",
            verifiedBy = "qc-01",
            acceptedQuantity = 5,
            rejectedQuantity = 0,
            verificationNotes = null,
            timestamp = "2026-08-17T11:00:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("received quantity"))
    }

    @Test
    fun `accepting a verified line transitions to ACCEPTED`() = runBlocking {
        repository.verifyReceivingLine("LINE-001", "qc-01", 10, 0, null, "2026-08-17T11:00:00Z", UserRole.MANAGER)
        val result = repository.acceptLine("LINE-001", "manager-01", "2026-08-17T11:30:00Z", UserRole.MANAGER)
        assertTrue(result is DomainResult.Success)
        assertEquals(InventoryReceivingLineStatus.ACCEPTED, (result as DomainResult.Success).data.lineStatus)
    }

    @Test
    fun `rejecting a verified line transitions to REJECTED`() = runBlocking {
        repository.verifyReceivingLine("LINE-001", "qc-01", 0, 10, null, "2026-08-17T11:00:00Z", UserRole.MANAGER)
        val result = repository.rejectLine("LINE-001", "Wrong product", "manager-01", "2026-08-17T11:30:00Z", UserRole.MANAGER)
        assertTrue(result is DomainResult.Success)
        assertEquals(InventoryReceivingLineStatus.REJECTED, (result as DomainResult.Success).data.lineStatus)
    }

    @Test
    fun `accepting a PENDING line fails (must be VERIFIED first)`() = runBlocking {
        val result = repository.acceptLine("LINE-001", "manager-01", "2026-08-17T11:30:00Z", UserRole.MANAGER)
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun `WAREHOUSE role cannot accept line`() = runBlocking {
        repository.verifyReceivingLine("LINE-001", "qc-01", 10, 0, null, "2026-08-17T11:00:00Z", UserRole.MANAGER)
        val result = repository.acceptLine("LINE-001", "warehouse-01", "2026-08-17T11:30:00Z", UserRole.WAREHOUSE)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("ADMIN or MANAGER"))
    }

    @Test
    fun `re-verifying an already verified line fails`() = runBlocking {
        repository.verifyReceivingLine("LINE-001", "qc-01", 10, 0, null, "2026-08-17T11:00:00Z", UserRole.MANAGER)
        val result = repository.verifyReceivingLine("LINE-001", "qc-02", 10, 0, null, "2026-08-17T12:00:00Z", UserRole.MANAGER)
        assertTrue(result is DomainResult.Error)
    }

    private fun buildReceiving() = InventoryReceiving(
        receivingId = "RCV-001", projectId = "PRJ-01", receivingReference = "RCV-REF-001",
        warehouseId = "WH-01", receivingDate = "2026-08-17", createdBy = "admin-01",
        createdAt = "2026-08-17T08:00:00Z", updatedAt = "2026-08-17T08:00:00Z"
    )

    private fun buildLine() = InventoryReceivingLine(
        receivingLineId = "LINE-001", receivingId = "RCV-001", projectId = "PRJ-01",
        inventoryProductId = "PROD-01", warehouseId = "WH-01", locationId = "LOC-01",
        expectedQuantity = 10, unit = InventoryUnit.PCS,
        createdAt = "2026-08-17T08:00:00Z", updatedAt = "2026-08-17T08:00:00Z"
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

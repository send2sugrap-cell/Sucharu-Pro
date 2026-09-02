package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.data.datasource.FakeInventoryMovementLedgerDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryReceivingDataSource
import com.sucharu.sucharupro.data.datasource.FakeReturnDataSource
import com.sucharu.sucharupro.data.repository.ReturnRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementDirection
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementLedgerType
import com.sucharu.sucharupro.domain.model.returns.ReturnItem
import com.sucharu.sucharupro.domain.model.returns.ReturnReason
import com.sucharu.sucharupro.domain.model.returns.ReturnReceivingInfo
import com.sucharu.sucharupro.domain.model.returns.ReturnRequest
import com.sucharu.sucharupro.domain.model.returns.ReturnStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReturnReconciliationPersistenceTest {

    private lateinit var returnDataSource: FakeReturnDataSource
    private lateinit var receivingDataSource: FakeInventoryReceivingDataSource
    private lateinit var ledgerDataSource: FakeInventoryMovementLedgerDataSource
    private lateinit var repository: ReturnRepositoryImpl

    private val testReturn = ReturnRequest(
        returnId = "RET-RECON-01",
        projectId = "PRJ-01",
        returnNo = "RET-2026-001",
        customerId = "CUST-01",
        originalChallanId = "CH-01",
        status = ReturnStatus.RETURN_RECEIVED,
        reason = ReturnReason.PRINTING_DEFECT,
        requestedBy = "user-1",
        version = 4L
    )

    private val testItem = ReturnItem(
        returnItemId = "RI-01",
        returnId = "RET-RECON-01",
        productId = "PROD-FINISHED-01",
        originalChallanItemId = "CHI-01",
        requestedQuantity = 10,
        acceptedQuantity = 10,
        rejectedQuantity = 0
    )

    private val testReceiving = ReturnReceivingInfo(
        receivingEventId = "RCV-EVENT-01",
        returnId = "RET-RECON-01",
        projectId = "PRJ-01",
        receiverId = "wh-user-1",
        approvedQty = 10,
        actualQty = 10,
        acceptedQty = 7,
        rejectedQty = 2,
        damagedQty = 1,
        mismatchFlag = false,
        version = 1L,
        idempotencyKey = "IDEMP-RCV-01"
    )

    @Before
    fun setup() = runBlocking {
        returnDataSource = FakeReturnDataSource()
        receivingDataSource = FakeInventoryReceivingDataSource()
        ledgerDataSource = FakeInventoryMovementLedgerDataSource()
        repository = ReturnRepositoryImpl(
            dataSource = returnDataSource,
            inventoryReceivingDataSource = receivingDataSource,
            inventoryLedgerDataSource = ledgerDataSource
        )

        returnDataSource.insertReturn(testReturn, listOf(testItem))
        returnDataSource.insertOrUpdateReceiving(testReceiving)
    }

    @Test
    fun `reconciliation with accepted quantity creates StockInRecord, ledger entry, and marks return PROCESSED`() = runBlocking {
        val result = repository.reconcileInventoryAndProcess(
            returnId = testReturn.returnId,
            warehouseId = "WH-MAIN",
            locationId = "LOC-A1",
            actorId = "wh-user-1",
            expectedVersion = 4L,
            callerRole = UserRole.WAREHOUSE,
            callerProjectId = testReturn.projectId
        )

        assertTrue("Reconciliation should succeed", result is DomainResult.Success)
        val reconciliation = (result as DomainResult.Success).data

        assertEquals(testReturn.returnId, reconciliation.returnId)
        assertEquals(testReceiving.receivingEventId, reconciliation.receivingEventId)
        assertEquals(7, reconciliation.acceptedQty)
        assertTrue(reconciliation.inventoryMutationApplied)
        assertNotNull(reconciliation.stockInRecordId)
        assertNotNull(reconciliation.ledgerEntryId)
        assertEquals(ReturnStatus.PROCESSED, reconciliation.resultingStatus)

        // Verify updated ReturnRequest
        val updatedReturn = returnDataSource.getReturn(testReturn.returnId)
        assertNotNull(updatedReturn)
        assertEquals(ReturnStatus.PROCESSED, updatedReturn!!.status)
        assertEquals(5L, updatedReturn.version)

        // Verify canonical StockInRecord created
        val stockInRecords = receivingDataSource.observeStockInRecords().first()
        assertEquals(1, stockInRecords.size)
        val stockIn = stockInRecords.first()
        assertEquals(reconciliation.stockInRecordId, stockIn.stockInId)
        assertEquals(testReturn.returnId, stockIn.receivingId)
        assertEquals(testItem.returnItemId, stockIn.receivingLineId)
        assertEquals("PROD-FINISHED-01", stockIn.inventoryProductId)
        assertEquals("WH-MAIN", stockIn.warehouseId)
        assertEquals("LOC-A1", stockIn.locationId)
        assertEquals(7, stockIn.quantity)
        assertEquals(InventoryUnit.PCS, stockIn.unit)
        assertEquals("RETURN:${testReturn.returnNo}", stockIn.sourceReference)

        // Verify canonical Movement Ledger Entry created
        val ledgerEntries = ledgerDataSource.getEntries(testReturn.projectId)
        assertEquals(1, ledgerEntries.size)
        val ledgerEntry = ledgerEntries.first()
        assertEquals(reconciliation.ledgerEntryId, ledgerEntry.ledgerEntryId)
        assertEquals(InventoryMovementLedgerType.STOCK_IN, ledgerEntry.movementType)
        assertEquals(InventoryMovementDirection.IN, ledgerEntry.direction)
        assertEquals(7.0, ledgerEntry.quantity, 0.001)
        assertEquals(testReturn.returnId, ledgerEntry.referenceId)
        assertEquals("RECEIVING", ledgerEntry.referenceType)
        assertEquals(stockIn.stockInId, ledgerEntry.sourceMovementId)

        // Verify persisted reconciliation query
        val fetchedRecon = repository.getReconciliationResult(testReturn.returnId, UserRole.WAREHOUSE, testReturn.projectId)
        assertTrue(fetchedRecon is DomainResult.Success)
        assertEquals(reconciliation, (fetchedRecon as DomainResult.Success).data)
    }

    @Test
    fun `reconciliation with zero accepted quantity creates NO StockInRecord, NO ledger entry, but advances to PROCESSED`() = runBlocking {
        val zeroReceiving = testReceiving.copy(
            receivingEventId = "RCV-ZERO-01",
            acceptedQty = 0,
            rejectedQty = 8,
            damagedQty = 2
        )
        returnDataSource.insertOrUpdateReceiving(zeroReceiving)

        val result = repository.reconcileInventoryAndProcess(
            returnId = testReturn.returnId,
            warehouseId = "WH-MAIN",
            locationId = "LOC-A1",
            actorId = "wh-user-1",
            expectedVersion = 4L,
            callerRole = UserRole.WAREHOUSE,
            callerProjectId = testReturn.projectId
        )

        assertTrue("Reconciliation should succeed", result is DomainResult.Success)
        val reconciliation = (result as DomainResult.Success).data

        assertEquals(0, reconciliation.acceptedQty)
        assertFalse(reconciliation.inventoryMutationApplied)
        assertNull(reconciliation.stockInRecordId)
        assertNull(reconciliation.ledgerEntryId)
        assertEquals(ReturnStatus.PROCESSED, reconciliation.resultingStatus)

        // Verify return status updated to PROCESSED
        val updatedReturn = returnDataSource.getReturn(testReturn.returnId)
        assertNotNull(updatedReturn)
        assertEquals(ReturnStatus.PROCESSED, updatedReturn!!.status)
        assertEquals(5L, updatedReturn.version)

        // Verify NO stock-in records or ledger entries were created
        val stockInRecords = receivingDataSource.observeStockInRecords().first()
        assertTrue("No stock-in records should exist", stockInRecords.isEmpty())

        val ledgerEntries = ledgerDataSource.getEntries(testReturn.projectId)
        assertTrue("No ledger entries should exist", ledgerEntries.isEmpty())
    }

    @Test
    fun `rejected and damaged quantities are excluded from finished product inventory`() = runBlocking {
        // Total actual = 10, accepted = 4, rejected = 3, damaged = 3
        val partialReceiving = testReceiving.copy(
            receivingEventId = "RCV-EXCLUDE-01",
            approvedQty = 10,
            actualQty = 10,
            acceptedQty = 4,
            rejectedQty = 3,
            damagedQty = 3
        )
        returnDataSource.insertOrUpdateReceiving(partialReceiving)

        val result = repository.reconcileInventoryAndProcess(
            returnId = testReturn.returnId,
            warehouseId = "WH-DEFECT-ISOLATION",
            locationId = "LOC-B2",
            actorId = "wh-user-1",
            expectedVersion = 4L,
            callerRole = UserRole.ADMIN,
            callerProjectId = testReturn.projectId
        )

        assertTrue(result is DomainResult.Success)
        val reconciliation = (result as DomainResult.Success).data
        assertEquals(4, reconciliation.acceptedQty)

        val stockIn = receivingDataSource.observeStockInRecords().first().first()
        assertEquals("Stock-in quantity must strictly equal acceptedQty", 4, stockIn.quantity)

        val ledgerEntry = ledgerDataSource.getEntries(testReturn.projectId).first()
        assertEquals(4.0, ledgerEntry.quantity, 0.001)
    }
}

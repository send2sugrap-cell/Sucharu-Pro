package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.data.datasource.FakeInventoryMovementLedgerDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryReceivingDataSource
import com.sucharu.sucharupro.data.datasource.FakeReturnDataSource
import com.sucharu.sucharupro.data.repository.ReturnRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.returns.ReturnItem
import com.sucharu.sucharupro.domain.model.returns.ReturnReason
import com.sucharu.sucharupro.domain.model.returns.ReturnReceivingInfo
import com.sucharu.sucharupro.domain.model.returns.ReturnRequest
import com.sucharu.sucharupro.domain.model.returns.ReturnStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReturnReconciliationIdempotencyTest {

    private lateinit var returnDataSource: FakeReturnDataSource
    private lateinit var receivingDataSource: FakeInventoryReceivingDataSource
    private lateinit var ledgerDataSource: FakeInventoryMovementLedgerDataSource
    private lateinit var repository: ReturnRepositoryImpl

    private val testReturn = ReturnRequest(
        returnId = "RET-IDEMP-01",
        projectId = "PRJ-01",
        returnNo = "RET-2026-001",
        customerId = "CUST-01",
        originalChallanId = "CH-01",
        status = ReturnStatus.RETURN_RECEIVED,
        reason = ReturnReason.PRINTING_DEFECT,
        requestedBy = "user-1",
        version = 2L
    )

    private val testItem = ReturnItem(
        returnItemId = "RI-01",
        returnId = "RET-IDEMP-01",
        productId = "PROD-01",
        originalChallanItemId = "CHI-01",
        requestedQuantity = 5,
        acceptedQuantity = 5,
        rejectedQuantity = 0
    )

    private val testReceiving = ReturnReceivingInfo(
        receivingEventId = "RCV-IDEMP-01",
        returnId = "RET-IDEMP-01",
        projectId = "PRJ-01",
        receiverId = "wh-user-1",
        approvedQty = 5,
        actualQty = 5,
        acceptedQty = 5,
        rejectedQty = 0,
        damagedQty = 0,
        mismatchFlag = false,
        version = 1L,
        idempotencyKey = "IDEMP-KEY-999"
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
    fun `replaying reconciliation on already PROCESSED return returns existing result without duplicating stock-in or ledger`() = runBlocking {
        // First call
        val firstResult = repository.reconcileInventoryAndProcess(
            returnId = testReturn.returnId,
            warehouseId = "WH-01",
            locationId = "LOC-01",
            actorId = "wh-user-1",
            expectedVersion = 2L,
            callerRole = UserRole.WAREHOUSE,
            callerProjectId = testReturn.projectId
        )
        assertTrue(firstResult is DomainResult.Success)
        val firstData = (firstResult as DomainResult.Success).data

        assertEquals(1, receivingDataSource.observeStockInRecords().first().size)
        assertEquals(1, ledgerDataSource.getEntries(testReturn.projectId).size)

        // Second call (replay)
        val secondResult = repository.reconcileInventoryAndProcess(
            returnId = testReturn.returnId,
            warehouseId = "WH-01",
            locationId = "LOC-01",
            actorId = "wh-user-1",
            expectedVersion = 3L, // or any version
            callerRole = UserRole.WAREHOUSE,
            callerProjectId = testReturn.projectId
        )
        assertTrue(secondResult is DomainResult.Success)
        val secondData = (secondResult as DomainResult.Success).data

        // Must return identical reconciliation result
        assertEquals(firstData.stockInRecordId, secondData.stockInRecordId)
        assertEquals(firstData.ledgerEntryId, secondData.ledgerEntryId)
        assertEquals(firstData.receivingEventId, secondData.receivingEventId)

        // Verifying NO duplicate StockInRecord was created
        assertEquals(1, receivingDataSource.observeStockInRecords().first().size)

        // Verifying NO duplicate Movement Ledger Entry was created
        assertEquals(1, ledgerDataSource.getEntries(testReturn.projectId).size)

        // Verifying version remained 3L (no double increment)
        val returnAfter = returnDataSource.getReturn(testReturn.returnId)
        assertEquals(3L, returnAfter!!.version)
    }
}

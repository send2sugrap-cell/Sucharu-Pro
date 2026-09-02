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
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReturnReconciliationConcurrencyTest {

    private lateinit var returnDataSource: FakeReturnDataSource
    private lateinit var receivingDataSource: FakeInventoryReceivingDataSource
    private lateinit var ledgerDataSource: FakeInventoryMovementLedgerDataSource
    private lateinit var repository: ReturnRepositoryImpl

    private val testReturn = ReturnRequest(
        returnId = "RET-CONC-01",
        projectId = "PRJ-01",
        returnNo = "RET-2026-001",
        customerId = "CUST-01",
        originalChallanId = "CH-01",
        status = ReturnStatus.RETURN_RECEIVED,
        reason = ReturnReason.PRINTING_DEFECT,
        requestedBy = "user-1",
        version = 3L
    )

    private val testItem = ReturnItem(
        returnItemId = "RI-01",
        returnId = "RET-CONC-01",
        productId = "PROD-01",
        originalChallanItemId = "CHI-01",
        requestedQuantity = 10,
        acceptedQuantity = 10,
        rejectedQuantity = 0
    )

    private val testReceiving = ReturnReceivingInfo(
        receivingEventId = "RCV-CONC-01",
        returnId = "RET-CONC-01",
        projectId = "PRJ-01",
        receiverId = "wh-user-1",
        approvedQty = 10,
        actualQty = 10,
        acceptedQty = 10,
        rejectedQty = 0,
        damagedQty = 0,
        mismatchFlag = false,
        version = 1L,
        idempotencyKey = "IDEMP-CONC-01"
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
    fun `reconciliation with stale expectedVersion fails with concurrency error`() = runBlocking {
        val result = repository.reconcileInventoryAndProcess(
            returnId = testReturn.returnId,
            warehouseId = "WH-01",
            locationId = "LOC-01",
            actorId = "wh-user-1",
            expectedVersion = 2L, // Current version is 3L
            callerRole = UserRole.WAREHOUSE,
            callerProjectId = testReturn.projectId
        )

        assertTrue("Stale version must fail", result is DomainResult.Error)
        val errorMsg = (result as DomainResult.Error).message
        assertTrue(errorMsg.contains("Concurrency conflict"))
        assertTrue(errorMsg.contains("expected version 2 but found 3"))

        // Verify status was NOT changed
        val returnAfterFailed = returnDataSource.getReturn(testReturn.returnId)
        assertEquals(ReturnStatus.RETURN_RECEIVED, returnAfterFailed!!.status)
        assertEquals(3L, returnAfterFailed.version)
    }

    @Test
    fun `successful reconciliation increments version from 3 to 4`() = runBlocking {
        val result = repository.reconcileInventoryAndProcess(
            returnId = testReturn.returnId,
            warehouseId = "WH-01",
            locationId = "LOC-01",
            actorId = "wh-user-1",
            expectedVersion = 3L,
            callerRole = UserRole.WAREHOUSE,
            callerProjectId = testReturn.projectId
        )

        assertTrue("Matching version must succeed", result is DomainResult.Success)

        val returnAfter = returnDataSource.getReturn(testReturn.returnId)
        assertEquals(ReturnStatus.PROCESSED, returnAfter!!.status)
        assertEquals(4L, returnAfter.version)
    }
}

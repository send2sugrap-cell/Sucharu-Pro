package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.data.datasource.FakeInventoryMovementLedgerDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryReceivingDataSource
import com.sucharu.sucharupro.data.datasource.FakeReturnDataSource
import com.sucharu.sucharupro.data.repository.ReturnRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.returns.ReturnActivityType
import com.sucharu.sucharupro.domain.model.returns.ReturnItem
import com.sucharu.sucharupro.domain.model.returns.ReturnReason
import com.sucharu.sucharupro.domain.model.returns.ReturnReceivingInfo
import com.sucharu.sucharupro.domain.model.returns.ReturnRequest
import com.sucharu.sucharupro.domain.model.returns.ReturnStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReturnReconciliationAuditTest {

    private lateinit var returnDataSource: FakeReturnDataSource
    private lateinit var receivingDataSource: FakeInventoryReceivingDataSource
    private lateinit var ledgerDataSource: FakeInventoryMovementLedgerDataSource
    private lateinit var repository: ReturnRepositoryImpl

    private val testReturn = ReturnRequest(
        returnId = "RET-AUDIT-01",
        projectId = "PRJ-01",
        returnNo = "RET-2026-001",
        customerId = "CUST-01",
        originalChallanId = "CH-01",
        status = ReturnStatus.RETURN_RECEIVED,
        reason = ReturnReason.PRINTING_DEFECT,
        requestedBy = "user-1",
        version = 1L
    )

    private val testItem = ReturnItem(
        returnItemId = "RI-01",
        returnId = "RET-AUDIT-01",
        productId = "PROD-01",
        originalChallanItemId = "CHI-01",
        requestedQuantity = 5,
        acceptedQuantity = 5,
        rejectedQuantity = 0
    )

    private val testReceiving = ReturnReceivingInfo(
        receivingEventId = "RCV-AUDIT-01",
        returnId = "RET-AUDIT-01",
        projectId = "PRJ-01",
        receiverId = "wh-user-1",
        approvedQty = 5,
        actualQty = 5,
        acceptedQty = 5,
        rejectedQty = 0,
        damagedQty = 0,
        mismatchFlag = false,
        version = 1L,
        idempotencyKey = "IDEMP-AUDIT-01"
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
    fun `reconciliation records RETURN_PROCESSED activity event with full audit metadata`() = runBlocking {
        val result = repository.reconcileInventoryAndProcess(
            returnId = testReturn.returnId,
            warehouseId = "WH-01",
            locationId = "LOC-01",
            actorId = "wh-auditor-1",
            expectedVersion = 1L,
            callerRole = UserRole.WAREHOUSE,
            callerProjectId = testReturn.projectId
        )

        assertTrue(result is DomainResult.Success)

        val history = repository.observeAuditHistory(testReturn.returnId).first()
        val processedEvent = history.find { it.activityType == ReturnActivityType.RETURN_PROCESSED }

        assertNotNull("RETURN_PROCESSED activity event must be recorded", processedEvent)
        assertEquals("wh-auditor-1", processedEvent!!.actorId)
        assertEquals(UserRole.WAREHOUSE, processedEvent.actorRole)
        assertEquals(ReturnStatus.RETURN_RECEIVED, processedEvent.previousStatus)
        assertEquals(ReturnStatus.PROCESSED, processedEvent.newStatus)

        assertEquals("RCV-AUDIT-01", processedEvent.metadata["receivingEventId"])
        assertEquals("5", processedEvent.metadata["acceptedQty"])
        assertEquals("true", processedEvent.metadata["inventoryMutationApplied"])
        assertNotNull(processedEvent.metadata["stockInRecordId"])
        assertNotNull(processedEvent.metadata["ledgerEntryId"])
        assertEquals("WH-01", processedEvent.metadata["warehouseId"])
        assertEquals("LOC-01", processedEvent.metadata["locationId"])
    }
}

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

class ReturnReconciliationBoundaryTest {

    private lateinit var returnDataSource: FakeReturnDataSource
    private lateinit var receivingDataSource: FakeInventoryReceivingDataSource
    private lateinit var ledgerDataSource: FakeInventoryMovementLedgerDataSource
    private lateinit var repository: ReturnRepositoryImpl

    private val testReturn = ReturnRequest(
        returnId = "RET-BOUND-01",
        projectId = "PRJ-01",
        returnNo = "RET-2026-001",
        customerId = "CUST-01",
        originalChallanId = "CHALLAN-IMMUTABLE-999",
        status = ReturnStatus.RETURN_RECEIVED,
        reason = ReturnReason.PRINTING_DEFECT,
        requestedBy = "user-1",
        version = 1L
    )

    private val testItem = ReturnItem(
        returnItemId = "RI-01",
        returnId = "RET-BOUND-01",
        productId = "PROD-01",
        originalChallanItemId = "CHI-01",
        requestedQuantity = 5,
        acceptedQuantity = 5,
        rejectedQuantity = 0
    )

    private val testReceiving = ReturnReceivingInfo(
        receivingEventId = "RCV-BOUND-01",
        returnId = "RET-BOUND-01",
        projectId = "PRJ-01",
        receiverId = "wh-user-1",
        approvedQty = 5,
        actualQty = 5,
        acceptedQty = 5,
        rejectedQty = 0,
        damagedQty = 0,
        mismatchFlag = false,
        version = 1L,
        idempotencyKey = "IDEMP-BOUND-01"
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
    fun `originalChallanId remains read-only and unmutated during reconciliation`() = runBlocking {
        val result = repository.reconcileInventoryAndProcess(
            returnId = testReturn.returnId,
            warehouseId = "WH-01",
            locationId = "LOC-01",
            actorId = "wh-user-1",
            expectedVersion = 1L,
            callerRole = UserRole.WAREHOUSE,
            callerProjectId = testReturn.projectId
        )

        assertTrue(result is DomainResult.Success)

        val updatedReturn = returnDataSource.getReturn(testReturn.returnId)
        assertEquals(
            "originalChallanId must remain untouched",
            "CHALLAN-IMMUTABLE-999",
            updatedReturn!!.originalChallanId
        )
    }

    @Test
    fun `reconciliation on return in non-RETURN_RECEIVED status is rejected`() = runBlocking {
        listOf(
            ReturnStatus.REQUESTED,
            ReturnStatus.UNDER_INSPECTION,
            ReturnStatus.APPROVED,
            ReturnStatus.REJECTED,
            ReturnStatus.CANCELLED
        ).forEach { nonReceivedStatus ->
            val invalidReturn = testReturn.copy(
                returnId = "RET-${nonReceivedStatus.name}",
                status = nonReceivedStatus,
                version = 1L
            )
            val receiving = testReceiving.copy(
                receivingEventId = "RCV-${nonReceivedStatus.name}",
                returnId = invalidReturn.returnId,
                idempotencyKey = "IDEMP-${nonReceivedStatus.name}"
            )
            returnDataSource.insertReturn(invalidReturn, listOf(testItem))
            returnDataSource.insertOrUpdateReceiving(receiving)

            val result = repository.reconcileInventoryAndProcess(
                returnId = invalidReturn.returnId,
                warehouseId = "WH-01",
                locationId = "LOC-01",
                actorId = "wh-user-1",
                expectedVersion = 1L,
                callerRole = UserRole.WAREHOUSE,
                callerProjectId = testReturn.projectId
            )

            assertTrue(
                "Return in status $nonReceivedStatus cannot be reconciled",
                result is DomainResult.Error
            )
        }
    }

    @Test
    fun `reconciliation fails if physical receiving record does not exist`() = runBlocking {
        val noReceivingReturn = testReturn.copy(
            returnId = "RET-NO-RCV",
            version = 1L
        )
        returnDataSource.insertReturn(noReceivingReturn, listOf(testItem))

        val result = repository.reconcileInventoryAndProcess(
            returnId = noReceivingReturn.returnId,
            warehouseId = "WH-01",
            locationId = "LOC-01",
            actorId = "wh-user-1",
            expectedVersion = 1L,
            callerRole = UserRole.WAREHOUSE,
            callerProjectId = testReturn.projectId
        )

        assertTrue("Missing receiving record must fail reconciliation", result is DomainResult.Error)
        val errorMsg = (result as DomainResult.Error).message
        assertTrue(errorMsg.contains("No receiving record found"))
    }

    @Test
    fun `blank warehouseId or locationId with acceptedQty greater than 0 is rejected`() = runBlocking {
        val blankWhResult = repository.reconcileInventoryAndProcess(
            returnId = testReturn.returnId,
            warehouseId = "   ",
            locationId = "LOC-01",
            actorId = "wh-user-1",
            expectedVersion = 1L,
            callerRole = UserRole.WAREHOUSE,
            callerProjectId = testReturn.projectId
        )
        assertTrue("Blank warehouseId must be rejected", blankWhResult is DomainResult.Error)

        val blankLocResult = repository.reconcileInventoryAndProcess(
            returnId = testReturn.returnId,
            warehouseId = "WH-01",
            locationId = "",
            actorId = "wh-user-1",
            expectedVersion = 1L,
            callerRole = UserRole.WAREHOUSE,
            callerProjectId = testReturn.projectId
        )
        assertTrue("Blank locationId must be rejected", blankLocResult is DomainResult.Error)
    }
}

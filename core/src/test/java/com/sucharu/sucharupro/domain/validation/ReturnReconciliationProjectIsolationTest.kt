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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReturnReconciliationProjectIsolationTest {

    private lateinit var returnDataSource: FakeReturnDataSource
    private lateinit var receivingDataSource: FakeInventoryReceivingDataSource
    private lateinit var ledgerDataSource: FakeInventoryMovementLedgerDataSource
    private lateinit var repository: ReturnRepositoryImpl

    private val projectA = "PRJ-A"
    private val projectB = "PRJ-B"

    private val testReturnA = ReturnRequest(
        returnId = "RET-ISOLATION-A",
        projectId = projectA,
        returnNo = "RET-2026-A",
        customerId = "CUST-A",
        originalChallanId = "CH-A",
        status = ReturnStatus.RETURN_RECEIVED,
        reason = ReturnReason.PRINTING_DEFECT,
        requestedBy = "user-A",
        version = 1L
    )

    private val testItemA = ReturnItem(
        returnItemId = "RI-A",
        returnId = "RET-ISOLATION-A",
        productId = "PROD-A",
        originalChallanItemId = "CHI-A",
        requestedQuantity = 5,
        acceptedQuantity = 5,
        rejectedQuantity = 0
    )

    private val testReceivingA = ReturnReceivingInfo(
        receivingEventId = "RCV-A",
        returnId = "RET-ISOLATION-A",
        projectId = projectA,
        receiverId = "wh-user-A",
        approvedQty = 5,
        actualQty = 5,
        acceptedQty = 5,
        rejectedQty = 0,
        damagedQty = 0,
        mismatchFlag = false,
        version = 1L,
        idempotencyKey = "IDEMP-ISO-A"
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

        returnDataSource.insertReturn(testReturnA, listOf(testItemA))
        returnDataSource.insertOrUpdateReceiving(testReceivingA)
    }

    @Test
    fun `cross project reconcile call is rejected`() = runBlocking {
        val res = repository.reconcileInventoryAndProcess(
            returnId = testReturnA.returnId,
            warehouseId = "WH-01",
            locationId = "LOC-01",
            actorId = "wh-user-B",
            expectedVersion = 1L,
            callerRole = UserRole.WAREHOUSE,
            callerProjectId = projectB // Different project
        )

        assertTrue("Cross project reconciliation must be rejected", res is DomainResult.Error)
    }

    @Test
    fun `cross project getReconciliationResult is rejected`() = runBlocking {
        // Reconcile under Project A first
        repository.reconcileInventoryAndProcess(
            returnId = testReturnA.returnId,
            warehouseId = "WH-01",
            locationId = "LOC-01",
            actorId = "wh-user-A",
            expectedVersion = 1L,
            callerRole = UserRole.WAREHOUSE,
            callerProjectId = projectA
        )

        // Query under Project B
        val queryRes = repository.getReconciliationResult(
            returnId = testReturnA.returnId,
            callerRole = UserRole.WAREHOUSE,
            callerProjectId = projectB
        )

        assertTrue("Cross project query must be rejected", queryRes is DomainResult.Error)
    }
}

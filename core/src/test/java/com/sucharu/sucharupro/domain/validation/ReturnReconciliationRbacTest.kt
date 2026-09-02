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

class ReturnReconciliationRbacTest {

    private lateinit var returnDataSource: FakeReturnDataSource
    private lateinit var receivingDataSource: FakeInventoryReceivingDataSource
    private lateinit var ledgerDataSource: FakeInventoryMovementLedgerDataSource
    private lateinit var repository: ReturnRepositoryImpl

    private val testReturn = ReturnRequest(
        returnId = "RET-RBAC-01",
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
        returnId = "RET-RBAC-01",
        productId = "PROD-01",
        originalChallanItemId = "CHI-01",
        requestedQuantity = 5,
        acceptedQuantity = 5,
        rejectedQuantity = 0
    )

    private val testReceiving = ReturnReceivingInfo(
        receivingEventId = "RCV-RBAC-01",
        returnId = "RET-RBAC-01",
        projectId = "PRJ-01",
        receiverId = "wh-user-1",
        approvedQty = 5,
        actualQty = 5,
        acceptedQty = 5,
        rejectedQty = 0,
        damagedQty = 0,
        mismatchFlag = false,
        version = 1L,
        idempotencyKey = "IDEMP-RBAC-01"
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
    fun `authorized roles WAREHOUSE, ADMIN, and MANAGER can reconcile return`() = runBlocking {
        listOf(UserRole.WAREHOUSE, UserRole.ADMIN, UserRole.MANAGER).forEach { role ->
            val returnId = "RET-AUTH-${role.name}"
            val ret = testReturn.copy(returnId = returnId, version = 1L)
            val receiving = testReceiving.copy(
                receivingEventId = "RCV-${role.name}",
                returnId = returnId,
                idempotencyKey = "IDEMP-${role.name}"
            )
            val item = testItem.copy(returnItemId = "RI-${role.name}", returnId = returnId)

            returnDataSource.insertReturn(ret, listOf(item))
            returnDataSource.insertOrUpdateReceiving(receiving)

            val result = repository.reconcileInventoryAndProcess(
                returnId = returnId,
                warehouseId = "WH-01",
                locationId = "LOC-01",
                actorId = "actor-${role.name}",
                expectedVersion = 1L,
                callerRole = role,
                callerProjectId = testReturn.projectId
            )

            assertTrue("Role $role should be authorized to reconcile return", result is DomainResult.Success)
        }
    }

    @Test
    fun `unauthorized roles are rejected`() = runBlocking {
        listOf(
            UserRole.CUSTOMER,
            UserRole.STAFF,
            UserRole.DESIGNER,
            UserRole.QC_INSPECTOR,
            UserRole.ACCOUNTS,
            UserRole.VENDOR,
            UserRole.AFFILIATE
        ).forEach { role ->
            val result = repository.reconcileInventoryAndProcess(
                returnId = testReturn.returnId,
                warehouseId = "WH-01",
                locationId = "LOC-01",
                actorId = "actor-unauth",
                expectedVersion = 1L,
                callerRole = role,
                callerProjectId = testReturn.projectId
            )

            assertTrue("Role $role must be rejected", result is DomainResult.Error)
        }
    }
}

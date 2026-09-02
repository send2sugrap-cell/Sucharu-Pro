package com.sucharu.sucharupro.domain.validation

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

class ReturnReceivingRbacTest {

    private lateinit var dataSource: FakeReturnDataSource
    private lateinit var repository: ReturnRepositoryImpl

    private val testReturn = ReturnRequest(
        returnId = "RET-RBAC-01",
        projectId = "PRJ-01",
        returnNo = "RET-2026-001",
        customerId = "CUST-01",
        originalChallanId = "CH-01",
        status = ReturnStatus.APPROVED,
        reason = ReturnReason.PRINTING_DEFECT,
        requestedBy = "user-1",
        version = 3L
    )

    private val testItem = ReturnItem(
        returnItemId = "RI-01",
        returnId = "RET-RBAC-01",
        productId = "PROD-01",
        originalChallanItemId = "CHI-01",
        requestedQuantity = 10,
        acceptedQuantity = 10,
        rejectedQuantity = 0
    )

    private fun sampleReceiving(key: String) = ReturnReceivingInfo(
        receivingEventId = "RCV-$key",
        returnId = testReturn.returnId,
        projectId = testReturn.projectId,
        receiverId = "user-1",
        approvedQty = 10,
        actualQty = 10,
        acceptedQty = 10,
        rejectedQty = 0,
        damagedQty = 0,
        mismatchFlag = false,
        version = 1L,
        idempotencyKey = "IDEMP-$key"
    )

    @Before
    fun setup() = runBlocking {
        dataSource = FakeReturnDataSource()
        repository = ReturnRepositoryImpl(dataSource)
        dataSource.insertReturn(testReturn, listOf(testItem))
    }

    @Test
    fun `ADMIN, MANAGER, and WAREHOUSE roles are permitted to receive return`() = runBlocking {
        val allowedRoles = listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.WAREHOUSE)

        for (role in allowedRoles) {
            val returnItem = testReturn.copy(returnId = "RET-RBAC-$role", status = ReturnStatus.APPROVED)
            dataSource.insertReturn(returnItem, listOf(testItem.copy(returnId = returnItem.returnId)))

            val receiving = sampleReceiving(role.name).copy(returnId = returnItem.returnId)
            val res = repository.receiveReturn(
                receivingInfo = receiving,
                actorId = "user-$role",
                expectedVersion = returnItem.version,
                callerRole = role,
                callerProjectId = returnItem.projectId
            )
            assertTrue("Role $role must be allowed to receive return", res is DomainResult.Success)
        }
    }

    @Test
    fun `unauthorized roles cannot receive return`() = runBlocking {
        val disallowedRoles = listOf(
            UserRole.QC_INSPECTOR,
            UserRole.STAFF,
            UserRole.ACCOUNTS,
            UserRole.DESIGNER,
            UserRole.CUSTOMER,
            UserRole.VENDOR,
            UserRole.AFFILIATE
        )

        for (role in disallowedRoles) {
            val returnItem = testReturn.copy(returnId = "RET-RBAC-DIS-$role", status = ReturnStatus.APPROVED)
            dataSource.insertReturn(returnItem, listOf(testItem.copy(returnId = returnItem.returnId)))

            val receiving = sampleReceiving("DIS-$role").copy(returnId = returnItem.returnId)
            val res = repository.receiveReturn(
                receivingInfo = receiving,
                actorId = "user-$role",
                expectedVersion = returnItem.version,
                callerRole = role,
                callerProjectId = returnItem.projectId
            )
            assertTrue("Role $role must be rejected from receiving return", res is DomainResult.Error)
        }
    }
}

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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReturnReceivingIdempotencyTest {

    private lateinit var dataSource: FakeReturnDataSource
    private lateinit var repository: ReturnRepositoryImpl

    private val testReturn = ReturnRequest(
        returnId = "RET-IDEMP-01",
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
        returnId = "RET-IDEMP-01",
        productId = "PROD-01",
        originalChallanItemId = "CHI-01",
        requestedQuantity = 10,
        acceptedQuantity = 10,
        rejectedQuantity = 0
    )

    @Before
    fun setup() = runBlocking {
        dataSource = FakeReturnDataSource()
        repository = ReturnRepositoryImpl(dataSource)
        dataSource.insertReturn(testReturn, listOf(testItem))
    }

    @Test
    fun `replaying receiveReturn with same idempotencyKey is idempotent and succeeds`() = runBlocking {
        val receiving = ReturnReceivingInfo(
            receivingEventId = "RCV-IDEMP-01",
            returnId = testReturn.returnId,
            projectId = testReturn.projectId,
            receiverId = "warehouse-1",
            approvedQty = 10,
            actualQty = 10,
            acceptedQty = 10,
            rejectedQty = 0,
            damagedQty = 0,
            mismatchFlag = false,
            version = 1L,
            idempotencyKey = "IDEMP-KEY-999"
        )

        val firstRes = repository.receiveReturn(
            receivingInfo = receiving,
            actorId = "warehouse-1",
            expectedVersion = testReturn.version,
            callerRole = UserRole.WAREHOUSE,
            callerProjectId = testReturn.projectId
        )
        assertTrue(firstRes is DomainResult.Success)

        // Replay call with same idempotencyKey
        val replayRes = repository.receiveReturn(
            receivingInfo = receiving,
            actorId = "warehouse-1",
            expectedVersion = testReturn.version,
            callerRole = UserRole.WAREHOUSE,
            callerProjectId = testReturn.projectId
        )
        assertTrue("Replay with same idempotency key must succeed", replayRes is DomainResult.Success)
        assertEquals(1, dataSource.countReceivings())
    }

    @Test
    fun `subsequent receiveReturn with different idempotencyKey on already received return is rejected`() = runBlocking {
        val receiving1 = ReturnReceivingInfo(
            receivingEventId = "RCV-IDEMP-01",
            returnId = testReturn.returnId,
            projectId = testReturn.projectId,
            receiverId = "warehouse-1",
            approvedQty = 10,
            actualQty = 10,
            acceptedQty = 10,
            rejectedQty = 0,
            damagedQty = 0,
            mismatchFlag = false,
            version = 1L,
            idempotencyKey = "IDEMP-KEY-AAA"
        )

        val firstRes = repository.receiveReturn(
            receivingInfo = receiving1,
            actorId = "warehouse-1",
            expectedVersion = testReturn.version,
            callerRole = UserRole.WAREHOUSE,
            callerProjectId = testReturn.projectId
        )
        assertTrue(firstRes is DomainResult.Success)

        val receiving2 = receiving1.copy(
            receivingEventId = "RCV-IDEMP-02",
            idempotencyKey = "IDEMP-KEY-BBB"
        )

        val secondRes = repository.receiveReturn(
            receivingInfo = receiving2,
            actorId = "warehouse-1",
            expectedVersion = testReturn.version + 1L,
            callerRole = UserRole.WAREHOUSE,
            callerProjectId = testReturn.projectId
        )
        assertTrue("Subsequent receive on already received return must be rejected", secondRes is DomainResult.Error)
    }

    @Test
    fun `reusing same idempotencyKey for different return is rejected`() = runBlocking {
        val return2 = testReturn.copy(returnId = "RET-IDEMP-02", returnNo = "RET-2026-002")
        dataSource.insertReturn(return2, listOf(testItem.copy(returnId = "RET-IDEMP-02")))

        val receiving1 = ReturnReceivingInfo(
            receivingEventId = "RCV-IDEMP-01",
            returnId = testReturn.returnId,
            projectId = testReturn.projectId,
            receiverId = "warehouse-1",
            approvedQty = 10,
            actualQty = 10,
            acceptedQty = 10,
            rejectedQty = 0,
            damagedQty = 0,
            mismatchFlag = false,
            version = 1L,
            idempotencyKey = "SHARED-KEY-123"
        )

        repository.receiveReturn(
            receivingInfo = receiving1,
            actorId = "warehouse-1",
            expectedVersion = testReturn.version,
            callerRole = UserRole.WAREHOUSE,
            callerProjectId = testReturn.projectId
        )

        val receiving2 = receiving1.copy(
            receivingEventId = "RCV-IDEMP-02",
            returnId = return2.returnId,
            idempotencyKey = "SHARED-KEY-123"
        )

        val res = repository.receiveReturn(
            receivingInfo = receiving2,
            actorId = "warehouse-1",
            expectedVersion = return2.version,
            callerRole = UserRole.WAREHOUSE,
            callerProjectId = return2.projectId
        )

        assertTrue("Reusing idempotency key for different return must be rejected", res is DomainResult.Error)
    }
}

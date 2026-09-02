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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReturnReceivingConcurrencyTest {

    private lateinit var dataSource: FakeReturnDataSource
    private lateinit var repository: ReturnRepositoryImpl

    private val testReturn = ReturnRequest(
        returnId = "RET-CONC-01",
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
        returnId = "RET-CONC-01",
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
    fun `receiveReturn fails with stale expectedVersion`() = runBlocking {
        val receiving = ReturnReceivingInfo(
            receivingEventId = "RCV-CONC-01",
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
            idempotencyKey = "IDEMP-CONC-01"
        )

        val res = repository.receiveReturn(
            receivingInfo = receiving,
            actorId = "warehouse-1",
            expectedVersion = 999L, // Stale version
            callerRole = UserRole.WAREHOUSE,
            callerProjectId = testReturn.projectId
        )

        assertTrue(res is DomainResult.Error)
        val error = res as DomainResult.Error
        assertTrue(error.message.contains("Concurrency conflict"))
    }

    @Test
    fun `concurrent receiveReturn calls result in exactly one success and rejection for the other`() = runBlocking {
        val receiving1 = ReturnReceivingInfo(
            receivingEventId = "RCV-CONC-A",
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
            idempotencyKey = "IDEMP-A"
        )

        val receiving2 = ReturnReceivingInfo(
            receivingEventId = "RCV-CONC-B",
            returnId = testReturn.returnId,
            projectId = testReturn.projectId,
            receiverId = "warehouse-2",
            approvedQty = 10,
            actualQty = 10,
            acceptedQty = 10,
            rejectedQty = 0,
            damagedQty = 0,
            mismatchFlag = false,
            version = 1L,
            idempotencyKey = "IDEMP-B"
        )

        val d1 = async {
            repository.receiveReturn(
                receivingInfo = receiving1,
                actorId = "warehouse-1",
                expectedVersion = testReturn.version,
                callerRole = UserRole.WAREHOUSE,
                callerProjectId = testReturn.projectId
            )
        }

        val d2 = async {
            repository.receiveReturn(
                receivingInfo = receiving2,
                actorId = "warehouse-2",
                expectedVersion = testReturn.version,
                callerRole = UserRole.WAREHOUSE,
                callerProjectId = testReturn.projectId
            )
        }

        val results = awaitAll(d1, d2)
        val successes = results.count { it is DomainResult.Success }
        val errors = results.count { it is DomainResult.Error }

        assertEquals("Exactly one concurrent receive must succeed", 1, successes)
        assertEquals("Exactly one concurrent receive must fail", 1, errors)
        assertEquals("Only one receiving record persisted", 1, dataSource.countReceivings())
    }
}

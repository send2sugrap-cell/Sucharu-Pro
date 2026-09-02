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

class ReturnReceivingProjectIsolationTest {

    private lateinit var dataSource: FakeReturnDataSource
    private lateinit var repository: ReturnRepositoryImpl

    private val testReturn = ReturnRequest(
        returnId = "RET-ISOL-01",
        projectId = "PRJ-ALPHA",
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
        returnId = "RET-ISOL-01",
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
    fun `cross project receiveReturn is rejected`() = runBlocking {
        val receiving = ReturnReceivingInfo(
            receivingEventId = "RCV-ISOL-01",
            returnId = testReturn.returnId,
            projectId = "PRJ-BETA", // Mismatched project
            receiverId = "warehouse-1",
            approvedQty = 10,
            actualQty = 10,
            acceptedQty = 10,
            rejectedQty = 0,
            damagedQty = 0,
            mismatchFlag = false,
            version = 1L,
            idempotencyKey = "IDEMP-ISOL-01"
        )

        val res = repository.receiveReturn(
            receivingInfo = receiving,
            actorId = "warehouse-1",
            expectedVersion = testReturn.version,
            callerRole = UserRole.WAREHOUSE,
            callerProjectId = "PRJ-BETA" // Cross-project caller
        )

        assertTrue("Cross project receive must be rejected", res is DomainResult.Error)
    }

    @Test
    fun `cross project getReceiving is rejected`() = runBlocking {
        val res = repository.getReceiving(
            returnId = testReturn.returnId,
            callerRole = UserRole.WAREHOUSE,
            callerProjectId = "PRJ-BETA" // Cross-project caller
        )

        assertTrue("Cross project getReceiving must be rejected", res is DomainResult.Error)
    }
}

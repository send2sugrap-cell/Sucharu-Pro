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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReturnReceivingPersistenceTest {

    private lateinit var dataSource: FakeReturnDataSource
    private lateinit var repository: ReturnRepositoryImpl

    private val testReturn = ReturnRequest(
        returnId = "RET-RCV-01",
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
        returnId = "RET-RCV-01",
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
    fun `receiveReturn successfully persists receiving info and transitions status to RETURN_RECEIVED`() = runBlocking {
        val receiving = ReturnReceivingInfo(
            receivingEventId = "RCV-01",
            returnId = testReturn.returnId,
            projectId = testReturn.projectId,
            receiverId = "warehouse-user-1",
            approvedQty = 10,
            actualQty = 10,
            acceptedQty = 8,
            rejectedQty = 1,
            damagedQty = 1,
            mismatchFlag = false,
            condition = "Carton slightly damaged",
            packaging = "Original box",
            damageNotes = "1 unit scratched",
            version = 1L,
            idempotencyKey = "IDEMP-RCV-01"
        )

        val res = repository.receiveReturn(
            receivingInfo = receiving,
            actorId = "warehouse-user-1",
            expectedVersion = testReturn.version,
            callerCustomerId = testReturn.customerId,
            callerRole = UserRole.WAREHOUSE,
            callerProjectId = testReturn.projectId
        )

        assertTrue(res is DomainResult.Success)
        val updated = (res as DomainResult.Success).data
        assertEquals(ReturnStatus.RETURN_RECEIVED, updated.status)
        assertEquals(testReturn.version + 1L, updated.version)

        // Verify retrieval via getReceiving
        val fetchedReceiving = repository.getReceiving(testReturn.returnId, UserRole.WAREHOUSE, testReturn.projectId)
        assertTrue(fetchedReceiving is DomainResult.Success)
        val rcvData = (fetchedReceiving as DomainResult.Success).data
        assertNotNull(rcvData)
        assertEquals("RCV-01", rcvData?.receivingEventId)
        assertEquals(8, rcvData?.acceptedQty)
        assertEquals(1, rcvData?.rejectedQty)
        assertEquals(1, rcvData?.damagedQty)

        // Verify observation
        val observed = repository.observeReceiving(testReturn.returnId).first()
        assertNotNull(observed)
        assertEquals("RCV-01", observed?.receivingEventId)

        // Verify count
        assertEquals(1, dataSource.countReceivings())
    }

    @Test
    fun `receiveReturn fails if Return is not in APPROVED status`() = runBlocking {
        val underInspReturn = testReturn.copy(
            returnId = "RET-RCV-02",
            status = ReturnStatus.UNDER_INSPECTION
        )
        dataSource.insertReturn(underInspReturn, listOf(testItem.copy(returnId = "RET-RCV-02")))

        val receiving = ReturnReceivingInfo(
            receivingEventId = "RCV-02",
            returnId = underInspReturn.returnId,
            projectId = underInspReturn.projectId,
            receiverId = "warehouse-user-1",
            approvedQty = 10,
            actualQty = 10,
            acceptedQty = 10,
            rejectedQty = 0,
            damagedQty = 0,
            mismatchFlag = false,
            version = 1L,
            idempotencyKey = "IDEMP-RCV-02"
        )

        val res = repository.receiveReturn(
            receivingInfo = receiving,
            actorId = "warehouse-user-1",
            expectedVersion = underInspReturn.version,
            callerRole = UserRole.WAREHOUSE,
            callerProjectId = underInspReturn.projectId
        )

        assertTrue("Receiving a non-APPROVED return must fail", res is DomainResult.Error)
    }
}

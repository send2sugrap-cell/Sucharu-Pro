package com.sucharu.sucharupro.domain.validation

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
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReturnReceivingAuditTest {

    private lateinit var dataSource: FakeReturnDataSource
    private lateinit var repository: ReturnRepositoryImpl

    private val testReturn = ReturnRequest(
        returnId = "RET-AUDIT-01",
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
        returnId = "RET-AUDIT-01",
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
    fun `receiveReturn appends audit event with RETURN_RECEIVED and metadata`() = runBlocking {
        val receiving = ReturnReceivingInfo(
            receivingEventId = "RCV-AUDIT-01",
            returnId = testReturn.returnId,
            projectId = testReturn.projectId,
            receiverId = "warehouse-actor-1",
            approvedQty = 10,
            actualQty = 10,
            acceptedQty = 7,
            rejectedQty = 2,
            damagedQty = 1,
            mismatchFlag = false,
            condition = "Fair",
            packaging = "Sealed",
            damageNotes = "1 unit torn",
            version = 1L,
            idempotencyKey = "IDEMP-AUDIT-01"
        )

        val res = repository.receiveReturn(
            receivingInfo = receiving,
            actorId = "warehouse-actor-1",
            expectedVersion = testReturn.version,
            callerRole = UserRole.WAREHOUSE,
            callerProjectId = testReturn.projectId
        )
        assertTrue(res is DomainResult.Success)

        val auditRes = repository.getAuditHistory(testReturn.returnId, UserRole.WAREHOUSE, testReturn.projectId)
        assertTrue(auditRes is DomainResult.Success)
        val history = (auditRes as DomainResult.Success).data

        val receiveEvent = history.find { it.activityType == ReturnActivityType.RETURN_RECEIVED }
        assertNotNull("RETURN_RECEIVED audit event must be present", receiveEvent)
        assertEquals("warehouse-actor-1", receiveEvent?.actorId)
        assertEquals(UserRole.WAREHOUSE, receiveEvent?.actorRole)
        assertEquals(ReturnStatus.APPROVED, receiveEvent?.previousStatus)
        assertEquals(ReturnStatus.RETURN_RECEIVED, receiveEvent?.newStatus)
        assertEquals("RCV-AUDIT-01", receiveEvent?.metadata?.get("receivingEventId"))
        assertEquals("IDEMP-AUDIT-01", receiveEvent?.metadata?.get("idempotencyKey"))
        assertEquals("10", receiveEvent?.metadata?.get("actualQty"))
        assertEquals("7", receiveEvent?.metadata?.get("acceptedQty"))
        assertEquals("2", receiveEvent?.metadata?.get("rejectedQty"))
        assertEquals("1", receiveEvent?.metadata?.get("damagedQty"))
    }
}

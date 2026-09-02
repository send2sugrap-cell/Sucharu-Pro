package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.data.datasource.FakeReturnDataSource
import com.sucharu.sucharupro.data.repository.ReturnRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.returns.ReturnActivityType
import com.sucharu.sucharupro.domain.model.returns.ReturnItem
import com.sucharu.sucharupro.domain.model.returns.ReturnReason
import com.sucharu.sucharupro.domain.model.returns.ReturnRequest
import com.sucharu.sucharupro.domain.model.returns.ReturnStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Audit trail verification test suite for Return Requests (Module 11 Step 02).
 */
class ReturnRequestAuditTest {

    private lateinit var dataSource: FakeReturnDataSource
    private lateinit var repository: ReturnRepositoryImpl

    private val projectId = "PRJ-AUDIT"
    private val customerId = "CUST-01"
    private val actorId = "STAFF-01"

    @Before
    fun setUp() {
        dataSource = FakeReturnDataSource()
        repository = ReturnRepositoryImpl(dataSource)
    }

    private fun createValidRequest(returnId: String = "RET-AUDIT-01") = ReturnRequest(
        returnId = returnId,
        projectId = projectId,
        returnNo = "RN-AUDIT-01",
        customerId = customerId,
        originalChallanId = "CHAL-01",
        status = ReturnStatus.REQUESTED,
        reason = ReturnReason.PRINTING_DEFECT,
        requestedBy = actorId,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        version = 1L
    )

    private fun createValidItem(returnId: String = "RET-AUDIT-01") = ReturnItem(
        returnItemId = "RI-AUDIT-01",
        returnId = returnId,
        productId = "PROD-1",
        originalChallanItemId = "CI-1",
        requestedQuantity = 20
    )

    @Test
    fun `creation logs RETURN_REQUEST_CREATED activity event`() = runBlocking {
        val request = createValidRequest()
        val item = createValidItem()

        val result = repository.createReturn(
            request = request,
            items = listOf(item),
            actorId = actorId,
            callerRole = UserRole.STAFF,
            callerProjectId = projectId
        )
        assertTrue(result is DomainResult.Success)

        val auditResult = repository.getAuditHistory(
            returnId = request.returnId,
            callerRole = UserRole.STAFF,
            callerProjectId = projectId
        )
        assertTrue(auditResult is DomainResult.Success)
        val events = (auditResult as DomainResult.Success).data
        assertEquals(1, events.size)

        val event = events[0]
        assertEquals(ReturnActivityType.RETURN_REQUEST_CREATED, event.activityType)
        assertEquals(projectId, event.projectId)
        assertEquals(request.returnId, event.returnId)
        assertEquals(actorId, event.actorId)
        assertEquals(ReturnStatus.REQUESTED, event.newStatus)
        Unit
    }

    @Test
    fun `submit for inspection logs RETURN_REQUEST_SUBMITTED_FOR_INSPECTION event`() = runBlocking {
        val request = createValidRequest()
        val item = createValidItem()

        repository.createReturn(
            request = request,
            items = listOf(item),
            actorId = actorId,
            callerRole = UserRole.ADMIN,
            callerProjectId = projectId
        )

        val submitRes = repository.submitForInspection(
            returnId = request.returnId,
            actorId = "INSP-ACTOR",
            expectedVersion = 1L,
            callerRole = UserRole.ADMIN,
            callerProjectId = projectId
        )
        assertTrue(submitRes is DomainResult.Success)

        val eventsRes = repository.getAuditHistory(
            returnId = request.returnId,
            callerRole = UserRole.ADMIN,
            callerProjectId = projectId
        )
        val events = (eventsRes as DomainResult.Success).data
        assertEquals(2, events.size)

        val submitEvent = events[1]
        assertEquals(ReturnActivityType.RETURN_REQUEST_SUBMITTED_FOR_INSPECTION, submitEvent.activityType)
        assertEquals("INSP-ACTOR", submitEvent.actorId)
        assertEquals(ReturnStatus.REQUESTED, submitEvent.previousStatus)
        assertEquals(ReturnStatus.UNDER_INSPECTION, submitEvent.newStatus)
        Unit
    }

    @Test
    fun `cancel return logs RETURN_REQUEST_CANCELLED event`() = runBlocking {
        val request = createValidRequest()
        val item = createValidItem()

        repository.createReturn(
            request = request,
            items = listOf(item),
            actorId = actorId,
            callerRole = UserRole.ADMIN,
            callerProjectId = projectId
        )

        val cancelRes = repository.cancelReturnRequest(
            returnId = request.returnId,
            actorId = "ADMIN-01",
            expectedVersion = 1L,
            callerRole = UserRole.ADMIN,
            callerProjectId = projectId
        )
        assertTrue(cancelRes is DomainResult.Success)

        val eventsRes = repository.getAuditHistory(
            returnId = request.returnId,
            callerRole = UserRole.ADMIN,
            callerProjectId = projectId
        )
        val events = (eventsRes as DomainResult.Success).data
        assertEquals(2, events.size)

        val cancelEvent = events[1]
        assertEquals(ReturnActivityType.RETURN_REQUEST_CANCELLED, cancelEvent.activityType)
        assertEquals("ADMIN-01", cancelEvent.actorId)
        assertEquals(ReturnStatus.REQUESTED, cancelEvent.previousStatus)
        assertEquals(ReturnStatus.CANCELLED, cancelEvent.newStatus)
        Unit
    }

    @Test
    fun `update return logs RETURN_REQUEST_UPDATED event`() = runBlocking {
        val request = createValidRequest()
        val item = createValidItem()

        repository.createReturn(
            request = request,
            items = listOf(item),
            actorId = actorId,
            callerRole = UserRole.STAFF,
            callerProjectId = projectId
        )

        val updated = request.copy(
            reason = ReturnReason.DAMAGED,
            description = "Updated damage notes"
        )

        val updateRes = repository.updateReturnRequest(
            request = updated,
            items = listOf(item),
            actorId = actorId,
            callerRole = UserRole.STAFF,
            callerProjectId = projectId
        )
        assertTrue(updateRes is DomainResult.Success)

        val eventsRes = repository.getAuditHistory(
            returnId = request.returnId,
            callerRole = UserRole.STAFF,
            callerProjectId = projectId
        )
        val events = (eventsRes as DomainResult.Success).data
        assertEquals(2, events.size)

        val updateEvent = events[1]
        assertEquals(ReturnActivityType.RETURN_REQUEST_UPDATED, updateEvent.activityType)
        assertEquals(ReturnStatus.REQUESTED, updateEvent.previousStatus)
        assertEquals(ReturnStatus.REQUESTED, updateEvent.newStatus)
        Unit
    }
}

package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.data.datasource.FakeReturnDataSource
import com.sucharu.sucharupro.data.repository.ReturnRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.returns.ReturnActivityType
import com.sucharu.sucharupro.domain.model.returns.ReturnItem
import com.sucharu.sucharupro.domain.model.returns.ReturnReason
import com.sucharu.sucharupro.domain.model.returns.ReturnRequest
import com.sucharu.sucharupro.domain.model.returns.ReturnResolutionType
import com.sucharu.sucharupro.domain.model.returns.ReturnSettlement
import com.sucharu.sucharupro.domain.model.returns.ReturnSettlementStatus
import com.sucharu.sucharupro.domain.model.returns.ReturnStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Audit trail verification tests for Return Settlement (Module 11 Step 05 Chunk 03).
 */
class ReturnSettlementAuditTest {

    private lateinit var dataSource: FakeReturnDataSource
    private lateinit var repository: ReturnRepositoryImpl

    private val projectId = "PRJ-AUDIT-01"
    private val customerId = "CUST-AUDIT-01"
    private val returnId = "RET-AUDIT-101"
    private val actorId = "ACTOR-ACCOUNTS"

    private val testReturn = ReturnRequest(
        returnId = returnId,
        projectId = projectId,
        returnNo = "RN-AUDIT-101",
        customerId = customerId,
        originalChallanId = "CHAL-01",
        status = ReturnStatus.PROCESSED,
        reason = ReturnReason.PRINTING_DEFECT,
        requestedBy = "CUST-USER",
        version = 1L
    )

    private val testItem = ReturnItem(
        returnItemId = "RI-AUDIT-01",
        returnId = returnId,
        productId = "PROD-01",
        originalChallanItemId = "CI-01",
        requestedQuantity = 10,
        acceptedQuantity = 10,
        rejectedQuantity = 0,
        unit = "PCS"
    )

    private val testSettlement = ReturnSettlement(
        settlementId = "SETTLE-AUDIT-01",
        returnId = returnId,
        projectId = projectId,
        customerId = customerId,
        resolutionType = ReturnResolutionType.CREDIT_NOTE,
        amount = Money(1500.0),
        status = ReturnSettlementStatus.COMPLETED,
        creditNoteId = "CN-AUDIT-01",
        settledBy = actorId,
        version = 1L,
        idempotencyKey = "KEY-AUDIT-01"
    )

    @Before
    fun setUp() = runBlocking {
        dataSource = FakeReturnDataSource()
        repository = ReturnRepositoryImpl(dataSource)
        dataSource.insertReturn(testReturn, listOf(testItem))
    }

    @Test
    fun `settlement records append-only RETURN_SETTLED activity event with full audit metadata`() = runBlocking {
        val result = repository.settleReturn(
            settlement = testSettlement,
            actorId = actorId,
            expectedVersion = 1L,
            callerRole = UserRole.ACCOUNTS,
            callerProjectId = projectId
        )

        assertTrue(result is DomainResult.Success)

        val history = repository.observeAuditHistory(returnId).first()
        val settledEvent = history.find { it.activityType == ReturnActivityType.RETURN_SETTLED }

        assertNotNull("RETURN_SETTLED activity event must be recorded", settledEvent)
        assertEquals(actorId, settledEvent!!.actorId)
        assertEquals(UserRole.ACCOUNTS, settledEvent.actorRole)
        assertEquals(ReturnStatus.PROCESSED, settledEvent.previousStatus)
        assertEquals(ReturnStatus.PROCESSED, settledEvent.newStatus)

        assertEquals("SETTLE-AUDIT-01", settledEvent.metadata["settlementId"])
        assertEquals("CREDIT_NOTE", settledEvent.metadata["resolutionType"])
        assertEquals("CN-AUDIT-01", settledEvent.metadata["creditNoteId"])
        assertEquals(customerId, settledEvent.metadata["customerId"])
        assertEquals("KEY-AUDIT-01", settledEvent.metadata["idempotencyKey"])
    }
}

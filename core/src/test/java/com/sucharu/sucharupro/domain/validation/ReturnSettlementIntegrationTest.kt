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
 * Commercial, Financial, and Operational Resolution Integration tests
 * (Module 11 Step 05 Chunk 04).
 *
 * Verifies:
 *   - All 5 resolution modes (CREDIT_NOTE, REFUND, REPLACEMENT, REWORK, SCRAP_WRITE_OFF)
 *   - Reference tracking (creditNoteId, replacementOrderId, reworkId)
 *   - Invariant: Zero inventory or stock-in side effects during settlement.
 */
class ReturnSettlementIntegrationTest {

    private lateinit var dataSource: FakeReturnDataSource
    private lateinit var repository: ReturnRepositoryImpl

    private val projectId = "PRJ-INTEG-01"
    private val customerId = "CUST-INTEG-01"
    private val returnId = "RET-INTEG-101"
    private val actorId = "ACTOR-ACCOUNTS-01"

    private val testReturn = ReturnRequest(
        returnId = returnId,
        projectId = projectId,
        returnNo = "RN-INTEG-101",
        customerId = customerId,
        originalChallanId = "CHAL-101",
        status = ReturnStatus.PROCESSED,
        reason = ReturnReason.PRINTING_DEFECT,
        requestedBy = "CUST-USER",
        version = 1L
    )

    private val testItem = ReturnItem(
        returnItemId = "RI-INTEG-01",
        returnId = returnId,
        productId = "PROD-101",
        originalChallanItemId = "CI-101",
        requestedQuantity = 100,
        acceptedQuantity = 100,
        rejectedQuantity = 0,
        unit = "PCS"
    )

    @Before
    fun setUp() = runBlocking {
        dataSource = FakeReturnDataSource()
        repository = ReturnRepositoryImpl(dataSource)
        dataSource.insertReturn(testReturn, listOf(testItem))
    }

    @Test
    fun `CREDIT_NOTE resolution successfully records credit note link and audit event`() = runBlocking {
        val settlement = ReturnSettlement(
            settlementId = "SETTLE-CN-01",
            returnId = returnId,
            projectId = projectId,
            customerId = customerId,
            resolutionType = ReturnResolutionType.CREDIT_NOTE,
            amount = Money(4500.0),
            status = ReturnSettlementStatus.COMPLETED,
            creditNoteId = "CN-9901",
            notes = "Issued full credit note for defective batch",
            settledBy = actorId,
            idempotencyKey = "IDEMP-CN-01"
        )

        val result = repository.settleReturn(
            settlement = settlement,
            actorId = actorId,
            expectedVersion = 1L,
            callerRole = UserRole.ACCOUNTS,
            callerProjectId = projectId
        )

        assertTrue(result is DomainResult.Success)
        val settled = (result as DomainResult.Success).data
        assertEquals("CN-9901", settled.creditNoteId)
        assertEquals(ReturnResolutionType.CREDIT_NOTE, settled.resolutionType)

        val history = repository.observeAuditHistory(returnId).first()
        val event = history.find { it.activityType == ReturnActivityType.RETURN_SETTLED }
        assertNotNull(event)
        assertEquals("CN-9901", event!!.metadata["creditNoteId"])
    }

    @Test
    fun `REFUND resolution successfully records monetary payout resolution`() = runBlocking {
        val settlement = ReturnSettlement(
            settlementId = "SETTLE-REFUND-01",
            returnId = returnId,
            projectId = projectId,
            customerId = customerId,
            resolutionType = ReturnResolutionType.REFUND,
            amount = Money(3200.0),
            status = ReturnSettlementStatus.COMPLETED,
            notes = "Direct bank transfer refund completed",
            settledBy = actorId,
            idempotencyKey = "IDEMP-REFUND-01"
        )

        val result = repository.settleReturn(
            settlement = settlement,
            actorId = actorId,
            expectedVersion = 1L,
            callerRole = UserRole.ACCOUNTS,
            callerProjectId = projectId
        )

        assertTrue(result is DomainResult.Success)
        val settled = (result as DomainResult.Success).data
        assertEquals(ReturnResolutionType.REFUND, settled.resolutionType)
        assertEquals(Money(3200.0), settled.amount)
    }

    @Test
    fun `REPLACEMENT resolution successfully links replacement order reference`() = runBlocking {
        val settlement = ReturnSettlement(
            settlementId = "SETTLE-REP-01",
            returnId = returnId,
            projectId = projectId,
            customerId = customerId,
            resolutionType = ReturnResolutionType.REPLACEMENT,
            amount = Money.ZERO,
            status = ReturnSettlementStatus.COMPLETED,
            replacementOrderId = "ORD-REPLACE-5501",
            notes = "No-charge replacement order raised",
            settledBy = actorId,
            idempotencyKey = "IDEMP-REP-01"
        )

        val result = repository.settleReturn(
            settlement = settlement,
            actorId = actorId,
            expectedVersion = 1L,
            callerRole = UserRole.MANAGER,
            callerProjectId = projectId
        )

        assertTrue(result is DomainResult.Success)
        val settled = (result as DomainResult.Success).data
        assertEquals("ORD-REPLACE-5501", settled.replacementOrderId)
        assertEquals(ReturnResolutionType.REPLACEMENT, settled.resolutionType)
    }

    @Test
    fun `REWORK resolution successfully links production rework reference`() = runBlocking {
        val settlement = ReturnSettlement(
            settlementId = "SETTLE-REW-01",
            returnId = returnId,
            projectId = projectId,
            customerId = customerId,
            resolutionType = ReturnResolutionType.REWORK,
            amount = Money.ZERO,
            status = ReturnSettlementStatus.COMPLETED,
            reworkId = "REW-JOB-7701",
            notes = "Sent back to finishing unit for re-lamination",
            settledBy = actorId,
            idempotencyKey = "IDEMP-REW-01"
        )

        val result = repository.settleReturn(
            settlement = settlement,
            actorId = actorId,
            expectedVersion = 1L,
            callerRole = UserRole.MANAGER,
            callerProjectId = projectId
        )

        assertTrue(result is DomainResult.Success)
        val settled = (result as DomainResult.Success).data
        assertEquals("REW-JOB-7701", settled.reworkId)
        assertEquals(ReturnResolutionType.REWORK, settled.resolutionType)
    }

    @Test
    fun `SCRAP_WRITE_OFF resolution records disposal loss write-off`() = runBlocking {
        val settlement = ReturnSettlement(
            settlementId = "SETTLE-SCRAP-01",
            returnId = returnId,
            projectId = projectId,
            customerId = customerId,
            resolutionType = ReturnResolutionType.SCRAP_WRITE_OFF,
            amount = Money(1500.0),
            status = ReturnSettlementStatus.COMPLETED,
            notes = "Damaged goods recycled with scrap disposal write-off",
            settledBy = actorId,
            idempotencyKey = "IDEMP-SCRAP-01"
        )

        val result = repository.settleReturn(
            settlement = settlement,
            actorId = actorId,
            expectedVersion = 1L,
            callerRole = UserRole.ADMIN,
            callerProjectId = projectId
        )

        assertTrue(result is DomainResult.Success)
        val settled = (result as DomainResult.Success).data
        assertEquals(ReturnResolutionType.SCRAP_WRITE_OFF, settled.resolutionType)
        assertEquals(Money(1500.0), settled.amount)
    }

    @Test
    fun `settlement preserves zero inventory mutation invariant`() = runBlocking {
        // Count reconciliations before settlement
        val recCountBefore = dataSource.countReconciliations()

        val settlement = ReturnSettlement(
            settlementId = "SETTLE-INV-01",
            returnId = returnId,
            projectId = projectId,
            customerId = customerId,
            resolutionType = ReturnResolutionType.CREDIT_NOTE,
            amount = Money(500.0),
            status = ReturnSettlementStatus.COMPLETED,
            creditNoteId = "CN-INV-01",
            settledBy = actorId,
            idempotencyKey = "IDEMP-INV-01"
        )

        val result = repository.settleReturn(
            settlement = settlement,
            actorId = actorId,
            expectedVersion = 1L,
            callerRole = UserRole.ACCOUNTS,
            callerProjectId = projectId
        )

        assertTrue(result is DomainResult.Success)

        // Reconciliation count remains unchanged — Step 05 does not mutate stock
        val recCountAfter = dataSource.countReconciliations()
        assertEquals(recCountBefore, recCountAfter)
    }
}

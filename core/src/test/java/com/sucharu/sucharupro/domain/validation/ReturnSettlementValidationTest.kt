package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.data.datasource.FakeReturnDataSource
import com.sucharu.sucharupro.data.repository.ReturnRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.returns.ReturnItem
import com.sucharu.sucharupro.domain.model.returns.ReturnReason
import com.sucharu.sucharupro.domain.model.returns.ReturnRequest
import com.sucharu.sucharupro.domain.model.returns.ReturnResolutionType
import com.sucharu.sucharupro.domain.model.returns.ReturnSettlement
import com.sucharu.sucharupro.domain.model.returns.ReturnSettlementStatus
import com.sucharu.sucharupro.domain.model.returns.ReturnStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

/**
 * Domain validation and eligibility tests for Return Settlement (Module 11 Step 05 Chunk 03).
 */
class ReturnSettlementValidationTest {

    private lateinit var dataSource: FakeReturnDataSource
    private lateinit var repository: ReturnRepositoryImpl

    private val projectId = "PRJ-VAL-01"
    private val customerId = "CUST-VAL-01"
    private val returnId = "RET-VAL-101"
    private val actorId = "ACTOR-ACCOUNTS-01"

    private fun sampleReturn(status: ReturnStatus = ReturnStatus.PROCESSED, version: Long = 1L) = ReturnRequest(
        returnId = returnId,
        projectId = projectId,
        returnNo = "RN-VAL-101",
        customerId = customerId,
        originalChallanId = "CHAL-01",
        status = status,
        reason = ReturnReason.PRINTING_DEFECT,
        requestedBy = "CUST-USER",
        version = version
    )

    private fun sampleItem() = ReturnItem(
        returnItemId = "RI-VAL-01",
        returnId = returnId,
        productId = "PROD-01",
        originalChallanItemId = "CI-01",
        requestedQuantity = 10,
        acceptedQuantity = 10,
        rejectedQuantity = 0,
        unit = "PCS"
    )

    private fun sampleSettlement(
        amount: Money = Money(1000.0),
        resType: ReturnResolutionType = ReturnResolutionType.CREDIT_NOTE
    ) = ReturnSettlement(
        settlementId = "SETTLE-VAL-01",
        returnId = returnId,
        projectId = projectId,
        customerId = customerId,
        resolutionType = resType,
        amount = amount,
        status = ReturnSettlementStatus.COMPLETED,
        creditNoteId = "CN-101",
        settledBy = actorId,
        version = 1L,
        idempotencyKey = "IDEMP-VAL-01"
    )

    @Before
    fun setUp() {
        dataSource = FakeReturnDataSource()
        repository = ReturnRepositoryImpl(dataSource)
    }

    @Test
    fun `settlement on PROCESSED return succeeds`() = runBlocking {
        dataSource.insertReturn(sampleReturn(status = ReturnStatus.PROCESSED), listOf(sampleItem()))

        val result = repository.settleReturn(
            settlement = sampleSettlement(),
            actorId = actorId,
            expectedVersion = 1L,
            callerRole = UserRole.ACCOUNTS,
            callerProjectId = projectId
        )

        assertTrue("PROCESSED return settlement must succeed", result is DomainResult.Success)
    }

    @Test
    fun `settlement on non-PROCESSED return statuses is rejected`() = runBlocking {
        val nonProcessedStatuses = listOf(
            ReturnStatus.REQUESTED,
            ReturnStatus.UNDER_INSPECTION,
            ReturnStatus.APPROVED,
            ReturnStatus.RETURN_RECEIVED,
            ReturnStatus.REJECTED,
            ReturnStatus.CANCELLED
        )

        for (status in nonProcessedStatuses) {
            val req = sampleReturn(status = status, version = 1L).copy(returnId = "RET-ST-$status")
            dataSource.insertReturn(req, listOf(sampleItem().copy(returnId = req.returnId)))

            val result = repository.settleReturn(
                settlement = sampleSettlement().copy(returnId = req.returnId, idempotencyKey = "KEY-$status"),
                actorId = actorId,
                expectedVersion = 1L,
                callerRole = UserRole.ACCOUNTS,
                callerProjectId = projectId
            )

            assertTrue("Settlement on status $status must be rejected", result is DomainResult.Error)
            val error = (result as DomainResult.Error).message
            assertTrue(error.contains("cannot be settled", ignoreCase = true) || error.contains("PROCESSED", ignoreCase = true))
        }
    }

    @Test
    fun `settlement with negative amount is rejected by model invariant`() {
        var threw = false
        try {
            sampleSettlement().copy(amount = Money(BigDecimal("-50.00")))
        } catch (e: IllegalArgumentException) {
            threw = true
            assertTrue(e.message?.contains("negative", ignoreCase = true) == true)
        }
        assertTrue("Model invariant must reject negative settlement amount", threw)
    }

    @Test
    fun `settlement customer mismatch against return owner is rejected`() = runBlocking {
        dataSource.insertReturn(sampleReturn(), listOf(sampleItem()))

        val mismatchSettlement = sampleSettlement().copy(customerId = "CUST-OTHER")

        val result = repository.settleReturn(
            settlement = mismatchSettlement,
            actorId = actorId,
            expectedVersion = 1L,
            callerRole = UserRole.ACCOUNTS,
            callerProjectId = projectId
        )

        assertTrue("Customer mismatch must be rejected", result is DomainResult.Error)
    }
}

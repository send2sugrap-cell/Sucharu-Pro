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

/**
 * Idempotency and duplicate prevention tests for Return Settlement (Module 11 Step 05 Chunk 03).
 */
class ReturnSettlementIdempotencyTest {

    private lateinit var dataSource: FakeReturnDataSource
    private lateinit var repository: ReturnRepositoryImpl

    private val projectId = "PRJ-IDEMP-01"
    private val customerId = "CUST-IDEMP-01"
    private val returnId = "RET-IDEMP-101"

    private val testReturn = ReturnRequest(
        returnId = returnId,
        projectId = projectId,
        returnNo = "RN-IDEMP-101",
        customerId = customerId,
        originalChallanId = "CHAL-01",
        status = ReturnStatus.PROCESSED,
        reason = ReturnReason.PRINTING_DEFECT,
        requestedBy = "CUST-USER",
        version = 1L
    )

    private val testItem = ReturnItem(
        returnItemId = "RI-IDEMP-01",
        returnId = returnId,
        productId = "PROD-01",
        originalChallanItemId = "CI-01",
        requestedQuantity = 10,
        acceptedQuantity = 10,
        rejectedQuantity = 0,
        unit = "PCS"
    )

    private val testSettlement = ReturnSettlement(
        settlementId = "SETTLE-IDEMP-01",
        returnId = returnId,
        projectId = projectId,
        customerId = customerId,
        resolutionType = ReturnResolutionType.CREDIT_NOTE,
        amount = Money(800.0),
        status = ReturnSettlementStatus.COMPLETED,
        creditNoteId = "CN-IDEMP-01",
        settledBy = "ACTOR-ACCOUNTS",
        version = 1L,
        idempotencyKey = "IDEMP-KEY-01"
    )

    @Before
    fun setUp() = runBlocking {
        dataSource = FakeReturnDataSource()
        repository = ReturnRepositoryImpl(dataSource)
        dataSource.insertReturn(testReturn, listOf(testItem))
    }

    @Test
    fun `same idempotency key safely returns existing settlement without duplicate records`() = runBlocking {
        val result1 = repository.settleReturn(
            settlement = testSettlement,
            actorId = "ACTOR-ACCOUNTS",
            expectedVersion = 1L,
            callerRole = UserRole.ACCOUNTS,
            callerProjectId = projectId
        )
        assertTrue(result1 is DomainResult.Success)
        val s1 = (result1 as DomainResult.Success).data

        // Repeat call
        val result2 = repository.settleReturn(
            settlement = testSettlement,
            actorId = "ACTOR-ACCOUNTS",
            expectedVersion = 2L,
            callerRole = UserRole.ACCOUNTS,
            callerProjectId = projectId
        )
        assertTrue(result2 is DomainResult.Success)
        val s2 = (result2 as DomainResult.Success).data

        assertEquals(s1.settlementId, s2.settlementId)
        assertEquals(1, dataSource.countSettlements())
    }

    @Test
    fun `reusing existing idempotency key for different return is rejected`() = runBlocking {
        repository.settleReturn(testSettlement, "ACTOR-ACCOUNTS", 1L, null, UserRole.ACCOUNTS, projectId)

        val return2 = testReturn.copy(returnId = "RET-2", version = 1L)
        dataSource.insertReturn(return2, listOf(testItem.copy(returnId = "RET-2", returnItemId = "RI-2")))

        val result = repository.settleReturn(
            settlement = testSettlement.copy(settlementId = "SETTLE-2", returnId = "RET-2"),
            actorId = "ACTOR-ACCOUNTS",
            expectedVersion = 1L,
            callerRole = UserRole.ACCOUNTS,
            callerProjectId = projectId
        )

        assertTrue(result is DomainResult.Error)
        val error = (result as DomainResult.Error).message
        assertTrue(error.contains("already used", ignoreCase = true))
    }
}

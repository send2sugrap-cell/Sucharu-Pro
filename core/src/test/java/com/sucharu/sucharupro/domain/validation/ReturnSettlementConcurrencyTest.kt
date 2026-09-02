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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Optimistic concurrency control tests for Return Settlement (Module 11 Step 05 Chunk 03).
 */
class ReturnSettlementConcurrencyTest {

    private lateinit var dataSource: FakeReturnDataSource
    private lateinit var repository: ReturnRepositoryImpl

    private val projectId = "PRJ-CONC-01"
    private val customerId = "CUST-CONC-01"
    private val returnId = "RET-CONC-101"

    private val testReturn = ReturnRequest(
        returnId = returnId,
        projectId = projectId,
        returnNo = "RN-CONC-101",
        customerId = customerId,
        originalChallanId = "CHAL-01",
        status = ReturnStatus.PROCESSED,
        reason = ReturnReason.PRINTING_DEFECT,
        requestedBy = "CUST-USER",
        version = 2L
    )

    private val testItem = ReturnItem(
        returnItemId = "RI-CONC-01",
        returnId = returnId,
        productId = "PROD-01",
        originalChallanItemId = "CI-01",
        requestedQuantity = 10,
        acceptedQuantity = 10,
        rejectedQuantity = 0,
        unit = "PCS"
    )

    private val testSettlement = ReturnSettlement(
        settlementId = "SETTLE-CONC-01",
        returnId = returnId,
        projectId = projectId,
        customerId = customerId,
        resolutionType = ReturnResolutionType.CREDIT_NOTE,
        amount = Money(1200.0),
        status = ReturnSettlementStatus.COMPLETED,
        creditNoteId = "CN-CONC-01",
        settledBy = "ACTOR-ACCOUNTS",
        version = 1L,
        idempotencyKey = "IDEMP-CONC-01"
    )

    @Before
    fun setUp() = runBlocking {
        dataSource = FakeReturnDataSource()
        repository = ReturnRepositoryImpl(dataSource)
        dataSource.insertReturn(testReturn, listOf(testItem))
    }

    @Test
    fun `stale expectedVersion is rejected with concurrency conflict`() = runBlocking {
        val result = repository.settleReturn(
            settlement = testSettlement,
            actorId = "ACTOR-ACCOUNTS",
            expectedVersion = 1L, // Stale, current is 2L
            callerRole = UserRole.ACCOUNTS,
            callerProjectId = projectId
        )

        assertTrue("Stale version must fail", result is DomainResult.Error)
        val error = (result as DomainResult.Error).message
        assertTrue(error.contains("concurrency", ignoreCase = true) || error.contains("version", ignoreCase = true))
    }

    @Test
    fun `matching expectedVersion succeeds and increments version`() = runBlocking {
        val result = repository.settleReturn(
            settlement = testSettlement,
            actorId = "ACTOR-ACCOUNTS",
            expectedVersion = 2L, // Matching current
            callerRole = UserRole.ACCOUNTS,
            callerProjectId = projectId
        )

        assertTrue("Matching version must succeed", result is DomainResult.Success)

        // Repeat call with now-stale expectedVersion 2L should fail if different key
        val staleResult = repository.settleReturn(
            settlement = testSettlement.copy(settlementId = "SETTLE-NEW", idempotencyKey = "NEW-KEY"),
            actorId = "ACTOR-ACCOUNTS",
            expectedVersion = 2L, // Stale now (version is 3L)
            callerRole = UserRole.ACCOUNTS,
            callerProjectId = projectId
        )
        assertTrue("Subsequent mutation with stale version must fail", staleResult is DomainResult.Error)
    }
}

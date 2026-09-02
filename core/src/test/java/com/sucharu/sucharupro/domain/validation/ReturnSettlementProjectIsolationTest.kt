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
 * Multi-tenant project isolation tests for Return Settlement (Module 11 Step 05 Chunk 03).
 */
class ReturnSettlementProjectIsolationTest {

    private lateinit var dataSource: FakeReturnDataSource
    private lateinit var repository: ReturnRepositoryImpl

    private val projectA = "PRJ-AAA"
    private val projectB = "PRJ-BBB"
    private val customerA = "CUST-AAA"
    private val customerB = "CUST-BBB"
    private val returnIdA = "RET-PRJ-A"

    private val returnA = ReturnRequest(
        returnId = returnIdA,
        projectId = projectA,
        returnNo = "RN-A",
        customerId = customerA,
        originalChallanId = "CHAL-A",
        status = ReturnStatus.PROCESSED,
        reason = ReturnReason.PRINTING_DEFECT,
        requestedBy = "USER-A",
        version = 1L
    )

    private val itemA = ReturnItem(
        returnItemId = "RI-A",
        returnId = returnIdA,
        productId = "PROD-A",
        originalChallanItemId = "CI-A",
        requestedQuantity = 10,
        acceptedQuantity = 10,
        rejectedQuantity = 0,
        unit = "PCS"
    )

    private val settlementA = ReturnSettlement(
        settlementId = "SETTLE-A",
        returnId = returnIdA,
        projectId = projectA,
        customerId = customerA,
        resolutionType = ReturnResolutionType.CREDIT_NOTE,
        amount = Money(500.0),
        status = ReturnSettlementStatus.COMPLETED,
        creditNoteId = "CN-A",
        settledBy = "ACTOR-A",
        version = 1L,
        idempotencyKey = "KEY-A"
    )

    @Before
    fun setUp() = runBlocking {
        dataSource = FakeReturnDataSource()
        repository = ReturnRepositoryImpl(dataSource)
        dataSource.insertReturn(returnA, listOf(itemA))
    }

    @Test
    fun `cross-project settlement mutation is rejected`() = runBlocking {
        // Caller from project B tries to settle return in project A
        val result = repository.settleReturn(
            settlement = settlementA.copy(projectId = projectB),
            actorId = "ACTOR-B",
            expectedVersion = 1L,
            callerRole = UserRole.ACCOUNTS,
            callerProjectId = projectB
        )

        assertTrue("Cross project mutation must fail", result is DomainResult.Error)
    }

    @Test
    fun `cross-project settlement retrieval is rejected`() = runBlocking {
        dataSource.insertOrUpdateSettlement(settlementA)

        val result = repository.getSettlement(
            returnId = returnIdA,
            callerRole = UserRole.ACCOUNTS,
            callerProjectId = projectB
        )

        assertTrue("Cross project query must fail", result is DomainResult.Error)
    }
}

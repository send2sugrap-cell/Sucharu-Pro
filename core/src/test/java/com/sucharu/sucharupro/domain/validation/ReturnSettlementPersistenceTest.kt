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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Persistence, query, observation, and idempotency tests for Customer Return Settlement
 * (Module 11 Step 05 Chunk 02).
 */
class ReturnSettlementPersistenceTest {

    private lateinit var dataSource: FakeReturnDataSource
    private lateinit var repository: ReturnRepositoryImpl

    private val testProjectId = "PRJ-SETTLE-01"
    private val testCustomerId = "CUST-SETTLE-01"
    private val testReturnId = "RET-SETTLE-101"
    private val testActorId = "ACTOR-ACCOUNTS-01"

    private val testReturn = ReturnRequest(
        returnId = testReturnId,
        projectId = testProjectId,
        returnNo = "RN-SETTLE-101",
        customerId = testCustomerId,
        originalChallanId = "CHAL-101",
        status = ReturnStatus.PROCESSED,
        reason = ReturnReason.PRINTING_DEFECT,
        description = "Defective covers",
        requestedBy = "CUST-USER",
        version = 5L
    )

    private val testItem = ReturnItem(
        returnItemId = "RI-SETTLE-01",
        returnId = testReturnId,
        productId = "PROD-101",
        originalChallanItemId = "CI-101",
        requestedQuantity = 10,
        acceptedQuantity = 10,
        rejectedQuantity = 0,
        unit = "PCS"
    )

    private val testSettlement = ReturnSettlement(
        settlementId = "SETTLE-901",
        returnId = testReturnId,
        projectId = testProjectId,
        customerId = testCustomerId,
        resolutionType = ReturnResolutionType.CREDIT_NOTE,
        amount = Money(2500.0),
        status = ReturnSettlementStatus.COMPLETED,
        creditNoteId = "CN-8801",
        notes = "Full credit approved for batch",
        settledBy = testActorId,
        settledAt = System.currentTimeMillis(),
        version = 1L,
        idempotencyKey = "IDEMP-SETTLE-901"
    )

    @Before
    fun setUp() = runBlocking {
        dataSource = FakeReturnDataSource()
        repository = ReturnRepositoryImpl(dataSource)
        dataSource.insertReturn(testReturn, listOf(testItem))
    }

    @Test
    fun `settleReturn persists settlement record and increments return version`() = runBlocking {
        val result = repository.settleReturn(
            settlement = testSettlement,
            actorId = testActorId,
            expectedVersion = 5L,
            callerCustomerId = testCustomerId,
            callerRole = UserRole.ACCOUNTS,
            callerProjectId = testProjectId
        )

        assertTrue("Settlement should succeed", result is DomainResult.Success)
        val settled = (result as DomainResult.Success).data
        assertEquals("SETTLE-901", settled.settlementId)
        assertEquals(ReturnResolutionType.CREDIT_NOTE, settled.resolutionType)
        assertEquals(Money(2500.0), settled.amount)
        assertEquals("CN-8801", settled.creditNoteId)

        // Verify persisted return version incremented
        val updatedReturn = dataSource.getReturn(testReturnId)
        assertNotNull(updatedReturn)
        assertEquals(6L, updatedReturn!!.version)

        // Verify count in datasource
        assertEquals(1, dataSource.countSettlements())
    }

    @Test
    fun `getSettlement retrieves persisted settlement with project isolation and RBAC`() = runBlocking {
        dataSource.insertOrUpdateSettlement(testSettlement)

        // Authorized retrieval
        val queryResult = repository.getSettlement(
            returnId = testReturnId,
            callerRole = UserRole.ACCOUNTS,
            callerProjectId = testProjectId
        )
        assertTrue(queryResult is DomainResult.Success)
        val retrieved = (queryResult as DomainResult.Success).data
        assertNotNull(retrieved)
        assertEquals("SETTLE-901", retrieved?.settlementId)
        assertEquals(Money(2500.0), retrieved?.amount)

        // Cross-project query rejected
        val crossProjectResult = repository.getSettlement(
            returnId = testReturnId,
            callerRole = UserRole.ACCOUNTS,
            callerProjectId = "PRJ-OTHER"
        )
        assertTrue("Cross-project query should fail", crossProjectResult is DomainResult.Error)
    }

    @Test
    fun `observeSettlement emits live updates reactively`() = runBlocking {
        val initialObservation = repository.observeSettlement(testReturnId).first()
        assertNull("Initial settlement observation should be null", initialObservation)

        dataSource.insertOrUpdateSettlement(testSettlement)

        val updatedObservation = repository.observeSettlement(testReturnId).first()
        assertNotNull("Updated settlement observation should emit data", updatedObservation)
        assertEquals(testSettlement.settlementId, updatedObservation?.settlementId)
        assertEquals(ReturnResolutionType.CREDIT_NOTE, updatedObservation?.resolutionType)
    }

    @Test
    fun `idempotency returns existing settlement when called repeatedly with same key`() = runBlocking {
        // First settlement call
        val res1 = repository.settleReturn(
            settlement = testSettlement,
            actorId = testActorId,
            expectedVersion = 5L,
            callerCustomerId = testCustomerId,
            callerRole = UserRole.ACCOUNTS,
            callerProjectId = testProjectId
        )
        assertTrue(res1 is DomainResult.Success)
        val settled1 = (res1 as DomainResult.Success).data

        // Repeated call with same idempotency key
        val res2 = repository.settleReturn(
            settlement = testSettlement,
            actorId = testActorId,
            expectedVersion = 6L,
            callerCustomerId = testCustomerId,
            callerRole = UserRole.ACCOUNTS,
            callerProjectId = testProjectId
        )
        assertTrue(res2 is DomainResult.Success)
        val settled2 = (res2 as DomainResult.Success).data

        assertEquals(settled1.settlementId, settled2.settlementId)
        assertEquals(1, dataSource.countSettlements())
    }

    @Test
    fun `idempotency key reused against different return is rejected`() = runBlocking {
        dataSource.insertOrUpdateSettlement(testSettlement)

        val otherReturn = testReturn.copy(
            returnId = "RET-OTHER-99",
            returnNo = "RN-OTHER-99",
            version = 1L
        )
        dataSource.insertReturn(otherReturn, listOf(testItem.copy(returnItemId = "RI-OTHER-99", returnId = "RET-OTHER-99")))

        val conflictingSettlement = testSettlement.copy(
            settlementId = "SETTLE-OTHER-99",
            returnId = "RET-OTHER-99",
            idempotencyKey = testSettlement.idempotencyKey // Reusing same key
        )

        val result = repository.settleReturn(
            settlement = conflictingSettlement,
            actorId = testActorId,
            expectedVersion = 1L,
            callerCustomerId = testCustomerId,
            callerRole = UserRole.ACCOUNTS,
            callerProjectId = testProjectId
        )

        assertTrue("Conflicting idempotency key must fail", result is DomainResult.Error)
        val errorMsg = (result as DomainResult.Error).message
        assertTrue(errorMsg.contains("already used", ignoreCase = true))
    }

    @Test
    fun `project isolation blocks cross-project settlement mutation`() = runBlocking {
        val crossSettlement = testSettlement.copy(projectId = "PRJ-OTHER")

        val result = repository.settleReturn(
            settlement = crossSettlement,
            actorId = testActorId,
            expectedVersion = 5L,
            callerRole = UserRole.ACCOUNTS,
            callerProjectId = "PRJ-OTHER"
        )

        assertTrue("Cross project settlement must fail", result is DomainResult.Error)
    }
}

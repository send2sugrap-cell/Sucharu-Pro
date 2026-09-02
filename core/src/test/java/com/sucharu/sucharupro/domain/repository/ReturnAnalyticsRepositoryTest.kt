package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeReturnAnalyticsDataSource
import com.sucharu.sucharupro.data.datasource.FakeReturnDataSource
import com.sucharu.sucharupro.data.repository.ReturnAnalyticsRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.returns.ReturnAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.returns.ReturnExceptionStatus
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit and repository integration tests for [ReturnAnalyticsRepositoryImpl] (Module 11 Step 06 Chunk 02).
 */
class ReturnAnalyticsRepositoryTest {

    private lateinit var returnDataSource: FakeReturnDataSource
    private lateinit var analyticsDataSource: FakeReturnAnalyticsDataSource
    private lateinit var repository: ReturnAnalyticsRepositoryImpl

    private val projectId = "PRJ-REPO-01"

    @Before
    fun setUp() {
        returnDataSource = FakeReturnDataSource()
        analyticsDataSource = FakeReturnAnalyticsDataSource()
        repository = ReturnAnalyticsRepositoryImpl(returnDataSource, analyticsDataSource)
    }

    @Test
    fun `test getAnalyticsSummary returns valid aggregations`() = runBlocking {
        val now = System.currentTimeMillis()
        val ret = ReturnRequest(
            returnId = "RET-01",
            projectId = projectId,
            returnNo = "RN-01",
            customerId = "CUST-01",
            originalChallanId = "CH-01",
            status = ReturnStatus.PROCESSED,
            reason = ReturnReason.PRINTING_DEFECT,
            requestedBy = "USER-01",
            createdAt = now
        )
        val items = listOf(
            ReturnItem("RI-01", "RET-01", "P-1", "CI-1", 100, 80, 20, "PCS")
        )
        val settlement = ReturnSettlement(
            settlementId = "SET-01",
            returnId = "RET-01",
            projectId = projectId,
            customerId = "CUST-01",
            resolutionType = ReturnResolutionType.CREDIT_NOTE,
            amount = Money(25000.0),
            status = ReturnSettlementStatus.COMPLETED,
            settledBy = "ADMIN",
            idempotencyKey = "K-01"
        )

        returnDataSource.insertReturn(ret, items)
        returnDataSource.insertOrUpdateSettlement(settlement)

        val result = repository.getAnalyticsSummary(
            projectId = projectId,
            period = ReturnAnalyticsPeriod.ALL_TIME,
            totalDispatchedCount = 20,
            callerRole = UserRole.ADMIN,
            callerProjectId = projectId
        )

        assertTrue(result is DomainResult.Success)
        val summary = (result as DomainResult.Success).data
        assertEquals(1, summary.totalReturns)
        assertEquals(5.0, summary.returnRate, 0.001)
        assertEquals(100, summary.totalRequestedQuantity)
        assertEquals(80, summary.totalAcceptedQuantity)
        assertEquals(20, summary.totalRejectedQuantity)
        assertEquals(Money(25000.0), summary.totalSettledValue)
    }

    @Test
    fun `test runGovernanceInspection and exception acknowledgement lifecycle`() = runBlocking {
        val now = System.currentTimeMillis()
        val oneHour = 3_600_000L
        val oldUninspected = ReturnRequest(
            returnId = "RET-AGING-01",
            projectId = projectId,
            returnNo = "RN-AGING-01",
            customerId = "CUST-01",
            originalChallanId = "CH-01",
            status = ReturnStatus.UNDER_INSPECTION,
            reason = ReturnReason.DAMAGED,
            requestedBy = "USER-01",
            createdAt = now - (60 * oneHour) // 60h ago
        )
        returnDataSource.insertReturn(oldUninspected, emptyList())

        // Run inspection
        val inspectRes = repository.runGovernanceInspection(
            projectId = projectId,
            actorId = "SYSTEM",
            callerRole = UserRole.ADMIN,
            callerProjectId = projectId
        )
        assertTrue(inspectRes is DomainResult.Success)
        val exceptions = (inspectRes as DomainResult.Success).data
        assertEquals(1, exceptions.size)
        val ex = exceptions.first()
        assertEquals(ReturnExceptionStatus.OPEN, ex.status)

        // Acknowledge
        val ackRes = repository.acknowledgeException(
            exceptionId = ex.exceptionId,
            actorId = "MANAGER-01",
            expectedVersion = ex.version,
            callerRole = UserRole.MANAGER,
            callerProjectId = projectId
        )
        assertTrue(ackRes is DomainResult.Success)
        val acked = (ackRes as DomainResult.Success).data
        assertEquals(ReturnExceptionStatus.ACKNOWLEDGED, acked.status)
        assertEquals("MANAGER-01", acked.acknowledgedBy)

        // Resolve
        val resolveRes = repository.resolveException(
            exceptionId = acked.exceptionId,
            actorId = "MANAGER-01",
            resolutionNotes = "Inspector assigned and report submitted.",
            expectedVersion = acked.version,
            callerRole = UserRole.MANAGER,
            callerProjectId = projectId
        )
        assertTrue(resolveRes is DomainResult.Success)
        val resolved = (resolveRes as DomainResult.Success).data
        assertEquals(ReturnExceptionStatus.RESOLVED, resolved.status)
        assertEquals("MANAGER-01", resolved.resolvedBy)
    }
}

package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.FakeReturnAnalyticsDataSource
import com.sucharu.sucharupro.data.datasource.FakeReturnDataSource
import com.sucharu.sucharupro.data.repository.ReturnAnalyticsRepositoryImpl
import com.sucharu.sucharupro.data.repository.ReturnRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.returns.ReturnActivityType
import com.sucharu.sucharupro.domain.model.returns.ReturnExceptionStatus
import com.sucharu.sucharupro.domain.model.returns.ReturnExceptionType
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
 * End-to-End Governance and Exception Lifecycle test suite for Return Management (Module 11 Step 06 Chunk 05).
 */
class ReturnGovernanceEndToEndTest {

    private lateinit var returnDataSource: FakeReturnDataSource
    private lateinit var analyticsDataSource: FakeReturnAnalyticsDataSource
    private lateinit var repository: ReturnAnalyticsRepositoryImpl

    private val projectId = "PRJ-E2E-GOV"
    private val now = 1_700_000_000_000L
    private val oneHour = 3_600_000L

    @Before
    fun setUp() {
        returnDataSource = FakeReturnDataSource()
        analyticsDataSource = FakeReturnAnalyticsDataSource()
        repository = ReturnAnalyticsRepositoryImpl(returnDataSource, analyticsDataSource)
    }

    @Test
    fun `test end to end governance scan, exception lifecycle and audit verification`() = runBlocking {
        // Setup 1: Aging uninspected return
        val ret1 = ReturnRequest(
            returnId = "RET-AGING-1",
            projectId = projectId,
            returnNo = "RN-AGING-1",
            customerId = "CUST-A",
            originalChallanId = "CH-1",
            status = ReturnStatus.UNDER_INSPECTION,
            reason = ReturnReason.PRINTING_DEFECT,
            requestedBy = "USER-1",
            createdAt = now - (72 * oneHour) // 72h old (> 48h SLA)
        )

        // Setup 2: High value return
        val ret2 = ReturnRequest(
            returnId = "RET-HV-2",
            projectId = projectId,
            returnNo = "RN-HV-2",
            customerId = "CUST-B",
            originalChallanId = "CH-2",
            status = ReturnStatus.PROCESSED,
            reason = ReturnReason.DAMAGED,
            requestedBy = "USER-2",
            createdAt = now - (10 * oneHour)
        )
        val set2 = ReturnSettlement(
            settlementId = "SET-HV-2",
            returnId = "RET-HV-2",
            projectId = projectId,
            customerId = "CUST-B",
            resolutionType = ReturnResolutionType.CREDIT_NOTE,
            amount = Money(75000.0), // > 50,000 threshold
            status = ReturnSettlementStatus.COMPLETED,
            settledBy = "ADMIN",
            settledAt = now - (5 * oneHour),
            idempotencyKey = "K-SET-HV-2"
        )

        returnDataSource.insertReturn(ret1, emptyList())
        returnDataSource.insertReturn(ret2, emptyList())
        returnDataSource.insertOrUpdateSettlement(set2)

        // 1. Run Governance Scan
        val scanRes = repository.runGovernanceInspection(
            projectId = projectId,
            actorId = "GOV-SCAN-DAEMON",
            callerRole = UserRole.ADMIN,
            callerProjectId = projectId
        )
        assertTrue(scanRes is DomainResult.Success)
        val detected = (scanRes as DomainResult.Success).data
        assertEquals(2, detected.size)

        val agingEx = detected.find { it.exceptionType == ReturnExceptionType.AGING_UNINSPECTED }
        val highValEx = detected.find { it.exceptionType == ReturnExceptionType.HIGH_VALUE_RETURN }

        assertNotNull(agingEx)
        assertNotNull(highValEx)
        assertEquals(ReturnExceptionStatus.OPEN, agingEx!!.status)
        assertEquals(ReturnExceptionStatus.OPEN, highValEx!!.status)

        // 2. Acknowledge the Aging Exception
        val ackRes = repository.acknowledgeException(
            exceptionId = agingEx.exceptionId,
            actorId = "QC-MANAGER",
            expectedVersion = agingEx.version,
            callerRole = UserRole.MANAGER,
            callerProjectId = projectId
        )
        assertTrue(ackRes is DomainResult.Success)
        val acked = (ackRes as DomainResult.Success).data
        assertEquals(ReturnExceptionStatus.ACKNOWLEDGED, acked.status)
        assertEquals("QC-MANAGER", acked.acknowledgedBy)

        // 3. Resolve the Aging Exception
        val resolveRes = repository.resolveException(
            exceptionId = acked.exceptionId,
            actorId = "QC-MANAGER",
            resolutionNotes = "Inspector dispatched and physical verification completed.",
            expectedVersion = acked.version,
            callerRole = UserRole.MANAGER,
            callerProjectId = projectId
        )
        assertTrue(resolveRes is DomainResult.Success)
        val resolved = (resolveRes as DomainResult.Success).data
        assertEquals(ReturnExceptionStatus.RESOLVED, resolved.status)
        assertEquals("QC-MANAGER", resolved.resolvedBy)

        // 4. Dismiss the High Value Exception with rationale
        val dismissRes = repository.dismissException(
            exceptionId = highValEx.exceptionId,
            actorId = "FINANCE-DIRECTOR",
            resolutionNotes = "High value pre-approved by executive committee.",
            expectedVersion = highValEx.version,
            callerRole = UserRole.ACCOUNTS,
            callerProjectId = projectId
        )
        assertTrue(dismissRes is DomainResult.Success)
        val dismissed = (dismissRes as DomainResult.Success).data
        assertEquals(ReturnExceptionStatus.DISMISSED, dismissed.status)
        assertEquals("FINANCE-DIRECTOR", dismissed.resolvedBy)

        // 5. Verify Audit Trail for RET-AGING-1
        val events = returnDataSource.getActivityEvents("RET-AGING-1")
        assertEquals(3, events.size)
        assertEquals(ReturnActivityType.RETURN_EXCEPTION_DETECTED, events[0].activityType)
        assertEquals(ReturnActivityType.RETURN_EXCEPTION_ACKNOWLEDGED, events[1].activityType)
        assertEquals(ReturnActivityType.RETURN_EXCEPTION_RESOLVED, events[2].activityType)
    }
}

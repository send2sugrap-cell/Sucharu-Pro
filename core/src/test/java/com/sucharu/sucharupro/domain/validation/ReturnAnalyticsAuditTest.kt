package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.data.datasource.FakeReturnAnalyticsDataSource
import com.sucharu.sucharupro.data.datasource.FakeReturnDataSource
import com.sucharu.sucharupro.data.repository.ReturnAnalyticsRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.returns.ReturnActivityType
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
 * Audit trail verification tests for Return Governance Exceptions (Module 11 Step 06 Chunk 03).
 */
class ReturnAnalyticsAuditTest {

    private lateinit var returnDataSource: FakeReturnDataSource
    private lateinit var analyticsDataSource: FakeReturnAnalyticsDataSource
    private lateinit var repository: ReturnAnalyticsRepositoryImpl

    private val projectId = "PRJ-AUDIT-01"

    @Before
    fun setUp() {
        returnDataSource = FakeReturnDataSource()
        analyticsDataSource = FakeReturnAnalyticsDataSource()
        repository = ReturnAnalyticsRepositoryImpl(returnDataSource, analyticsDataSource)
    }

    @Test
    fun `test governance lifecycle emits append-only activity events`() = runBlocking {
        val now = System.currentTimeMillis()
        val oneHour = 3_600_000L
        val ret = ReturnRequest(
            returnId = "RET-AUDIT-01",
            projectId = projectId,
            returnNo = "RN-AUDIT-01",
            customerId = "C-01",
            originalChallanId = "CH-1",
            status = ReturnStatus.UNDER_INSPECTION,
            reason = ReturnReason.PRINTING_DEFECT,
            requestedBy = "USER-1",
            createdAt = now - (60 * oneHour)
        )
        returnDataSource.insertReturn(ret, emptyList())

        // 1. Detect
        val detectRes = repository.runGovernanceInspection(projectId, "SYSTEM", callerRole = UserRole.ADMIN, callerProjectId = projectId)
        assertTrue(detectRes is DomainResult.Success)
        val ex = (detectRes as DomainResult.Success).data.first()

        // 2. Acknowledge
        repository.acknowledgeException(ex.exceptionId, "MGR-01", ex.version, callerRole = UserRole.MANAGER, callerProjectId = projectId)

        // 3. Resolve
        repository.resolveException(ex.exceptionId, "MGR-01", "Action taken.", ex.version + 1, callerRole = UserRole.MANAGER, callerProjectId = projectId)

        val events = returnDataSource.getActivityEvents("RET-AUDIT-01")
        assertEquals(3, events.size)
        assertEquals(ReturnActivityType.RETURN_EXCEPTION_DETECTED, events[0].activityType)
        assertEquals(ReturnActivityType.RETURN_EXCEPTION_ACKNOWLEDGED, events[1].activityType)
        assertEquals(ReturnActivityType.RETURN_EXCEPTION_RESOLVED, events[2].activityType)
    }
}

package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.data.datasource.FakeReturnAnalyticsDataSource
import com.sucharu.sucharupro.data.datasource.FakeReturnDataSource
import com.sucharu.sucharupro.data.repository.ReturnAnalyticsRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.returns.ReturnReason
import com.sucharu.sucharupro.domain.model.returns.ReturnRequest
import com.sucharu.sucharupro.domain.model.returns.ReturnStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Idempotency and duplicate exception prevention unit tests (Module 11 Step 06 Chunk 03).
 */
class ReturnAnalyticsIdempotencyTest {

    private lateinit var returnDataSource: FakeReturnDataSource
    private lateinit var analyticsDataSource: FakeReturnAnalyticsDataSource
    private lateinit var repository: ReturnAnalyticsRepositoryImpl

    private val projectId = "PRJ-IDEMP-01"

    @Before
    fun setUp() {
        returnDataSource = FakeReturnDataSource()
        analyticsDataSource = FakeReturnAnalyticsDataSource()
        repository = ReturnAnalyticsRepositoryImpl(returnDataSource, analyticsDataSource)
    }

    @Test
    fun `test repeated governance inspections do not produce duplicate exception records`() = runBlocking {
        val now = System.currentTimeMillis()
        val oneHour = 3_600_000L
        val oldReturn = ReturnRequest(
            returnId = "RET-IDEMP-1",
            projectId = projectId,
            returnNo = "RN-IDEMP-1",
            customerId = "C-01",
            originalChallanId = "CH-1",
            status = ReturnStatus.UNDER_INSPECTION,
            reason = ReturnReason.PRINTING_DEFECT,
            requestedBy = "USER-1",
            createdAt = now - (60 * oneHour)
        )
        returnDataSource.insertReturn(oldReturn, emptyList())

        // Run inspection first time
        val res1 = repository.runGovernanceInspection(projectId, "SYSTEM", callerRole = UserRole.ADMIN, callerProjectId = projectId)
        assertTrue(res1 is DomainResult.Success)
        val list1 = (res1 as DomainResult.Success).data
        assertEquals(1, list1.size)

        // Run inspection second time
        val res2 = repository.runGovernanceInspection(projectId, "SYSTEM", callerRole = UserRole.ADMIN, callerProjectId = projectId)
        assertTrue(res2 is DomainResult.Success)
        val list2 = (res2 as DomainResult.Success).data
        assertEquals(1, list2.size)
        assertEquals(list1.first().exceptionId, list2.first().exceptionId)
    }
}

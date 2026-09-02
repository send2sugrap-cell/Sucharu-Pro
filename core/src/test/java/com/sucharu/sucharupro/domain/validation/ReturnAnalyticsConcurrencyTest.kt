package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.data.datasource.FakeReturnAnalyticsDataSource
import com.sucharu.sucharupro.data.datasource.FakeReturnDataSource
import com.sucharu.sucharupro.data.repository.ReturnAnalyticsRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.returns.ReturnException
import com.sucharu.sucharupro.domain.model.returns.ReturnExceptionStatus
import com.sucharu.sucharupro.domain.model.returns.ReturnExceptionType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Optimistic concurrency control tests for Return Governance Exceptions (Module 11 Step 06 Chunk 03).
 */
class ReturnAnalyticsConcurrencyTest {

    private lateinit var analyticsDataSource: FakeReturnAnalyticsDataSource
    private lateinit var repository: ReturnAnalyticsRepositoryImpl

    private val projectId = "PRJ-CONC-01"

    @Before
    fun setUp() {
        analyticsDataSource = FakeReturnAnalyticsDataSource()
        repository = ReturnAnalyticsRepositoryImpl(
            FakeReturnDataSource(),
            analyticsDataSource
        )
    }

    @Test
    fun `test matching version updates increment record version`() = runBlocking {
        val ex = ReturnException(
            exceptionId = "EX-01",
            projectId = projectId,
            exceptionType = ReturnExceptionType.AGING_UNINSPECTED,
            status = ReturnExceptionStatus.OPEN,
            description = "Test exception",
            version = 1L,
            idempotencyKey = "K-01"
        )
        analyticsDataSource.insertOrUpdateException(ex)

        val res = repository.acknowledgeException(
            exceptionId = "EX-01",
            actorId = "MANAGER-1",
            expectedVersion = 1L,
            callerRole = UserRole.MANAGER,
            callerProjectId = projectId
        )
        assertTrue(res is DomainResult.Success)
        val updated = (res as DomainResult.Success).data
        assertEquals(2L, updated.version)
    }

    @Test
    fun `test stale version update returns concurrency conflict`() = runBlocking {
        val ex = ReturnException(
            exceptionId = "EX-02",
            projectId = projectId,
            exceptionType = ReturnExceptionType.UNSETTLED_PROCESSED,
            status = ReturnExceptionStatus.OPEN,
            description = "Test exception",
            version = 2L,
            idempotencyKey = "K-02"
        )
        analyticsDataSource.insertOrUpdateException(ex)

        val res = repository.acknowledgeException(
            exceptionId = "EX-02",
            actorId = "MANAGER-1",
            expectedVersion = 1L, // Stale version
            callerRole = UserRole.MANAGER,
            callerProjectId = projectId
        )
        assertTrue(res is DomainResult.Error)
        val err = res as DomainResult.Error
        assertTrue(err.message.contains("Concurrency conflict"))
    }
}

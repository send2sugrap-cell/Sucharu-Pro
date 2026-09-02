package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.data.datasource.FakeReturnAnalyticsDataSource
import com.sucharu.sucharupro.data.datasource.FakeReturnDataSource
import com.sucharu.sucharupro.data.repository.ReturnAnalyticsRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.returns.ReturnAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Multi-tenant project isolation unit tests for Return Analytics (Module 11 Step 06 Chunk 03).
 */
class ReturnAnalyticsProjectIsolationTest {

    private lateinit var repository: ReturnAnalyticsRepositoryImpl

    @Before
    fun setUp() {
        repository = ReturnAnalyticsRepositoryImpl(
            FakeReturnDataSource(),
            FakeReturnAnalyticsDataSource()
        )
    }

    @Test
    fun `test matching project access succeeds`() = runBlocking {
        val result = repository.getAnalyticsSummary(
            projectId = "PRJ-ALPHA",
            period = ReturnAnalyticsPeriod.THIS_MONTH,
            callerRole = UserRole.ADMIN,
            callerProjectId = "PRJ-ALPHA"
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `test mismatched cross-project access is rejected`() = runBlocking {
        val result = repository.getAnalyticsSummary(
            projectId = "PRJ-ALPHA",
            period = ReturnAnalyticsPeriod.THIS_MONTH,
            callerRole = UserRole.ADMIN,
            callerProjectId = "PRJ-BETA"
        )
        assertTrue(result is DomainResult.Error)
        val error = result as DomainResult.Error
        assertTrue(error.message.contains("Access denied: Caller project 'PRJ-BETA'"))
    }
}

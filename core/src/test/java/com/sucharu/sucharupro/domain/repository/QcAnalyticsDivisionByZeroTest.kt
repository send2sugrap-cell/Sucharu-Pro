package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionJobDataSource
import com.sucharu.sucharupro.data.datasource.FakeQcAnalyticsDataSource
import com.sucharu.sucharupro.data.repository.QcAnalyticsRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QcAnalyticsDivisionByZeroTest {

    private lateinit var analyticsDataSource: FakeQcAnalyticsDataSource
    private lateinit var jobDataSource: FakeProductionJobDataSource
    private lateinit var repository: QcAnalyticsRepository

    @Before
    fun setup() {
        analyticsDataSource = FakeQcAnalyticsDataSource()
        jobDataSource = FakeProductionJobDataSource()

        repository = QcAnalyticsRepositoryImpl(
            analyticsDataSource = analyticsDataSource,
            productionJobDataSource = jobDataSource
        )
    }

    @Test
    fun `empty dataset returns zeros without division-by-zero crashes or NaN`() = runBlocking {
        val res = repository.getSummary(
            period = QcAnalyticsPeriod.custom("2026-08-01T00:00:00Z", "2026-08-31T23:59:59Z"),
            projectId = "PRJ-01",
            callerRole = UserRole.ADMIN
        )

        assertTrue(res is DomainResult.Success)
        val summary = (res as DomainResult.Success).data
        assertEquals(0, summary.totalJobs)
        assertEquals(0.0, summary.totalQcCost, 0.001)
        assertEquals(0L, summary.totalQcTimeMinutes)
        assertEquals(0.0, summary.averageQcCostPerJob, 0.001)
        assertEquals(0.0, summary.averageQcTimeMinutesPerJob, 0.001)
        assertEquals(0.0, summary.firstPassQcRate, 0.001)
        assertEquals(0.0, summary.reworkRate, 0.001)
        assertEquals(0.0, summary.reQcRate, 0.001)
        assertFalse(summary.averageQcCostPerJob.isNaN())
        assertFalse(summary.averageQcCostPerJob.isInfinite())
    }
}

package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionJobDataSource
import com.sucharu.sucharupro.data.datasource.FakeQcAnalyticsDataSource
import com.sucharu.sucharupro.data.repository.QcAnalyticsRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QcAnalyticsSourceIntegrityTest {

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
    fun `invalid period timestamp formats safely fail with explicit domain errors`() = runBlocking {
        val invalidPeriod = QcAnalyticsPeriod.custom("invalid-start", "invalid-end")
        val result = repository.getSummary(
            period = invalidPeriod,
            projectId = "PRJ-01",
            callerRole = UserRole.ADMIN
        )

        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Invalid timestamp format"))
    }
}

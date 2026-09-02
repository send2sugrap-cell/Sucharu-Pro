package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionJobDataSource
import com.sucharu.sucharupro.data.datasource.FakeQcAnalyticsDataSource
import com.sucharu.sucharupro.data.datasource.FakeQcCostTimeDataSource
import com.sucharu.sucharupro.data.repository.QcAnalyticsRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.QcCostEntry
import com.sucharu.sucharupro.domain.model.qc.QcCostStatus
import com.sucharu.sucharupro.domain.model.qc.QcCostType
import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QcAnalyticsTrendTest {

    private lateinit var analyticsDataSource: FakeQcAnalyticsDataSource
    private lateinit var jobDataSource: FakeProductionJobDataSource
    private lateinit var costTimeDataSource: FakeQcCostTimeDataSource
    private lateinit var repository: QcAnalyticsRepository

    @Before
    fun setup() {
        analyticsDataSource = FakeQcAnalyticsDataSource()
        jobDataSource = FakeProductionJobDataSource()
        costTimeDataSource = FakeQcCostTimeDataSource()

        repository = QcAnalyticsRepositoryImpl(
            analyticsDataSource = analyticsDataSource,
            productionJobDataSource = jobDataSource,
            qcCostTimeDataSource = costTimeDataSource
        )
    }

    @Test
    fun `getTrends returns structured temporal trend points`() = runBlocking {
        costTimeDataSource.insertCostEntry(
            QcCostEntry(
                id = "C1",
                projectId = "PRJ-01",
                productionJobId = "JOB-01",
                costType = QcCostType.INSPECTION,
                description = "Day 1 Cost",
                quantity = 1.0,
                unitCost = 100.0,
                totalCost = 100.0,
                recordedBy = "insp-01",
                recordedAt = "2026-08-10T10:00:00Z",
                status = QcCostStatus.RECORDED,
                createdAt = "2026-08-10T10:00:00Z",
                updatedAt = "2026-08-10T10:00:00Z"
            )
        )

        val res = repository.getTrends(
            period = QcAnalyticsPeriod.custom("2026-08-08T00:00:00Z", "2026-08-12T00:00:00Z"),
            projectId = "PRJ-01",
            callerRole = UserRole.ADMIN
        )

        assertTrue(res is DomainResult.Success)
        val trendPoints = (res as DomainResult.Success).data
        assertTrue(trendPoints.isNotEmpty())
    }
}

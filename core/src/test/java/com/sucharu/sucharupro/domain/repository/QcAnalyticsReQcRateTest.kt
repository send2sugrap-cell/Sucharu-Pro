package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionJobDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionReQcDataSource
import com.sucharu.sucharupro.data.datasource.FakeQcAnalyticsDataSource
import com.sucharu.sucharupro.data.repository.QcAnalyticsRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.qc.ReQcInspection
import com.sucharu.sucharupro.domain.model.qc.ReQcStatus
import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QcAnalyticsReQcRateTest {

    private lateinit var analyticsDataSource: FakeQcAnalyticsDataSource
    private lateinit var jobDataSource: FakeProductionJobDataSource
    private lateinit var reQcDataSource: FakeProductionReQcDataSource
    private lateinit var repository: QcAnalyticsRepository

    @Before
    fun setup() {
        analyticsDataSource = FakeQcAnalyticsDataSource()
        jobDataSource = FakeProductionJobDataSource()
        reQcDataSource = FakeProductionReQcDataSource()

        repository = QcAnalyticsRepositoryImpl(
            analyticsDataSource = analyticsDataSource,
            productionJobDataSource = jobDataSource,
            reQcDataSource = reQcDataSource
        )
    }

    @Test
    fun `reQc rate and failed cycles are tracked accurately`() = runBlocking {
        val j1Res = jobDataSource.insertJob(
            ProductionJob(
                jobId = "JOB-01",
                jobNumber = "JOB-001",
                orderId = "PRJ-01",
                orderNumber = "ORD-001",
                customerId = "cust-01",
                handoffId = "ho-01",
                title = "Job 1",
                quantity = 1000,
                status = ProductionJobStatus.IN_PROGRESS,
                createdAt = "2026-08-17T08:00:00Z",
                updatedAt = "2026-08-17T08:00:00Z"
            )
        )
        val j2Res = jobDataSource.insertJob(
            ProductionJob(
                jobId = "JOB-02",
                jobNumber = "JOB-002",
                orderId = "PRJ-01",
                orderNumber = "ORD-001",
                customerId = "cust-01",
                handoffId = "ho-02",
                title = "Job 2",
                quantity = 1000,
                status = ProductionJobStatus.IN_PROGRESS,
                createdAt = "2026-08-17T08:00:00Z",
                updatedAt = "2026-08-17T08:00:00Z"
            )
        )
        assertTrue(j1Res is DomainResult.Success)
        assertTrue(j2Res is DomainResult.Success)

        reQcDataSource.insertReQc(
            ReQcInspection(
                reQcId = "REQC-1",
                projectId = "PRJ-01",
                productionJobId = "JOB-01",
                productionReworkId = "REW-1",
                cycleNumber = 1,
                status = ReQcStatus.FAILED,
                createdBy = "insp-01",
                createdAt = "2026-08-17T09:00:00Z",
                updatedAt = "2026-08-17T09:00:00Z"
            )
        )

        val summaryRes = repository.getSummary(
            period = QcAnalyticsPeriod.custom("2026-08-01T00:00:00Z", "2026-08-31T23:59:59Z"),
            projectId = "PRJ-01",
            callerRole = UserRole.ADMIN
        )

        assertTrue(summaryRes is DomainResult.Success)
        val summary = (summaryRes as DomainResult.Success).data
        assertEquals(2, summary.totalJobs)
        assertEquals(1, summary.totalReQcCycles)
        assertEquals(50.0, summary.reQcRate, 0.001) // 1 out of 2 jobs
        assertEquals(1, summary.failedReQcCount)
    }
}

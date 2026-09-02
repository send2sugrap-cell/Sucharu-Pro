package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeFinalQcDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionJobDataSource
import com.sucharu.sucharupro.data.datasource.FakeQcAnalyticsDataSource
import com.sucharu.sucharupro.data.repository.QcAnalyticsRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.qc.FinalQcDecision
import com.sucharu.sucharupro.domain.model.qc.FinalQcInspection
import com.sucharu.sucharupro.domain.model.qc.FinalQcReleaseStatus
import com.sucharu.sucharupro.domain.model.qc.FinalQcStatus
import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QcAnalyticsFinalQcPassRateTest {

    private lateinit var analyticsDataSource: FakeQcAnalyticsDataSource
    private lateinit var jobDataSource: FakeProductionJobDataSource
    private lateinit var finalQcDataSource: FakeFinalQcDataSource
    private lateinit var repository: QcAnalyticsRepository

    @Before
    fun setup() {
        analyticsDataSource = FakeQcAnalyticsDataSource()
        jobDataSource = FakeProductionJobDataSource()
        finalQcDataSource = FakeFinalQcDataSource()

        repository = QcAnalyticsRepositoryImpl(
            analyticsDataSource = analyticsDataSource,
            productionJobDataSource = jobDataSource,
            finalQcDataSource = finalQcDataSource
        )
    }

    @Test
    fun `final qc pass rate is 100 percent when final qc passes and is released`() = runBlocking {
        jobDataSource.insertJob(
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

        finalQcDataSource.insertFinalQc(
            FinalQcInspection(
                finalQcId = "FQC-01",
                projectId = "PRJ-01",
                productionJobId = "JOB-01",
                totalQuantity = 1000,
                status = FinalQcStatus.RELEASED,
                decision = FinalQcDecision.PASS,
                releaseStatus = FinalQcReleaseStatus.AUTHORIZED,
                createdAt = "2026-08-17T10:00:00Z",
                updatedAt = "2026-08-17T10:00:00Z"
            )
        )

        val summaryRes = repository.getSummary(
            period = QcAnalyticsPeriod.custom("2026-08-01T00:00:00Z", "2026-08-31T23:59:59Z"),
            projectId = "PRJ-01",
            callerRole = UserRole.ADMIN
        )

        assertTrue(summaryRes is DomainResult.Success)
        val summary = (summaryRes as DomainResult.Success).data
        assertEquals(100.0, summary.finalQcPassRate, 0.001)
        assertEquals(1, summary.releasedJobCount)
    }
}

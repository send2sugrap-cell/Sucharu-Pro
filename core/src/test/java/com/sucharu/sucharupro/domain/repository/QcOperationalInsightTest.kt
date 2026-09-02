package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionDefectDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionJobDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionReQcDataSource
import com.sucharu.sucharupro.data.datasource.FakeQcAnalyticsDataSource
import com.sucharu.sucharupro.data.datasource.FakeQcCostTimeDataSource
import com.sucharu.sucharupro.data.repository.QcAnalyticsRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.qc.QcCostEntry
import com.sucharu.sucharupro.domain.model.qc.QcCostStatus
import com.sucharu.sucharupro.domain.model.qc.QcCostType
import com.sucharu.sucharupro.domain.model.qc.ReQcInspection
import com.sucharu.sucharupro.domain.model.qc.ReQcStatus
import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsThresholdConfig
import com.sucharu.sucharupro.domain.model.qc.analytics.QcInsightSeverity
import com.sucharu.sucharupro.domain.model.qc.analytics.QcInsightType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QcOperationalInsightTest {

    private lateinit var analyticsDataSource: FakeQcAnalyticsDataSource
    private lateinit var jobDataSource: FakeProductionJobDataSource
    private lateinit var reQcDataSource: FakeProductionReQcDataSource
    private lateinit var costTimeDataSource: FakeQcCostTimeDataSource
    private lateinit var repository: QcAnalyticsRepository

    @Before
    fun setup() {
        analyticsDataSource = FakeQcAnalyticsDataSource()
        jobDataSource = FakeProductionJobDataSource()
        reQcDataSource = FakeProductionReQcDataSource()
        costTimeDataSource = FakeQcCostTimeDataSource()

        repository = QcAnalyticsRepositoryImpl(
            analyticsDataSource = analyticsDataSource,
            productionJobDataSource = jobDataSource,
            reQcDataSource = reQcDataSource,
            qcCostTimeDataSource = costTimeDataSource
        )
    }

    @Test
    fun `repeated failure cycles trigger deterministic REPEATED_FAILURE critical insight`() = runBlocking {
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

        // 2 Re-QC cycles on JOB-01
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
        reQcDataSource.insertReQc(
            ReQcInspection(
                reQcId = "REQC-2",
                projectId = "PRJ-01",
                productionJobId = "JOB-01",
                productionReworkId = "REW-2",
                cycleNumber = 2,
                status = ReQcStatus.PASSED,
                createdBy = "insp-01",
                createdAt = "2026-08-17T10:00:00Z",
                updatedAt = "2026-08-17T10:00:00Z"
            )
        )

        val res = repository.getOperationalInsights(
            period = QcAnalyticsPeriod.custom("2026-08-01T00:00:00Z", "2026-08-31T23:59:59Z"),
            projectId = "PRJ-01",
            thresholdConfig = QcAnalyticsThresholdConfig(repeatedFailureCycleThreshold = 2),
            callerRole = UserRole.ADMIN
        )

        assertTrue(res is DomainResult.Success)
        val insights = (res as DomainResult.Success).data
        val repeatedFailureInsight = insights.find { it.type == QcInsightType.REPEATED_FAILURE }
        assertTrue(repeatedFailureInsight != null)
        assertEquals(QcInsightSeverity.CRITICAL, repeatedFailureInsight!!.severity)
    }
}

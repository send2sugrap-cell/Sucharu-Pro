package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionJobDataSource
import com.sucharu.sucharupro.data.datasource.FakeQcAnalyticsDataSource
import com.sucharu.sucharupro.data.datasource.FakeQcCostTimeDataSource
import com.sucharu.sucharupro.data.repository.QcAnalyticsRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.qc.QcCostEntry
import com.sucharu.sucharupro.domain.model.qc.QcCostStatus
import com.sucharu.sucharupro.domain.model.qc.QcCostType
import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsMetricType
import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QcAnalyticsCostMetricTest {

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
    fun `getMetric calculates total cost and ignores cancelled entries`() = runBlocking {
        val job = ProductionJob(
            jobId = "JOB-01",
            jobNumber = "JOB-001",
            orderId = "PRJ-01",
            orderNumber = "ORD-001",
            customerId = "cust-01",
            handoffId = "ho-01",
            title = "Job",
            quantity = 1000,
            status = ProductionJobStatus.IN_PROGRESS,
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z"
        )
        jobDataSource.insertJob(job)

        costTimeDataSource.insertCostEntry(
            QcCostEntry(
                id = "C1",
                projectId = "PRJ-01",
                productionJobId = "JOB-01",
                costType = QcCostType.INSPECTION,
                description = "Valid Entry",
                quantity = 1.0,
                unitCost = 300.0,
                totalCost = 300.0,
                recordedBy = "insp-01",
                recordedAt = "2026-08-17T09:00:00Z",
                status = QcCostStatus.RECORDED,
                createdAt = "2026-08-17T09:00:00Z",
                updatedAt = "2026-08-17T09:00:00Z"
            )
        )
        costTimeDataSource.insertCostEntry(
            QcCostEntry(
                id = "C2",
                projectId = "PRJ-01",
                productionJobId = "JOB-01",
                costType = QcCostType.INSPECTION,
                description = "Cancelled Entry",
                quantity = 1.0,
                unitCost = 500.0,
                totalCost = 500.0,
                recordedBy = "insp-01",
                recordedAt = "2026-08-17T09:00:00Z",
                status = QcCostStatus.CANCELLED,
                createdAt = "2026-08-17T09:00:00Z",
                updatedAt = "2026-08-17T09:00:00Z"
            )
        )

        val res = repository.getMetric(
            metricType = QcAnalyticsMetricType.TOTAL_QC_COST,
            period = QcAnalyticsPeriod.custom("2026-08-01T00:00:00Z", "2026-08-31T23:59:59Z"),
            projectId = "PRJ-01",
            callerRole = UserRole.ADMIN
        )

        assertTrue(res is DomainResult.Success)
        assertEquals(300.0, (res as DomainResult.Success).data, 0.001)
    }
}

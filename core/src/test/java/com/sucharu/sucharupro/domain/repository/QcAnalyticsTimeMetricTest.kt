package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionJobDataSource
import com.sucharu.sucharupro.data.datasource.FakeQcAnalyticsDataSource
import com.sucharu.sucharupro.data.datasource.FakeQcCostTimeDataSource
import com.sucharu.sucharupro.data.repository.QcAnalyticsRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.qc.QcTimeEntry
import com.sucharu.sucharupro.domain.model.qc.QcTimeEntryType
import com.sucharu.sucharupro.domain.model.qc.QcTimeStatus
import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsMetricType
import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QcAnalyticsTimeMetricTest {

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
    fun `getMetric calculates total time and ignores cancelled entries`() = runBlocking {
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

        costTimeDataSource.insertTimeEntry(
            QcTimeEntry(
                id = "T1",
                projectId = "PRJ-01",
                productionJobId = "JOB-01",
                entryType = QcTimeEntryType.INSPECTION,
                actorId = "insp-01",
                startedAt = "2026-08-17T08:30:00Z",
                durationMinutes = 50L,
                status = QcTimeStatus.RECORDED,
                createdAt = "2026-08-17T09:20:00Z",
                updatedAt = "2026-08-17T09:20:00Z"
            )
        )
        costTimeDataSource.insertTimeEntry(
            QcTimeEntry(
                id = "T2",
                projectId = "PRJ-01",
                productionJobId = "JOB-01",
                entryType = QcTimeEntryType.INSPECTION,
                actorId = "insp-01",
                startedAt = "2026-08-17T08:30:00Z",
                durationMinutes = 100L,
                status = QcTimeStatus.CANCELLED,
                createdAt = "2026-08-17T09:20:00Z",
                updatedAt = "2026-08-17T09:20:00Z"
            )
        )

        val res = repository.getMetric(
            metricType = QcAnalyticsMetricType.TOTAL_QC_TIME,
            period = QcAnalyticsPeriod.custom("2026-08-01T00:00:00Z", "2026-08-31T23:59:59Z"),
            projectId = "PRJ-01",
            callerRole = UserRole.ADMIN
        )

        assertTrue(res is DomainResult.Success)
        assertEquals(50.0, (res as DomainResult.Success).data, 0.001)
    }
}

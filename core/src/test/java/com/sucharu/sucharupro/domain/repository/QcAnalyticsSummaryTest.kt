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
import com.sucharu.sucharupro.domain.model.qc.QcTimeEntry
import com.sucharu.sucharupro.domain.model.qc.QcTimeEntryType
import com.sucharu.sucharupro.domain.model.qc.QcTimeStatus
import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QcAnalyticsSummaryTest {

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
    fun `getSummary aggregates cost and time across jobs accurately`() = runBlocking {
        val job1 = ProductionJob(
            jobId = "JOB-01",
            jobNumber = "JOB-001",
            orderId = "PRJ-01",
            orderNumber = "ORD-001",
            customerId = "cust-01",
            handoffId = "ho-01",
            title = "Flyer 1000",
            quantity = 1000,
            status = ProductionJobStatus.IN_PROGRESS,
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z"
        )
        val job2 = ProductionJob(
            jobId = "JOB-02",
            jobNumber = "JOB-002",
            orderId = "PRJ-01",
            orderNumber = "ORD-001",
            customerId = "cust-01",
            handoffId = "ho-02",
            title = "Brochure 500",
            quantity = 500,
            status = ProductionJobStatus.IN_PROGRESS,
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z"
        )
        val j1Res = jobDataSource.insertJob(job1)
        val j2Res = jobDataSource.insertJob(job2)
        assertTrue(j1Res is DomainResult.Success)
        assertTrue(j2Res is DomainResult.Success)

        costTimeDataSource.insertCostEntry(
            QcCostEntry(
                id = "C1",
                projectId = "PRJ-01",
                productionJobId = "JOB-01",
                costType = QcCostType.INSPECTION,
                description = "QC Check",
                quantity = 1.0,
                unitCost = 150.0,
                totalCost = 150.0,
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
                productionJobId = "JOB-02",
                costType = QcCostType.INSPECTION,
                description = "QC Check 2",
                quantity = 1.0,
                unitCost = 250.0,
                totalCost = 250.0,
                recordedBy = "insp-01",
                recordedAt = "2026-08-17T09:00:00Z",
                status = QcCostStatus.RECORDED,
                createdAt = "2026-08-17T09:00:00Z",
                updatedAt = "2026-08-17T09:00:00Z"
            )
        )

        costTimeDataSource.insertTimeEntry(
            QcTimeEntry(
                id = "T1",
                projectId = "PRJ-01",
                productionJobId = "JOB-01",
                entryType = QcTimeEntryType.INSPECTION,
                actorId = "insp-01",
                startedAt = "2026-08-17T08:30:00Z",
                endedAt = "2026-08-17T09:00:00Z",
                durationMinutes = 30L,
                status = QcTimeStatus.RECORDED,
                createdAt = "2026-08-17T09:00:00Z",
                updatedAt = "2026-08-17T09:00:00Z"
            )
        )
        costTimeDataSource.insertTimeEntry(
            QcTimeEntry(
                id = "T2",
                projectId = "PRJ-01",
                productionJobId = "JOB-02",
                entryType = QcTimeEntryType.INSPECTION,
                actorId = "insp-01",
                startedAt = "2026-08-17T09:00:00Z",
                endedAt = "2026-08-17T09:45:00Z",
                durationMinutes = 45L,
                status = QcTimeStatus.RECORDED,
                createdAt = "2026-08-17T09:45:00Z",
                updatedAt = "2026-08-17T09:45:00Z"
            )
        )

        val result = repository.getSummary(
            period = QcAnalyticsPeriod.custom("2026-08-01T00:00:00Z", "2026-08-31T23:59:59Z"),
            projectId = "PRJ-01",
            callerRole = UserRole.ADMIN
        )

        assertTrue(result is DomainResult.Success)
        val summary = (result as DomainResult.Success).data
        assertEquals(2, summary.totalJobs)
        assertEquals(400.0, summary.totalQcCost, 0.001)
        assertEquals(200.0, summary.averageQcCostPerJob, 0.001)
        assertEquals(75L, summary.totalQcTimeMinutes)
        assertEquals(37.5, summary.averageQcTimeMinutesPerJob, 0.001)
        assertEquals(100.0, summary.firstPassQcRate, 0.001)
    }
}

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

class QcAnalyticsVarianceTest {

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
    fun `variance calculation correctly measures overrun against planned baseline`() = runBlocking {
        val job = ProductionJob(
            jobId = "JOB-V1",
            jobNumber = "JOB-V1",
            orderId = "PRJ-01",
            orderNumber = "ORD-001",
            customerId = "cust-01",
            handoffId = "ho-01",
            title = "Job V1",
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
                productionJobId = "JOB-V1",
                costType = QcCostType.INSPECTION,
                description = "Cost",
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
                productionJobId = "JOB-V1",
                entryType = QcTimeEntryType.INSPECTION,
                actorId = "insp-01",
                startedAt = "2026-08-17T08:00:00Z",
                durationMinutes = 50L,
                status = QcTimeStatus.RECORDED,
                createdAt = "2026-08-17T08:50:00Z",
                updatedAt = "2026-08-17T08:50:00Z"
            )
        )

        val summaryRes = repository.getSummary(
            period = QcAnalyticsPeriod.custom("2026-08-01T00:00:00Z", "2026-08-31T23:59:59Z"),
            projectId = "PRJ-01",
            callerRole = UserRole.ADMIN
        )

        assertTrue(summaryRes is DomainResult.Success)
        val summary = (summaryRes as DomainResult.Success).data
        assertEquals(150.0, summary.totalCostVariance, 0.001)
        assertEquals(20L, summary.totalTimeVariance)
    }
}

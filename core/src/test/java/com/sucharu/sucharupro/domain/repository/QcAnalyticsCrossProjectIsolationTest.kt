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
import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QcAnalyticsCrossProjectIsolationTest {

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
    fun `analytics query for Project A never includes Project B records`() = runBlocking {
        val jobA = ProductionJob(
            jobId = "JOB-A",
            jobNumber = "JOB-A",
            orderId = "PRJ-A",
            orderNumber = "ORD-A",
            customerId = "cust-01",
            handoffId = "ho-01",
            title = "Job A",
            quantity = 1000,
            status = ProductionJobStatus.IN_PROGRESS,
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z"
        )
        val jobB = ProductionJob(
            jobId = "JOB-B",
            jobNumber = "JOB-B",
            orderId = "PRJ-B",
            orderNumber = "ORD-B",
            customerId = "cust-02",
            handoffId = "ho-02",
            title = "Job B",
            quantity = 1000,
            status = ProductionJobStatus.IN_PROGRESS,
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z"
        )
        jobDataSource.insertJob(jobA)
        jobDataSource.insertJob(jobB)

        costTimeDataSource.insertCostEntry(
            QcCostEntry(
                id = "C-A",
                projectId = "PRJ-A",
                productionJobId = "JOB-A",
                costType = QcCostType.INSPECTION,
                description = "Cost A",
                quantity = 1.0,
                unitCost = 100.0,
                totalCost = 100.0,
                recordedBy = "insp-01",
                recordedAt = "2026-08-17T09:00:00Z",
                status = QcCostStatus.RECORDED,
                createdAt = "2026-08-17T09:00:00Z",
                updatedAt = "2026-08-17T09:00:00Z"
            )
        )
        costTimeDataSource.insertCostEntry(
            QcCostEntry(
                id = "C-B",
                projectId = "PRJ-B",
                productionJobId = "JOB-B",
                costType = QcCostType.INSPECTION,
                description = "Cost B",
                quantity = 1.0,
                unitCost = 500.0,
                totalCost = 500.0,
                recordedBy = "insp-01",
                recordedAt = "2026-08-17T09:00:00Z",
                status = QcCostStatus.RECORDED,
                createdAt = "2026-08-17T09:00:00Z",
                updatedAt = "2026-08-17T09:00:00Z"
            )
        )

        val summaryA = repository.getSummary(
            period = QcAnalyticsPeriod.custom("2026-08-01T00:00:00Z", "2026-08-31T23:59:59Z"),
            projectId = "PRJ-A",
            callerRole = UserRole.ADMIN
        )

        assertTrue(summaryA is DomainResult.Success)
        val dataA = (summaryA as DomainResult.Success).data
        assertEquals(1, dataA.totalJobs)
        assertEquals(100.0, dataA.totalQcCost, 0.001) // Does NOT include 500.0 from PRJ-B
    }
}

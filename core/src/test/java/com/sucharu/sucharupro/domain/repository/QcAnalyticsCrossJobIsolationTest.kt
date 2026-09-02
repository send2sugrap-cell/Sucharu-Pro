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

class QcAnalyticsCrossJobIsolationTest {

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
    fun `per job analytics strictly partitions costs and time by productionJobId`() = runBlocking {
        val j1Res = jobDataSource.insertJob(
            ProductionJob(
                jobId = "JOB-1",
                jobNumber = "JOB-1",
                orderId = "PRJ-01",
                orderNumber = "ORD-01",
                customerId = "cust-01",
                handoffId = "ho-01",
                title = "Job 1",
                quantity = 500,
                status = ProductionJobStatus.IN_PROGRESS,
                createdAt = "2026-08-17T08:00:00Z",
                updatedAt = "2026-08-17T08:00:00Z"
            )
        )
        val j2Res = jobDataSource.insertJob(
            ProductionJob(
                jobId = "JOB-2",
                jobNumber = "JOB-2",
                orderId = "PRJ-01",
                orderNumber = "ORD-01",
                customerId = "cust-01",
                handoffId = "ho-02",
                title = "Job 2",
                quantity = 500,
                status = ProductionJobStatus.IN_PROGRESS,
                createdAt = "2026-08-17T08:00:00Z",
                updatedAt = "2026-08-17T08:00:00Z"
            )
        )
        assertTrue(j1Res is DomainResult.Success)
        assertTrue(j2Res is DomainResult.Success)

        costTimeDataSource.insertCostEntry(
            QcCostEntry(
                id = "C1",
                projectId = "PRJ-01",
                productionJobId = "JOB-1",
                costType = QcCostType.INSPECTION,
                description = "Cost 1",
                quantity = 1.0,
                unitCost = 120.0,
                totalCost = 120.0,
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
                productionJobId = "JOB-2",
                costType = QcCostType.INSPECTION,
                description = "Cost 2",
                quantity = 1.0,
                unitCost = 280.0,
                totalCost = 280.0,
                recordedBy = "insp-01",
                recordedAt = "2026-08-17T09:00:00Z",
                status = QcCostStatus.RECORDED,
                createdAt = "2026-08-17T09:00:00Z",
                updatedAt = "2026-08-17T09:00:00Z"
            )
        )

        val jobAnalyticsRes = repository.getJobAnalytics(
            period = QcAnalyticsPeriod.custom("2026-08-01T00:00:00Z", "2026-08-31T23:59:59Z"),
            projectId = "PRJ-01",
            callerRole = UserRole.ADMIN
        )

        assertTrue(jobAnalyticsRes is DomainResult.Success)
        val list = (jobAnalyticsRes as DomainResult.Success).data
        val j1 = list.find { it.productionJobId == "JOB-1" }
        val j2 = list.find { it.productionJobId == "JOB-2" }
        assertEquals(120.0, j1?.totalQcCost ?: 0.0, 0.001)
        assertEquals(280.0, j2?.totalQcCost ?: 0.0, 0.001)
    }
}

package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionDefectDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionJobDataSource
import com.sucharu.sucharupro.data.datasource.FakeQcAnalyticsDataSource
import com.sucharu.sucharupro.data.datasource.FakeQcCostTimeDataSource
import com.sucharu.sucharupro.data.repository.QcAnalyticsRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.qc.DefectCategory
import com.sucharu.sucharupro.domain.model.qc.DefectSeverity
import com.sucharu.sucharupro.domain.model.qc.DefectSource
import com.sucharu.sucharupro.domain.model.qc.DefectStatus
import com.sucharu.sucharupro.domain.model.qc.ProductionDefect
import com.sucharu.sucharupro.domain.model.qc.QcCostEntry
import com.sucharu.sucharupro.domain.model.qc.QcCostStatus
import com.sucharu.sucharupro.domain.model.qc.QcCostType
import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QcJobAnalyticsTest {

    private lateinit var analyticsDataSource: FakeQcAnalyticsDataSource
    private lateinit var jobDataSource: FakeProductionJobDataSource
    private lateinit var defectDataSource: FakeProductionDefectDataSource
    private lateinit var costTimeDataSource: FakeQcCostTimeDataSource
    private lateinit var repository: QcAnalyticsRepository

    @Before
    fun setup() {
        analyticsDataSource = FakeQcAnalyticsDataSource()
        jobDataSource = FakeProductionJobDataSource()
        defectDataSource = FakeProductionDefectDataSource()
        costTimeDataSource = FakeQcCostTimeDataSource()

        repository = QcAnalyticsRepositoryImpl(
            analyticsDataSource = analyticsDataSource,
            productionJobDataSource = jobDataSource,
            defectDataSource = defectDataSource,
            qcCostTimeDataSource = costTimeDataSource
        )
    }

    @Test
    fun `getJobAnalytics produces accurate per-job metrics and deterministic efficiency score`() = runBlocking {
        val job = ProductionJob(
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
        jobDataSource.insertJob(job)

        defectDataSource.insertDefect(
            ProductionDefect(
                defectId = "DEF-01",
                productionJobId = "JOB-01",
                category = DefectCategory.PRINT_QUALITY,
                severity = DefectSeverity.MAJOR,
                source = DefectSource.PRODUCTION_STAGE,
                title = "Defect",
                description = "Defect",
                affectedQuantity = 10,
                status = DefectStatus.RESOLVED,
                detectedBy = "insp-01",
                detectedAt = "2026-08-17T09:00:00Z",
                createdAt = "2026-08-17T09:00:00Z",
                updatedAt = "2026-08-17T09:00:00Z"
            )
        )

        costTimeDataSource.insertCostEntry(
            QcCostEntry(
                id = "C1",
                projectId = "PRJ-01",
                productionJobId = "JOB-01",
                costType = QcCostType.INSPECTION,
                description = "Cost",
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

        val result = repository.getJobAnalytics(
            period = QcAnalyticsPeriod.custom("2026-08-01T00:00:00Z", "2026-08-31T23:59:59Z"),
            projectId = "PRJ-01",
            callerRole = UserRole.ADMIN
        )

        assertTrue(result is DomainResult.Success)
        val list = (result as DomainResult.Success).data
        assertEquals(1, list.size)
        val jobAnalytics = list.first()
        assertEquals("JOB-01", jobAnalytics.productionJobId)
        assertEquals(150.0, jobAnalytics.totalQcCost, 0.001)
        assertEquals(1, jobAnalytics.defectCount)
        assertFalse(jobAnalytics.firstPassQc)
        assertTrue(jobAnalytics.efficiencyScore < 100.0)
    }
}

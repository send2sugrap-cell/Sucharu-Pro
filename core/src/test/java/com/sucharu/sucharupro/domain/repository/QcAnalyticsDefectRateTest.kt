package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionDefectDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionJobDataSource
import com.sucharu.sucharupro.data.datasource.FakeQcAnalyticsDataSource
import com.sucharu.sucharupro.data.repository.QcAnalyticsRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.qc.DefectCategory
import com.sucharu.sucharupro.domain.model.qc.DefectSeverity
import com.sucharu.sucharupro.domain.model.qc.DefectSource
import com.sucharu.sucharupro.domain.model.qc.DefectStatus
import com.sucharu.sucharupro.domain.model.qc.ProductionDefect
import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QcAnalyticsDefectRateTest {

    private lateinit var analyticsDataSource: FakeQcAnalyticsDataSource
    private lateinit var jobDataSource: FakeProductionJobDataSource
    private lateinit var defectDataSource: FakeProductionDefectDataSource
    private lateinit var repository: QcAnalyticsRepository

    @Before
    fun setup() {
        analyticsDataSource = FakeQcAnalyticsDataSource()
        jobDataSource = FakeProductionJobDataSource()
        defectDataSource = FakeProductionDefectDataSource()

        repository = QcAnalyticsRepositoryImpl(
            analyticsDataSource = analyticsDataSource,
            productionJobDataSource = jobDataSource,
            defectDataSource = defectDataSource
        )
    }

    @Test
    fun `defect metrics calculate counts and per-job averages accurately`() = runBlocking {
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

        defectDataSource.insertDefect(
            ProductionDefect(
                defectId = "DEF-1",
                productionJobId = "JOB-01",
                category = DefectCategory.PRINT_QUALITY,
                severity = DefectSeverity.MAJOR,
                source = DefectSource.PRODUCTION_STAGE,
                title = "Streak",
                description = "Streak",
                affectedQuantity = 10,
                status = DefectStatus.OPEN,
                detectedBy = "insp-01",
                detectedAt = "2026-08-17T08:30:00Z",
                createdAt = "2026-08-17T08:30:00Z",
                updatedAt = "2026-08-17T08:30:00Z"
            )
        )
        defectDataSource.insertDefect(
            ProductionDefect(
                defectId = "DEF-2",
                productionJobId = "JOB-01",
                category = DefectCategory.COLOR_MISMATCH,
                severity = DefectSeverity.MINOR,
                source = DefectSource.PRODUCTION_STAGE,
                title = "Color shift",
                description = "Shift",
                affectedQuantity = 5,
                status = DefectStatus.OPEN,
                detectedBy = "insp-01",
                detectedAt = "2026-08-17T08:40:00Z",
                createdAt = "2026-08-17T08:40:00Z",
                updatedAt = "2026-08-17T08:40:00Z"
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
        assertEquals(2, summary.totalDefects)
        assertEquals(1.0, summary.averageDefectsPerJob, 0.001) // 2 defects / 2 jobs = 1.0
        assertEquals(2, summary.openDefectCount)
    }
}

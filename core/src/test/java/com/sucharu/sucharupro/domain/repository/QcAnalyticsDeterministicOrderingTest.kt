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

class QcAnalyticsDeterministicOrderingTest {

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
    fun `defect category analytics maintains deterministic sorting order across multiple calls`() = runBlocking {
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

        defectDataSource.insertDefect(
            ProductionDefect(
                defectId = "DEF-01",
                productionJobId = "JOB-01",
                category = DefectCategory.PRINT_QUALITY,
                severity = DefectSeverity.MAJOR,
                source = DefectSource.PRODUCTION_STAGE,
                title = "Streak",
                description = "Streak",
                affectedQuantity = 20,
                status = DefectStatus.RESOLVED,
                detectedBy = "insp-01",
                detectedAt = "2026-08-17T09:00:00Z",
                createdAt = "2026-08-17T09:00:00Z",
                updatedAt = "2026-08-17T09:00:00Z"
            )
        )

        val run1 = repository.getDefectAnalytics(
            period = QcAnalyticsPeriod.custom("2026-08-01T00:00:00Z", "2026-08-31T23:59:59Z"),
            projectId = "PRJ-01",
            callerRole = UserRole.ADMIN
        )
        val run2 = repository.getDefectAnalytics(
            period = QcAnalyticsPeriod.custom("2026-08-01T00:00:00Z", "2026-08-31T23:59:59Z"),
            projectId = "PRJ-01",
            callerRole = UserRole.ADMIN
        )

        assertTrue(run1 is DomainResult.Success)
        assertTrue(run2 is DomainResult.Success)
        val list1 = (run1 as DomainResult.Success).data.map { it.defectCategory }
        val list2 = (run2 as DomainResult.Success).data.map { it.defectCategory }
        assertEquals(list1, list2)
    }
}

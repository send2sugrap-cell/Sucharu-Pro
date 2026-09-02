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

class QcDefectAnalyticsTest {

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
    fun `getDefectAnalytics categorizes and ranks defect occurrences`() = runBlocking {
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

        // 2 Print quality defects, 1 Finishing defect
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
        defectDataSource.insertDefect(
            ProductionDefect(
                defectId = "DEF-02",
                productionJobId = "JOB-01",
                category = DefectCategory.PRINT_QUALITY,
                severity = DefectSeverity.MINOR,
                source = DefectSource.PRODUCTION_STAGE,
                title = "Hickey",
                description = "Hickey",
                affectedQuantity = 10,
                status = DefectStatus.RESOLVED,
                detectedBy = "insp-01",
                detectedAt = "2026-08-17T09:10:00Z",
                createdAt = "2026-08-17T09:10:00Z",
                updatedAt = "2026-08-17T09:10:00Z"
            )
        )
        defectDataSource.insertDefect(
            ProductionDefect(
                defectId = "DEF-03",
                productionJobId = "JOB-01",
                category = DefectCategory.FINISHING_ERROR,
                severity = DefectSeverity.MINOR,
                source = DefectSource.PRODUCTION_STAGE,
                title = "Crease",
                description = "Crease",
                affectedQuantity = 5,
                status = DefectStatus.RESOLVED,
                detectedBy = "insp-01",
                detectedAt = "2026-08-17T09:20:00Z",
                createdAt = "2026-08-17T09:20:00Z",
                updatedAt = "2026-08-17T09:20:00Z"
            )
        )

        val res = repository.getDefectAnalytics(
            period = QcAnalyticsPeriod.custom("2026-08-01T00:00:00Z", "2026-08-31T23:59:59Z"),
            projectId = "PRJ-01",
            callerRole = UserRole.ADMIN
        )

        assertTrue(res is DomainResult.Success)
        val list = (res as DomainResult.Success).data
        val topCategory = list.first()
        assertEquals(DefectCategory.PRINT_QUALITY, topCategory.defectCategory)
        assertEquals(2, topCategory.defectCount)
        assertEquals(30, topCategory.affectedQuantity)
        assertEquals(66.666, topCategory.percentageOfTotalDefects, 0.1) // 2 / 3 = 66.7%
    }
}

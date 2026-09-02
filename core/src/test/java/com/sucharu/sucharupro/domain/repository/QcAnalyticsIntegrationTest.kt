package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionDefectDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionJobDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionReworkDataSource
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
import com.sucharu.sucharupro.domain.model.qc.ProductionRework
import com.sucharu.sucharupro.domain.model.qc.QcCostEntry
import com.sucharu.sucharupro.domain.model.qc.QcCostStatus
import com.sucharu.sucharupro.domain.model.qc.QcCostType
import com.sucharu.sucharupro.domain.model.qc.ReworkReason
import com.sucharu.sucharupro.domain.model.qc.ReworkStatus
import com.sucharu.sucharupro.domain.model.qc.ReworkType
import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QcAnalyticsIntegrationTest {

    private lateinit var analyticsDataSource: FakeQcAnalyticsDataSource
    private lateinit var jobDataSource: FakeProductionJobDataSource
    private lateinit var defectDataSource: FakeProductionDefectDataSource
    private lateinit var reworkDataSource: FakeProductionReworkDataSource
    private lateinit var costTimeDataSource: FakeQcCostTimeDataSource
    private lateinit var repository: QcAnalyticsRepository

    @Before
    fun setup() {
        analyticsDataSource = FakeQcAnalyticsDataSource()
        jobDataSource = FakeProductionJobDataSource()
        defectDataSource = FakeProductionDefectDataSource()
        reworkDataSource = FakeProductionReworkDataSource()
        costTimeDataSource = FakeQcCostTimeDataSource()

        repository = QcAnalyticsRepositoryImpl(
            analyticsDataSource = analyticsDataSource,
            productionJobDataSource = jobDataSource,
            defectDataSource = defectDataSource,
            reworkDataSource = reworkDataSource,
            qcCostTimeDataSource = costTimeDataSource
        )
    }

    @Test
    fun `analytics integrates multi-step QC operational sources into cohesive summary`() = runBlocking {
        val job = ProductionJob(
            jobId = "JOB-INT-1",
            jobNumber = "JOB-001",
            orderId = "PRJ-01",
            orderNumber = "ORD-001",
            customerId = "cust-01",
            handoffId = "ho-01",
            title = "Hardcover Book 1000 Qty",
            quantity = 1000,
            status = ProductionJobStatus.IN_PROGRESS,
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z"
        )
        jobDataSource.insertJob(job)

        val defect = ProductionDefect(
            defectId = "DEF-INT-1",
            productionJobId = "JOB-INT-1",
            category = DefectCategory.BINDING_ERROR,
            severity = DefectSeverity.MAJOR,
            source = DefectSource.PRODUCTION_STAGE,
            title = "Glue separation",
            description = "Glue separation on spine",
            affectedQuantity = 50,
            status = DefectStatus.RESOLVED,
            detectedBy = "insp-01",
            detectedAt = "2026-08-17T09:00:00Z",
            createdAt = "2026-08-17T09:00:00Z",
            updatedAt = "2026-08-17T09:30:00Z"
        )
        defectDataSource.insertDefect(defect)

        val rework = ProductionRework(
            reworkId = "REW-INT-1",
            projectId = "PRJ-01",
            productionJobId = "JOB-INT-1",
            qcId = "QC-1",
            defectId = defect.defectId,
            reworkType = ReworkType.BINDING_CORRECTION,
            reason = ReworkReason.DEFECT_CORRECTION,
            affectedQuantity = 50,
            description = "Re-glue spine",
            requestedBy = "insp-01",
            requestedAt = "2026-08-17T09:30:00Z",
            status = ReworkStatus.COMPLETED,
            createdAt = "2026-08-17T09:30:00Z",
            updatedAt = "2026-08-17T10:00:00Z"
        )
        reworkDataSource.insertRework(rework)

        costTimeDataSource.insertCostEntry(
            QcCostEntry(
                id = "C-INT-1",
                projectId = "PRJ-01",
                productionJobId = "JOB-INT-1",
                costType = QcCostType.REWORK_QC,
                description = "Rebind inspection cost",
                quantity = 1.0,
                unitCost = 220.0,
                totalCost = 220.0,
                productionDefectId = defect.defectId,
                productionReworkId = rework.reworkId,
                recordedBy = "insp-01",
                recordedAt = "2026-08-17T10:15:00Z",
                status = QcCostStatus.RECORDED,
                createdAt = "2026-08-17T10:15:00Z",
                updatedAt = "2026-08-17T10:15:00Z"
            )
        )

        val summaryRes = repository.getSummary(
            period = QcAnalyticsPeriod.custom("2026-08-01T00:00:00Z", "2026-08-31T23:59:59Z"),
            projectId = "PRJ-01",
            callerRole = UserRole.ADMIN
        )

        assertTrue(summaryRes is DomainResult.Success)
        val summary = (summaryRes as DomainResult.Success).data
        assertEquals(1, summary.totalJobs)
        assertEquals(220.0, summary.totalQcCost, 0.001)
        assertEquals(1, summary.totalDefects)
        assertEquals(1, summary.totalReworks)
        assertEquals(100.0, summary.reworkRate, 0.001)
        assertEquals(0.0, summary.firstPassQcRate, 0.001)
    }
}

package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionDefectDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionJobDataSource
import com.sucharu.sucharupro.data.datasource.FakeQcAnalyticsDataSource
import com.sucharu.sucharupro.data.datasource.FakeQcGovernanceDataSource
import com.sucharu.sucharupro.data.repository.QcAnalyticsRepositoryImpl
import com.sucharu.sucharupro.data.repository.QcGovernanceRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.qc.DefectCategory
import com.sucharu.sucharupro.domain.model.qc.DefectSeverity
import com.sucharu.sucharupro.domain.model.qc.DefectSource
import com.sucharu.sucharupro.domain.model.qc.DefectStatus
import com.sucharu.sucharupro.domain.model.qc.ProductionDefect
import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.qc.governance.QcGovernanceKpi
import com.sucharu.sucharupro.domain.model.qc.governance.QcKpiTarget
import com.sucharu.sucharupro.domain.model.qc.governance.QcThresholdStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QcGovernanceAnalyticsIntegrationTest {

    private lateinit var governanceDataSource: FakeQcGovernanceDataSource
    private lateinit var analyticsDataSource: FakeQcAnalyticsDataSource
    private lateinit var jobDataSource: FakeProductionJobDataSource
    private lateinit var defectDataSource: FakeProductionDefectDataSource

    private lateinit var analyticsRepository: QcAnalyticsRepository
    private lateinit var governanceRepository: QcGovernanceRepository

    @Before
    fun setup() {
        governanceDataSource = FakeQcGovernanceDataSource()
        analyticsDataSource = FakeQcAnalyticsDataSource()
        jobDataSource = FakeProductionJobDataSource()
        defectDataSource = FakeProductionDefectDataSource()

        analyticsRepository = QcAnalyticsRepositoryImpl(
            analyticsDataSource = analyticsDataSource,
            productionJobDataSource = jobDataSource,
            defectDataSource = defectDataSource
        )

        governanceRepository = QcGovernanceRepositoryImpl(
            governanceDataSource = governanceDataSource,
            analyticsRepository = analyticsRepository,
            productionJobDataSource = jobDataSource,
            defectDataSource = defectDataSource
        )
    }

    @Test
    fun `evaluates thresholds seamlessly against Step 09 analytics output`() = runBlocking {
        // Create 2 jobs
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
        jobDataSource.insertJob(
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

        // Add 1 defect to JOB-01
        defectDataSource.insertDefect(
            ProductionDefect(
                defectId = "DEF-01",
                productionJobId = "JOB-01",
                category = DefectCategory.PRINT_QUALITY,
                severity = DefectSeverity.MAJOR,
                source = DefectSource.PRODUCTION_STAGE,
                title = "Cyan Misregistration",
                description = "Shifted",
                affectedQuantity = 10,
                status = DefectStatus.OPEN,
                detectedBy = "insp-01",
                detectedAt = "2026-08-17T08:30:00Z",
                createdAt = "2026-08-17T08:30:00Z",
                updatedAt = "2026-08-17T08:30:00Z"
            )
        )

        // Set target: First Pass Rate 90%
        governanceRepository.setTarget(
            QcKpiTarget(
                targetId = "TGT-FPR",
                projectId = "PRJ-01",
                kpiType = QcGovernanceKpi.FIRST_PASS_RATE,
                targetValue = 90.0,
                minimumAcceptableValue = 70.0,
                effectiveFrom = "2026-08-01T00:00:00Z",
                configuredBy = "admin-1",
                createdAt = "2026-08-01T00:00:00Z",
                updatedAt = "2026-08-01T00:00:00Z"
            ),
            callerRole = UserRole.ADMIN
        )

        val period = QcAnalyticsPeriod.custom("2026-08-01T00:00:00Z", "2026-08-31T23:59:59Z")
        val evalRes = governanceRepository.evaluateProjectKpis(period, "PRJ-01", UserRole.ADMIN)

        assertTrue(evalRes is DomainResult.Success)
        val evaluations = (evalRes as DomainResult.Success).data

        // First pass rate is 50% (1 of 2 jobs clean) -> below min acceptable (70%) -> CRITICAL_BREACH
        val fprEval = evaluations.find { it.kpiType == QcGovernanceKpi.FIRST_PASS_RATE }
        assertTrue(fprEval != null)
        assertEquals(50.0, fprEval!!.currentValue, 0.001)
        assertEquals(QcThresholdStatus.CRITICAL_BREACH, fprEval.status)
    }
}

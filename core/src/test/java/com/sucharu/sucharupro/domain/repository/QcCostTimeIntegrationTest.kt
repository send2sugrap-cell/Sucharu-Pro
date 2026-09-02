package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionDefectDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionJobDataSource
import com.sucharu.sucharupro.data.datasource.FakeQcCostTimeDataSource
import com.sucharu.sucharupro.data.repository.QcCostTimeRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.qc.DefectCategory
import com.sucharu.sucharupro.domain.model.qc.DefectSeverity
import com.sucharu.sucharupro.domain.model.qc.DefectSource
import com.sucharu.sucharupro.domain.model.qc.DefectStatus
import com.sucharu.sucharupro.domain.model.qc.ProductionDefect
import com.sucharu.sucharupro.domain.model.qc.QcCostType
import com.sucharu.sucharupro.domain.model.qc.QcTimeEntryType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QcCostTimeIntegrationTest {

    private lateinit var costTimeDataSource: FakeQcCostTimeDataSource
    private lateinit var jobDataSource: FakeProductionJobDataSource
    private lateinit var defectDataSource: FakeProductionDefectDataSource
    private lateinit var repository: QcCostTimeRepository

    @Before
    fun setup() {
        costTimeDataSource = FakeQcCostTimeDataSource()
        jobDataSource = FakeProductionJobDataSource()
        defectDataSource = FakeProductionDefectDataSource()

        repository = QcCostTimeRepositoryImpl(
            costTimeDataSource = costTimeDataSource,
            productionJobDataSource = jobDataSource,
            defectDataSource = defectDataSource
        )
    }

    @Test
    fun `reconciliation integrates defect counts and calculates failure-driven variance`() = runBlocking {
        val jobId = "JOB-100"
        val prjId = "PRJ-100"

        val job = ProductionJob(
            jobId = jobId,
            jobNumber = "JOB-100",
            orderId = prjId,
            orderNumber = "ORD-100",
            customerId = "cust-01",
            handoffId = "ho-100",
            title = "Magazine 1000 Qty",
            quantity = 1000,
            status = ProductionJobStatus.IN_PROGRESS,
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z"
        )
        jobDataSource.insertJob(job)

        val defect = ProductionDefect(
            defectId = "DEF-100",
            productionJobId = jobId,
            category = DefectCategory.PRINT_QUALITY,
            severity = DefectSeverity.CRITICAL,
            source = DefectSource.PRODUCTION_STAGE,
            title = "Plate Scratch",
            description = "Scratch on plate cylinder",
            affectedQuantity = 100,
            status = DefectStatus.RESOLVED,
            detectedBy = "insp-01",
            detectedAt = "2026-08-17T09:00:00Z",
            createdAt = "2026-08-17T09:00:00Z",
            updatedAt = "2026-08-17T09:30:00Z"
        )
        defectDataSource.insertDefect(defect)

        // Baseline inspection cost + Defect investigation cost
        repository.createCostEntry(
            projectId = prjId,
            productionJobId = jobId,
            costType = QcCostType.INSPECTION,
            description = "Standard QC pass",
            quantity = 1.0,
            unitCost = 100.0,
            recordedBy = "insp-01",
            timestamp = "2026-08-17T09:00:00Z"
        )
        repository.createCostEntry(
            projectId = prjId,
            productionJobId = jobId,
            costType = QcCostType.DEFECT_INVESTIGATION,
            description = "Defect root cause investigation",
            quantity = 1.0,
            unitCost = 250.0,
            productionDefectId = defect.defectId,
            recordedBy = "insp-01",
            timestamp = "2026-08-17T09:30:00Z"
        )

        // Baseline inspection time + Defect investigation time
        repository.createTimeEntry(
            projectId = prjId,
            productionJobId = jobId,
            entryType = QcTimeEntryType.INSPECTION,
            actorId = "insp-01",
            startedAt = "2026-08-17T09:00:00Z",
            durationMinutes = 30L,
            timestamp = "2026-08-17T09:30:00Z"
        )
        repository.createTimeEntry(
            projectId = prjId,
            productionJobId = jobId,
            entryType = QcTimeEntryType.INVESTIGATION,
            actorId = "insp-01",
            startedAt = "2026-08-17T09:30:00Z",
            durationMinutes = 45L,
            productionDefectId = defect.defectId,
            timestamp = "2026-08-17T10:15:00Z"
        )

        // Reconcile against Planned: 100.0 BDT / 30m
        val reconRes = repository.calculateReconciliation(
            productionJobId = jobId,
            plannedCost = 100.0,
            plannedMinutes = 30L,
            reconciledBy = "mgr-01",
            timestamp = "2026-08-17T10:30:00Z",
            callerRole = UserRole.MANAGER
        )

        assertTrue(reconRes is DomainResult.Success)
        val recon = (reconRes as DomainResult.Success).data
        assertEquals(350.0, recon.actualCost, 0.001)
        assertEquals(250.0, recon.costVariance, 0.001) // 250 overrun caused by defect
        assertEquals(75L, recon.actualMinutes)
        assertEquals(45L, recon.timeVarianceMinutes) // 45m overrun caused by defect
        assertEquals(1, recon.defectCount)
    }
}

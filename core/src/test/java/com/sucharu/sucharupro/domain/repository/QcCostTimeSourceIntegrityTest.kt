package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeFinalQcDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionDefectDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionJobDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionQcDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionReQcDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionReworkDataSource
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
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QcCostTimeSourceIntegrityTest {

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
    fun `createCostEntry rejects defect belonging to a different job`() = runBlocking {
        val jobA = ProductionJob(
            jobId = "JOB-A",
            jobNumber = "JOB-A",
            orderId = "PRJ-A",
            orderNumber = "ORD-A",
            customerId = "cust-01",
            handoffId = "ho-A",
            title = "Book Printing 500 Qty",
            quantity = 500,
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
            handoffId = "ho-B",
            title = "Flyer Printing 1000 Qty",
            quantity = 1000,
            status = ProductionJobStatus.IN_PROGRESS,
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z"
        )
        jobDataSource.insertJob(jobA)
        jobDataSource.insertJob(jobB)

        // Create defect on Job B
        val defectOnJobB = ProductionDefect(
            defectId = "DEF-JOB-B",
            productionJobId = "JOB-B",
            category = DefectCategory.PRINT_QUALITY,
            severity = DefectSeverity.MAJOR,
            source = DefectSource.PRODUCTION_STAGE,
            title = "Misregistration",
            description = "Magenta shifted",
            affectedQuantity = 50,
            status = DefectStatus.OPEN,
            detectedBy = "insp-01",
            detectedAt = "2026-08-17T09:00:00Z",
            createdAt = "2026-08-17T09:00:00Z",
            updatedAt = "2026-08-17T09:00:00Z"
        )
        defectDataSource.insertDefect(defectOnJobB)

        // Attempt to create cost entry for Job A pointing to defect on Job B
        val result = repository.createCostEntry(
            projectId = "PRJ-A",
            productionJobId = "JOB-A",
            costType = QcCostType.DEFECT_INVESTIGATION,
            description = "Investigating Job B defect on Job A",
            quantity = 1.0,
            unitCost = 100.0,
            productionDefectId = "DEF-JOB-B",
            recordedBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Cross-job reference violation"))
    }
}

package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeQcCostTimeDataSource
import com.sucharu.sucharupro.data.repository.QcCostTimeRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.QcCostType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QcCostTimeDuplicatePreventionTest {

    private lateinit var dataSource: FakeQcCostTimeDataSource
    private lateinit var repository: QcCostTimeRepository

    @Before
    fun setup() {
        dataSource = FakeQcCostTimeDataSource()
        repository = QcCostTimeRepositoryImpl(costTimeDataSource = dataSource)
    }

    @Test
    fun `recalculation updates existing reconciliation without creating duplicate record`() = runBlocking {
        val jobId = "JOB-01"

        repository.createCostEntry(
            projectId = "PRJ-01",
            productionJobId = jobId,
            costType = QcCostType.INSPECTION,
            description = "Consumable 1",
            quantity = 1.0,
            unitCost = 100.0,
            recordedBy = "insp-01",
            timestamp = "2026-08-17T09:00:00Z"
        )

        val recon1 = (repository.calculateReconciliation(
            productionJobId = jobId,
            plannedCost = 100.0,
            plannedMinutes = 30L,
            reconciledBy = "mgr-01",
            timestamp = "2026-08-17T09:30:00Z"
        ) as DomainResult.Success).data

        // Add second cost entry
        repository.createCostEntry(
            projectId = "PRJ-01",
            productionJobId = jobId,
            costType = QcCostType.DEFECT_INVESTIGATION,
            description = "Investigation",
            quantity = 1.0,
            unitCost = 80.0,
            recordedBy = "insp-01",
            timestamp = "2026-08-17T09:45:00Z"
        )

        // Recalculate
        val recon2 = (repository.calculateReconciliation(
            productionJobId = jobId,
            plannedCost = 100.0,
            plannedMinutes = 30L,
            reconciledBy = "mgr-01",
            timestamp = "2026-08-17T10:00:00Z"
        ) as DomainResult.Success).data

        assertEquals(recon1.id, recon2.id)
        assertEquals(180.0, recon2.actualCost, 0.001)

        val allRecons = (repository.getReconciliation(jobId) as DomainResult.Success).data
        assertEquals(recon1.id, allRecons?.id)
    }
}

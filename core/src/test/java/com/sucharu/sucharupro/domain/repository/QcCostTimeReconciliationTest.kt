package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeQcCostTimeDataSource
import com.sucharu.sucharupro.data.repository.QcCostTimeRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.QcCostStatus
import com.sucharu.sucharupro.domain.model.qc.QcCostType
import com.sucharu.sucharupro.domain.model.qc.QcTimeEntryType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QcCostTimeReconciliationTest {

    private lateinit var dataSource: FakeQcCostTimeDataSource
    private lateinit var repository: QcCostTimeRepository

    @Before
    fun setup() {
        dataSource = FakeQcCostTimeDataSource()
        repository = QcCostTimeRepositoryImpl(costTimeDataSource = dataSource)
    }

    @Test
    fun `calculateReconciliation calculates aggregate actuals and variances correctly`() = runBlocking {
        val jobId = "JOB-01"
        val prjId = "PRJ-01"

        // Add 2 Cost entries
        repository.createCostEntry(
            projectId = prjId,
            productionJobId = jobId,
            costType = QcCostType.INSPECTION,
            description = "Consumables",
            quantity = 2.0,
            unitCost = 100.0,
            recordedBy = "insp-01",
            timestamp = "2026-08-17T09:00:00Z"
        )
        repository.createCostEntry(
            projectId = prjId,
            productionJobId = jobId,
            costType = QcCostType.DEFECT_INVESTIGATION,
            description = "Investigation labor",
            quantity = 1.0,
            unitCost = 150.0,
            recordedBy = "insp-01",
            timestamp = "2026-08-17T09:30:00Z"
        )

        // Add 2 Time entries
        repository.createTimeEntry(
            projectId = prjId,
            productionJobId = jobId,
            entryType = QcTimeEntryType.INSPECTION,
            actorId = "insp-01",
            startedAt = "2026-08-17T09:00:00Z",
            durationMinutes = 40L,
            timestamp = "2026-08-17T09:40:00Z"
        )
        repository.createTimeEntry(
            projectId = prjId,
            productionJobId = jobId,
            entryType = QcTimeEntryType.INVESTIGATION,
            actorId = "insp-01",
            startedAt = "2026-08-17T09:45:00Z",
            durationMinutes = 35L,
            timestamp = "2026-08-17T10:20:00Z"
        )

        // Reconcile with Planned Cost 300.0 and Planned Time 60 mins
        val reconRes = repository.calculateReconciliation(
            productionJobId = jobId,
            plannedCost = 300.0,
            plannedMinutes = 60L,
            reconciledBy = "mgr-01",
            notes = "Initial reconciliation",
            timestamp = "2026-08-17T10:30:00Z",
            callerRole = UserRole.MANAGER
        )

        assertTrue(reconRes is DomainResult.Success)
        val recon = (reconRes as DomainResult.Success).data
        assertEquals(350.0, recon.actualCost, 0.001)
        assertEquals(50.0, recon.costVariance, 0.001) // 350 - 300 = +50
        assertEquals(75L, recon.actualMinutes)
        assertEquals(15L, recon.timeVarianceMinutes) // 75 - 60 = +15
        assertEquals(2, recon.qcEntryCount)
        assertEquals(2, recon.timeEntryCount)
        assertEquals(QcCostStatus.RECONCILED, recon.status)
    }
}

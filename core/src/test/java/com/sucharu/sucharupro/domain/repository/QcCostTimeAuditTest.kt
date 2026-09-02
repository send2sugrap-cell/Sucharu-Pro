package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeQcCostTimeDataSource
import com.sucharu.sucharupro.data.repository.QcCostTimeRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.QcCostTimeActivityType
import com.sucharu.sucharupro.domain.model.qc.QcCostType
import com.sucharu.sucharupro.domain.model.qc.QcTimeEntryType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QcCostTimeAuditTest {

    private lateinit var dataSource: FakeQcCostTimeDataSource
    private lateinit var repository: QcCostTimeRepository

    @Before
    fun setup() {
        dataSource = FakeQcCostTimeDataSource()
        repository = QcCostTimeRepositoryImpl(costTimeDataSource = dataSource)
    }

    @Test
    fun `complete lifecycle logs immutable audit activity events`() = runBlocking {
        val jobId = "JOB-01"
        val prjId = "PRJ-01"

        repository.createCostEntry(
            projectId = prjId,
            productionJobId = jobId,
            costType = QcCostType.INSPECTION,
            description = "Consumables",
            quantity = 1.0,
            unitCost = 100.0,
            recordedBy = "insp-01",
            timestamp = "2026-08-17T09:00:00Z"
        )

        repository.createTimeEntry(
            projectId = prjId,
            productionJobId = jobId,
            entryType = QcTimeEntryType.INSPECTION,
            actorId = "insp-01",
            startedAt = "2026-08-17T09:00:00Z",
            durationMinutes = 30L,
            timestamp = "2026-08-17T09:30:00Z"
        )

        val recon = (repository.calculateReconciliation(
            productionJobId = jobId,
            plannedCost = 100.0,
            plannedMinutes = 30L,
            reconciledBy = "mgr-01",
            timestamp = "2026-08-17T09:40:00Z"
        ) as DomainResult.Success).data

        repository.adjustReconciliation(
            reconciliationId = recon.id,
            adjustedPlannedCost = 110.0,
            adjustedPlannedMinutes = 35L,
            adjustmentReason = "Revised setup benchmark",
            adjustedBy = "mgr-01",
            timestamp = "2026-08-17T09:45:00Z",
            callerRole = UserRole.MANAGER
        )

        repository.lockReconciliation(
            reconciliationId = recon.id,
            lockedBy = "admin-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.ADMIN
        )

        val events = repository.observeActivityEvents(jobId).first()
        val activityTypes = events.map { it.activityType }

        assertTrue(activityTypes.contains(QcCostTimeActivityType.QC_COST_ENTRY_CREATED))
        assertTrue(activityTypes.contains(QcCostTimeActivityType.QC_TIME_ENTRY_CREATED))
        assertTrue(activityTypes.contains(QcCostTimeActivityType.QC_RECONCILIATION_COMPLETED))
        assertTrue(activityTypes.contains(QcCostTimeActivityType.QC_RECONCILIATION_ADJUSTED))
        assertTrue(activityTypes.contains(QcCostTimeActivityType.QC_COST_TIME_SNAPSHOT_CREATED))
        assertTrue(activityTypes.contains(QcCostTimeActivityType.QC_RECONCILIATION_LOCKED))
    }
}

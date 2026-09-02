package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeQcCostTimeDataSource
import com.sucharu.sucharupro.data.repository.QcCostTimeRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.QcCostStatus
import com.sucharu.sucharupro.domain.model.qc.QcCostType
import com.sucharu.sucharupro.domain.model.qc.QcTimeEntryType
import com.sucharu.sucharupro.domain.model.qc.QcTimeStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QcCostTimeLockTest {

    private lateinit var dataSource: FakeQcCostTimeDataSource
    private lateinit var repository: QcCostTimeRepository

    @Before
    fun setup() {
        dataSource = FakeQcCostTimeDataSource()
        repository = QcCostTimeRepositoryImpl(costTimeDataSource = dataSource)
    }

    @Test
    fun `locking seals all underlying cost and time entries to LOCKED status`() = runBlocking {
        val jobId = "JOB-01"
        val prjId = "PRJ-01"

        val cost = (repository.createCostEntry(
            projectId = prjId,
            productionJobId = jobId,
            costType = QcCostType.INSPECTION,
            description = "Consumables",
            quantity = 1.0,
            unitCost = 100.0,
            recordedBy = "insp-01",
            timestamp = "2026-08-17T09:00:00Z"
        ) as DomainResult.Success).data

        val time = (repository.createTimeEntry(
            projectId = prjId,
            productionJobId = jobId,
            entryType = QcTimeEntryType.INSPECTION,
            actorId = "insp-01",
            startedAt = "2026-08-17T09:00:00Z",
            durationMinutes = 30L,
            timestamp = "2026-08-17T09:30:00Z"
        ) as DomainResult.Success).data

        val recon = (repository.calculateReconciliation(
            productionJobId = jobId,
            plannedCost = 100.0,
            plannedMinutes = 30L,
            reconciledBy = "mgr-01",
            timestamp = "2026-08-17T09:40:00Z"
        ) as DomainResult.Success).data

        repository.lockReconciliation(
            reconciliationId = recon.id,
            lockedBy = "mgr-01",
            timestamp = "2026-08-17T09:50:00Z",
            callerRole = UserRole.MANAGER
        )

        // Verify cost entry is locked
        val updatedCost = (repository.findCostEntryById(cost.id) as DomainResult.Success).data
        assertEquals(QcCostStatus.LOCKED, updatedCost.status)
        assertTrue(updatedCost.isLocked)

        // Verify time entry is locked
        val updatedTime = (repository.findTimeEntryById(time.id) as DomainResult.Success).data
        assertEquals(QcTimeStatus.LOCKED, updatedTime.status)
        assertTrue(updatedTime.isLocked)

        // Verify updating cost or time fails
        val updateCostRes = repository.updateCostEntry(
            id = cost.id,
            description = "New desc",
            quantity = 2.0,
            unitCost = 100.0,
            adjustmentReason = "Test",
            updatedBy = "mgr-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(updateCostRes is DomainResult.Error)

        val updateTimeRes = repository.updateTimeEntry(
            id = time.id,
            durationMinutes = 40L,
            endedAt = null,
            notes = "Test",
            updatedBy = "mgr-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(updateTimeRes is DomainResult.Error)
    }
}

package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeQcCostTimeDataSource
import com.sucharu.sucharupro.data.repository.QcCostTimeRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.QcCostType
import com.sucharu.sucharupro.domain.model.qc.QcTimeEntryType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QcCostTimeSnapshotTest {

    private lateinit var dataSource: FakeQcCostTimeDataSource
    private lateinit var repository: QcCostTimeRepository

    @Before
    fun setup() {
        dataSource = FakeQcCostTimeDataSource()
        repository = QcCostTimeRepositoryImpl(costTimeDataSource = dataSource)
    }

    @Test
    fun `lockReconciliation creates immutable snapshot preserving source IDs and metrics`() = runBlocking {
        val jobId = "JOB-01"
        val prjId = "PRJ-01"

        val costRes = repository.createCostEntry(
            projectId = prjId,
            productionJobId = jobId,
            costType = QcCostType.INSPECTION,
            description = "Consumables",
            quantity = 1.0,
            unitCost = 120.0,
            recordedBy = "insp-01",
            timestamp = "2026-08-17T09:00:00Z"
        )
        val timeRes = repository.createTimeEntry(
            projectId = prjId,
            productionJobId = jobId,
            entryType = QcTimeEntryType.INSPECTION,
            actorId = "insp-01",
            startedAt = "2026-08-17T09:00:00Z",
            durationMinutes = 45L,
            timestamp = "2026-08-17T09:45:00Z"
        )

        val reconRes = repository.calculateReconciliation(
            productionJobId = jobId,
            plannedCost = 100.0,
            plannedMinutes = 40L,
            reconciledBy = "mgr-01",
            timestamp = "2026-08-17T10:00:00Z"
        )
        val recon = (reconRes as DomainResult.Success).data

        val lockRes = repository.lockReconciliation(
            reconciliationId = recon.id,
            lockedBy = "admin-01",
            lockNotes = "Verified and sealed.",
            timestamp = "2026-08-17T10:15:00Z",
            callerRole = UserRole.ADMIN
        )

        assertTrue(lockRes is DomainResult.Success)
        val snapshot = (lockRes as DomainResult.Success).data

        assertEquals(jobId, snapshot.productionJobId)
        assertEquals(prjId, snapshot.projectId)
        assertEquals(120.0, snapshot.actualCost, 0.001)
        assertEquals(45L, snapshot.actualMinutes)
        assertEquals(1, snapshot.costEntryIds.size)
        assertEquals(1, snapshot.timeEntryIds.size)
        assertEquals((costRes as DomainResult.Success).data.id, snapshot.costEntryIds.first())
        assertEquals((timeRes as DomainResult.Success).data.id, snapshot.timeEntryIds.first())
    }
}

package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeQcCostTimeDataSource
import com.sucharu.sucharupro.data.repository.QcCostTimeRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.QcCostStatus
import com.sucharu.sucharupro.domain.model.qc.QcCostType
import com.sucharu.sucharupro.domain.model.qc.QcTimeEntryType
import com.sucharu.sucharupro.domain.model.qc.QcTimeStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QcCostTimeRepositoryTest {

    private lateinit var dataSource: FakeQcCostTimeDataSource
    private lateinit var repository: QcCostTimeRepository

    @Before
    fun setup() {
        dataSource = FakeQcCostTimeDataSource()
        repository = QcCostTimeRepositoryImpl(costTimeDataSource = dataSource)
    }

    @Test
    fun `updateCostEntry and changeCostStatus modify cost entry correctly`() = runBlocking {
        val cost = (repository.createCostEntry(
            projectId = "PRJ-01",
            productionJobId = "JOB-01",
            costType = QcCostType.INSPECTION,
            description = "Initial description",
            quantity = 1.0,
            unitCost = 100.0,
            recordedBy = "insp-01",
            timestamp = "2026-08-17T09:00:00Z"
        ) as DomainResult.Success).data

        val updated = (repository.updateCostEntry(
            id = cost.id,
            description = "Updated description",
            quantity = 2.0,
            unitCost = 100.0,
            adjustmentReason = "Quantity corrected",
            updatedBy = "mgr-01",
            timestamp = "2026-08-17T09:15:00Z",
            callerRole = UserRole.MANAGER
        ) as DomainResult.Success).data

        assertEquals("Updated description", updated.description)
        assertEquals(200.0, updated.totalCost, 0.001)

        val statusChanged = (repository.changeCostStatus(
            id = cost.id,
            targetStatus = QcCostStatus.CANCELLED,
            notes = "Mistaken entry",
            actorId = "mgr-01",
            timestamp = "2026-08-17T09:20:00Z",
            callerRole = UserRole.MANAGER
        ) as DomainResult.Success).data

        assertEquals(QcCostStatus.CANCELLED, statusChanged.status)
    }

    @Test
    fun `updateTimeEntry and changeTimeStatus modify time entry correctly`() = runBlocking {
        val time = (repository.createTimeEntry(
            projectId = "PRJ-01",
            productionJobId = "JOB-01",
            entryType = QcTimeEntryType.INSPECTION,
            actorId = "insp-01",
            startedAt = "2026-08-17T09:00:00Z",
            durationMinutes = 25L,
            timestamp = "2026-08-17T09:25:00Z"
        ) as DomainResult.Success).data

        val updated = (repository.updateTimeEntry(
            id = time.id,
            durationMinutes = 30L,
            endedAt = "2026-08-17T09:30:00Z",
            notes = "Extended by 5m",
            updatedBy = "mgr-01",
            timestamp = "2026-08-17T09:30:00Z",
            callerRole = UserRole.MANAGER
        ) as DomainResult.Success).data

        assertEquals(30L, updated.durationMinutes)

        val statusChanged = (repository.changeTimeStatus(
            id = time.id,
            targetStatus = QcTimeStatus.CANCELLED,
            notes = "Mistaken timer",
            actorId = "mgr-01",
            timestamp = "2026-08-17T09:35:00Z",
            callerRole = UserRole.MANAGER
        ) as DomainResult.Success).data

        assertEquals(QcTimeStatus.CANCELLED, statusChanged.status)
    }
}

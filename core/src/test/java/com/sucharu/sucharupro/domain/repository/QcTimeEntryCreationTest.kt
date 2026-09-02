package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeQcCostTimeDataSource
import com.sucharu.sucharupro.data.repository.QcCostTimeRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.QcTimeEntryType
import com.sucharu.sucharupro.domain.model.qc.QcTimeStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QcTimeEntryCreationTest {

    private lateinit var dataSource: FakeQcCostTimeDataSource
    private lateinit var repository: QcCostTimeRepository

    @Before
    fun setup() {
        dataSource = FakeQcCostTimeDataSource()
        repository = QcCostTimeRepositoryImpl(costTimeDataSource = dataSource)
    }

    @Test
    fun `createTimeEntry successfully creates and stores time entry`() = runBlocking {
        val result = repository.createTimeEntry(
            projectId = "PRJ-01",
            productionJobId = "JOB-01",
            entryType = QcTimeEntryType.INVESTIGATION,
            actorId = "insp-01",
            startedAt = "2026-08-17T09:00:00Z",
            endedAt = "2026-08-17T09:50:00Z",
            durationMinutes = 50L,
            notes = "Investigated magenta misalignment on sheet 4",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        assertTrue(result is DomainResult.Success)
        val entry = (result as DomainResult.Success).data
        assertEquals("JOB-01", entry.productionJobId)
        assertEquals(50L, entry.durationMinutes)
        assertEquals(QcTimeStatus.RECORDED, entry.status)

        val fetched = repository.findTimeEntryById(entry.id)
        assertTrue(fetched is DomainResult.Success)
        assertEquals(entry.id, (fetched as DomainResult.Success).data.id)
    }
}

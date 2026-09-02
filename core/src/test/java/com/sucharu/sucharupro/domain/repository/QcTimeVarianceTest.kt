package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeQcCostTimeDataSource
import com.sucharu.sucharupro.data.repository.QcCostTimeRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.QcTimeEntryType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QcTimeVarianceTest {

    private lateinit var dataSource: FakeQcCostTimeDataSource
    private lateinit var repository: QcCostTimeRepository

    @Before
    fun setup() {
        dataSource = FakeQcCostTimeDataSource()
        repository = QcCostTimeRepositoryImpl(costTimeDataSource = dataSource)
    }

    @Test
    fun `time variance correctly indicates overrun and underrun`() = runBlocking {
        val jobId = "JOB-01"

        repository.createTimeEntry(
            projectId = "PRJ-01",
            productionJobId = jobId,
            entryType = QcTimeEntryType.INSPECTION,
            actorId = "insp-01",
            startedAt = "2026-08-17T09:00:00Z",
            durationMinutes = 60L,
            timestamp = "2026-08-17T10:00:00Z"
        )

        // Case 1: Overrun (Planned 45 < Actual 60)
        val overrun = repository.calculateReconciliation(
            productionJobId = jobId,
            plannedCost = 100.0,
            plannedMinutes = 45L,
            reconciledBy = "mgr-01",
            timestamp = "2026-08-17T10:05:00Z"
        )
        val reconOver = (overrun as DomainResult.Success).data
        assertEquals(15L, reconOver.timeVarianceMinutes)
        assertTrue(reconOver.hasTimeOverrun)

        // Case 2: Underrun (Planned 90 > Actual 60)
        val underrun = repository.calculateReconciliation(
            productionJobId = jobId,
            plannedCost = 100.0,
            plannedMinutes = 90L,
            reconciledBy = "mgr-01",
            timestamp = "2026-08-17T10:10:00Z"
        )
        val reconUnder = (underrun as DomainResult.Success).data
        assertEquals(-30L, reconUnder.timeVarianceMinutes)
        assertFalse(reconUnder.hasTimeOverrun)
    }
}

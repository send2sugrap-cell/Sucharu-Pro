package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeQcCostTimeDataSource
import com.sucharu.sucharupro.data.repository.QcCostTimeRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.QcCostType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QcCostVarianceTest {

    private lateinit var dataSource: FakeQcCostTimeDataSource
    private lateinit var repository: QcCostTimeRepository

    @Before
    fun setup() {
        dataSource = FakeQcCostTimeDataSource()
        repository = QcCostTimeRepositoryImpl(costTimeDataSource = dataSource)
    }

    @Test
    fun `cost variance correctly indicates overrun and underrun`() = runBlocking {
        val jobId = "JOB-01"

        repository.createCostEntry(
            projectId = "PRJ-01",
            productionJobId = jobId,
            costType = QcCostType.INSPECTION,
            description = "Consumables",
            quantity = 1.0,
            unitCost = 200.0,
            recordedBy = "insp-01",
            timestamp = "2026-08-17T09:00:00Z"
        )

        // Case 1: Overrun (Planned 150 < Actual 200)
        val overrun = repository.calculateReconciliation(
            productionJobId = jobId,
            plannedCost = 150.0,
            plannedMinutes = 30L,
            reconciledBy = "mgr-01",
            timestamp = "2026-08-17T10:00:00Z"
        )
        val reconOver = (overrun as DomainResult.Success).data
        assertEquals(50.0, reconOver.costVariance, 0.001)
        assertTrue(reconOver.hasCostOverrun)

        // Case 2: Underrun (Planned 250 > Actual 200)
        val underrun = repository.calculateReconciliation(
            productionJobId = jobId,
            plannedCost = 250.0,
            plannedMinutes = 30L,
            reconciledBy = "mgr-01",
            timestamp = "2026-08-17T10:05:00Z"
        )
        val reconUnder = (underrun as DomainResult.Success).data
        assertEquals(-50.0, reconUnder.costVariance, 0.001)
        assertFalse(reconUnder.hasCostOverrun)
    }
}

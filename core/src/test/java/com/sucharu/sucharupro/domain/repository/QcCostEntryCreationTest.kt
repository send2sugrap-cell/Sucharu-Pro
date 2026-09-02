package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeQcCostTimeDataSource
import com.sucharu.sucharupro.data.repository.QcCostTimeRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.QcCostStatus
import com.sucharu.sucharupro.domain.model.qc.QcCostType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QcCostEntryCreationTest {

    private lateinit var dataSource: FakeQcCostTimeDataSource
    private lateinit var repository: QcCostTimeRepository

    @Before
    fun setup() {
        dataSource = FakeQcCostTimeDataSource()
        repository = QcCostTimeRepositoryImpl(costTimeDataSource = dataSource)
    }

    @Test
    fun `createCostEntry successfully creates and stores cost entry`() = runBlocking {
        val result = repository.createCostEntry(
            projectId = "PRJ-01",
            productionJobId = "JOB-01",
            costType = QcCostType.INSPECTION,
            description = "Specialized ink density tester consumables",
            quantity = 2.0,
            unitCost = 125.0,
            currency = "BDT",
            recordedBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        assertTrue(result is DomainResult.Success)
        val entry = (result as DomainResult.Success).data
        assertEquals("JOB-01", entry.productionJobId)
        assertEquals("PRJ-01", entry.projectId)
        assertEquals(250.0, entry.totalCost, 0.001)
        assertEquals(QcCostStatus.RECORDED, entry.status)

        val fetched = repository.findCostEntryById(entry.id)
        assertTrue(fetched is DomainResult.Success)
        assertEquals(entry.id, (fetched as DomainResult.Success).data.id)
    }
}

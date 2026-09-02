package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeQcCostTimeDataSource
import com.sucharu.sucharupro.data.repository.QcCostTimeRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.QcCostType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QcCostEntryQuantityTest {

    private lateinit var dataSource: FakeQcCostTimeDataSource
    private lateinit var repository: QcCostTimeRepository

    @Before
    fun setup() {
        dataSource = FakeQcCostTimeDataSource()
        repository = QcCostTimeRepositoryImpl(costTimeDataSource = dataSource)
    }

    @Test
    fun `createCostEntry rejects zero or negative quantity`() = runBlocking {
        val zeroRes = repository.createCostEntry(
            projectId = "PRJ-01",
            productionJobId = "JOB-01",
            costType = QcCostType.INSPECTION,
            description = "Test",
            quantity = 0.0,
            unitCost = 100.0,
            recordedBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(zeroRes is DomainResult.Error)

        val negRes = repository.createCostEntry(
            projectId = "PRJ-01",
            productionJobId = "JOB-01",
            costType = QcCostType.INSPECTION,
            description = "Test",
            quantity = -5.0,
            unitCost = 100.0,
            recordedBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(negRes is DomainResult.Error)
    }
}

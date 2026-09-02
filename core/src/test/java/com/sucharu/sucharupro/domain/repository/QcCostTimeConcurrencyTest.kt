package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeQcCostTimeDataSource
import com.sucharu.sucharupro.data.repository.QcCostTimeRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.QcCostType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QcCostTimeConcurrencyTest {

    private lateinit var dataSource: FakeQcCostTimeDataSource
    private lateinit var repository: QcCostTimeRepository

    @Before
    fun setup() {
        dataSource = FakeQcCostTimeDataSource()
        repository = QcCostTimeRepositoryImpl(costTimeDataSource = dataSource)
    }

    @Test
    fun `concurrent cost entry creations do not lose updates`() = runBlocking {
        val count = 25
        val deferred = (1..count).map { i ->
            async {
                repository.createCostEntry(
                    projectId = "PRJ-01",
                    productionJobId = "JOB-01",
                    costType = QcCostType.INSPECTION,
                    description = "Consumable #$i",
                    quantity = 1.0,
                    unitCost = 10.0,
                    recordedBy = "insp-$i",
                    timestamp = "2026-08-17T10:00:00Z",
                    callerRole = UserRole.QC_INSPECTOR
                )
            }
        }

        val results = deferred.awaitAll()
        assertTrue(results.all { it is DomainResult.Success })

        val entries = (repository.getCostEntriesForJob("JOB-01") as DomainResult.Success).data
        assertEquals(count, entries.size)
    }

    @Test
    fun `concurrent lock operations execute idempotently with single snapshot`() = runBlocking {
        repository.createCostEntry(
            projectId = "PRJ-01",
            productionJobId = "JOB-01",
            costType = QcCostType.INSPECTION,
            description = "Consumable",
            quantity = 1.0,
            unitCost = 50.0,
            recordedBy = "insp-01",
            timestamp = "2026-08-17T09:00:00Z"
        )
        val recon = (repository.calculateReconciliation(
            productionJobId = "JOB-01",
            plannedCost = 50.0,
            plannedMinutes = 30L,
            reconciledBy = "mgr-01",
            timestamp = "2026-08-17T09:30:00Z"
        ) as DomainResult.Success).data

        val deferred = (1..10).map { i ->
            async {
                repository.lockReconciliation(
                    reconciliationId = recon.id,
                    lockedBy = "admin-$i",
                    timestamp = "2026-08-17T10:00:00Z",
                    callerRole = UserRole.ADMIN
                )
            }
        }

        val results = deferred.awaitAll()
        assertTrue(results.all { it is DomainResult.Success })

        val snapshots = (results.map { (it as DomainResult.Success).data.snapshotId }).distinct()
        assertEquals(1, snapshots.size)
    }
}

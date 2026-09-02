package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionReQcDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionReworkDataSource
import com.sucharu.sucharupro.data.repository.ProductionReQcRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ProductionRework
import com.sucharu.sucharupro.domain.model.qc.ReQcStatus
import com.sucharu.sucharupro.domain.model.qc.ReworkReason
import com.sucharu.sucharupro.domain.model.qc.ReworkStatus
import com.sucharu.sucharupro.domain.model.qc.ReworkType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for concurrency safety and mutex race condition protection (Module 06 Step 06).
 */
class ReQcConcurrencyTest {

    private lateinit var reQcDataSource: FakeProductionReQcDataSource
    private lateinit var reworkDataSource: FakeProductionReworkDataSource
    private lateinit var repository: ProductionReQcRepository

    @Before
    fun setUp() {
        runBlocking {
            reQcDataSource = FakeProductionReQcDataSource()
            reworkDataSource = FakeProductionReworkDataSource()
            repository = ProductionReQcRepositoryImpl(
                reQcDataSource = reQcDataSource,
                reworkDataSource = reworkDataSource
            )

            reworkDataSource.insertRework(
                ProductionRework(
                    reworkId = "rew-001",
                    projectId = "proj-001",
                    productionJobId = "job-001",
                    reworkType = ReworkType.COLOR_CORRECTION,
                    reason = ReworkReason.DEFECT_CORRECTION,
                    status = ReworkStatus.RETURNED_TO_QC,
                    affectedQuantity = 100,
                    description = "Color fixed",
                    requestedBy = "user-01",
                    requestedAt = "2026-08-17T10:00:00Z",
                    createdAt = "2026-08-17T10:00:00Z",
                    updatedAt = "2026-08-17T10:00:00Z"
                )
            )
        }
    }

    @Test
    fun concurrentReQcCreation_exactlyOneSucceeds() = runBlocking {
        val jobs = (1..10).map { i ->
            async(Dispatchers.Default) {
                repository.createReQc(
                    projectId = "proj-001",
                    productionJobId = "job-001",
                    productionReworkId = "rew-001",
                    createdBy = "insp-0$i",
                    timestamp = "2026-08-17T11:00:00Z",
                    callerRole = UserRole.QC_INSPECTOR
                )
            }
        }

        val results = jobs.awaitAll()
        val successCount = results.count { it is DomainResult.Success }
        val errorCount = results.count { it is DomainResult.Error }

        assertEquals(1, successCount)
        assertEquals(9, errorCount)

        val totalStored = repository.observeReQcList().first()
        assertEquals(1, totalStored.size)
    }

    @Test
    fun concurrentPass_resultsInExactlyOneValidPass() = runBlocking {
        val createRes = repository.createReQc(
            projectId = "proj-001",
            productionJobId = "job-001",
            productionReworkId = "rew-001",
            createdBy = "insp-01",
            timestamp = "2026-08-17T11:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val reQcId = (createRes as DomainResult.Success).data.reQcId

        repository.startInspection(
            reQcId = reQcId,
            inspectorId = "insp-01",
            timestamp = "2026-08-17T11:05:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        val passJobs = (1..5).map { i ->
            async(Dispatchers.Default) {
                repository.passReQc(
                    reQcId = reQcId,
                    inspectorId = "insp-01",
                    passNotes = "Pass $i",
                    timestamp = "2026-08-17T11:10:00Z",
                    callerRole = UserRole.QC_INSPECTOR
                )
            }
        }

        val results = passJobs.awaitAll()
        assertTrue(results.all { it is DomainResult.Success })

        val finalReQc = (repository.findReQcById(reQcId) as DomainResult.Success).data
        assertEquals(ReQcStatus.PASSED, finalReQc.status)
    }
}

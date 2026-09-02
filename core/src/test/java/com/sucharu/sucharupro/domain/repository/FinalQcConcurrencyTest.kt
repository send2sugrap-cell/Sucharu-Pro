package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeFinalQcDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionQcDataSource
import com.sucharu.sucharupro.data.repository.FinalQcRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ProductionQc
import com.sucharu.sucharupro.domain.model.qc.QcDecision
import com.sucharu.sucharupro.domain.model.qc.QcStatus
import com.sucharu.sucharupro.domain.model.qc.QcType
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
 * Concurrency tests verifying race-condition and atomic release safety (Module 06 Step 07).
 */
class FinalQcConcurrencyTest {

    private lateinit var finalQcDataSource: FakeFinalQcDataSource
    private lateinit var qcDataSource: FakeProductionQcDataSource
    private lateinit var repository: FinalQcRepository

    @Before
    fun setUp() {
        runBlocking {
            finalQcDataSource = FakeFinalQcDataSource()
            qcDataSource = FakeProductionQcDataSource()

            repository = FinalQcRepositoryImpl(
                finalQcDataSource = finalQcDataSource,
                qcDataSource = qcDataSource
            )

            // Seed Pre-Prod QC
            qcDataSource.insertQc(
                ProductionQc(
                    qcId = "qc-pre-01",
                    productionJobId = "job-01",
                    qcType = QcType.PRE_PRODUCTION,
                    status = QcStatus.PASSED,
                    decision = QcDecision.PASS,
                    createdAt = "2026-08-17T08:00:00Z",
                    updatedAt = "2026-08-17T08:00:00Z"
                )
            )
        }
    }

    @Test
    fun concurrentCreation_exactlyOneSucceeds() = runBlocking {
        val jobs = (1..10).map { i ->
            async(Dispatchers.Default) {
                repository.createFinalQc(
                    projectId = "proj-01",
                    productionJobId = "job-01",
                    totalQuantity = 500,
                    preProductionQcId = "qc-pre-01",
                    createdBy = "insp-0$i",
                    timestamp = "2026-08-17T10:00:00Z",
                    callerRole = UserRole.QC_INSPECTOR
                )
            }
        }

        val results = jobs.awaitAll()
        val successCount = results.count { it is DomainResult.Success }
        val errorCount = results.count { it is DomainResult.Error }

        assertEquals(1, successCount)
        assertEquals(9, errorCount)

        val totalStored = repository.observeFinalQcList().first()
        assertEquals(1, totalStored.size)
    }

    @Test
    fun concurrentReleaseAttempts_exactlyOneUniqueAuthorizationCreated() = runBlocking {
        val createRes = repository.createFinalQc(
            projectId = "proj-01",
            productionJobId = "job-01",
            totalQuantity = 500,
            preProductionQcId = "qc-pre-01",
            createdBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val finalQcId = (createRes as DomainResult.Success).data.finalQcId

        repository.startInspection(finalQcId, "insp-01", timestamp = "2026-08-17T10:05:00Z", callerRole = UserRole.QC_INSPECTOR)
        repository.submitPass(finalQcId, 500, "Pass", "insp-01", timestamp = "2026-08-17T10:10:00Z", callerRole = UserRole.QC_INSPECTOR)

        val releaseJobs = (1..10).map { i ->
            async(Dispatchers.Default) {
                repository.authorizeProductionRelease(
                    finalQcId = finalQcId,
                    releaseNotes = "Release attempt $i",
                    authorizedBy = "mgr-0$i",
                    authorizedByName = "Manager $i",
                    timestamp = "2026-08-17T10:15:00Z",
                    callerRole = UserRole.MANAGER
                )
            }
        }

        val results = releaseJobs.awaitAll()
        assertTrue(results.all { it is DomainResult.Success })

        val distinctAuthIds = results.map { (it as DomainResult.Success).data.releaseAuthorizationId }.distinct()
        assertEquals(1, distinctAuthIds.size)
    }
}

package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeFinalQcDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionQcDataSource
import com.sucharu.sucharupro.data.repository.FinalQcRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.FinalQcStatus
import com.sucharu.sucharupro.domain.model.qc.ProductionQc
import com.sucharu.sucharupro.domain.model.qc.QcDecision
import com.sucharu.sucharupro.domain.model.qc.QcStatus
import com.sucharu.sucharupro.domain.model.qc.QcType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Idempotency and atomic release tests for [FinalQcRepositoryImpl] (Module 06 Step 07).
 */
class FinalQcReleaseIdempotencyTest {

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

            // Seed passed Pre-Prod QC
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
    fun duplicateReleaseAttempts_areIdempotent() = runBlocking {
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
        repository.submitPass(finalQcId, 500, "All verified", "insp-01", timestamp = "2026-08-17T10:10:00Z", callerRole = UserRole.QC_INSPECTOR)

        // First release authorization
        val release1 = repository.authorizeProductionRelease(
            finalQcId = finalQcId,
            releaseNotes = "First authorization",
            authorizedBy = "mgr-01",
            authorizedByName = "Rahim Manager",
            timestamp = "2026-08-17T10:15:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(release1 is DomainResult.Success)
        val auth1 = (release1 as DomainResult.Success).data

        // Second duplicate release authorization
        val release2 = repository.authorizeProductionRelease(
            finalQcId = finalQcId,
            releaseNotes = "Duplicate authorization attempt",
            authorizedBy = "mgr-01",
            authorizedByName = "Rahim Manager",
            timestamp = "2026-08-17T10:20:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(release2 is DomainResult.Success)
        val auth2 = (release2 as DomainResult.Success).data

        // Must return identical authorization ID
        assertEquals(auth1.releaseAuthorizationId, auth2.releaseAuthorizationId)
    }
}

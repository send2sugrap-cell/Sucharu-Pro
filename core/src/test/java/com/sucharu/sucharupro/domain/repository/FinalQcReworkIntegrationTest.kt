package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeFinalQcDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionQcDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionReworkDataSource
import com.sucharu.sucharupro.data.repository.FinalQcRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ProductionQc
import com.sucharu.sucharupro.domain.model.qc.ProductionRework
import com.sucharu.sucharupro.domain.model.qc.QcDecision
import com.sucharu.sucharupro.domain.model.qc.QcStatus
import com.sucharu.sucharupro.domain.model.qc.QcType
import com.sucharu.sucharupro.domain.model.qc.ReworkReason
import com.sucharu.sucharupro.domain.model.qc.ReworkStatus
import com.sucharu.sucharupro.domain.model.qc.ReworkType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Rework integration tests for Final QC (Module 06 Step 07).
 */
class FinalQcReworkIntegrationTest {

    private lateinit var finalQcDataSource: FakeFinalQcDataSource
    private lateinit var qcDataSource: FakeProductionQcDataSource
    private lateinit var reworkDataSource: FakeProductionReworkDataSource
    private lateinit var repository: FinalQcRepository

    @Before
    fun setUp() {
        runBlocking {
            finalQcDataSource = FakeFinalQcDataSource()
            qcDataSource = FakeProductionQcDataSource()
            reworkDataSource = FakeProductionReworkDataSource()

            repository = FinalQcRepositoryImpl(
                finalQcDataSource = finalQcDataSource,
                qcDataSource = qcDataSource,
                reworkDataSource = reworkDataSource
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
    fun release_blockedWhenActiveReworkInProgress() = runBlocking {
        reworkDataSource.insertRework(
            ProductionRework(
                reworkId = "rew-01",
                projectId = "proj-01",
                productionJobId = "job-01",
                reworkType = ReworkType.COLOR_CORRECTION,
                reason = ReworkReason.DEFECT_CORRECTION,
                status = ReworkStatus.IN_PROGRESS,
                affectedQuantity = 50,
                description = "Plate adjustment",
                requestedBy = "user-01",
                requestedAt = "2026-08-17T09:00:00Z",
                createdAt = "2026-08-17T09:00:00Z",
                updatedAt = "2026-08-17T09:00:00Z"
            )
        )

        val createRes = repository.createFinalQc(
            projectId = "proj-01",
            productionJobId = "job-01",
            totalQuantity = 500,
            preProductionQcId = "qc-pre-01",
            sourceReworkIds = listOf("rew-01"),
            createdBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val finalQcId = (createRes as DomainResult.Success).data.finalQcId

        repository.startInspection(finalQcId, "insp-01", timestamp = "2026-08-17T10:05:00Z", callerRole = UserRole.QC_INSPECTOR)
        repository.submitPass(finalQcId, 500, "Passed", "insp-01", timestamp = "2026-08-17T10:10:00Z", callerRole = UserRole.QC_INSPECTOR)

        val releaseRes = repository.authorizeProductionRelease(
            finalQcId = finalQcId,
            authorizedBy = "mgr-01",
            timestamp = "2026-08-17T10:15:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(releaseRes is DomainResult.Error)
        assertTrue((releaseRes as DomainResult.Error).message.contains("Active rework process is in progress"))
    }
}

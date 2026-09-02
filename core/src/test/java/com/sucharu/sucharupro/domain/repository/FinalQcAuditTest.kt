package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeFinalQcDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionQcDataSource
import com.sucharu.sucharupro.data.repository.FinalQcRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.FinalQcActivityType
import com.sucharu.sucharupro.domain.model.qc.ProductionQc
import com.sucharu.sucharupro.domain.model.qc.QcDecision
import com.sucharu.sucharupro.domain.model.qc.QcStatus
import com.sucharu.sucharupro.domain.model.qc.QcType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Audit trail tests for [FinalQcRepositoryImpl] (Module 06 Step 07).
 */
class FinalQcAuditTest {

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
    fun auditEvents_recordedForCompleteLifecycle() = runBlocking {
        // 1. Create -> FINAL_QC_CREATED
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

        // 2. Assign -> FINAL_QC_ASSIGNED
        repository.assignInspector(
            finalQcId = finalQcId,
            inspectorId = "insp-01",
            inspectorName = "Tariq Inspector",
            assignedBy = "mgr-01",
            timestamp = "2026-08-17T10:05:00Z",
            callerRole = UserRole.MANAGER
        )

        // 3. Start -> FINAL_QC_STARTED
        repository.startInspection(
            finalQcId = finalQcId,
            inspectorId = "insp-01",
            timestamp = "2026-08-17T10:10:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        // 4. Pass -> FINAL_QC_PASSED
        repository.submitPass(
            finalQcId = finalQcId,
            acceptedQuantity = 500,
            notes = "Passed all checks",
            inspectorId = "insp-01",
            timestamp = "2026-08-17T10:15:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        // 5. Evaluate Eligibility -> FINAL_QC_RELEASE_ELIGIBILITY_CHECKED
        repository.evaluateReleaseEligibility(finalQcId)

        // 6. Release -> FINAL_QC_RELEASE_AUTHORIZED
        repository.authorizeProductionRelease(
            finalQcId = finalQcId,
            releaseNotes = "Approved by manager",
            authorizedBy = "mgr-01",
            authorizedByName = "Rahim Manager",
            timestamp = "2026-08-17T10:20:00Z",
            callerRole = UserRole.MANAGER
        )

        val events = repository.observeFinalQcActivity(finalQcId).first()
        val types = events.map { it.activityType }

        assertTrue(types.contains(FinalQcActivityType.FINAL_QC_CREATED))
        assertTrue(types.contains(FinalQcActivityType.FINAL_QC_ASSIGNED))
        assertTrue(types.contains(FinalQcActivityType.FINAL_QC_STARTED))
        assertTrue(types.contains(FinalQcActivityType.FINAL_QC_PASSED))
        assertTrue(types.contains(FinalQcActivityType.FINAL_QC_RELEASE_ELIGIBILITY_CHECKED))
        assertTrue(types.contains(FinalQcActivityType.FINAL_QC_RELEASE_AUTHORIZED))
        assertEquals(6, events.size)
    }
}

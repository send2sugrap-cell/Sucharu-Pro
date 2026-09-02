package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionReQcDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionReworkDataSource
import com.sucharu.sucharupro.data.repository.ProductionReQcRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ProductionRework
import com.sucharu.sucharupro.domain.model.qc.ReQcActivityType
import com.sucharu.sucharupro.domain.model.qc.ReQcFailureReason
import com.sucharu.sucharupro.domain.model.qc.ReworkReason
import com.sucharu.sucharupro.domain.model.qc.ReworkStatus
import com.sucharu.sucharupro.domain.model.qc.ReworkType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for audit activity recording across Re-QC operations (Module 06 Step 06).
 */
class ReQcAuditTest {

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
    fun auditEvents_recordedForFullLifecycle() = runBlocking {
        // 1. Create -> RE_QC_CREATED
        val cRes = repository.createReQc(
            projectId = "proj-001",
            productionJobId = "job-001",
            productionReworkId = "rew-001",
            createdBy = "user-01",
            timestamp = "2026-08-17T11:00:00Z",
            callerRole = UserRole.MANAGER
        )
        val reQcId = (cRes as DomainResult.Success).data.reQcId

        // 2. Assign -> RE_QC_ASSIGNED
        repository.assignReQc(
            reQcId = reQcId,
            inspectorId = "insp-01",
            inspectorName = "Tariq Inspector",
            assignedBy = "mgr-01",
            timestamp = "2026-08-17T11:05:00Z",
            callerRole = UserRole.MANAGER
        )

        // 3. Start -> RE_QC_STARTED
        repository.startInspection(
            reQcId = reQcId,
            inspectorId = "insp-01",
            timestamp = "2026-08-17T11:10:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        // 4. Fail -> RE_QC_FAILURE_RECORDED + RE_QC_FAILED
        repository.failReQc(
            reQcId = reQcId,
            failureReason = ReQcFailureReason.DEFECT_REMAINS,
            failureNotes = "Defect persists",
            affectedQuantity = 15,
            inspectorId = "insp-01",
            timestamp = "2026-08-17T11:20:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        // 5. Return to rework -> RE_QC_RETURNED_TO_REWORK
        repository.returnToRework(
            reQcId = reQcId,
            actorId = "insp-01",
            timestamp = "2026-08-17T11:25:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        val events = repository.observeReQcActivity(reQcId).first()
        val types = events.map { it.activityType }

        assertTrue(types.contains(ReQcActivityType.RE_QC_CREATED))
        assertTrue(types.contains(ReQcActivityType.RE_QC_ASSIGNED))
        assertTrue(types.contains(ReQcActivityType.RE_QC_STARTED))
        assertTrue(types.contains(ReQcActivityType.RE_QC_FAILURE_RECORDED))
        assertTrue(types.contains(ReQcActivityType.RE_QC_FAILED))
        assertTrue(types.contains(ReQcActivityType.RE_QC_RETURNED_TO_REWORK))
        assertEquals(6, events.size)
    }
}

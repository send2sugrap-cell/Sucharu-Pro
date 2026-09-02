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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive repository test suite for [ProductionReQcRepositoryImpl] (Module 06 Step 06).
 */
class ReQcRepositoryTest {

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
    fun assignAndReassignAndUnassignFlow() = runBlocking {
        val createRes = repository.createReQc(
            projectId = "proj-001",
            productionJobId = "job-001",
            productionReworkId = "rew-001",
            createdBy = "insp-01",
            timestamp = "2026-08-17T11:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val reQcId = (createRes as DomainResult.Success).data.reQcId

        // 1. Assign
        val assignRes = repository.assignReQc(
            reQcId = reQcId,
            inspectorId = "insp-01",
            inspectorName = "Tariq Inspector",
            assignedBy = "mgr-01",
            timestamp = "2026-08-17T11:05:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(assignRes is DomainResult.Success)
        val assigned = (assignRes as DomainResult.Success).data
        assertEquals(ReQcStatus.ASSIGNED, assigned.status)
        assertEquals("insp-01", assigned.assignedInspectorId)

        // 2. Reassign
        val reassignRes = repository.reassignReQc(
            reQcId = reQcId,
            newInspectorId = "insp-02",
            newInspectorName = "Rahim Inspector",
            reassignedBy = "mgr-01",
            timestamp = "2026-08-17T11:10:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(reassignRes is DomainResult.Success)
        val reassigned = (reassignRes as DomainResult.Success).data
        assertEquals("insp-02", reassigned.assignedInspectorId)

        // 3. Unassign
        val unassignRes = repository.unassignReQc(
            reQcId = reQcId,
            unassignedBy = "mgr-01",
            timestamp = "2026-08-17T11:15:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(unassignRes is DomainResult.Success)
        val unassigned = (unassignRes as DomainResult.Success).data
        assertEquals(ReQcStatus.PENDING, unassigned.status)
        assertNull(unassigned.assignedInspectorId)
    }

    @Test
    fun cancelReQc_fromPending_success() = runBlocking {
        val createRes = repository.createReQc(
            projectId = "proj-001",
            productionJobId = "job-001",
            productionReworkId = "rew-001",
            createdBy = "insp-01",
            timestamp = "2026-08-17T11:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val reQcId = (createRes as DomainResult.Success).data.reQcId

        val cancelRes = repository.cancelReQc(
            reQcId = reQcId,
            reason = "Job was cancelled by customer",
            cancelledBy = "mgr-01",
            timestamp = "2026-08-17T11:20:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(cancelRes is DomainResult.Success)
        val cancelled = (cancelRes as DomainResult.Success).data
        assertEquals(ReQcStatus.CANCELLED, cancelled.status)
        assertTrue(cancelled.isTerminal)
    }

    @Test
    fun observeQueries_byJobAndProject() = runBlocking {
        repository.createReQc(
            projectId = "proj-001",
            productionJobId = "job-001",
            productionReworkId = "rew-001",
            createdBy = "insp-01",
            timestamp = "2026-08-17T11:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        val byJob = repository.observeReQcByJob("job-001").first()
        assertEquals(1, byJob.size)

        val byProj = repository.observeReQcByProject("proj-001").first()
        assertEquals(1, byProj.size)
    }
}

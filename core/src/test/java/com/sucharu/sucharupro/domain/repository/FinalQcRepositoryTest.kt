package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeFinalQcDataSource
import com.sucharu.sucharupro.data.repository.FinalQcRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.FinalQcStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * General CRUD and inspector assignment repository tests for [FinalQcRepositoryImpl] (Module 06 Step 07).
 */
class FinalQcRepositoryTest {

    private lateinit var finalQcDataSource: FakeFinalQcDataSource
    private lateinit var repository: FinalQcRepository

    @Before
    fun setUp() {
        runBlocking {
            finalQcDataSource = FakeFinalQcDataSource()
            repository = FinalQcRepositoryImpl(finalQcDataSource = finalQcDataSource)
        }
    }

    @Test
    fun assignReassignAndUnassignInspector() = runBlocking {
        val createRes = repository.createFinalQc(
            projectId = "proj-01",
            productionJobId = "job-01",
            totalQuantity = 500,
            createdBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val finalQcId = (createRes as DomainResult.Success).data.finalQcId

        // 1. Assign
        val assignRes = repository.assignInspector(
            finalQcId = finalQcId,
            inspectorId = "insp-01",
            inspectorName = "Tariq Inspector",
            assignedBy = "mgr-01",
            timestamp = "2026-08-17T10:05:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(assignRes is DomainResult.Success)
        val assigned = (assignRes as DomainResult.Success).data
        assertEquals(FinalQcStatus.ASSIGNED, assigned.status)
        assertEquals("insp-01", assigned.assignedInspectorId)

        // 2. Reassign
        val reassignRes = repository.reassignInspector(
            finalQcId = finalQcId,
            newInspectorId = "insp-02",
            newInspectorName = "Rahim Inspector",
            reassignedBy = "mgr-01",
            timestamp = "2026-08-17T10:10:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(reassignRes is DomainResult.Success)
        val reassigned = (reassignRes as DomainResult.Success).data
        assertEquals("insp-02", reassigned.assignedInspectorId)

        // 3. Unassign
        val unassignRes = repository.unassignInspector(
            finalQcId = finalQcId,
            unassignedBy = "mgr-01",
            timestamp = "2026-08-17T10:15:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(unassignRes is DomainResult.Success)
        val unassigned = (unassignRes as DomainResult.Success).data
        assertEquals(FinalQcStatus.PENDING, unassigned.status)
        assertNull(unassigned.assignedInspectorId)
    }

    @Test
    fun cancelFinalQc_fromPending_success() = runBlocking {
        val createRes = repository.createFinalQc(
            projectId = "proj-01",
            productionJobId = "job-01",
            totalQuantity = 500,
            createdBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val finalQcId = (createRes as DomainResult.Success).data.finalQcId

        val cancelRes = repository.cancelFinalQc(
            finalQcId = finalQcId,
            reason = "Customer cancelled the print order.",
            cancelledBy = "mgr-01",
            timestamp = "2026-08-17T10:20:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(cancelRes is DomainResult.Success)
        val cancelled = (cancelRes as DomainResult.Success).data
        assertEquals(FinalQcStatus.CANCELLED, cancelled.status)
        assertTrue(cancelled.isTerminal)
    }

    @Test
    fun queryFilters_byJobAndProject() = runBlocking {
        repository.createFinalQc(
            projectId = "proj-01",
            productionJobId = "job-01",
            totalQuantity = 500,
            createdBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        val byJob = repository.observeFinalQcByJob("job-01").first()
        assertEquals(1, byJob.size)

        val byProj = repository.observeFinalQcByProject("proj-01").first()
        assertEquals(1, byProj.size)
    }
}

package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionQcDataSource
import com.sucharu.sucharupro.data.repository.ProductionQcRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ProductionQc
import com.sucharu.sucharupro.domain.model.qc.QcStatus
import com.sucharu.sucharupro.domain.model.qc.QcType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for Inspector Assignment, Reassignment, and Unassignment (Module 06 Step 01).
 */
class ProductionQcAssignmentTest {

    private lateinit var qcDataSource: FakeProductionQcDataSource
    private lateinit var qcRepository: ProductionQcRepository

    private val sampleQc = ProductionQc(
        qcId = "qc-asgn-01",
        productionJobId = "job-01",
        qcType = QcType.PRE_PRODUCTION,
        status = QcStatus.DRAFT,
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    @Before
    fun setUp() {
        qcDataSource = FakeProductionQcDataSource(initialQcList = listOf(sampleQc))
        qcRepository = ProductionQcRepositoryImpl(qcDataSource)
    }

    @Test
    fun assignInspector_advancesDraftToPendingInspectionAndRecordsAssignment() = runBlocking {
        val result = qcRepository.assignInspector(
            qcId = "qc-asgn-01",
            inspectorId = "insp-01",
            inspectorName = "রফিক আহমেদ",
            assignedBy = "mgr-01",
            timestamp = "2026-08-16T10:15:00Z",
            callerRole = UserRole.MANAGER
        )

        assertTrue(result is DomainResult.Success)
        val updated = (result as DomainResult.Success).data
        assertEquals(QcStatus.PENDING_INSPECTION, updated.status)
        assertEquals("insp-01", updated.assignedInspectorId)
        assertEquals("রফিক আহমেদ", updated.assignedInspectorName)

        val assignments = qcRepository.observeAssignments("qc-asgn-01").first()
        assertEquals(1, assignments.size)
        assertTrue(assignments.first().isActive)
        assertEquals("insp-01", assignments.first().inspectorId)
    }

    @Test
    fun reassignInspector_updatesActiveInspectorAndPreservesHistory() = runBlocking {
        qcRepository.assignInspector(
            qcId = "qc-asgn-01",
            inspectorId = "insp-01",
            inspectorName = "রফিক আহমেদ",
            assignedBy = "mgr-01",
            timestamp = "2026-08-16T10:15:00Z",
            callerRole = UserRole.MANAGER
        )

        val reassignRes = qcRepository.reassignInspector(
            qcId = "qc-asgn-01",
            newInspectorId = "insp-02",
            newInspectorName = "সালাহউদ্দিন",
            reassignedBy = "mgr-01",
            reason = "শিফট পরিবর্তন",
            timestamp = "2026-08-16T10:30:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(reassignRes is DomainResult.Success)
        val updated = (reassignRes as DomainResult.Success).data
        assertEquals("insp-02", updated.assignedInspectorId)

        val assignments = qcRepository.observeAssignments("qc-asgn-01").first()
        assertEquals(2, assignments.size)
        val oldAssignment = assignments.find { it.inspectorId == "insp-01" }
        val newAssignment = assignments.find { it.inspectorId == "insp-02" }
        assertFalse(oldAssignment!!.isActive)
        assertTrue(newAssignment!!.isActive)
    }

    @Test
    fun unassignInspector_clearsAssignmentAndRevertsToDraft() = runBlocking {
        qcRepository.assignInspector(
            qcId = "qc-asgn-01",
            inspectorId = "insp-01",
            inspectorName = "রফিক আহমেদ",
            assignedBy = "mgr-01",
            timestamp = "2026-08-16T10:15:00Z",
            callerRole = UserRole.MANAGER
        )

        val unassignRes = qcRepository.unassignInspector(
            qcId = "qc-asgn-01",
            unassignedBy = "mgr-01",
            reason = "স্থগিত",
            timestamp = "2026-08-16T10:45:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(unassignRes is DomainResult.Success)
        val updated = (unassignRes as DomainResult.Success).data
        assertNull(updated.assignedInspectorId)
        assertEquals(QcStatus.DRAFT, updated.status)
    }
}

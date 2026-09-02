package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionDefectDataSource
import com.sucharu.sucharupro.data.repository.ProductionDefectRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.DefectCategory
import com.sucharu.sucharupro.domain.model.qc.DefectSeverity
import com.sucharu.sucharupro.domain.model.qc.DefectSource
import com.sucharu.sucharupro.domain.model.qc.DefectStatus
import com.sucharu.sucharupro.domain.model.qc.ProductionDefect
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
 * Tests for [DefectAssignment] lifecycle, reassignment, and unassignment (Module 06 Step 04).
 */
class ProductionDefectAssignmentTest {

    private lateinit var dataSource: FakeProductionDefectDataSource
    private lateinit var repository: ProductionDefectRepository

    private val sampleDefect = ProductionDefect(
        defectId = "def-asgn-01",
        productionJobId = "job-01",
        category = DefectCategory.FOIL_ERROR,
        severity = DefectSeverity.MAJOR,
        source = DefectSource.PRODUCTION_STAGE,
        status = DefectStatus.OPEN,
        title = "Foil flaking off",
        description = "Gold foil stamping did not adhere properly.",
        affectedQuantity = 300,
        detectedAt = "2026-08-17T10:00:00Z",
        detectedBy = "insp-01",
        createdAt = "2026-08-17T10:00:00Z",
        updatedAt = "2026-08-17T10:00:00Z"
    )

    @Before
    fun setUp() {
        dataSource = FakeProductionDefectDataSource(initialDefects = listOf(sampleDefect))
        repository = ProductionDefectRepositoryImpl(dataSource)
    }

    @Test
    fun assignDefect_assignsTechnicianAndRecordsHistory() = runBlocking {
        val res = repository.assignDefect(
            defectId = "def-asgn-01",
            assigneeId = "tech-01",
            assigneeName = "Karim Foil Specialist",
            assignedBy = "admin-01",
            reason = "Assigned for foil temperature recalibration",
            timestamp = "2026-08-17T10:15:00Z",
            callerRole = UserRole.ADMIN
        )

        assertTrue(res is DomainResult.Success)
        val defect = (res as DomainResult.Success).data
        assertEquals("tech-01", defect.assignedToId)
        assertEquals("Karim Foil Specialist", defect.assignedToName)
        assertTrue(defect.isAssigned)

        val assignments = repository.observeAssignments("def-asgn-01").first()
        assertEquals(1, assignments.size)
        assertEquals("tech-01", assignments[0].assigneeId)
        assertTrue(assignments[0].active)
    }

    @Test
    fun reassignDefect_deactivatesPreviousAndCreatesNewAssignment() = runBlocking {
        repository.assignDefect(
            defectId = "def-asgn-01",
            assigneeId = "tech-01",
            assigneeName = "Karim",
            assignedBy = "admin-01",
            timestamp = "2026-08-17T10:15:00Z",
            callerRole = UserRole.ADMIN
        )

        val reassignRes = repository.reassignDefect(
            defectId = "def-asgn-01",
            newAssigneeId = "tech-02",
            newAssigneeName = "Jamal Supervisor",
            reassignedBy = "admin-01",
            reason = "Reassigned to senior technician",
            timestamp = "2026-08-17T10:30:00Z",
            callerRole = UserRole.ADMIN
        )

        assertTrue(reassignRes is DomainResult.Success)
        val defect = (reassignRes as DomainResult.Success).data
        assertEquals("tech-02", defect.assignedToId)

        val assignments = repository.observeAssignments("def-asgn-01").first()
        assertEquals(2, assignments.size)
        val active = assignments.filter { it.active }
        assertEquals(1, active.size)
        assertEquals("tech-02", active[0].assigneeId)
    }

    @Test
    fun unassignDefect_clearsAssignmentAndDeactivatesRecords() = runBlocking {
        repository.assignDefect(
            defectId = "def-asgn-01",
            assigneeId = "tech-01",
            assigneeName = "Karim",
            assignedBy = "admin-01",
            timestamp = "2026-08-17T10:15:00Z",
            callerRole = UserRole.ADMIN
        )

        val unassignRes = repository.unassignDefect(
            defectId = "def-asgn-01",
            unassignedBy = "admin-01",
            timestamp = "2026-08-17T10:45:00Z",
            callerRole = UserRole.ADMIN
        )

        assertTrue(unassignRes is DomainResult.Success)
        val defect = (unassignRes as DomainResult.Success).data
        assertNull(defect.assignedToId)
        assertFalse(defect.isAssigned)

        val assignments = repository.observeAssignments("def-asgn-01").first()
        assertTrue(assignments.all { !it.active })
    }
}

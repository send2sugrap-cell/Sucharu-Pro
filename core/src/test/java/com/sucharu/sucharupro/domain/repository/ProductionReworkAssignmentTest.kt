package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionReworkDataSource
import com.sucharu.sucharupro.data.repository.ProductionReworkRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ReworkReason
import com.sucharu.sucharupro.domain.model.qc.ReworkStatus
import com.sucharu.sucharupro.domain.model.qc.ReworkType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for Rework assignment, reassignment, and unassignment (Module 06 Step 05).
 */
class ProductionReworkAssignmentTest {

    private lateinit var dataSource: FakeProductionReworkDataSource
    private lateinit var repository: ProductionReworkRepository

    @Before
    fun setUp() {
        dataSource = FakeProductionReworkDataSource()
        repository = ProductionReworkRepositoryImpl(dataSource)
    }

    @Test
    fun assignRework_fromApproved_transitionsToAssigned() = runBlocking {
        val createRes = repository.createRework(
            projectId = "proj-01",
            productionJobId = "job-01",
            reworkType = ReworkType.PRINT_CORRECTION,
            reason = ReworkReason.PRINT_ERROR,
            affectedQuantity = 50,
            quantityUnit = "pcs",
            description = "Print correction",
            requestedBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val reworkId = (createRes as DomainResult.Success).data.reworkId
        repository.approveRework(reworkId, "mgr-01", "Manager", null, "2026-08-17T10:10:00Z", UserRole.MANAGER)

        val assignRes = repository.assignRework(
            reworkId = reworkId,
            assignedTo = "tech-01",
            assignedToName = "Karim Pressman",
            assignedBy = "mgr-01",
            assignedByName = "Manager",
            notes = "Priority job",
            timestamp = "2026-08-17T10:20:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(assignRes is DomainResult.Success)
        val assigned = (assignRes as DomainResult.Success).data
        assertEquals(ReworkStatus.ASSIGNED, assigned.status)
        assertEquals("tech-01", assigned.assignedTo)
        assertEquals("Karim Pressman", assigned.assignedToName)
        assertTrue(assigned.isAssigned)
    }

    @Test
    fun reassignRework_updatesAssignee() = runBlocking {
        val createRes = repository.createRework(
            projectId = "proj-01",
            productionJobId = "job-01",
            reworkType = ReworkType.PRINT_CORRECTION,
            reason = ReworkReason.PRINT_ERROR,
            affectedQuantity = 50,
            quantityUnit = "pcs",
            description = "Print correction",
            requestedBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val reworkId = (createRes as DomainResult.Success).data.reworkId
        repository.approveRework(reworkId, "mgr-01", "Manager", null, "2026-08-17T10:10:00Z", UserRole.MANAGER)
        repository.assignRework(reworkId, "tech-01", "Karim Pressman", "mgr-01", "Manager", null, "2026-08-17T10:20:00Z", UserRole.MANAGER)

        val reassignRes = repository.reassignRework(
            reworkId = reworkId,
            newAssignedTo = "tech-02",
            newAssignedToName = "Salim Offset Master",
            reassignedBy = "mgr-01",
            reassignedByName = "Manager",
            notes = "Shift handoff",
            timestamp = "2026-08-17T14:00:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(reassignRes is DomainResult.Success)
        val reassigned = (reassignRes as DomainResult.Success).data
        assertEquals("tech-02", reassigned.assignedTo)
        assertEquals("Salim Offset Master", reassigned.assignedToName)
    }

    @Test
    fun unassignRework_revertsToApprovedAndClearsAssignee() = runBlocking {
        val createRes = repository.createRework(
            projectId = "proj-01",
            productionJobId = "job-01",
            reworkType = ReworkType.PRINT_CORRECTION,
            reason = ReworkReason.PRINT_ERROR,
            affectedQuantity = 50,
            quantityUnit = "pcs",
            description = "Print correction",
            requestedBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val reworkId = (createRes as DomainResult.Success).data.reworkId
        repository.approveRework(reworkId, "mgr-01", "Manager", null, "2026-08-17T10:10:00Z", UserRole.MANAGER)
        repository.assignRework(reworkId, "tech-01", "Karim Pressman", "mgr-01", "Manager", null, "2026-08-17T10:20:00Z", UserRole.MANAGER)

        val unassignRes = repository.unassignRework(
            reworkId = reworkId,
            unassignedBy = "mgr-01",
            unassignedByName = "Manager",
            notes = "Operator called in sick",
            timestamp = "2026-08-17T11:00:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(unassignRes is DomainResult.Success)
        val unassigned = (unassignRes as DomainResult.Success).data
        assertEquals(ReworkStatus.APPROVED, unassigned.status)
        assertNull(unassigned.assignedTo)
        assertNull(unassigned.assignedToName)
    }
}

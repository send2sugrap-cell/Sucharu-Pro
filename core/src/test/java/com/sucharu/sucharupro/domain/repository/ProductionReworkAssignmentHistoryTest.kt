package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionReworkDataSource
import com.sucharu.sucharupro.data.repository.ProductionReworkRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ReworkReason
import com.sucharu.sucharupro.domain.model.qc.ReworkType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests verifying immutable assignment history tracking across multiple reassignments (Module 06 Step 05).
 */
class ProductionReworkAssignmentHistoryTest {

    private lateinit var dataSource: FakeProductionReworkDataSource
    private lateinit var repository: ProductionReworkRepository

    @Before
    fun setUp() {
        dataSource = FakeProductionReworkDataSource()
        repository = ProductionReworkRepositoryImpl(dataSource)
    }

    @Test
    fun reassignment_preservesPreviousAssignmentRecordsInHistory() = runBlocking {
        val createRes = repository.createRework(
            projectId = "proj-01",
            productionJobId = "job-01",
            reworkType = ReworkType.FOLDING_CORRECTION,
            reason = ReworkReason.FAILED_QC,
            affectedQuantity = 200,
            quantityUnit = "sheets",
            description = "Folding misalignment",
            requestedBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val reworkId = (createRes as DomainResult.Success).data.reworkId
        repository.approveRework(reworkId, "mgr-01", "Manager", null, "2026-08-17T10:10:00Z", UserRole.MANAGER)

        // 1st Assignment
        repository.assignRework(
            reworkId = reworkId,
            assignedTo = "tech-01",
            assignedToName = "Karim",
            assignedBy = "mgr-01",
            assignedByName = "Manager",
            timestamp = "2026-08-17T10:20:00Z",
            callerRole = UserRole.MANAGER
        )

        // 2nd Assignment (Reassignment)
        repository.reassignRework(
            reworkId = reworkId,
            newAssignedTo = "tech-02",
            newAssignedToName = "Salim",
            reassignedBy = "mgr-01",
            reassignedByName = "Manager",
            timestamp = "2026-08-17T12:00:00Z",
            callerRole = UserRole.MANAGER
        )

        val assignments = repository.observeAssignments(reworkId).first()
        assertEquals(2, assignments.size)

        // First assignment should be inactive with unassignedAt timestamp
        val firstAsgn = assignments.find { it.assignedTo == "tech-01" }
        assertNotNull(firstAsgn)
        assertFalse(firstAsgn!!.active)
        assertNotNull(firstAsgn.unassignedAt)

        // Second assignment should be active
        val secondAsgn = assignments.find { it.assignedTo == "tech-02" }
        assertNotNull(secondAsgn)
        assertTrue(secondAsgn!!.active)
    }
}

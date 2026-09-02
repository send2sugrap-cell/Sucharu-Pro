package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeDesignProjectDataSource
import com.sucharu.sucharupro.data.repository.DesignProjectRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.design.DesignAssignmentStatus
import com.sucharu.sucharupro.domain.model.design.DesignProject
import com.sucharu.sucharupro.domain.model.design.DesignStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for designer assignment, reassignment, and unassignment workflows (Module 05 Step 01).
 */
class DesignerAssignmentTest {

    private lateinit var dataSource: FakeDesignProjectDataSource
    private lateinit var repository: DesignProjectRepository

    private val sampleProject = DesignProject(
        projectId = "des-01",
        projectNumber = "DES-2026-0001",
        productionJobId = "job-01",
        orderId = "ord-01",
        orderNumber = "ORD-2026-0001",
        customerId = "cus-01",
        title = "বই কভার ডিজাইন",
        status = DesignStatus.NOT_STARTED,
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    @Before
    fun setUp() = runBlocking {
        dataSource = FakeDesignProjectDataSource(initialProjects = listOf(sampleProject))
        repository = DesignProjectRepositoryImpl(dataSource)
    }

    @Test
    fun assignDesigner_success_updatesProjectAndRecordsAssignment() = runBlocking {
        val result = repository.assignDesigner(
            projectId = "des-01",
            designerId = "des-01",
            designerName = "তানভীর হাসান",
            assignedBy = "Manager",
            notes = "প্রথম ড্রাফট ২ দিনের মধ্যে প্রস্তুত করতে হবে",
            timestamp = "2026-08-16T11:00:00Z"
        )

        assertTrue(result is DomainResult.Success)
        val updated = (result as DomainResult.Success).data
        assertEquals("des-01", updated.assignedDesignerId)
        assertEquals("তানভীর হাসান", updated.assignedDesignerName)
        assertEquals(DesignStatus.ASSIGNED, updated.status)

        val assignments = repository.getAssignmentsForProject("des-01").first()
        assertEquals(1, assignments.size)
        assertEquals(DesignAssignmentStatus.ACTIVE, assignments.first().status)
        assertEquals("des-01", assignments.first().designerId)
    }

    @Test
    fun reassignDesigner_success_updatesPreviousAndCreatesNewActive() = runBlocking {
        // First assignment
        repository.assignDesigner(
            projectId = "des-01",
            designerId = "des-01",
            designerName = "তানভীর হাসান",
            assignedBy = "Manager",
            timestamp = "2026-08-16T11:00:00Z"
        )

        // Reassign
        val reassignResult = repository.reassignDesigner(
            projectId = "des-01",
            newDesignerId = "des-02",
            newDesignerName = "নুসরাত জাহান",
            reassignedBy = "Admin",
            reason = "তানভীর অসুস্থতাজনিত ছুটিতে আছেন",
            timestamp = "2026-08-16T12:00:00Z"
        )

        assertTrue(reassignResult is DomainResult.Success)
        val updated = (reassignResult as DomainResult.Success).data
        assertEquals("des-02", updated.assignedDesignerId)
        assertEquals("নুসরাত জাহান", updated.assignedDesignerName)

        val assignments = repository.getAssignmentsForProject("des-01").first()
        assertEquals(2, assignments.size)
        val oldAssignment = assignments.find { it.designerId == "des-01" }
        val newAssignment = assignments.find { it.designerId == "des-02" }

        assertEquals(DesignAssignmentStatus.REASSIGNED, oldAssignment?.status)
        assertEquals(DesignAssignmentStatus.ACTIVE, newAssignment?.status)
    }

    @Test
    fun unassignDesigner_success_resetsProjectAndMarksUnassigned() = runBlocking {
        // First assignment
        repository.assignDesigner(
            projectId = "des-01",
            designerId = "des-01",
            designerName = "তানভীর হাসান",
            assignedBy = "Manager",
            timestamp = "2026-08-16T11:00:00Z"
        )

        // Unassign
        val unassignResult = repository.unassignDesigner(
            projectId = "des-01",
            unassignedBy = "Manager",
            reason = "কাস্টমার কনফার্মেশন অপেক্ষমান",
            timestamp = "2026-08-16T13:00:00Z"
        )

        assertTrue(unassignResult is DomainResult.Success)
        val updated = (unassignResult as DomainResult.Success).data
        assertNull(updated.assignedDesignerId)
        assertNull(updated.assignedDesignerName)
        assertEquals(DesignStatus.NOT_STARTED, updated.status)

        val assignments = repository.getAssignmentsForProject("des-01").first()
        assertEquals(1, assignments.size)
        assertEquals(DesignAssignmentStatus.UNASSIGNED, assignments.first().status)
    }

    @Test
    fun unassignDesigner_whileInDesign_fails() = runBlocking {
        repository.assignDesigner(
            projectId = "des-01",
            designerId = "des-01",
            designerName = "তানভীর হাসান",
            assignedBy = "Manager",
            timestamp = "2026-08-16T11:00:00Z"
        )

        repository.startDesign(
            projectId = "des-01",
            actorId = "des-01",
            actorName = "তানভীর হাসান",
            timestamp = "2026-08-16T11:30:00Z"
        )

        val unassignResult = repository.unassignDesigner(
            projectId = "des-01",
            unassignedBy = "Manager",
            timestamp = "2026-08-16T12:00:00Z"
        )

        assertTrue("Unassigning while in design should fail", unassignResult is DomainResult.Error)
        val error = unassignResult as DomainResult.Error
        assertTrue(error.message.contains("while project is actively in design"))
    }
}

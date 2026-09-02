package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeDesignProjectDataSource
import com.sucharu.sucharupro.data.repository.DesignProjectRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.design.DesignProject
import com.sucharu.sucharupro.domain.model.design.DesignStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying CRUD and reactive flow operations of [DesignProjectRepositoryImpl] (Module 05 Step 01).
 */
class DesignRepositoryTest {

    private lateinit var dataSource: FakeDesignProjectDataSource
    private lateinit var repository: DesignProjectRepository

    private val project1 = DesignProject(
        projectId = "des-01",
        projectNumber = "DES-2026-0001",
        productionJobId = "job-01",
        orderId = "ord-01",
        orderNumber = "ORD-2026-0001",
        customerId = "cus-01",
        title = "বই কভার ডিজাইন",
        status = DesignStatus.NOT_STARTED,
        assignedDesignerId = "des-user-01",
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    private val project2 = DesignProject(
        projectId = "des-02",
        projectNumber = "DES-2026-0002",
        productionJobId = "job-02",
        orderId = "ord-02",
        orderNumber = "ORD-2026-0002",
        customerId = "cus-02",
        title = "ক্যালেন্ডার আর্টওয়ার্ক",
        status = DesignStatus.NOT_STARTED,
        assignedDesignerId = "des-user-02",
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    @Before
    fun setUp() {
        dataSource = FakeDesignProjectDataSource(initialProjects = listOf(project1, project2))
        repository = DesignProjectRepositoryImpl(dataSource)
    }

    @Test
    fun observeDesignProjects_emitsAllProjects() = runBlocking {
        val projects = repository.observeDesignProjects().first()
        assertEquals(2, projects.size)
    }

    @Test
    fun getDesignProjectById_existingProject_returnsProject() = runBlocking {
        val found = repository.getDesignProjectById("des-01").first()
        assertNotNull(found)
        assertEquals("DES-2026-0001", found?.projectNumber)
    }

    @Test
    fun getDesignProjectById_nonExistent_returnsNull() = runBlocking {
        val found = repository.getDesignProjectById("invalid-id").first()
        assertNull(found)
    }

    @Test
    fun getDesignProjectForJob_returnsCorrectProject() = runBlocking {
        val found = repository.getDesignProjectForJob("job-02").first()
        assertNotNull(found)
        assertEquals("des-02", found?.projectId)
    }

    @Test
    fun getDesignProjectsForDesigner_returnsFilteredList() = runBlocking {
        val projects = repository.getDesignProjectsForDesigner("des-user-01").first()
        assertEquals(1, projects.size)
        assertEquals("des-01", projects.first().projectId)
    }

    @Test
    fun updateDesignStatus_advancesStatusSuccessfully() = runBlocking {
        // Assign designer first
        repository.assignDesigner(
            projectId = "des-01",
            designerId = "des-01",
            designerName = "Tanveer",
            timestamp = "2026-08-16T10:05:00Z"
        )

        val result = repository.updateDesignStatus(
            projectId = "des-01",
            targetStatus = DesignStatus.IN_DESIGN,
            actorId = "des-01",
            actorName = "Tanveer",
            notes = "Artboard layout setup done",
            timestamp = "2026-08-16T10:10:00Z"
        )

        assertTrue(result is DomainResult.Success)
        val updated = (result as DomainResult.Success).data
        assertEquals(DesignStatus.IN_DESIGN, updated.status)
        assertEquals("2026-08-16T10:10:00Z", updated.startedAt)
    }

    @Test
    fun cancelDesignProject_updatesStatusAndPreservesReason() = runBlocking {
        val result = repository.cancelDesignProject(
            projectId = "des-01",
            reason = "অর্ডার বাতিল হয়েছে",
            cancelledBy = "Admin",
            timestamp = "2026-08-16T12:00:00Z"
        )

        assertTrue(result is DomainResult.Success)
        val cancelled = (result as DomainResult.Success).data
        assertEquals(DesignStatus.CANCELLED, cancelled.status)
        assertTrue(cancelled.notes?.contains("Cancelled: অর্ডার বাতিল হয়েছে") == true)
    }
}

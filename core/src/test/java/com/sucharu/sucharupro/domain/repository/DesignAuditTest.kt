package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeDesignProjectDataSource
import com.sucharu.sucharupro.data.repository.DesignProjectRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.design.DesignActivityType
import com.sucharu.sucharupro.domain.model.design.DesignStatus
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Audit trail activity event generation tests for Design domain (Module 05 Step 01).
 */
class DesignAuditTest {

    private lateinit var dataSource: FakeDesignProjectDataSource
    private lateinit var repository: DesignProjectRepository

    private val sampleJob = ProductionJob(
        jobId = "job-audit-01",
        jobNumber = "JOB-AUDIT-0001",
        orderId = "ord-audit-01",
        orderNumber = "ORD-AUDIT-0001",
        customerId = "cus-audit-01",
        handoffId = "hnd-audit-01",
        title = "বই কভার ডিজাইন ও বাঁধাই",
        priority = OrderPriority.NORMAL,
        status = ProductionJobStatus.READY_FOR_PRODUCTION,
        quantity = 2000,
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    @Before
    fun setUp() {
        dataSource = FakeDesignProjectDataSource()
        repository = DesignProjectRepositoryImpl(dataSource)
    }

    @Test
    fun createProject_generatesProjectCreatedAuditEvent() = runBlocking {
        val result = repository.createDesignProject(
            job = sampleJob,
            createdBy = "Admin",
            timestamp = "2026-08-16T10:00:00Z"
        )
        assertTrue(result is DomainResult.Success)
        val project = (result as DomainResult.Success).data

        val events = repository.getActivityEventsForProject(project.projectId).first()
        assertEquals(1, events.size)
        val event = events.first()
        assertEquals(DesignActivityType.PROJECT_CREATED, event.eventType)
        assertEquals("Admin", event.createdBy)
        assertTrue(event.message?.contains("created for Job") == true)
    }

    @Test
    fun assignmentFlow_recordsComprehensiveAuditTrail() = runBlocking {
        // 1. Create
        val createResult = repository.createDesignProject(
            job = sampleJob,
            createdBy = "Admin",
            timestamp = "2026-08-16T10:00:00Z"
        )
        val project = (createResult as DomainResult.Success).data

        // 2. Assign
        repository.assignDesigner(
            projectId = project.projectId,
            designerId = "des-01",
            designerName = "তানভীর",
            assignedBy = "Manager",
            timestamp = "2026-08-16T10:15:00Z"
        )

        // 3. Start Design
        repository.startDesign(
            projectId = project.projectId,
            actorId = "des-01",
            actorName = "তানভীর",
            notes = "Vector layout in progress",
            timestamp = "2026-08-16T10:30:00Z"
        )

        // 4. Cancel
        repository.cancelDesignProject(
            projectId = project.projectId,
            reason = "Customer cancelled",
            cancelledBy = "Admin",
            timestamp = "2026-08-16T11:00:00Z"
        )

        val events = repository.getActivityEventsForProject(project.projectId).first()
        assertEquals(4, events.size)

        val eventTypes = events.map { it.eventType }
        assertTrue(eventTypes.contains(DesignActivityType.PROJECT_CREATED))
        assertTrue(eventTypes.contains(DesignActivityType.DESIGNER_ASSIGNED))
        assertTrue(eventTypes.contains(DesignActivityType.PROJECT_STARTED))
        assertTrue(eventTypes.contains(DesignActivityType.PROJECT_CANCELLED))
    }
}

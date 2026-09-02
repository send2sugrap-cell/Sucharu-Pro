package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeDesignProjectDataSource
import com.sucharu.sucharupro.data.repository.DesignProjectRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.design.DesignStatus
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit and integration tests for Design Project Creation (Module 05 Step 01).
 */
class DesignProjectCreationTest {

    private lateinit var dataSource: FakeDesignProjectDataSource
    private lateinit var repository: DesignProjectRepository

    private val sampleJob = ProductionJob(
        jobId = "job-2026-001",
        jobNumber = "JOB-2026-0001",
        orderId = "ord-2026-001",
        orderNumber = "ORD-2026-0001",
        customerId = "cus-2026-001",
        handoffId = "hnd-2026-001",
        title = "বই প্রিন্টিং ও বাঁধাই",
        priority = OrderPriority.URGENT,
        status = ProductionJobStatus.READY_FOR_PRODUCTION,
        quantity = 5000,
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    @Before
    fun setUp() {
        dataSource = FakeDesignProjectDataSource()
        repository = DesignProjectRepositoryImpl(dataSource)
    }

    @Test
    fun createDesignProject_validProductionJob_createsProjectSuccessfully() = runBlocking {
        val result = repository.createDesignProject(
            job = sampleJob,
            title = "বাংলা ব্যাকরণ কভার ডিজাইন",
            notes = "৪ কালার ডিজাইন প্রয়োজন",
            createdBy = "Manager",
            timestamp = "2026-08-16T10:30:00Z"
        )

        assertTrue("Expected Success result", result is DomainResult.Success)
        val project = (result as DomainResult.Success).data
        assertNotNull(project.projectId)
        assertEquals("DES-2026-0001", project.projectNumber)
        assertEquals("job-2026-001", project.productionJobId)
        assertEquals("ord-2026-001", project.orderId)
        assertEquals("cus-2026-001", project.customerId)
        assertEquals(DesignStatus.NOT_STARTED, project.status)
        assertEquals("বাংলা ব্যাকরণ কভার ডিজাইন", project.title)
        assertEquals(null, project.assignedDesignerId)

        val observed = repository.observeDesignProjects().first()
        assertEquals(1, observed.size)
        assertEquals(project.projectId, observed.first().projectId)
    }

    @Test
    fun createDesignProject_terminalJob_failsValidation() = runBlocking {
        val cancelledJob = sampleJob.copy(status = ProductionJobStatus.CANCELLED)
        val result = repository.createDesignProject(
            job = cancelledJob,
            timestamp = "2026-08-16T10:30:00Z"
        )

        assertTrue("Expected Error result for cancelled job", result is DomainResult.Error)
        val error = result as DomainResult.Error
        assertTrue(error.message.contains("Cannot create Design Project for a Cancelled production job"))
    }

    @Test
    fun createDesignProject_duplicateActiveProject_failsValidation() = runBlocking {
        val firstResult = repository.createDesignProject(
            job = sampleJob,
            timestamp = "2026-08-16T10:30:00Z"
        )
        assertTrue(firstResult is DomainResult.Success)

        val secondResult = repository.createDesignProject(
            job = sampleJob,
            timestamp = "2026-08-16T10:35:00Z"
        )
        assertTrue("Duplicate creation should fail", secondResult is DomainResult.Error)
        val error = secondResult as DomainResult.Error
        assertTrue(error.message.contains("Active Design Project 'DES-2026-0001' already exists"))
    }
}

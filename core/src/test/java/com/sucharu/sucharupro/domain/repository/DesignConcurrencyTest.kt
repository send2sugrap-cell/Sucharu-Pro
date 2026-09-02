package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeDesignProjectDataSource
import com.sucharu.sucharupro.data.repository.DesignProjectRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.design.DesignProject
import com.sucharu.sucharupro.domain.model.design.DesignStatus
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Concurrency and race condition safety tests for Design Domain Foundation (Module 05 Step 01).
 */
class DesignConcurrencyTest {

    private lateinit var dataSource: FakeDesignProjectDataSource
    private lateinit var repository: DesignProjectRepository

    @Before
    fun setUp() {
        dataSource = FakeDesignProjectDataSource()
        repository = DesignProjectRepositoryImpl(dataSource)
    }

    @Test
    fun concurrentProjectCreation_forDifferentJobs_isThreadSafe() = runBlocking {
        val jobs = (1..20).map { i ->
            ProductionJob(
                jobId = "job-conc-$i",
                jobNumber = "JOB-CONC-$i",
                orderId = "ord-conc-$i",
                orderNumber = "ORD-CONC-$i",
                customerId = "cus-conc-$i",
                handoffId = "hnd-conc-$i",
                title = "Job $i",
                priority = OrderPriority.NORMAL,
                status = ProductionJobStatus.READY_FOR_PRODUCTION,
                quantity = 1000,
                createdAt = "2026-08-16T10:00:00Z",
                updatedAt = "2026-08-16T10:00:00Z"
            )
        }

        val deferreds = jobs.map { job ->
            async {
                repository.createDesignProject(
                    job = job,
                    title = "Design for ${job.jobNumber}",
                    timestamp = "2026-08-16T10:00:00Z"
                )
            }
        }

        val results = deferreds.awaitAll()
        assertTrue(results.all { it is DomainResult.Success })

        val persisted = repository.observeDesignProjects().first()
        assertEquals(20, persisted.size)
    }

    @Test
    fun concurrentDuplicateCreation_sameJob_onlyOneSucceeds() = runBlocking {
        val singleJob = ProductionJob(
            jobId = "job-single",
            jobNumber = "JOB-SINGLE",
            orderId = "ord-single",
            orderNumber = "ORD-SINGLE",
            customerId = "cus-single",
            handoffId = "hnd-single",
            title = "Single Job",
            priority = OrderPriority.NORMAL,
            status = ProductionJobStatus.READY_FOR_PRODUCTION,
            quantity = 1000,
            createdAt = "2026-08-16T10:00:00Z",
            updatedAt = "2026-08-16T10:00:00Z"
        )

        val deferreds = (1..10).map {
            async {
                repository.createDesignProject(
                    job = singleJob,
                    timestamp = "2026-08-16T10:00:00Z"
                )
            }
        }

        val results = deferreds.awaitAll()
        val successCount = results.count { it is DomainResult.Success }
        val errorCount = results.count { it is DomainResult.Error }

        assertEquals(1, successCount)
        assertEquals(9, errorCount)

        val persisted = repository.observeDesignProjects().first()
        assertEquals(1, persisted.size)
    }

    @Test
    fun concurrentDesignerAssignment_preservesAtomicity() = runBlocking {
        val initialProject = DesignProject(
            projectId = "des-atomic",
            projectNumber = "DES-2026-9999",
            productionJobId = "job-atomic",
            orderId = "ord-atomic",
            orderNumber = "ORD-ATOMIC",
            customerId = "cus-atomic",
            title = "Atomic Design",
            status = DesignStatus.NOT_STARTED,
            createdAt = "2026-08-16T10:00:00Z",
            updatedAt = "2026-08-16T10:00:00Z"
        )
        dataSource.insertProject(initialProject)

        val designers = listOf(
            Triple("des-01", "তানভীর", "2026-08-16T10:01:00Z"),
            Triple("des-02", "নুসরাত", "2026-08-16T10:02:00Z"),
            Triple("des-03", "আরিফ", "2026-08-16T10:03:00Z")
        )

        val deferreds = designers.map { d ->
            async {
                repository.assignDesigner(
                    projectId = "des-atomic",
                    designerId = d.first,
                    designerName = d.second,
                    timestamp = d.third
                )
            }
        }

        val results = deferreds.awaitAll()
        assertTrue(results.any { it is DomainResult.Success })

        val finalProject = repository.findDesignProjectById("des-atomic")
        assertTrue(finalProject is DomainResult.Success)
        val project = (finalProject as DomainResult.Success).data
        assertTrue(project.isAssigned)
    }
}

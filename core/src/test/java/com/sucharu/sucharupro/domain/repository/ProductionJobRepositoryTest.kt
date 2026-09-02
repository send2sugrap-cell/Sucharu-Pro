package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionJobDataSource
import com.sucharu.sucharupro.data.repository.ProductionJobRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobItem
import com.sucharu.sucharupro.domain.model.job.ProductionJobStage
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.production.ProductionStageStatus
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit test suite for [ProductionJobRepository] and [FakeProductionJobDataSource].
 */
class ProductionJobRepositoryTest {

    private lateinit var dataSource: FakeProductionJobDataSource
    private lateinit var repository: ProductionJobRepository

    private val sampleItem = ProductionJobItem(
        itemId = "item-01",
        description = "ব্যানার প্রিন্টিং",
        specification = "১০x৩ ফিট",
        quantity = 5,
        unit = "Pcs"
    )

    private val sampleJob = ProductionJob(
        jobId = "job-001",
        jobNumber = "JOB-2026-0001",
        orderId = "ord-001",
        orderNumber = "ORD-2026-0001",
        customerId = "cus-001",
        handoffId = "hnd-001",
        title = "ব্যানার প্রিন্টিং",
        quantity = 5,
        unit = "Pcs",
        priority = OrderPriority.URGENT,
        status = ProductionJobStatus.READY_FOR_PRODUCTION,
        items = listOf(sampleItem),
        stages = ProductionJobStage.createInitialStages("job-001"),
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    @Before
    fun setUp() {
        dataSource = FakeProductionJobDataSource()
        repository = ProductionJobRepositoryImpl(dataSource)
    }

    @Test
    fun createJob_succeedsForValidData() = runBlocking {
        val result = repository.createJob(sampleJob)
        assertTrue(result is DomainResult.Success<*>)
        val created = (result as DomainResult.Success<ProductionJob>).data
        assertEquals("job-001", created.jobId)
        assertEquals("JOB-2026-0001", created.jobNumber)
        assertEquals("ORD-2026-0001", created.orderNumber)
        assertEquals(OrderPriority.URGENT, created.priority)
        assertEquals(5, created.quantity)
        assertEquals(13, created.stages.size)
    }

    @Test
    fun duplicateJobId_rejected() = runBlocking {
        repository.createJob(sampleJob)

        val duplicateIdJob = sampleJob.copy(
            jobNumber = "JOB-2026-0002",
            handoffId = "hnd-002"
        )
        val result = repository.createJob(duplicateIdJob)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("already exists"))
    }

    @Test
    fun duplicateHandoffId_rejected() = runBlocking {
        repository.createJob(sampleJob)

        val duplicateHandoffJob = sampleJob.copy(
            jobId = "job-002",
            jobNumber = "JOB-2026-0002"
        )
        val result = repository.createJob(duplicateHandoffJob)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("already exists"))
    }

    @Test
    fun invalidJobCreation_doesNotMutateState() = runBlocking {
        val invalidJob = sampleJob.copy(title = "")
        val result = repository.createJob(invalidJob)
        assertTrue(result is DomainResult.Error)

        val list = repository.observeJobs().first()
        assertTrue(list.isEmpty())
    }

    @Test
    fun getJobById_andObservables_emitCorrectData() = runBlocking {
        repository.createJob(sampleJob)

        val fetched = repository.getJobById("job-001").first()
        assertNotNull(fetched)
        assertEquals("job-001", fetched?.jobId)

        val forOrder = repository.getJobsForOrder("ord-001").first()
        assertEquals(1, forOrder.size)

        val forHandoff = repository.getJobForHandoff("hnd-001").first()
        assertNotNull(forHandoff)
        assertEquals("job-001", forHandoff?.jobId)
    }

    @Test
    fun updateJob_updatesStateCorrectly() = runBlocking {
        repository.createJob(sampleJob)

        val updatedStages = sampleJob.stages.map {
            if (it.stageType == ProductionStageType.DESIGN) {
                it.copy(status = ProductionStageStatus.COMPLETED)
            } else {
                it
            }
        }
        val updatedJob = sampleJob.copy(
            status = ProductionJobStatus.IN_PROGRESS,
            stages = updatedStages,
            updatedAt = "2026-08-16T11:00:00Z"
        )

        val updateResult = repository.updateJob(updatedJob)
        assertTrue(updateResult is DomainResult.Success<*>)
        val saved = (updateResult as DomainResult.Success<ProductionJob>).data
        assertEquals(ProductionJobStatus.IN_PROGRESS, saved.status)
        assertEquals(1, saved.completedStagesCount)
        assertEquals(1f / 13f, saved.progressFraction, 0.001f)
    }
}

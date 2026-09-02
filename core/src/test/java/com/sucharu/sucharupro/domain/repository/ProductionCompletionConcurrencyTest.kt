package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionJobDataSource
import com.sucharu.sucharupro.data.repository.ProductionJobRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.job.ProductionActivityType
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobItem
import com.sucharu.sucharupro.domain.model.job.ProductionJobStage
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.production.ProductionStageStatus
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class ProductionCompletionConcurrencyTest {

    private lateinit var dataSource: FakeProductionJobDataSource
    private lateinit var repository: ProductionJobRepository

    private val sampleJob = ProductionJob(
        jobId = "job-conc-comp-01",
        jobNumber = "JOB-2026-CONCCOMP01",
        orderId = "ord-01",
        orderNumber = "ORD-2026-0001",
        handoffId = "hnd-conc-comp-01",
        customerId = "cust-01",
        title = "কনকারেন্ট সমাপ্তি টেস্ট জব",
        quantity = 1000,
        unit = "কপি",
        priority = OrderPriority.URGENT,
        status = ProductionJobStatus.IN_PROGRESS,
        items = listOf(
            ProductionJobItem(
                itemId = "item-01",
                description = "কনকারেন্ট সমাপ্তি টেস্ট জব",
                quantity = 1000,
                unit = "কপি"
            )
        ),
        stages = ProductionJobStage.createInitialStages("job-conc-comp-01"),
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    @Before
    fun setUp() {
        dataSource = FakeProductionJobDataSource()
        repository = ProductionJobRepositoryImpl(dataSource)
    }

    private suspend fun setupJobForCompletion(job: ProductionJob) {
        val completedStages = job.stages.map { stage ->
            if (stage.sequence < ProductionStageType.READY.displayOrder) {
                stage.copy(status = ProductionStageStatus.COMPLETED)
            } else {
                stage
            }
        }
        val preparedJob = job.copy(stages = completedStages)
        repository.createJob(preparedJob)

        dataSource.insertOutput(
            com.sucharu.sucharupro.domain.model.job.ProductionStageOutput(
                outputId = "out-conc-setup-${job.jobId}",
                jobId = job.jobId,
                stageId = job.stages[0].stageId,
                stageType = job.stages[0].stageType,
                quantity = job.quantity,
                unit = job.unit,
                recordedAt = "2026-08-16T10:30:00Z"
            )
        )
    }

    @Test
    fun concurrentCompletionConfirmation_exactlyOneSucceeds() = runBlocking {
        setupJobForCompletion(sampleJob)

        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)

        // 10 concurrent requests to confirm completion
        val coroutines = List(10) { idx ->
            launch(Dispatchers.Default) {
                val result = repository.confirmProductionCompletion(
                    jobId = sampleJob.jobId,
                    actorId = "supervisor-$idx",
                    actorName = "Supervisor $idx",
                    remarks = "Completion confirmation $idx",
                    timestamp = "2026-08-16T11:00:0${idx}Z"
                )
                if (result is DomainResult.Success) {
                    successCount.incrementAndGet()
                } else {
                    failureCount.incrementAndGet()
                }
            }
        }
        coroutines.joinAll()

        assertEquals("Exactly 1 request must succeed", 1, successCount.get())
        assertEquals("Remaining 9 requests must fail", 9, failureCount.get())

        // Verify exactly 1 JOB_READY activity event
        val activities = repository.getProductionActivityEvents(sampleJob.jobId).first()
        val readyEvents = activities.filter { it.eventType == ProductionActivityType.JOB_READY }
        assertEquals(1, readyEvents.size)

        // Verify status is READY
        val job = repository.getJobById(sampleJob.jobId).first()
        assertEquals(ProductionJobStatus.READY, job?.status)
    }
}

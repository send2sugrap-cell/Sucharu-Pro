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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProductionCompletionRepositoryTest {

    private lateinit var dataSource: FakeProductionJobDataSource
    private lateinit var repository: ProductionJobRepository

    private val sampleJob = ProductionJob(
        jobId = "job-comp-repo-01",
        jobNumber = "JOB-2026-COMP01",
        orderId = "ord-01",
        orderNumber = "ORD-2026-0001",
        handoffId = "hnd-comp-01",
        customerId = "cust-01",
        title = "বাংলা ব্যাকরণ ও নির্মিতি বই",
        quantity = 500,
        unit = "কপি",
        priority = OrderPriority.NORMAL,
        status = ProductionJobStatus.IN_PROGRESS,
        items = listOf(
            ProductionJobItem(
                itemId = "item-01",
                description = "বাংলা ব্যাকরণ ও নির্মিতি বই",
                quantity = 500,
                unit = "কপি"
            )
        ),
        stages = ProductionJobStage.createInitialStages("job-comp-repo-01"),
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    @Before
    fun setUp() {
        dataSource = FakeProductionJobDataSource()
        repository = ProductionJobRepositoryImpl(dataSource)
    }

    private suspend fun setupJobForCompletion(job: ProductionJob) {
        // Complete stages 1..11 and record full output
        val completedStages = job.stages.map { stage ->
            if (stage.sequence < ProductionStageType.READY.displayOrder) {
                stage.copy(status = ProductionStageStatus.COMPLETED)
            } else {
                stage
            }
        }
        val preparedJob = job.copy(stages = completedStages)
        repository.createJob(preparedJob)

        // Record output directly into dataSource
        dataSource.insertOutput(
            com.sucharu.sucharupro.domain.model.job.ProductionStageOutput(
                outputId = "out-setup-${job.jobId}",
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
    fun confirmProductionCompletion_updatesStatusToReadyAndEmitsEvent() = runBlocking {
        setupJobForCompletion(sampleJob)

        val result = repository.confirmProductionCompletion(
            jobId = sampleJob.jobId,
            actorId = "supervisor-01",
            actorName = "Akhtaruzzaman",
            remarks = "৫০০ কপি সম্পূর্ণ প্রস্তুত",
            timestamp = "2026-08-16T11:00:00Z"
        )

        assertTrue(result is DomainResult.Success)
        val completedJob = (result as DomainResult.Success).data
        assertEquals(ProductionJobStatus.READY, completedJob.status)
        assertEquals("2026-08-16T11:00:00Z", completedJob.updatedAt)

        // Verify READY stage is marked COMPLETED
        val readyStage = completedJob.stages.find { it.stageType == ProductionStageType.READY }
        assertNotNull(readyStage)
        assertEquals(ProductionStageStatus.COMPLETED, readyStage?.status)

        // Verify activity event generated
        val activities = repository.getProductionActivityEvents(sampleJob.jobId).first()
        val readyEvent = activities.find { it.eventType == ProductionActivityType.JOB_READY }
        assertNotNull(readyEvent)
        assertEquals("supervisor-01", readyEvent?.operatorId)
        assertEquals("Akhtaruzzaman", readyEvent?.operatorName)
        assertTrue(readyEvent?.message?.contains("৫০০ কপি সম্পূর্ণ প্রস্তুত") == true)
    }

    @Test
    fun failedCompletion_causesZeroMutation() = runBlocking {
        // Job with PENDING stages
        repository.createJob(sampleJob)

        val result = repository.confirmProductionCompletion(
            jobId = sampleJob.jobId,
            actorId = "supervisor-01",
            actorName = "Akhtaruzzaman",
            timestamp = "2026-08-16T11:00:00Z"
        )

        assertTrue(result is DomainResult.Error)

        // Verify job remains in IN_PROGRESS
        val job = repository.getJobById(sampleJob.jobId).first()
        assertNotNull(job)
        assertEquals(ProductionJobStatus.IN_PROGRESS, job?.status)

        // Verify zero JOB_READY activity events
        val activities = repository.getProductionActivityEvents(sampleJob.jobId).first()
        val readyEvent = activities.find { it.eventType == ProductionActivityType.JOB_READY }
        assertEquals(null, readyEvent)
    }

    @Test
    fun getProductionReadyHandoff_returnsImmutableSnapshot() = runBlocking {
        setupJobForCompletion(sampleJob)
        repository.confirmProductionCompletion(
            jobId = sampleJob.jobId,
            actorId = "supervisor-01",
            actorName = "Akhtaruzzaman",
            remarks = "উৎপাদন সম্পন্ন",
            timestamp = "2026-08-16T11:00:00Z"
        )

        val handoffResult = repository.getProductionReadyHandoff(sampleJob.jobId).first()
        assertTrue(handoffResult is DomainResult.Success)
        val handoff = (handoffResult as DomainResult.Success).data

        assertEquals(sampleJob.jobId, handoff.productionJobId)
        assertEquals(sampleJob.jobNumber, handoff.jobNumber)
        assertEquals(500, handoff.plannedQuantity)
        assertEquals(500, handoff.recordedQuantity)
        assertEquals(0, handoff.remainingQuantity)
        assertEquals("Akhtaruzzaman", handoff.confirmedByName)
        assertEquals(ProductionJobStatus.READY, handoff.productionStatus)
    }
}

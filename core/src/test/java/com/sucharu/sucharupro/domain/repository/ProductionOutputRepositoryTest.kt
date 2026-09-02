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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProductionOutputRepositoryTest {

    private lateinit var dataSource: FakeProductionJobDataSource
    private lateinit var repository: ProductionJobRepository

    private val sampleJob = ProductionJob(
        jobId = "job-out-repo-01",
        jobNumber = "JOB-2026-OUT01",
        orderId = "ord-01",
        orderNumber = "ORD-2026-0001",
        handoffId = "hnd-out-01",
        customerId = "cust-01",
        title = "পুস্তিকা মুদ্রণ",
        quantity = 500,
        unit = "কপি",
        priority = OrderPriority.NORMAL,
        status = ProductionJobStatus.READY_FOR_PRODUCTION,
        items = listOf(
            ProductionJobItem(
                itemId = "item-01",
                description = "পুস্তিকা মুদ্রণ",
                quantity = 500,
                unit = "কপি"
            )
        ),
        stages = ProductionJobStage.createInitialStages("job-out-repo-01"),
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    private val stage1Id = sampleJob.stages[0].stageId

    @Before
    fun setUp() {
        dataSource = FakeProductionJobDataSource()
        repository = ProductionJobRepositoryImpl(dataSource)
    }

    @Test
    fun recordOutput_createsRecordAndGeneratesActivityEvent() = runBlocking {
        repository.createJob(sampleJob)
        repository.startStage(sampleJob.jobId, stage1Id, timestamp = "2026-08-16T10:05:00Z")

        val outputResult = repository.recordStageOutput(
            jobId = sampleJob.jobId,
            stageId = stage1Id,
            quantity = 250,
            unit = "কপি",
            remarks = "প্রথম লট সম্পন্ন",
            timestamp = "2026-08-16T10:20:00Z"
        )

        assertTrue(outputResult is DomainResult.Success)
        val output = (outputResult as DomainResult.Success).data
        assertEquals(250, output.quantity)
        assertEquals("প্রথম লট সম্পন্ন", output.remarks)

        // Verify output is in stream
        val outputs = repository.getStageOutputs(sampleJob.jobId, stage1Id).first()
        assertEquals(1, outputs.size)
        assertEquals(250, outputs[0].quantity)

        // Verify activity event generated
        val activities = repository.getProductionActivityEvents(sampleJob.jobId).first()
        val outputActivity = activities.find { it.eventType == ProductionActivityType.STAGE_OUTPUT_RECORDED }
        assertTrue(outputActivity != null)
    }

    @Test
    fun failedOutputRecording_createsNoRecordAndNoActivity() = runBlocking {
        repository.createJob(sampleJob)
        // Note: Stage is in PENDING status, output record must fail

        val outputResult = repository.recordStageOutput(
            jobId = sampleJob.jobId,
            stageId = stage1Id,
            quantity = 250,
            unit = "কপি",
            timestamp = "2026-08-16T10:20:00Z"
        )

        assertTrue(outputResult is DomainResult.Error)

        // Verify no outputs
        val outputs = repository.getStageOutputs(sampleJob.jobId, stage1Id).first()
        assertTrue(outputs.isEmpty())

        // Verify no output activity event
        val activities = repository.getProductionActivityEvents(sampleJob.jobId).first()
        val outputActivity = activities.find { it.eventType == ProductionActivityType.STAGE_OUTPUT_RECORDED }
        assertTrue(outputActivity == null)
    }

    @Test
    fun observeProductionOutputReconciliation_emitsReactiveUpdates() = runBlocking {
        repository.createJob(sampleJob)
        repository.startStage(sampleJob.jobId, stage1Id, timestamp = "2026-08-16T10:05:00Z")

        val initialReconciliation = repository.observeProductionOutputReconciliation(sampleJob.jobId).first()
        assertTrue(initialReconciliation is DomainResult.Success)
        val r1 = (initialReconciliation as DomainResult.Success).data
        assertEquals(500, r1.remainingQuantity)
        assertEquals(0, r1.recordedQuantity)

        repository.recordStageOutput(
            jobId = sampleJob.jobId,
            stageId = stage1Id,
            quantity = 300,
            unit = "কপি",
            timestamp = "2026-08-16T10:20:00Z"
        )

        val updatedReconciliation = repository.observeProductionOutputReconciliation(sampleJob.jobId).first()
        assertTrue(updatedReconciliation is DomainResult.Success)
        val r2 = (updatedReconciliation as DomainResult.Success).data
        assertEquals(200, r2.remainingQuantity)
        assertEquals(300, r2.recordedQuantity)
        assertEquals(60.0, r2.completionPercentage, 0.001)
    }
}

package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionJobDataSource
import com.sucharu.sucharupro.data.repository.ProductionJobRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.job.ProductionActivityType
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStage
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Repository and data layer unit tests for Production Stage Output (Module 04 Step 06).
 */
class ProductionStageOutputRepositoryTest {

    private lateinit var dataSource: FakeProductionJobDataSource
    private lateinit var repository: ProductionJobRepository

    private val sampleJob = ProductionJob(
        jobId = "job-out-repo-01",
        jobNumber = "JOB-2026-OUT01",
        orderId = "ord-001",
        orderNumber = "ORD-2026-0001",
        customerId = "cus-001",
        handoffId = "hnd-001",
        title = "পুস্তিকা মুদ্রণ ও ল্যামিনেশন",
        quantity = 2000,
        unit = "কপি",
        priority = OrderPriority.NORMAL,
        status = ProductionJobStatus.READY_FOR_PRODUCTION,
        stages = ProductionJobStage.createInitialStages("job-out-repo-01"),
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    private val stage1Id = sampleJob.stages[0].stageId

    @Before
    fun setUp() {
        runBlocking {
            dataSource = FakeProductionJobDataSource()
            repository = ProductionJobRepositoryImpl(dataSource)
            repository.createJob(sampleJob)
            repository.startStage(sampleJob.jobId, stage1Id, timestamp = "2026-08-16T10:00:00Z")
        }
    }

    @Test
    fun recordStageOutput_persistsAndReturnsOutput() = runBlocking {
        val result = repository.recordStageOutput(
            jobId = sampleJob.jobId,
            stageId = stage1Id,
            quantity = 500,
            unit = "কপি",
            operatorId = "op-01",
            operatorName = "Rahim",
            remarks = "১ম ব্যাচ",
            timestamp = "2026-08-16T10:30:00Z"
        )

        assertTrue(result is DomainResult.Success)
        val output = (result as DomainResult.Success).data
        assertEquals(500, output.quantity)
        assertEquals("কপি", output.unit)
        assertEquals("১ম ব্যাচ", output.remarks)
        assertEquals("Rahim", output.operatorName)
    }

    @Test
    fun retrieveStageOutputs_filtersAndSortsNewestFirst() = runBlocking {
        repository.recordStageOutput(
            jobId = sampleJob.jobId,
            stageId = stage1Id,
            quantity = 250,
            unit = "কপি",
            timestamp = "2026-08-16T10:15:00Z"
        )
        repository.recordStageOutput(
            jobId = sampleJob.jobId,
            stageId = stage1Id,
            quantity = 350,
            unit = "কপি",
            timestamp = "2026-08-16T10:45:00Z"
        )

        val outputs = repository.getStageOutputs(sampleJob.jobId, stage1Id).first()
        assertEquals(2, outputs.size)
        assertEquals(350, outputs[0].quantity) // Newest first
        assertEquals(250, outputs[1].quantity)
    }

    @Test
    fun retrieveJobOutputs_returnsAllStageOutputs() = runBlocking {
        repository.recordStageOutput(
            jobId = sampleJob.jobId,
            stageId = stage1Id,
            quantity = 400,
            unit = "কপি",
            timestamp = "2026-08-16T10:15:00Z"
        )

        val allJobOutputs = repository.getStageOutputsForJob(sampleJob.jobId).first()
        assertEquals(1, allJobOutputs.size)
        assertEquals(400, allJobOutputs[0].quantity)
    }

    @Test
    fun accumulatedQuantity_sumsCorrectly() = runBlocking {
        repository.recordStageOutput(
            jobId = sampleJob.jobId,
            stageId = stage1Id,
            quantity = 300,
            unit = "কপি",
            timestamp = "2026-08-16T10:10:00Z"
        )
        repository.recordStageOutput(
            jobId = sampleJob.jobId,
            stageId = stage1Id,
            quantity = 400,
            unit = "কপি",
            timestamp = "2026-08-16T10:20:00Z"
        )

        val total = repository.getTotalStageOutput(sampleJob.jobId, stage1Id).first()
        assertEquals(700, total)
    }

    @Test
    fun remainingQuantity_decreasesAccurately() = runBlocking {
        repository.recordStageOutput(
            jobId = sampleJob.jobId,
            stageId = stage1Id,
            quantity = 600,
            unit = "কপি",
            timestamp = "2026-08-16T10:10:00Z"
        )

        val remaining = repository.getRemainingStageQuantity(sampleJob.jobId, stage1Id).first()
        assertEquals(1400, remaining) // 2000 - 600 = 1400
    }

    @Test
    fun reactiveStateFlow_updatesOnNewOutput() = runBlocking {
        val initialOutputs = repository.observeStageOutputs().first()
        assertEquals(0, initialOutputs.size)

        repository.recordStageOutput(
            jobId = sampleJob.jobId,
            stageId = stage1Id,
            quantity = 200,
            unit = "কপি",
            timestamp = "2026-08-16T10:10:00Z"
        )

        val updatedOutputs = repository.observeStageOutputs().first()
        assertEquals(1, updatedOutputs.size)
    }

    @Test
    fun activityEvent_createdOnSuccessfulOutput() = runBlocking {
        repository.recordStageOutput(
            jobId = sampleJob.jobId,
            stageId = stage1Id,
            quantity = 500,
            unit = "কপি",
            remarks = "প্রথম লট",
            timestamp = "2026-08-16T10:30:00Z"
        )

        val activities = repository.getProductionActivityEvents(sampleJob.jobId).first()
        val outputEvent = activities.find { it.eventType == ProductionActivityType.STAGE_OUTPUT_RECORDED }
        assertNotNull(outputEvent)
        assertTrue(outputEvent?.message?.contains("500 কপি") == true)
        assertTrue(outputEvent?.message?.contains("প্রথম লট") == true)
    }

    @Test
    fun failedOperation_producesZeroMutationsAndZeroActivities() = runBlocking {
        // Attempt recording 2500 when planned is 2000
        val result = repository.recordStageOutput(
            jobId = sampleJob.jobId,
            stageId = stage1Id,
            quantity = 2500,
            unit = "কপি",
            timestamp = "2026-08-16T10:30:00Z"
        )
        assertTrue(result is DomainResult.Error)

        val outputs = repository.getStageOutputs(sampleJob.jobId, stage1Id).first()
        assertEquals(0, outputs.size)

        val activities = repository.getProductionActivityEvents(sampleJob.jobId).first()
        val outputActivities = activities.filter { it.eventType == ProductionActivityType.STAGE_OUTPUT_RECORDED }
        assertEquals(0, outputActivities.size)
    }
}

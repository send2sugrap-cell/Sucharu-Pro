package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionJobDataSource
import com.sucharu.sucharupro.data.repository.ProductionJobRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.job.ProductionActivityType
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStage
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
 * Concurrency, race-condition safety, and commercial invariance tests for Production Stage Output (Module 04 Step 06).
 */
class ProductionStageOutputConcurrencyTest {

    private lateinit var dataSource: FakeProductionJobDataSource
    private lateinit var repository: ProductionJobRepository

    private val sampleJob = ProductionJob(
        jobId = "job-conc-out-01",
        jobNumber = "JOB-2026-CONC01",
        orderId = "ord-conc-01",
        orderNumber = "ORD-2026-CONC01",
        customerId = "cus-conc-01",
        handoffId = "hnd-conc-01",
        title = "বই মুদ্রণ ও ফিনিশিং",
        quantity = 1000,
        unit = "কপি",
        priority = OrderPriority.HIGH,
        status = ProductionJobStatus.READY_FOR_PRODUCTION,
        stages = ProductionJobStage.createInitialStages("job-conc-out-01"),
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
            // Record initial 900
            repository.recordStageOutput(
                jobId = sampleJob.jobId,
                stageId = stage1Id,
                quantity = 900,
                unit = "কপি",
                timestamp = "2026-08-16T10:05:00Z"
            )
        }
    }

    @Test
    fun concurrentOutputs_cannotExceedPlannedQuantity() = runBlocking {
        // Planned: 1000, Already produced: 900, Remaining: 100
        // 10 concurrent requests each attempting to record 50
        val results = (1..10).map { index ->
            async {
                repository.recordStageOutput(
                    jobId = sampleJob.jobId,
                    stageId = stage1Id,
                    quantity = 50,
                    unit = "কপি",
                    remarks = "Concurrent batch $index",
                    timestamp = "2026-08-16T10:10:00Z"
                )
            }
        }.awaitAll()

        val successCount = results.count { it is DomainResult.Success }
        val errorCount = results.count { it is DomainResult.Error }

        // Exactly 2 of 50 can succeed (900 + 50 + 50 = 1000)
        assertEquals(2, successCount)
        assertEquals(8, errorCount)

        val totalOutput = repository.getTotalStageOutput(sampleJob.jobId, stage1Id).first()
        assertEquals(1000, totalOutput)
    }

    @Test
    fun exactAccumulatedQuantity_matchesTotalAllowed() = runBlocking {
        val total = repository.getTotalStageOutput(sampleJob.jobId, stage1Id).first()
        val remaining = repository.getRemainingStageQuantity(sampleJob.jobId, stage1Id).first()

        assertEquals(900, total)
        assertEquals(100, remaining)
    }

    @Test
    fun rejectedConcurrentAttempts_produceZeroActivityEvents() = runBlocking {
        // Attempt 5 requests of 200 when remaining is 100 -> all 5 must fail
        val results = (1..5).map { index ->
            async {
                repository.recordStageOutput(
                    jobId = sampleJob.jobId,
                    stageId = stage1Id,
                    quantity = 200,
                    unit = "কপি",
                    timestamp = "2026-08-16T10:15:00Z"
                )
            }
        }.awaitAll()

        assertEquals(0, results.count { it is DomainResult.Success })
        assertEquals(5, results.count { it is DomainResult.Error })

        val activities = repository.getProductionActivityEvents(sampleJob.jobId).first()
        // Only 1 initial output activity exists from setUp (the 900 output), none of the 5 failed attempts created an event
        val outputActivities = activities.filter { it.eventType == ProductionActivityType.STAGE_OUTPUT_RECORDED }
        assertEquals(1, outputActivities.size)
    }

    @Test
    fun snapshotInvariance_commercialEntitiesUntouched() = runBlocking {
        repository.recordStageOutput(
            jobId = sampleJob.jobId,
            stageId = stage1Id,
            quantity = 100,
            unit = "কপি",
            timestamp = "2026-08-16T10:20:00Z"
        )

        val job = (repository.findJobById(sampleJob.jobId) as DomainResult.Success).data
        assertEquals(1000, job.quantity)
        assertEquals("কপি", job.unit)
        assertEquals("ord-conc-01", job.orderId)
        assertEquals("hnd-conc-01", job.handoffId)
        assertEquals("JOB-2026-CONC01", job.jobNumber)
    }
}

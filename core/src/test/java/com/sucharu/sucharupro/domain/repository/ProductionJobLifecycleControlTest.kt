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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Master end-to-end integration and concurrency test suite for Production Job Lifecycle & Stage Control.
 */
class ProductionJobLifecycleControlTest {

    private lateinit var dataSource: FakeProductionJobDataSource
    private lateinit var repository: ProductionJobRepository

    private val sampleItem = ProductionJobItem(
        itemId = "item-01",
        description = "বাংলা ব্যাকরণ বই",
        specification = "চার কালার প্রচ্ছদ, ৮০ জিএসএম কাগজ",
        quantity = 2000,
        unit = "কপি"
    )

    private val sampleJob = ProductionJob(
        jobId = "job-e2e-01",
        jobNumber = "JOB-2026-E2E01",
        orderId = "ord-e2e-01",
        orderNumber = "ORD-2026-E2E01",
        customerId = "cus-e2e-01",
        handoffId = "hnd-e2e-01",
        title = "বাংলা ব্যাকরণ বই মুদ্রণ",
        quantity = 2000,
        unit = "কপি",
        priority = OrderPriority.HIGH,
        status = ProductionJobStatus.READY_FOR_PRODUCTION,
        items = listOf(sampleItem),
        stages = ProductionJobStage.createInitialStages("job-e2e-01"),
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    @Before
    fun setUp() {
        dataSource = FakeProductionJobDataSource()
        repository = ProductionJobRepositoryImpl(dataSource)
    }

    @Test
    fun fullSequentialProgression_fromReadyForProductionToDelivered_succeeds() = runBlocking {
        repository.createJob(sampleJob)

        // 1. Initial State: READY_FOR_PRODUCTION
        var job = repository.findJobById("job-e2e-01").let { (it as DomainResult.Success).data }
        assertEquals(ProductionJobStatus.READY_FOR_PRODUCTION, job.status)
        assertEquals(0, job.completedStagesCount)

        // 2. Start Stage 1: DESIGN -> Job automatically becomes IN_PROGRESS
        val stage1Id = job.stages[0].stageId
        val startStage1 = repository.startStage(job.jobId, stage1Id, actorId = "user-1", actorName = "ডিজাইনার", timestamp = "2026-08-16T10:05:00Z")
        assertTrue(startStage1 is DomainResult.Success<*>)
        job = (startStage1 as DomainResult.Success).data
        assertEquals(ProductionJobStatus.IN_PROGRESS, job.status)
        assertEquals(ProductionStageStatus.IN_PROGRESS, job.stages[0].status)
        assertEquals("2026-08-16T10:05:00Z", job.stages[0].startedAt)
        assertEquals("ডিজাইনার", job.stages[0].assignedUserName)

        // Complete Stage 1: DESIGN
        val completeStage1 = repository.completeStage(job.jobId, stage1Id, notes = "ডিজাইন সম্পন্ন", timestamp = "2026-08-16T10:30:00Z")
        assertTrue(completeStage1 is DomainResult.Success<*>)
        job = (completeStage1 as DomainResult.Success).data
        assertEquals(1, job.completedStagesCount)
        assertEquals(ProductionStageStatus.COMPLETED, job.stages[0].status)

        // 3. Progress through stages 2 through 12
        // Sequence: APPROVAL(2), QC(3), ITEM_APPROVAL(4), CTP(5), PRINTING(6), LAMINATION(7), FOLDING(8), BINDING(9), FINAL_QC(10), PACKAGING(11), READY(12)
        for (i in 1..11) {
            val stage = job.stages[i]
            val startRes = repository.startStage(job.jobId, stage.stageId, timestamp = "2026-08-16T11:00:00Z")
            assertTrue(startRes is DomainResult.Success<*>)
            val completeRes = repository.completeStage(job.jobId, stage.stageId, timestamp = "2026-08-16T11:30:00Z")
            assertTrue(completeRes is DomainResult.Success<*>)
            job = (completeRes as DomainResult.Success).data
        }

        // After completing Stage 12 (READY), the Job Card automatically synchronizes to READY status!
        assertEquals(ProductionJobStatus.READY, job.status)
        assertEquals(12, job.completedStagesCount)
        assertEquals(12f / 13f, job.progressFraction, 0.001f)

        // 4. Complete Stage 13 (DELIVERED) -> Job reaches terminal DELIVERED status!
        val stage13Id = job.stages[12].stageId
        val startStage13 = repository.startStage(job.jobId, stage13Id, timestamp = "2026-08-16T12:00:00Z")
        assertTrue(startStage13 is DomainResult.Success<*>)
        val completeStage13 = repository.completeStage(job.jobId, stage13Id, timestamp = "2026-08-16T12:30:00Z")
        assertTrue(completeStage13 is DomainResult.Success<*>)
        job = (completeStage13 as DomainResult.Success).data

        assertEquals(ProductionJobStatus.DELIVERED, job.status)
        assertEquals(13, job.completedStagesCount)
        assertEquals(1.0f, job.progressFraction, 0.001f)
        assertTrue(job.status.isTerminal)
    }

    @Test
    fun stageSkipping_forSkippableStage_preservesWorkflowIntegrity() = runBlocking {
        repository.createJob(sampleJob)

        // Complete stages 1..4 (DESIGN, APPROVAL, QC, ITEM_APPROVAL)
        for (i in 0..3) {
            val stageId = sampleJob.stages[i].stageId
            repository.startStage(sampleJob.jobId, stageId, timestamp = "2026-08-16T10:00:00Z")
            repository.completeStage(sampleJob.jobId, stageId, timestamp = "2026-08-16T10:15:00Z")
        }

        // CTP stage (seq 5) can be skipped for digital-only jobs
        val ctpStageId = sampleJob.stages[4].stageId
        val skipResult = repository.skipStage(sampleJob.jobId, ctpStageId, notes = "ডিজিটাল প্রিন্ট, প্লেট প্রয়োজন নেই", timestamp = "2026-08-16T10:20:00Z")
        assertTrue(skipResult is DomainResult.Success<*>)
        val job = (skipResult as DomainResult.Success).data
        assertEquals(ProductionStageStatus.SKIPPED, job.stages[4].status)

        // Printing (seq 6) can now start because CTP was skipped
        val printingStageId = job.stages[5].stageId
        val startPrinting = repository.startStage(job.jobId, printingStageId, timestamp = "2026-08-16T10:25:00Z")
        assertTrue(startPrinting is DomainResult.Success<*>)
    }

    @Test
    fun holdAndResume_preservesStageProgressAndTimestamps() = runBlocking {
        repository.createJob(sampleJob)

        val stage1Id = sampleJob.stages[0].stageId
        repository.startStage(sampleJob.jobId, stage1Id, timestamp = "2026-08-16T10:00:00Z")

        // Put Job on Hold
        val holdResult = repository.holdJob(sampleJob.jobId, reason = "গ্রাহকের কাঁচামাল সরবরাহে বিলম্ব", timestamp = "2026-08-16T10:10:00Z")
        assertTrue(holdResult is DomainResult.Success<*>)
        var job = (holdResult as DomainResult.Success).data
        assertEquals(ProductionJobStatus.ON_HOLD, job.status)
        assertTrue(job.notes?.contains("গ্রাহকের কাঁচামাল সরবরাহে বিলম্ব") == true)
        // Stage 1 remains IN_PROGRESS, not lost or reset
        assertEquals(ProductionStageStatus.IN_PROGRESS, job.stages[0].status)
        assertEquals("2026-08-16T10:00:00Z", job.stages[0].startedAt)

        // While On Hold, starting another stage is rejected
        val stage2Id = sampleJob.stages[1].stageId
        val startWhileHold = repository.startStage(sampleJob.jobId, stage2Id, timestamp = "2026-08-16T10:15:00Z")
        assertTrue(startWhileHold is DomainResult.Error)

        // Resume Job
        val resumeResult = repository.resumeJob(sampleJob.jobId, timestamp = "2026-08-16T10:20:00Z")
        assertTrue(resumeResult is DomainResult.Success<*>)
        job = (resumeResult as DomainResult.Success).data
        assertEquals(ProductionJobStatus.IN_PROGRESS, job.status)
        assertEquals(ProductionStageStatus.IN_PROGRESS, job.stages[0].status)
    }

    @Test
    fun cancellation_withMandatoryReason_marksTerminalAndPreservesHistory() = runBlocking {
        repository.createJob(sampleJob)

        // Blank reason rejected
        val blankCancel = repository.cancelJob(sampleJob.jobId, "   ", timestamp = "2026-08-16T10:10:00Z")
        assertTrue(blankCancel is DomainResult.Error)

        // Valid cancellation succeeds
        val cancelResult = repository.cancelJob(sampleJob.jobId, "ক্লায়েন্ট অর্ডার বাতিল করেছে", timestamp = "2026-08-16T10:10:00Z")
        assertTrue(cancelResult is DomainResult.Success<*>)
        val cancelledJob = (cancelResult as DomainResult.Success).data
        assertEquals(ProductionJobStatus.CANCELLED, cancelledJob.status)
        assertTrue(cancelledJob.notes?.contains("ক্লায়েন্ট অর্ডার বাতিল করেছে") == true)

        // Terminal cancelled job rejects further mutations
        val startAfterCancel = repository.startStage(sampleJob.jobId, sampleJob.stages[0].stageId, timestamp = "2026-08-16T10:15:00Z")
        assertTrue(startAfterCancel is DomainResult.Error)
    }

    @Test
    fun concurrentDuplicateStageStarts_onlyOneValidMutationWins() = runBlocking {
        repository.createJob(sampleJob)
        val stage1Id = sampleJob.stages[0].stageId

        // Concurrently invoke startStage 10 times
        val results = (1..10).map { i ->
            async {
                repository.startStage(sampleJob.jobId, stage1Id, actorId = "user-$i", timestamp = "2026-08-16T10:00:0$i" + "Z")
            }
        }.awaitAll()

        val successCount = results.count { it is DomainResult.Success }
        val errorCount = results.count { it is DomainResult.Error }

        // Exactly 1 start succeeded, 9 were safely rejected
        assertEquals(1, successCount)
        assertEquals(9, errorCount)

        val job = repository.findJobById(sampleJob.jobId).let { (it as DomainResult.Success).data }
        assertEquals(ProductionJobStatus.IN_PROGRESS, job.status)
        assertEquals(ProductionStageStatus.IN_PROGRESS, job.stages[0].status)
    }
}

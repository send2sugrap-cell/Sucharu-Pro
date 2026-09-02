package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionJobDataSource
import com.sucharu.sucharupro.data.repository.ProductionJobRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.job.ProductionDurationCalculator
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStage
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.job.ProductionStageExecution
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.production.ProductionStageStatus
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit and integration tests for Production Stage Execution runtime and duration calculation (Module 04 Step 05).
 */
class ProductionStageExecutionTest {

    private lateinit var dataSource: FakeProductionJobDataSource
    private lateinit var repository: ProductionJobRepository

    private val job = ProductionJob(
        jobId = "job-exec-01",
        jobNumber = "JOB-2026-0001",
        orderId = "ord-001",
        orderNumber = "ORD-2026-0001",
        customerId = "cus-001",
        handoffId = "hnd-001",
        title = "বই মুদ্রণ ও বাঁধাই",
        quantity = 2000,
        unit = "কপি",
        priority = OrderPriority.URGENT,
        status = ProductionJobStatus.READY_FOR_PRODUCTION,
        stages = ProductionJobStage.createInitialStages("job-exec-01"),
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    private val stage1Id = job.stages.first { it.stageType == ProductionStageType.DESIGN }.stageId

    @Before
    fun setUp() {
        runBlocking {
            dataSource = FakeProductionJobDataSource()
            repository = ProductionJobRepositoryImpl(dataSource)
            repository.createJob(job)
            repository.assignStageOperator(
                jobId = job.jobId,
                stageId = stage1Id,
                operatorId = "op-01",
                operatorName = "তানভীর হাসান",
                timestamp = "2026-08-16T10:05:00Z"
            )
        }
    }

    @Test
    fun startStage_createsExecutionRecord_withOperatorAttributionAndStartRemarks() = runBlocking {
        val startResult = repository.startStage(
            jobId = job.jobId,
            stageId = stage1Id,
            actorId = "op-01",
            actorName = "তানভীর হাসান",
            notes = "ডিজাইন কাজ শুরু করা হলো",
            timestamp = "2026-08-16T10:10:00Z"
        )

        assertTrue(startResult is DomainResult.Success)
        val updatedJob = (startResult as DomainResult.Success).data
        val stage = updatedJob.stages.find { it.stageId == stage1Id }
        assertEquals(ProductionStageStatus.IN_PROGRESS, stage?.status)
        assertEquals("2026-08-16T10:10:00Z", stage?.startedAt)

        val execution = repository.getStageExecution(job.jobId, stage1Id).first()
        assertNotNull(execution)
        assertEquals("op-01", execution?.operatorId)
        assertEquals("তানভীর হাসান", execution?.operatorName)
        assertEquals("2026-08-16T10:10:00Z", execution?.startedAt)
        assertEquals("ডিজাইন কাজ শুরু করা হলো", execution?.startRemarks)
        assertEquals(ProductionStageStatus.IN_PROGRESS, execution?.status)
    }

    @Test
    fun completeStage_calculatesDuration_andUpdatesExecutionRecord() = runBlocking {
        repository.startStage(
            jobId = job.jobId,
            stageId = stage1Id,
            actorId = "op-01",
            actorName = "তানভীর হাসান",
            notes = "শুরু",
            timestamp = "2026-08-16T10:00:00Z"
        )

        val completeResult = repository.completeStage(
            jobId = job.jobId,
            stageId = stage1Id,
            actorId = "op-01",
            notes = "ডিজাইন সম্পন্ন",
            timestamp = "2026-08-16T10:45:30Z"
        )

        assertTrue(completeResult is DomainResult.Success)
        val execution = repository.getStageExecution(job.jobId, stage1Id).first()
        assertNotNull(execution)
        assertEquals(ProductionStageStatus.COMPLETED, execution?.status)
        assertEquals("2026-08-16T10:45:30Z", execution?.completedAt)
        assertEquals("ডিজাইন সম্পন্ন", execution?.completionRemarks)
        assertEquals(2730L, execution?.durationSeconds) // 45 min 30 sec = 2730 seconds
        assertEquals("45 min 30 sec", execution?.formattedDuration)
    }

    @Test
    fun durationCalculator_handlesHoursMinutesSecondsCorrectly() {
        val duration1 = ProductionDurationCalculator.calculateDurationSeconds(
            "2026-08-16T10:00:00Z",
            "2026-08-16T10:00:45Z"
        )
        assertEquals(45L, duration1)

        val duration2 = ProductionDurationCalculator.calculateDurationSeconds(
            "2026-08-16T10:00:00Z",
            "2026-08-16T11:25:00Z"
        )
        assertEquals(5100L, duration2) // 1 hr 25 min = 5100s

        val execution = ProductionStageExecution(
            executionId = "exec-test",
            jobId = "job-test",
            stageId = "stage-test",
            stageType = ProductionStageType.PRINTING,
            durationSeconds = 5100L,
            createdAt = "2026-08-16T10:00:00Z"
        )
        assertEquals("1 hr 25 min", execution.formattedDuration)
    }

    @Test
    fun startNonPendingStage_isRejected() = runBlocking {
        repository.startStage(job.jobId, stage1Id, timestamp = "2026-08-16T10:10:00Z")

        val secondStart = repository.startStage(job.jobId, stage1Id, timestamp = "2026-08-16T10:15:00Z")
        assertTrue(secondStart is DomainResult.Error)
        assertTrue((secondStart as DomainResult.Error).message.contains("already in progress", ignoreCase = true))
    }

    @Test
    fun completePendingStageBeforeStart_isRejected() = runBlocking {
        val completeResult = repository.completeStage(job.jobId, stage1Id, timestamp = "2026-08-16T10:10:00Z")
        assertTrue(completeResult is DomainResult.Error)
        assertTrue((completeResult as DomainResult.Error).message.contains("Must be In Progress", ignoreCase = true))
    }

    @Test
    fun startStageOnOnHoldJob_isRejected() = runBlocking {
        repository.holdJob(job.jobId, reason = "কাগজ সংকট", timestamp = "2026-08-16T10:08:00Z")

        val startResult = repository.startStage(job.jobId, stage1Id, timestamp = "2026-08-16T10:10:00Z")
        assertTrue(startResult is DomainResult.Error)
        assertTrue((startResult as DomainResult.Error).message.contains("On Hold"))
    }

    @Test
    fun addStageExecutionNote_appendsNotesSuccessfully() = runBlocking {
        val result = repository.addStageExecutionNote(
            jobId = job.jobId,
            stageId = stage1Id,
            note = "কালার ম্যাচিং চেক করা হলো",
            actorId = "op-01",
            actorName = "তানভীর",
            timestamp = "2026-08-16T10:20:00Z"
        )

        assertTrue(result is DomainResult.Success)
        val updatedJob = (result as DomainResult.Success).data
        val stage = updatedJob.stages.find { it.stageId == stage1Id }
        assertTrue(stage?.notes?.contains("কালার ম্যাচিং চেক করা হলো") == true)
    }
}

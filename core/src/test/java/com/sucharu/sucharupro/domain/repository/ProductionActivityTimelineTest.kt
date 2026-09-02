package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionJobDataSource
import com.sucharu.sucharupro.data.repository.ProductionJobRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.job.ProductionActivityEvent
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
 * Unit and integration tests for chronological Production Activity Timeline (Module 04 Step 05).
 */
class ProductionActivityTimelineTest {

    private lateinit var dataSource: FakeProductionJobDataSource
    private lateinit var repository: ProductionJobRepository

    private val job = ProductionJob(
        jobId = "job-act-01",
        jobNumber = "JOB-2026-0001",
        orderId = "ord-001",
        orderNumber = "ORD-2026-0001",
        customerId = "cus-001",
        handoffId = "hnd-001",
        title = "পুস্তিকা প্রিন্টিং",
        quantity = 1000,
        unit = "Pcs",
        priority = OrderPriority.NORMAL,
        status = ProductionJobStatus.READY_FOR_PRODUCTION,
        stages = ProductionJobStage.createInitialStages("job-act-01"),
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
        }
    }

    @Test
    fun stageLifecycle_emitsCorrespondingChronologicalActivityEvents() = runBlocking {
        // 1. Assign Operator -> STAGE_ASSIGNED
        repository.assignStageOperator(
            jobId = job.jobId,
            stageId = stage1Id,
            operatorId = "op-01",
            operatorName = "Rahim",
            notes = "ডিজাইন বরাদ্দ",
            timestamp = "2026-08-16T10:05:00Z"
        )

        // 2. Start Stage -> STAGE_STARTED
        repository.startStage(
            jobId = job.jobId,
            stageId = stage1Id,
            actorId = "op-01",
            actorName = "Rahim",
            notes = "ডিজাইন শুরু",
            timestamp = "2026-08-16T10:10:00Z"
        )

        // 3. Complete Stage -> STAGE_COMPLETED
        repository.completeStage(
            jobId = job.jobId,
            stageId = stage1Id,
            actorId = "op-01",
            notes = "ডিজাইন শেষ",
            timestamp = "2026-08-16T10:40:00Z"
        )

        val activities = repository.getProductionActivityEvents(job.jobId).first()
        assertEquals(3, activities.size)

        // Verify sorted newest first
        assertEquals(ProductionActivityType.STAGE_COMPLETED, activities[0].eventType)
        assertEquals(ProductionActivityType.STAGE_STARTED, activities[1].eventType)
        assertEquals(ProductionActivityType.STAGE_ASSIGNED, activities[2].eventType)

        assertEquals("ডিজাইন শেষ", activities[0].message)
        assertEquals("ডিজাইন শুরু", activities[1].message)
        assertEquals("ডিজাইন বরাদ্দ", activities[2].message)
    }

    @Test
    fun jobHoldAndResume_emitsHeldAndResumedEvents() = runBlocking {
        repository.holdJob(job.jobId, reason = "কাঁচামাল ঘাটতি", timestamp = "2026-08-16T11:00:00Z")
        repository.resumeJob(job.jobId, timestamp = "2026-08-16T11:30:00Z")

        val activities = repository.getProductionActivityEvents(job.jobId).first()
        assertEquals(2, activities.size)
        assertEquals(ProductionActivityType.JOB_RESUMED, activities[0].eventType)
        assertEquals(ProductionActivityType.JOB_HELD, activities[1].eventType)
        assertEquals("কাঁচামাল ঘাটতি", activities[1].message)
    }

    @Test
    fun jobCancellation_emitsCancelledEvent() = runBlocking {
        repository.cancelJob(job.jobId, reason = "গ্রাহক বাতিল করেছেন", timestamp = "2026-08-16T12:00:00Z")

        val activities = repository.getProductionActivityEvents(job.jobId).first()
        assertEquals(1, activities.size)
        assertEquals(ProductionActivityType.JOB_CANCELLED, activities[0].eventType)
        assertEquals("গ্রাহক বাতিল করেছেন", activities[0].message)
    }

    @Test
    fun failedOperations_emitZeroActivityEvents() = runBlocking {
        // Attempt invalid complete on non-started stage
        val invalidResult = repository.completeStage(job.jobId, stage1Id, timestamp = "2026-08-16T10:10:00Z")
        assertTrue(invalidResult is DomainResult.Error)

        val activities = repository.getProductionActivityEvents(job.jobId).first()
        assertEquals(0, activities.size)
    }

    @Test
    fun unassignOperator_emitsUnassignedActivityEvent() = runBlocking {
        repository.assignStageOperator(
            jobId = job.jobId,
            stageId = stage1Id,
            operatorId = "op-01",
            operatorName = "Rahim",
            timestamp = "2026-08-16T10:05:00Z"
        )
        repository.unassignStageOperator(
            jobId = job.jobId,
            stageId = stage1Id,
            reason = "অপারেটর অসুস্থ",
            timestamp = "2026-08-16T10:15:00Z"
        )

        val activities = repository.getProductionActivityEvents(job.jobId).first()
        assertEquals(2, activities.size)
        assertEquals(ProductionActivityType.STAGE_UNASSIGNED, activities[0].eventType)
        assertEquals("অপারেটর অসুস্থ", activities[0].message)
    }

    @Test
    fun skipStage_emitsStageSkippedActivityEvent() = runBlocking {
        // Complete stages 1..4 so Stage 5 (CTP) is eligible to be skipped
        for (i in 0..3) {
            val sId = job.stages[i].stageId
            repository.startStage(job.jobId, sId, timestamp = "2026-08-16T10:00:00Z")
            repository.completeStage(job.jobId, sId, timestamp = "2026-08-16T10:10:00Z")
        }

        val ctpStage = job.stages.find { it.stageType == ProductionStageType.CTP }!!
        val result = repository.skipStage(
            jobId = job.jobId,
            stageId = ctpStage.stageId,
            notes = "ডিজিটাল প্রিন্ট, প্লেট প্রয়োজন নেই",
            timestamp = "2026-08-16T10:20:00Z"
        )
        assertTrue(result is DomainResult.Success)

        val activities = repository.getProductionActivityEvents(job.jobId).first()
        val skippedEvent = activities.find { it.eventType == ProductionActivityType.STAGE_SKIPPED }
        assertNotNull(skippedEvent)
        assertEquals("ডিজিটাল প্রিন্ট, প্লেট প্রয়োজন নেই", skippedEvent?.message)
    }
}

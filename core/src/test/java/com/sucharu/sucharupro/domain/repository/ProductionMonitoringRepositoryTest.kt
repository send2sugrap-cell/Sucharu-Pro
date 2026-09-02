package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionJobDataSource
import com.sucharu.sucharupro.data.repository.ProductionJobRepositoryImpl
import com.sucharu.sucharupro.domain.model.job.AttentionReasonType
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStage
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Reactive flow and repository tests for Live Production Monitoring (Module 04 Step 07).
 */
class ProductionMonitoringRepositoryTest {

    private lateinit var dataSource: FakeProductionJobDataSource
    private lateinit var repository: ProductionJobRepository

    private val sampleJob = ProductionJob(
        jobId = "job-mon-repo-01",
        jobNumber = "JOB-2026-MON01",
        orderId = "ord-001",
        orderNumber = "ORD-2026-0001",
        customerId = "cus-001",
        handoffId = "hnd-001",
        title = "পুস্তিকা মুদ্রণ ও বাঁধাই",
        quantity = 1000,
        unit = "কপি",
        priority = OrderPriority.NORMAL,
        status = ProductionJobStatus.READY_FOR_PRODUCTION,
        stages = ProductionJobStage.createInitialStages("job-mon-repo-01"),
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
    fun observeSnapshot_emitsAccurateInitialSnapshot() = runBlocking {
        repository.createJob(sampleJob)

        val snapshot = repository.observeProductionMonitoringSnapshot().first()
        assertEquals(1, snapshot.totalJobs)
        assertEquals(1, snapshot.activeJobs)
        assertEquals(1, snapshot.readyForProductionJobs)
        assertEquals(0, snapshot.inProgressJobs)
        assertEquals(13, snapshot.unassignedPendingStageCount)
    }

    @Test
    fun observeActiveProductionStages_updatesWhenStageStartsAndCompletes() = runBlocking {
        repository.createJob(sampleJob)

        val initialActive = repository.observeActiveProductionStages().first()
        assertEquals(0, initialActive.size)

        repository.startStage(sampleJob.jobId, stage1Id, timestamp = "2026-08-16T10:05:00Z")
        val activeAfterStart = repository.observeActiveProductionStages().first()
        assertEquals(1, activeAfterStart.size)
        assertEquals(sampleJob.jobNumber, activeAfterStart[0].jobNumber)

        repository.completeStage(sampleJob.jobId, stage1Id, timestamp = "2026-08-16T10:30:00Z")
        val activeAfterComplete = repository.observeActiveProductionStages().first()
        assertEquals(0, activeAfterComplete.size)
    }

    @Test
    fun observeOperatorWorkloads_updatesWhenOperatorAssigned() = runBlocking {
        repository.createJob(sampleJob)

        repository.assignStageOperator(
            jobId = sampleJob.jobId,
            stageId = stage1Id,
            operatorId = "op-01",
            operatorName = "Rahim",
            assignedBy = "Supervisor",
            timestamp = "2026-08-16T10:00:00Z"
        )

        val workloads = repository.observeOperatorWorkloads().first()
        val rahimWorkload = workloads.find { it.operatorId == "op-01" }
        assertEquals(1, rahimWorkload?.activeWorkCount)
        assertEquals(1, rahimWorkload?.pendingAssignedCount)
    }

    @Test
    fun observeAttentionItems_updatesWhenJobOnHoldAndResumes() = runBlocking {
        repository.createJob(sampleJob)

        repository.holdJob(sampleJob.jobId, reason = "কাগজের বিলম্ব", timestamp = "2026-08-16T10:10:00Z")
        val holdItems = repository.observeProductionAttentionItems().first()
        assertTrue(holdItems.any { it.reasonType == AttentionReasonType.ON_HOLD_JOB })

        repository.resumeJob(sampleJob.jobId, timestamp = "2026-08-16T10:20:00Z")
        val resumedItems = repository.observeProductionAttentionItems().first()
        assertTrue(resumedItems.none { it.reasonType == AttentionReasonType.ON_HOLD_JOB })
    }

    @Test
    fun deliveryTransition_decreasesActiveJobsCount() = runBlocking {
        repository.createJob(sampleJob)

        val snapshotBefore = repository.observeProductionMonitoringSnapshot().first()
        assertEquals(1, snapshotBefore.activeJobs)

        // Complete stages 1..11 (index 0..10)
        for (i in 0..10) {
            val stageId = sampleJob.stages[i].stageId
            repository.startStage(sampleJob.jobId, stageId, timestamp = "2026-08-16T10:00:00Z")
            repository.completeStage(sampleJob.jobId, stageId, timestamp = "2026-08-16T10:30:00Z")
        }

        val readyResult = repository.markJobReady(sampleJob.jobId, timestamp = "2026-08-16T11:00:00Z")
        assertTrue(readyResult is com.sucharu.sucharupro.domain.model.common.DomainResult.Success)
        val deliverResult = repository.deliverJob(sampleJob.jobId, timestamp = "2026-08-16T12:00:00Z")
        assertTrue(deliverResult is com.sucharu.sucharupro.domain.model.common.DomainResult.Success)

        val snapshotAfter = repository.observeProductionMonitoringSnapshot().first()
        assertEquals(0, snapshotAfter.activeJobs)
        assertEquals(1, snapshotAfter.deliveredJobs)
    }
}

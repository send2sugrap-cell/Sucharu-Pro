package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.job.AttentionReasonType
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobItem
import com.sucharu.sucharupro.domain.model.job.ProductionJobStage
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.job.ProductionOperator
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.production.ProductionStageStatus
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.model.user.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [ProductionMonitoringCalculator] pure aggregation logic (Module 04 Step 07).
 */
class ProductionMonitoringCalculatorTest {

    private val sampleOperators = listOf(
        ProductionOperator("op-01", "Rahim", UserRole.STAFF),
        ProductionOperator("op-02", "Karim", UserRole.STAFF)
    )

    private fun createSampleJob(
        jobId: String,
        jobNumber: String,
        status: ProductionJobStatus,
        priority: OrderPriority = OrderPriority.NORMAL,
        modifyStages: ((List<ProductionJobStage>) -> List<ProductionJobStage>)? = null
    ): ProductionJob {
        val initialStages = ProductionJobStage.createInitialStages(jobId)
        val stages = modifyStages?.invoke(initialStages) ?: initialStages
        return ProductionJob(
            jobId = jobId,
            jobNumber = jobNumber,
            orderId = "ord-$jobId",
            orderNumber = "ORD-$jobNumber",
            customerId = "cus-01",
            handoffId = "hnd-$jobId",
            title = "বাংলা ব্যাকরণ বই",
            quantity = 1000,
            unit = "কপি",
            priority = priority,
            status = status,
            stages = stages,
            createdAt = "2026-08-16T10:00:00Z",
            updatedAt = "2026-08-16T10:00:00Z"
        )
    }

    @Test
    fun emptyJobs_producesZeroSnapshot() {
        val snapshot = ProductionMonitoringCalculator.computeSnapshot(emptyList())

        assertEquals(0, snapshot.totalJobs)
        assertEquals(0, snapshot.activeJobs)
        assertEquals(0, snapshot.draftJobs)
        assertEquals(0, snapshot.inProgressJobs)
        assertEquals(0, snapshot.onHoldJobs)
        assertEquals(0, snapshot.readyJobs)
        assertEquals(0, snapshot.deliveredJobs)
        assertEquals(0, snapshot.cancelledJobs)
        assertEquals(0, snapshot.activeStageCount)
        assertEquals(0, snapshot.completedStageCount)
        assertEquals(0f, snapshot.overallProgressFraction, 0.001f)
        assertEquals(0, snapshot.attentionRequiredCount)
    }

    @Test
    fun multipleJobs_computesAccurateCounts() {
        val job1 = createSampleJob("job-01", "JOB-2026-0001", ProductionJobStatus.IN_PROGRESS, OrderPriority.URGENT) { stages ->
            stages.mapIndexed { idx, stage ->
                when (idx) {
                    0 -> stage.copy(status = ProductionStageStatus.COMPLETED, assignedUserId = "op-01", assignedUserName = "Rahim")
                    1 -> stage.copy(status = ProductionStageStatus.IN_PROGRESS, assignedUserId = "op-02", assignedUserName = "Karim")
                    else -> stage
                }
            }
        }

        val job2 = createSampleJob("job-02", "JOB-2026-0002", ProductionJobStatus.ON_HOLD, OrderPriority.HIGH)
        val job3 = createSampleJob("job-03", "JOB-2026-0003", ProductionJobStatus.READY, OrderPriority.NORMAL)
        val job4 = createSampleJob("job-04", "JOB-2026-0004", ProductionJobStatus.DELIVERED, OrderPriority.NORMAL)
        val job5 = createSampleJob("job-05", "JOB-2026-0005", ProductionJobStatus.READY_FOR_PRODUCTION)

        val jobs = listOf(job1, job2, job3, job4, job5)
        val snapshot = ProductionMonitoringCalculator.computeSnapshot(jobs)

        assertEquals(5, snapshot.totalJobs)
        assertEquals(4, snapshot.activeJobs) // job4 is delivered (terminal)
        assertEquals(1, snapshot.inProgressJobs)
        assertEquals(1, snapshot.onHoldJobs)
        assertEquals(1, snapshot.readyJobs)
        assertEquals(1, snapshot.deliveredJobs)
        assertEquals(1, snapshot.readyForProductionJobs)
        assertEquals(1, snapshot.activeStageCount) // 1 stage in progress in job1
        assertEquals(1, snapshot.urgentJobCount)
        assertEquals(1, snapshot.highPriorityJobCount)
    }

    @Test
    fun computeActiveStages_returnsOnlyInProgressStages() {
        val job = createSampleJob("job-01", "JOB-2026-0001", ProductionJobStatus.IN_PROGRESS) { stages ->
            stages.mapIndexed { idx, stage ->
                if (idx == 0) stage.copy(status = ProductionStageStatus.IN_PROGRESS, assignedUserId = "op-01", assignedUserName = "Rahim")
                else stage
            }
        }

        val activeStages = ProductionMonitoringCalculator.computeActiveStages(listOf(job))
        assertEquals(1, activeStages.size)
        assertEquals("JOB-2026-0001", activeStages[0].jobNumber)
        assertEquals(ProductionStageType.DESIGN, activeStages[0].stageType)
        assertEquals("Rahim", activeStages[0].assignedOperatorName)
    }

    @Test
    fun computeOperatorWorkloads_aggregatesOperatorMetrics() {
        val job = createSampleJob("job-01", "JOB-2026-0001", ProductionJobStatus.IN_PROGRESS, OrderPriority.URGENT) { stages ->
            stages.mapIndexed { idx, stage ->
                when (idx) {
                    0 -> stage.copy(status = ProductionStageStatus.COMPLETED, assignedUserId = "op-01", assignedUserName = "Rahim")
                    1 -> stage.copy(status = ProductionStageStatus.IN_PROGRESS, assignedUserId = "op-01", assignedUserName = "Rahim")
                    2 -> stage.copy(status = ProductionStageStatus.PENDING, assignedUserId = "op-01", assignedUserName = "Rahim")
                    else -> stage
                }
            }
        }

        val workloads = ProductionMonitoringCalculator.computeOperatorWorkloads(listOf(job), sampleOperators)
        val rahimWorkload = workloads.find { it.operatorId == "op-01" }
        assertNotNull(rahimWorkload)
        assertEquals(2, rahimWorkload?.activeWorkCount) // Stage 1 (in-progress) and Stage 2 (pending)
        assertEquals(1, rahimWorkload?.inProgressCount)
        assertEquals(1, rahimWorkload?.pendingAssignedCount)
        assertEquals(1, rahimWorkload?.completedCount)
        assertEquals(2, rahimWorkload?.urgentCount)
    }

    @Test
    fun computeAttentionItems_identifiesExceptions() {
        val onHoldJob = createSampleJob("job-hold", "JOB-2026-HOLD", ProductionJobStatus.ON_HOLD)
        val urgentJob = createSampleJob("job-urg", "JOB-2026-URG", ProductionJobStatus.IN_PROGRESS, OrderPriority.URGENT)
        val readyJob = createSampleJob("job-rdy", "JOB-2026-RDY", ProductionJobStatus.READY)
        val unassignedJob = createSampleJob("job-unassign", "JOB-2026-UNASS", ProductionJobStatus.READY_FOR_PRODUCTION)

        val items = ProductionMonitoringCalculator.computeAttentionItems(listOf(onHoldJob, urgentJob, readyJob, unassignedJob))

        assertTrue(items.any { it.reasonType == AttentionReasonType.ON_HOLD_JOB })
        assertTrue(items.any { it.reasonType == AttentionReasonType.URGENT_ACTIVE_JOB })
        assertTrue(items.any { it.reasonType == AttentionReasonType.READY_FOR_DELIVERY })
        assertTrue(items.any { it.reasonType == AttentionReasonType.UNASSIGNED_ELIGIBLE_STAGE })
    }

    @Test
    fun banglaUnicodeFidelity_preservedInAttentionAndWorkloadOutputs() {
        val job = createSampleJob("job-bn", "JOB-2026-BN", ProductionJobStatus.ON_HOLD)
        val attentionItems = ProductionMonitoringCalculator.computeAttentionItems(listOf(job))
        val item = attentionItems.find { it.jobId == "job-bn" }

        assertNotNull(item)
        assertTrue(item?.description?.contains("বাংলা ব্যাকরণ বই") == true)
    }
}

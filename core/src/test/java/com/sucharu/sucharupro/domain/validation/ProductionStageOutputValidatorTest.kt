package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStage
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.job.ProductionStageOutput
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.production.ProductionStageStatus
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprehensive validation tests for [ProductionStageOutputValidator] (Module 04 Step 06).
 */
class ProductionStageOutputValidatorTest {

    private val baseJob = ProductionJob(
        jobId = "job-val-01",
        jobNumber = "JOB-2026-VAL01",
        orderId = "ord-001",
        orderNumber = "ORD-2026-0001",
        customerId = "cus-001",
        handoffId = "hnd-001",
        title = "বই মুদ্রণ",
        quantity = 1000,
        unit = "কপি",
        priority = OrderPriority.NORMAL,
        status = ProductionJobStatus.READY_FOR_PRODUCTION,
        stages = ProductionJobStage.createInitialStages("job-val-01"),
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    private val stage1Id = baseJob.stages[0].stageId

    private fun getInProgressJob(): ProductionJob {
        val updatedStages = baseJob.stages.map {
            if (it.stageId == stage1Id) it.copy(status = ProductionStageStatus.IN_PROGRESS) else it
        }
        return baseJob.copy(status = ProductionJobStatus.IN_PROGRESS, stages = updatedStages)
    }

    @Test
    fun blankStageId_isRejected() {
        val job = getInProgressJob()
        val result = ProductionStageOutputValidator.validateOutput(
            job = job,
            stageId = "   ",
            existingOutputs = emptyList(),
            quantity = 100,
            unit = "কপি"
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Stage ID cannot be blank"))
    }

    @Test
    fun nonExistentStage_isRejected() {
        val job = getInProgressJob()
        val result = ProductionStageOutputValidator.validateOutput(
            job = job,
            stageId = "invalid-stage-id",
            existingOutputs = emptyList(),
            quantity = 100,
            unit = "কপি"
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("does not exist"))
    }

    @Test
    fun zeroQuantity_isRejected() {
        val job = getInProgressJob()
        val result = ProductionStageOutputValidator.validateOutput(
            job = job,
            stageId = stage1Id,
            existingOutputs = emptyList(),
            quantity = 0,
            unit = "কপি"
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("greater than 0"))
    }

    @Test
    fun negativeQuantity_isRejected() {
        val job = getInProgressJob()
        val result = ProductionStageOutputValidator.validateOutput(
            job = job,
            stageId = stage1Id,
            existingOutputs = emptyList(),
            quantity = -50,
            unit = "কপি"
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("greater than 0"))
    }

    @Test
    fun blankUnit_isRejected() {
        val job = getInProgressJob()
        val result = ProductionStageOutputValidator.validateOutput(
            job = job,
            stageId = stage1Id,
            existingOutputs = emptyList(),
            quantity = 100,
            unit = "   "
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("unit cannot be blank"))
    }

    @Test
    fun exceedsPlannedQuantity_isRejected() {
        val job = getInProgressJob()
        val existing = listOf(
            ProductionStageOutput(
                outputId = "out-01",
                jobId = job.jobId,
                stageId = stage1Id,
                stageType = ProductionStageType.DESIGN,
                quantity = 700,
                unit = "কপি",
                recordedAt = "2026-08-16T10:00:00Z"
            )
        )

        val result = ProductionStageOutputValidator.validateOutput(
            job = job,
            stageId = stage1Id,
            existingOutputs = existing,
            quantity = 400, // 700 + 400 = 1100 > 1000
            unit = "কপি"
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("exceeds planned quantity"))
    }

    @Test
    fun terminalDeliveredJob_isRejected() {
        val deliveredJob = getInProgressJob().copy(status = ProductionJobStatus.DELIVERED)
        val result = ProductionStageOutputValidator.validateOutput(
            job = deliveredJob,
            stageId = stage1Id,
            existingOutputs = emptyList(),
            quantity = 100,
            unit = "কপি"
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Delivered Job"))
    }

    @Test
    fun terminalCancelledJob_isRejected() {
        val cancelledJob = getInProgressJob().copy(status = ProductionJobStatus.CANCELLED)
        val result = ProductionStageOutputValidator.validateOutput(
            job = cancelledJob,
            stageId = stage1Id,
            existingOutputs = emptyList(),
            quantity = 100,
            unit = "কপি"
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Cancelled Job"))
    }

    @Test
    fun pendingStageNotStarted_isRejected() {
        // baseJob has stage1 in PENDING status
        val result = ProductionStageOutputValidator.validateOutput(
            job = baseJob,
            stageId = stage1Id,
            existingOutputs = emptyList(),
            quantity = 100,
            unit = "কপি"
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Start the stage first"))
    }

    @Test
    fun skippedStage_isRejected() {
        val updatedStages = baseJob.stages.map {
            if (it.stageId == stage1Id) it.copy(status = ProductionStageStatus.SKIPPED) else it
        }
        val skippedJob = baseJob.copy(stages = updatedStages)

        val result = ProductionStageOutputValidator.validateOutput(
            job = skippedJob,
            stageId = stage1Id,
            existingOutputs = emptyList(),
            quantity = 100,
            unit = "কপি"
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("skipped stage"))
    }

    @Test
    fun completedStage_isRejected() {
        val updatedStages = baseJob.stages.map {
            if (it.stageId == stage1Id) it.copy(status = ProductionStageStatus.COMPLETED) else it
        }
        val completedJob = baseJob.copy(stages = updatedStages)

        val result = ProductionStageOutputValidator.validateOutput(
            job = completedJob,
            stageId = stage1Id,
            existingOutputs = emptyList(),
            quantity = 100,
            unit = "কপি"
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("already completed stage"))
    }

    @Test
    fun validPartialOutput_isAccepted() {
        val job = getInProgressJob()
        val result = ProductionStageOutputValidator.validateOutput(
            job = job,
            stageId = stage1Id,
            existingOutputs = emptyList(),
            quantity = 500,
            unit = "কপি"
        )
        assertTrue(result is DomainResult.Success)
        val stage = (result as DomainResult.Success).data
        assertEquals(stage1Id, stage.stageId)
    }
}

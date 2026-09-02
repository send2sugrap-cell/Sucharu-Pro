package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobItem
import com.sucharu.sucharupro.domain.model.job.ProductionJobStage
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.job.ProductionStageExecution
import com.sucharu.sucharupro.domain.model.job.ProductionStageOutput
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.production.ProductionStageStatus
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductionCompletionValidatorTest {

    private fun createSampleJob(
        jobId: String = "job-01",
        quantity: Int = 1000,
        status: ProductionJobStatus = ProductionJobStatus.IN_PROGRESS,
        items: List<ProductionJobItem> = listOf(
            ProductionJobItem(
                itemId = "item-01",
                description = "বাংলা ব্যাকরণ ও নির্মিতি বই",
                quantity = quantity,
                unit = "কপি"
            )
        ),
        modifyStages: (List<ProductionJobStage>) -> List<ProductionJobStage> = { stages ->
            // By default, mark stages 1..11 as COMPLETED
            stages.map { stage ->
                if (stage.sequence < ProductionStageType.READY.displayOrder) {
                    stage.copy(status = ProductionStageStatus.COMPLETED)
                } else {
                    stage
                }
            }
        }
    ): ProductionJob {
        val initialStages = ProductionJobStage.createInitialStages(jobId)
        return ProductionJob(
            jobId = jobId,
            jobNumber = "JOB-2026-0001",
            orderId = "ord-01",
            orderNumber = "ORD-2026-0001",
            handoffId = "hnd-01",
            customerId = "cust-01",
            title = "বাংলা ব্যাকরণ ও নির্মিতি বই",
            quantity = quantity,
            unit = "কপি",
            priority = OrderPriority.NORMAL,
            status = status,
            items = items,
            stages = modifyStages(initialStages),
            createdAt = "2026-08-16T10:00:00Z",
            updatedAt = "2026-08-16T10:00:00Z"
        )
    }

    private fun createMatchingOutputs(job: ProductionJob, quantity: Int = job.quantity): List<ProductionStageOutput> {
        val firstStageId = job.stages[0].stageId
        return listOf(
            ProductionStageOutput(
                outputId = "out-01",
                jobId = job.jobId,
                stageId = firstStageId,
                stageType = job.stages[0].stageType,
                quantity = quantity,
                unit = job.unit,
                recordedAt = "2026-08-16T10:00:00Z"
            )
        )
    }

    @Test
    fun validCompletion_allStagesComplete_reconciledOutput_returnsSuccess() {
        val job = createSampleJob()
        val outputs = createMatchingOutputs(job)

        val result = ProductionCompletionValidator.validateCompletionEligibility(
            job = job,
            outputs = outputs
        )

        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun nullJob_returnsError() {
        val result = ProductionCompletionValidator.validateCompletionEligibility(job = null)
        assertTrue(result is DomainResult.Error)
        assertEquals("Target Production Job cannot be null.", (result as DomainResult.Error).message)
    }

    @Test
    fun cancelledJob_returnsError() {
        val job = createSampleJob(status = ProductionJobStatus.CANCELLED)
        val result = ProductionCompletionValidator.validateCompletionEligibility(job = job)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Cannot complete a cancelled Job"))
    }

    @Test
    fun deliveredJob_returnsError() {
        val job = createSampleJob(status = ProductionJobStatus.DELIVERED)
        val result = ProductionCompletionValidator.validateCompletionEligibility(job = job)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("has already been delivered"))
    }

    @Test
    fun alreadyReadyJob_returnsError() {
        val job = createSampleJob(status = ProductionJobStatus.READY)
        val result = ProductionCompletionValidator.validateCompletionEligibility(job = job)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("is already in Ready state"))
    }

    @Test
    fun pendingRequiredStage_returnsError() {
        val job = createSampleJob { stages ->
            stages.mapIndexed { idx, stage ->
                // Leave stage 5 (CTP) as PENDING
                if (stage.stageType == ProductionStageType.CTP) {
                    stage.copy(status = ProductionStageStatus.PENDING)
                } else if (stage.sequence < ProductionStageType.READY.displayOrder) {
                    stage.copy(status = ProductionStageStatus.COMPLETED)
                } else {
                    stage
                }
            }
        }
        val outputs = createMatchingOutputs(job)

        val result = ProductionCompletionValidator.validateCompletionEligibility(
            job = job,
            outputs = outputs
        )

        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("CTP"))
    }

    @Test
    fun inProgressRequiredStage_returnsError() {
        val job = createSampleJob { stages ->
            stages.map { stage ->
                if (stage.stageType == ProductionStageType.PRINTING) {
                    stage.copy(status = ProductionStageStatus.IN_PROGRESS)
                } else if (stage.sequence < ProductionStageType.READY.displayOrder) {
                    stage.copy(status = ProductionStageStatus.COMPLETED)
                } else {
                    stage
                }
            }
        }
        val outputs = createMatchingOutputs(job)

        val result = ProductionCompletionValidator.validateCompletionEligibility(
            job = job,
            outputs = outputs
        )

        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("still IN_PROGRESS"))
    }

    @Test
    fun invalidSkippedNonSkippableStage_returnsError() {
        val job = createSampleJob { stages ->
            stages.map { stage ->
                // PRINTING cannot be skipped
                if (stage.stageType == ProductionStageType.PRINTING) {
                    stage.copy(status = ProductionStageStatus.SKIPPED)
                } else if (stage.sequence < ProductionStageType.READY.displayOrder) {
                    stage.copy(status = ProductionStageStatus.COMPLETED)
                } else {
                    stage
                }
            }
        }
        val outputs = createMatchingOutputs(job)

        val result = ProductionCompletionValidator.validateCompletionEligibility(
            job = job,
            outputs = outputs
        )

        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("is mandatory and cannot be skipped"))
    }

    @Test
    fun remainingQuantity_returnsError() {
        val job = createSampleJob(quantity = 1000)
        val partialOutputs = createMatchingOutputs(job, quantity = 800)

        val result = ProductionCompletionValidator.validateCompletionEligibility(
            job = job,
            outputs = partialOutputs
        )

        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Remaining: 200 কপি"))
    }

    @Test
    fun overProduction_returnsSuccess() {
        val job = createSampleJob(quantity = 1000)
        val overOutputs = createMatchingOutputs(job, quantity = 1050)

        val result = ProductionCompletionValidator.validateCompletionEligibility(
            job = job,
            outputs = overOutputs
        )

        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun computeCompletionChecklist_evaluatesAllItems() {
        val job = createSampleJob()
        val outputs = createMatchingOutputs(job)

        val checklist = ProductionCompletionValidator.computeCompletionChecklist(
            job = job,
            outputs = outputs
        )

        assertTrue(checklist.isEligible)
        assertEquals(4, checklist.totalCount)
        assertEquals(4, checklist.passedCount)
        assertTrue(checklist.blockingReasons.isEmpty())
        assertFalse(checklist.isOverProduced)
    }

    @Test
    fun computeCompletionChecklist_withOverProduction_flagsOverProduced() {
        val job = createSampleJob(quantity = 1000)
        val overOutputs = createMatchingOutputs(job, quantity = 1100)

        val checklist = ProductionCompletionValidator.computeCompletionChecklist(
            job = job,
            outputs = overOutputs
        )

        assertTrue(checklist.isEligible)
        assertTrue(checklist.isOverProduced)
        assertEquals(100, checklist.overProductionQuantity)
    }
}

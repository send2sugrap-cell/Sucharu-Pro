package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobItem
import com.sucharu.sucharupro.domain.model.job.ProductionJobStage
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.production.ProductionStageStatus
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductionOutputValidatorTest {

    private fun createSampleJob(
        jobId: String = "job-01",
        status: ProductionJobStatus = ProductionJobStatus.IN_PROGRESS,
        modifyStages: (List<ProductionJobStage>) -> List<ProductionJobStage> = { it }
    ): ProductionJob {
        val initialStages = ProductionJobStage.createInitialStages(jobId)
        return ProductionJob(
            jobId = jobId,
            jobNumber = "JOB-2026-0001",
            orderId = "ord-01",
            orderNumber = "ORD-2026-0001",
            handoffId = "hnd-01",
            customerId = "cust-01",
            title = "পুস্তিকা মুদ্রণ ও বাঁধাই",
            quantity = 1000,
            unit = "কপি",
            priority = OrderPriority.NORMAL,
            status = status,
            items = listOf(
                ProductionJobItem(
                    itemId = "item-01",
                    description = "পুস্তিকা মুদ্রণ ও বাঁধাই",
                    quantity = 1000,
                    unit = "কপি"
                )
            ),
            stages = modifyStages(initialStages),
            createdAt = "2026-08-16T10:00:00Z",
            updatedAt = "2026-08-16T10:00:00Z"
        )
    }

    @Test
    fun nullJob_returnsError() {
        val result = ProductionOutputValidator.validateOutputRecord(
            job = null,
            stageId = "stg-01",
            quantity = 100,
            unit = "কপি"
        )
        assertTrue(result is DomainResult.Error)
        assertEquals("Target Production Job cannot be null.", (result as DomainResult.Error).message)
    }

    @Test
    fun nonExistentStage_returnsError() {
        val job = createSampleJob()
        val result = ProductionOutputValidator.validateOutputRecord(
            job = job,
            stageId = "invalid-stage-id",
            quantity = 100,
            unit = "কপি"
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("does not belong to Job"))
    }

    @Test
    fun terminalJob_delivered_returnsError() {
        val job = createSampleJob(status = ProductionJobStatus.DELIVERED)
        val stageId = job.stages[0].stageId

        val result = ProductionOutputValidator.validateOutputRecord(
            job = job,
            stageId = stageId,
            quantity = 100,
            unit = "কপি"
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Cannot record output on Delivered Job"))
    }

    @Test
    fun terminalJob_cancelled_returnsError() {
        val job = createSampleJob(status = ProductionJobStatus.CANCELLED)
        val stageId = job.stages[0].stageId

        val result = ProductionOutputValidator.validateOutputRecord(
            job = job,
            stageId = stageId,
            quantity = 100,
            unit = "কপি"
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Cannot record output on Cancelled Job"))
    }

    @Test
    fun pendingStage_returnsError() {
        val job = createSampleJob()
        val stageId = job.stages[0].stageId // PENDING by default

        val result = ProductionOutputValidator.validateOutputRecord(
            job = job,
            stageId = stageId,
            quantity = 100,
            unit = "কপি"
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Start the stage first"))
    }

    @Test
    fun completedStage_returnsError() {
        val job = createSampleJob { stages ->
            stages.mapIndexed { idx, stage ->
                if (idx == 0) stage.copy(status = ProductionStageStatus.COMPLETED) else stage
            }
        }
        val stageId = job.stages[0].stageId

        val result = ProductionOutputValidator.validateOutputRecord(
            job = job,
            stageId = stageId,
            quantity = 100,
            unit = "কপি"
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("already completed stage"))
    }

    @Test
    fun zeroOrNegativeQuantity_returnsError() {
        val job = createSampleJob { stages ->
            stages.mapIndexed { idx, stage ->
                if (idx == 0) stage.copy(status = ProductionStageStatus.IN_PROGRESS) else stage
            }
        }
        val stageId = job.stages[0].stageId

        val resultZero = ProductionOutputValidator.validateOutputRecord(
            job = job,
            stageId = stageId,
            quantity = 0,
            unit = "কপি"
        )
        assertTrue(resultZero is DomainResult.Error)

        val resultNeg = ProductionOutputValidator.validateOutputRecord(
            job = job,
            stageId = stageId,
            quantity = -10,
            unit = "কপি"
        )
        assertTrue(resultNeg is DomainResult.Error)
    }

    @Test
    fun blankUnit_returnsError() {
        val job = createSampleJob { stages ->
            stages.mapIndexed { idx, stage ->
                if (idx == 0) stage.copy(status = ProductionStageStatus.IN_PROGRESS) else stage
            }
        }
        val stageId = job.stages[0].stageId

        val result = ProductionOutputValidator.validateOutputRecord(
            job = job,
            stageId = stageId,
            quantity = 100,
            unit = "   "
        )
        assertTrue(result is DomainResult.Error)
        assertEquals("Output unit cannot be blank.", (result as DomainResult.Error).message)
    }

    @Test
    fun inProgressStage_validQuantity_returnsSuccess() {
        val job = createSampleJob { stages ->
            stages.mapIndexed { idx, stage ->
                if (idx == 0) stage.copy(status = ProductionStageStatus.IN_PROGRESS) else stage
            }
        }
        val stageId = job.stages[0].stageId

        val result = ProductionOutputValidator.validateOutputRecord(
            job = job,
            stageId = stageId,
            quantity = 500,
            unit = "কপি"
        )
        assertTrue(result is DomainResult.Success)
        assertEquals(stageId, (result as DomainResult.Success).data.stageId)
    }
}

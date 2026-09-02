package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.job.ProductionActivityEvent
import com.sucharu.sucharupro.domain.model.job.ProductionActivityType
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
import org.junit.Assert.assertNotNull
import org.junit.Test

class ProductionJobCompletionSummaryTest {

    private fun createSampleJob(
        jobId: String,
        jobNumber: String,
        status: ProductionJobStatus = ProductionJobStatus.DELIVERED,
        quantity: Int = 1000
    ): ProductionJob {
        val initialStages = ProductionJobStage.createInitialStages(jobId)
        return ProductionJob(
            jobId = jobId,
            jobNumber = jobNumber,
            orderId = "ord-$jobId",
            orderNumber = "ORD-2026-0001",
            handoffId = "hnd-$jobId",
            customerId = "cust-01",
            title = "বাংলা ব্যাকরণ ও নির্মিতি বই",
            quantity = quantity,
            unit = "কপি",
            priority = OrderPriority.URGENT,
            status = status,
            items = listOf(
                ProductionJobItem(
                    itemId = "item-$jobId",
                    description = "বাংলা ব্যাকরণ ও নির্মিতি বই",
                    quantity = quantity,
                    unit = "কপি"
                )
            ),
            stages = initialStages.mapIndexed { idx, stage ->
                when (idx) {
                    0 -> stage.copy(status = ProductionStageStatus.COMPLETED, assignedUserId = "op-01", assignedUserName = "Rahim Ahmed")
                    1 -> stage.copy(status = ProductionStageStatus.SKIPPED)
                    else -> stage
                }
            },
            createdAt = "2026-08-16T10:00:00Z",
            updatedAt = "2026-08-16T12:00:00Z"
        )
    }

    @Test
    fun computeJobCompletionSummary_returnsAccurateValues() {
        val job = createSampleJob("job-01", "JOB-2026-0001", ProductionJobStatus.DELIVERED, 1000)

        val executions = listOf(
            ProductionStageExecution(
                executionId = "exec-01",
                jobId = "job-01",
                stageId = job.stages[0].stageId,
                stageType = ProductionStageType.DESIGN,
                status = ProductionStageStatus.COMPLETED,
                operatorId = "op-01",
                operatorName = "Rahim Ahmed",
                durationSeconds = 2400L,
                createdAt = "2026-08-16T10:05:00Z"
            )
        )

        val outputs = listOf(
            ProductionStageOutput(
                outputId = "out-01",
                jobId = "job-01",
                stageId = job.stages[0].stageId,
                stageType = ProductionStageType.DESIGN,
                quantity = 1000,
                unit = "কপি",
                recordedAt = "2026-08-16T10:20:00Z"
            )
        )

        val activities = listOf(
            ProductionActivityEvent(
                eventId = "act-01",
                jobId = "job-01",
                eventType = ProductionActivityType.STAGE_COMPLETED,
                message = "Stage Design completed",
                timestamp = "2026-08-16T10:40:00Z"
            )
        )

        val summary = ProductionHistoryCalculator.computeJobCompletionSummary(
            job = job,
            executions = executions,
            outputs = outputs,
            activities = activities
        )

        assertEquals("JOB-2026-0001", summary.jobNumber)
        assertEquals("বাংলা ব্যাকরণ ও নির্মিতি বই", summary.jobTitle)
        assertEquals(1000, summary.plannedQuantity)
        assertEquals(1000, summary.totalRecordedOutput)
        assertEquals(0, summary.remainingQuantity)
        assertEquals(2400L, summary.totalDurationSeconds)
        assertEquals(1, summary.completedStageCount)
        assertEquals(1, summary.skippedStageCount)
        assertEquals(1, summary.operatorCount)
        assertEquals(13, summary.stageHistory.size)
        assertEquals(1, summary.recentActivities.size)
    }
}

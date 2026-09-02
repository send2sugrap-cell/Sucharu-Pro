package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobItem
import com.sucharu.sucharupro.domain.model.job.ProductionJobStage
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.job.ProductionOperator
import com.sucharu.sucharupro.domain.model.job.ProductionStageAssignment
import com.sucharu.sucharupro.domain.model.job.ProductionStageExecution
import com.sucharu.sucharupro.domain.model.job.ProductionStageOutput
import com.sucharu.sucharupro.domain.model.job.StageAssignmentStatus
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.production.ProductionStageStatus
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ProductionPerformanceTest {

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
            stages = initialStages,
            createdAt = "2026-08-16T10:00:00Z",
            updatedAt = "2026-08-16T12:00:00Z"
        )
    }

    @Test
    fun computePerformanceMetrics_calculatesAggregateStatistics() {
        val job1 = createSampleJob("job-01", "JOB-2026-0001", ProductionJobStatus.DELIVERED, 1000)
        val job2 = createSampleJob("job-02", "JOB-2026-0002", ProductionJobStatus.CANCELLED, 500)

        val executions = listOf(
            ProductionStageExecution(
                executionId = "exec-01",
                jobId = "job-01",
                stageId = job1.stages[0].stageId,
                stageType = ProductionStageType.DESIGN,
                status = ProductionStageStatus.COMPLETED,
                operatorId = "op-01",
                operatorName = "Rahim",
                durationSeconds = 3600L,
                createdAt = "2026-08-16T10:05:00Z"
            ),
            ProductionStageExecution(
                executionId = "exec-02",
                jobId = "job-01",
                stageId = job1.stages[5].stageId,
                stageType = ProductionStageType.PRINTING,
                status = ProductionStageStatus.COMPLETED,
                operatorId = "op-02",
                operatorName = "Karim",
                durationSeconds = 7200L,
                createdAt = "2026-08-16T10:30:00Z"
            )
        )

        val outputs = listOf(
            ProductionStageOutput(
                outputId = "out-01",
                jobId = "job-01",
                stageId = job1.stages[5].stageId,
                stageType = ProductionStageType.PRINTING,
                quantity = 1000,
                unit = "কপি",
                recordedAt = "2026-08-16T10:45:00Z"
            )
        )

        val assignments = listOf(
            ProductionStageAssignment(
                assignmentId = "asg-01",
                jobId = "job-01",
                stageId = job1.stages[0].stageId,
                stageType = ProductionStageType.DESIGN,
                operatorId = "op-01",
                operatorName = "Rahim",
                assignedAt = "2026-08-16T10:00:00Z",
                status = StageAssignmentStatus.COMPLETED
            ),
            ProductionStageAssignment(
                assignmentId = "asg-02",
                jobId = "job-01",
                stageId = job1.stages[5].stageId,
                stageType = ProductionStageType.PRINTING,
                operatorId = "op-02",
                operatorName = "Karim",
                assignedAt = "2026-08-16T10:00:00Z",
                status = StageAssignmentStatus.COMPLETED
            )
        )

        val metrics = ProductionHistoryCalculator.computePerformanceMetrics(
            jobs = listOf(job1, job2),
            executions = executions,
            outputs = outputs,
            assignments = assignments
        )

        assertEquals(2, metrics.totalHistoricalJobs)
        assertEquals(1, metrics.deliveredJobs)
        assertEquals(1, metrics.cancelledJobs)
        assertEquals(2, metrics.totalStageExecutions)
        assertEquals(2, metrics.completedStages)
        assertEquals(5400L, metrics.averageStageDurationSeconds) // (3600 + 7200) / 2
        assertEquals(7200L, metrics.longestStageDurationSeconds)
        assertEquals(3600L, metrics.shortestStageDurationSeconds)
        assertEquals(1500, metrics.plannedQuantity)
        assertEquals(1000, metrics.recordedOutput)
        assertEquals(500, metrics.remainingQuantity)
        assertEquals(2, metrics.operatorsInvolvedCount)
    }

    @Test
    fun computeOperatorPerformance_aggregatesAccurately() {
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
                durationSeconds = 1800L,
                createdAt = "2026-08-16T10:05:00Z"
            )
        )

        val operators = listOf(
            ProductionOperator("op-01", "Rahim Ahmed", com.sucharu.sucharupro.domain.model.user.UserRole.DESIGNER)
        )

        val opPerformances = ProductionHistoryCalculator.computeOperatorPerformance(
            jobs = listOf(job),
            executions = executions,
            availableOperators = operators
        )

        assertEquals(1, opPerformances.size)
        val rahim = opPerformances[0]
        assertEquals("op-01", rahim.operatorId)
        assertEquals("Rahim Ahmed", rahim.operatorName)
        assertEquals(1, rahim.completedStageCount)
        assertEquals(1800L, rahim.totalExecutionSeconds)
        assertEquals(1800L, rahim.averageExecutionSeconds)
        assertEquals(1, rahim.urgentStageCount)
    }

    @Test
    fun computeStagePerformance_aggregatesAll13Stages() {
        val job = createSampleJob("job-01", "JOB-2026-0001", ProductionJobStatus.DELIVERED, 1000)
        val stagePerformances = ProductionHistoryCalculator.computeStagePerformance(listOf(job))

        assertEquals(13, stagePerformances.size)
        assertEquals(ProductionStageType.DESIGN, stagePerformances[0].stageType)
        assertEquals(ProductionStageType.DELIVERED, stagePerformances[12].stageType)
    }
}

package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.job.CompletionFilter
import com.sucharu.sucharupro.domain.model.job.ProductionHistoryFilter
import com.sucharu.sucharupro.domain.model.job.ProductionHistorySortBy
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobItem
import com.sucharu.sucharupro.domain.model.job.ProductionJobStage
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.job.ProductionOperator
import com.sucharu.sucharupro.domain.model.job.ProductionStageExecution
import com.sucharu.sucharupro.domain.model.job.ProductionStageOutput
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.production.ProductionStageStatus
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductionHistoryCalculatorTest {

    private fun createSampleJob(
        jobId: String,
        jobNumber: String,
        status: ProductionJobStatus = ProductionJobStatus.IN_PROGRESS,
        priority: OrderPriority = OrderPriority.NORMAL,
        title: String = "পুস্তিকা মুদ্রণ ও বাঁধাই",
        quantity: Int = 1000,
        modifyStages: (List<ProductionJobStage>) -> List<ProductionJobStage> = { it }
    ): ProductionJob {
        val initialStages = ProductionJobStage.createInitialStages(jobId)
        return ProductionJob(
            jobId = jobId,
            jobNumber = jobNumber,
            orderId = "ord-$jobId",
            orderNumber = "ORD-2026-0001",
            handoffId = "hnd-$jobId",
            customerId = "cust-01",
            title = title,
            quantity = quantity,
            unit = "কপি",
            priority = priority,
            status = status,
            items = listOf(
                ProductionJobItem(
                    itemId = "item-$jobId",
                    description = title,
                    quantity = quantity,
                    unit = "কপি"
                )
            ),
            stages = modifyStages(initialStages),
            createdAt = "2026-08-16T10:00:00Z",
            updatedAt = "2026-08-16T11:00:00Z"
        )
    }

    @Test
    fun emptyHistory_returnsZeroMetrics() {
        val metrics = ProductionHistoryCalculator.computePerformanceMetrics(emptyList())
        assertEquals(0, metrics.totalHistoricalJobs)
        assertEquals(0, metrics.completedJobs)
        assertEquals(0, metrics.deliveredJobs)
        assertEquals(0.0, metrics.completionRate, 0.001)
        assertEquals(0L, metrics.averageStageDurationSeconds)
        assertEquals(0, metrics.plannedQuantity)
        assertEquals(0, metrics.recordedOutput)
    }

    @Test
    fun deliveredAndCancelledJobs_countedAccurately() {
        val job1 = createSampleJob("job-01", "JOB-2026-0001", ProductionJobStatus.DELIVERED)
        val job2 = createSampleJob("job-02", "JOB-2026-0002", ProductionJobStatus.CANCELLED)
        val job3 = createSampleJob("job-03", "JOB-2026-0003", ProductionJobStatus.IN_PROGRESS)

        val metrics = ProductionHistoryCalculator.computePerformanceMetrics(listOf(job1, job2, job3))
        assertEquals(3, metrics.totalHistoricalJobs)
        assertEquals(1, metrics.deliveredJobs)
        assertEquals(1, metrics.cancelledJobs)
        assertEquals(1, metrics.currentlyActiveJobs)
        assertEquals(1.0 / 3.0, metrics.completionRate, 0.01)
    }

    @Test
    fun stageHistory_andOutputs_calculatedAccurately() {
        val job = createSampleJob("job-01", "JOB-2026-0001") { stages ->
            stages.mapIndexed { idx, stage ->
                when (idx) {
                    0 -> stage.copy(status = ProductionStageStatus.COMPLETED, assignedUserId = "op-01", assignedUserName = "Rahim")
                    1 -> stage.copy(status = ProductionStageStatus.SKIPPED)
                    else -> stage
                }
            }
        }

        val executions = listOf(
            ProductionStageExecution(
                executionId = "exec-01",
                jobId = "job-01",
                stageId = job.stages[0].stageId,
                stageType = ProductionStageType.DESIGN,
                status = ProductionStageStatus.COMPLETED,
                operatorId = "op-01",
                operatorName = "Rahim",
                durationSeconds = 1800L,
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
                operatorId = "op-01",
                recordedAt = "2026-08-16T10:20:00Z"
            )
        )

        val summaries = ProductionHistoryCalculator.computeHistorySummaries(listOf(job), executions, outputs)
        assertEquals(1, summaries.size)
        val summary = summaries[0]
        assertEquals(1, summary.completedStageCount)
        assertEquals(1, summary.skippedStageCount)
        assertEquals(1800L, summary.totalDurationSeconds)
        assertEquals(1000, summary.totalRecordedOutput)
        assertEquals(0, summary.remainingQuantity)
    }

    @Test
    fun filterAndSort_appliesCriteriaAccurately() {
        val job1 = createSampleJob("job-01", "JOB-2026-0001", ProductionJobStatus.DELIVERED, OrderPriority.NORMAL, "ক্যাটালগ মুদ্রণ", 500)
        val job2 = createSampleJob("job-02", "JOB-2026-0002", ProductionJobStatus.IN_PROGRESS, OrderPriority.URGENT, "পোস্টার ডিজাইন", 2000)

        val summaries = ProductionHistoryCalculator.computeHistorySummaries(listOf(job1, job2))

        // Search by Bangla Title
        val searchBangla = ProductionHistoryCalculator.filterAndSortHistory(summaries, ProductionHistoryFilter(), "ক্যাটালগ")
        assertEquals(1, searchBangla.size)
        assertEquals("JOB-2026-0001", searchBangla[0].jobNumber)

        // Filter by Priority URGENT
        val filterUrgent = ProductionHistoryCalculator.filterAndSortHistory(
            summaries,
            ProductionHistoryFilter(priority = OrderPriority.URGENT),
            ""
        )
        assertEquals(1, filterUrgent.size)
        assertEquals("JOB-2026-0002", filterUrgent[0].jobNumber)

        // Sort by Priority Descending
        val sortedPriority = ProductionHistoryCalculator.filterAndSortHistory(
            summaries,
            ProductionHistoryFilter(sortBy = ProductionHistorySortBy.PRIORITY_DESC),
            ""
        )
        assertEquals("JOB-2026-0002", sortedPriority[0].jobNumber)
    }

    @Test
    fun banglaUnicodeFidelity_preservedInSummaries() {
        val banglaTitle = "৪ কালার অফসেট প্রিন্ট ও গ্লসি ল্যামিনেশন"
        val job = createSampleJob("job-bn", "JOB-2026-BN", title = banglaTitle)

        val summaries = ProductionHistoryCalculator.computeHistorySummaries(listOf(job))
        assertEquals(1, summaries.size)
        assertEquals(banglaTitle, summaries[0].title)
    }
}

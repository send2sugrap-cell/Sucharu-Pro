package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobItem
import com.sucharu.sucharupro.domain.model.job.ProductionJobStage
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.job.ProductionStageOutput
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductionOutputReconciliationTest {

    private fun createSampleJob(
        jobId: String = "job-01",
        quantity: Int = 1000,
        items: List<ProductionJobItem> = listOf(
            ProductionJobItem(
                itemId = "item-01",
                description = "বাংলা ব্যাকরণ ও নির্মিতি",
                quantity = quantity,
                unit = "কপি"
            )
        )
    ): ProductionJob {
        return ProductionJob(
            jobId = jobId,
            jobNumber = "JOB-2026-0001",
            orderId = "ord-01",
            orderNumber = "ORD-2026-0001",
            handoffId = "hnd-01",
            customerId = "cust-01",
            title = "বাংলা ব্যাকরণ ও নির্মিতি",
            quantity = quantity,
            unit = "কপি",
            priority = OrderPriority.NORMAL,
            status = ProductionJobStatus.IN_PROGRESS,
            items = items,
            stages = ProductionJobStage.createInitialStages(jobId),
            createdAt = "2026-08-16T10:00:00Z",
            updatedAt = "2026-08-16T10:00:00Z"
        )
    }

    @Test
    fun zeroRecordedOutput_calculatesAccurateZeroState() {
        val job = createSampleJob(quantity = 1000)
        val reconciliation = ProductionOutputReconciliationCalculator.computeJobReconciliation(job, emptyList())

        assertEquals(1000, reconciliation.plannedQuantity)
        assertEquals(0, reconciliation.recordedQuantity)
        assertEquals(1000, reconciliation.remainingQuantity)
        assertEquals(0, reconciliation.overProductionQuantity)
        assertEquals(1000, reconciliation.underProductionQuantity)
        assertEquals(0.0, reconciliation.completionPercentage, 0.001)
        assertFalse(reconciliation.isFullyProduced)
        assertFalse(reconciliation.isOverProduced)
        assertEquals(0, reconciliation.outputRecordCount)
    }

    @Test
    fun partialRecordedOutput_calculatesAccurateProgress() {
        val job = createSampleJob(quantity = 1000)
        val stageId = job.stages[0].stageId

        val outputs = listOf(
            ProductionStageOutput(
                outputId = "out-01",
                jobId = job.jobId,
                stageId = stageId,
                stageType = ProductionStageType.DESIGN,
                quantity = 400,
                unit = "কপি",
                recordedAt = "2026-08-16T10:00:00Z"
            ),
            ProductionStageOutput(
                outputId = "out-02",
                jobId = job.jobId,
                stageId = stageId,
                stageType = ProductionStageType.DESIGN,
                quantity = 450,
                unit = "কপি",
                recordedAt = "2026-08-16T10:30:00Z"
            )
        )

        val reconciliation = ProductionOutputReconciliationCalculator.computeJobReconciliation(job, outputs)

        assertEquals(1000, reconciliation.plannedQuantity)
        assertEquals(850, reconciliation.recordedQuantity)
        assertEquals(150, reconciliation.remainingQuantity)
        assertEquals(0, reconciliation.overProductionQuantity)
        assertEquals(150, reconciliation.underProductionQuantity)
        assertEquals(85.0, reconciliation.completionPercentage, 0.001)
        assertFalse(reconciliation.isFullyProduced)
        assertFalse(reconciliation.isOverProduced)
        assertEquals(2, reconciliation.outputRecordCount)
    }

    @Test
    fun exactPlannedOutput_calculatesCompleteState() {
        val job = createSampleJob(quantity = 1000)
        val stageId = job.stages[0].stageId

        val outputs = listOf(
            ProductionStageOutput(
                outputId = "out-01",
                jobId = job.jobId,
                stageId = stageId,
                stageType = ProductionStageType.DESIGN,
                quantity = 1000,
                unit = "কপি",
                recordedAt = "2026-08-16T10:00:00Z"
            )
        )

        val reconciliation = ProductionOutputReconciliationCalculator.computeJobReconciliation(job, outputs)

        assertEquals(1000, reconciliation.plannedQuantity)
        assertEquals(1000, reconciliation.recordedQuantity)
        assertEquals(0, reconciliation.remainingQuantity)
        assertEquals(0, reconciliation.overProductionQuantity)
        assertEquals(0, reconciliation.underProductionQuantity)
        assertEquals(100.0, reconciliation.completionPercentage, 0.001)
        assertTrue(reconciliation.isFullyProduced)
        assertFalse(reconciliation.isOverProduced)
    }

    @Test
    fun overProductionOutput_calculatesAccurateOverState() {
        val job = createSampleJob(quantity = 1000)
        val stageId = job.stages[0].stageId

        val outputs = listOf(
            ProductionStageOutput(
                outputId = "out-01",
                jobId = job.jobId,
                stageId = stageId,
                stageType = ProductionStageType.DESIGN,
                quantity = 1050,
                unit = "কপি",
                recordedAt = "2026-08-16T10:00:00Z"
            )
        )

        val reconciliation = ProductionOutputReconciliationCalculator.computeJobReconciliation(job, outputs)

        assertEquals(1000, reconciliation.plannedQuantity)
        assertEquals(1050, reconciliation.recordedQuantity)
        assertEquals(0, reconciliation.remainingQuantity)
        assertEquals(50, reconciliation.overProductionQuantity)
        assertEquals(0, reconciliation.underProductionQuantity)
        assertEquals(105.0, reconciliation.completionPercentage, 0.001)
        assertTrue(reconciliation.isFullyProduced)
        assertTrue(reconciliation.isOverProduced)
    }

    @Test
    fun divisionByZeroProtection_withZeroPlanned() {
        val job = createSampleJob(quantity = 0)
        val reconciliation = ProductionOutputReconciliationCalculator.computeJobReconciliation(job, emptyList())

        assertEquals(0, reconciliation.plannedQuantity)
        assertEquals(0, reconciliation.recordedQuantity)
        assertEquals(0.0, reconciliation.completionPercentage, 0.001)
    }

    @Test
    fun multiItemReconciliation_preservesItemIdentities() {
        val item1 = ProductionJobItem("item-01", "বইয়ের ভেতরের পাতা (Inner Pages)", null, 600, "কপি")
        val item2 = ProductionJobItem("item-02", "বইয়ের কভার (Cover Page)", null, 400, "কপি")
        val job = createSampleJob(quantity = 1000, items = listOf(item1, item2))
        val stageId = job.stages[0].stageId

        val outputs = listOf(
            ProductionStageOutput(
                outputId = "out-01",
                jobId = job.jobId,
                stageId = stageId,
                stageType = ProductionStageType.DESIGN,
                quantity = 1000,
                unit = "কপি",
                recordedAt = "2026-08-16T10:00:00Z"
            )
        )

        val reconciliation = ProductionOutputReconciliationCalculator.computeJobReconciliation(job, outputs)

        assertEquals(2, reconciliation.itemReconciliations.size)
        val rItem1 = reconciliation.itemReconciliations[0]
        val rItem2 = reconciliation.itemReconciliations[1]

        assertEquals("item-01", rItem1.itemId)
        assertEquals(600, rItem1.plannedQuantity)
        assertEquals(600, rItem1.recordedQuantity)
        assertTrue(rItem1.isFullyProduced)

        assertEquals("item-02", rItem2.itemId)
        assertEquals(400, rItem2.plannedQuantity)
        assertEquals(400, rItem2.recordedQuantity)
        assertTrue(rItem2.isFullyProduced)
    }
}

package com.sucharu.sucharupro.domain.model.job

import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.production.ProductionStageStatus
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.validation.ProductionCompletionValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductionCompletionSnapshotTest {

    @Test
    fun buildProductionReadyHandoff_createsImmutableSnapshot() {
        val jobId = "job-snap-01"
        val stages = ProductionJobStage.createInitialStages(jobId).map {
            if (it.sequence < ProductionStageType.READY.displayOrder) {
                it.copy(status = ProductionStageStatus.COMPLETED, assignedUserId = "op-01")
            } else {
                it
            }
        }

        val job = ProductionJob(
            jobId = jobId,
            jobNumber = "JOB-2026-SNAP01",
            orderId = "ord-01",
            orderNumber = "ORD-2026-0001",
            handoffId = "hnd-01",
            customerId = "cust-01",
            title = "পুস্তিকা মুদ্রণ",
            quantity = 1000,
            unit = "কপি",
            priority = OrderPriority.NORMAL,
            status = ProductionJobStatus.READY,
            items = listOf(ProductionJobItem("item-01", "পুস্তিকা মুদ্রণ", null, 1000, "কপি")),
            stages = stages,
            createdAt = "2026-08-16T10:00:00Z",
            updatedAt = "2026-08-16T11:00:00Z"
        )

        val outputs = listOf(
            ProductionStageOutput(
                outputId = "out-01",
                jobId = jobId,
                stageId = stages[0].stageId,
                stageType = ProductionStageType.DESIGN,
                quantity = 1000,
                unit = "কপি",
                recordedAt = "2026-08-16T10:30:00Z"
            )
        )

        val handoff = ProductionCompletionValidator.buildProductionReadyHandoff(
            job = job,
            outputs = outputs,
            confirmedBy = "supervisor-01",
            confirmedByName = "Akhtaruzzaman",
            remarks = "সকল কাজ সম্পন্ন",
            timestamp = "2026-08-16T11:00:00Z"
        )

        assertEquals("job-snap-01", handoff.productionJobId)
        assertEquals("JOB-2026-SNAP01", handoff.jobNumber)
        assertEquals(1000, handoff.plannedQuantity)
        assertEquals(1000, handoff.recordedQuantity)
        assertEquals(0, handoff.remainingQuantity)
        assertEquals(100.0, handoff.completionPercentage, 0.001)
        assertEquals("Akhtaruzzaman", handoff.confirmedByName)
        assertEquals("সকল কাজ সম্পন্ন", handoff.remarks)
        assertEquals(ProductionJobStatus.READY, handoff.productionStatus)
    }
}

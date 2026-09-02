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

/**
 * Unit test suite for [ProductionJobValidator] and Job Domain Foundation.
 */
class ProductionJobValidatorTest {

    private val sampleItem = ProductionJobItem(
        itemId = "item-01",
        description = "বই মুদ্রণ ও বাইন্ডিং",
        specification = "৮০ জিএসএম অফসেট কাগজ, চার কালার কভার",
        quantity = 1000,
        unit = "Pcs"
    )

    private val validJob = ProductionJob(
        jobId = "job-001",
        jobNumber = "JOB-2026-0001",
        orderId = "ord-001",
        orderNumber = "ORD-2026-0001",
        customerId = "cus-001",
        handoffId = "hnd-001",
        title = "বই মুদ্রণ ও বাইন্ডিং",
        quantity = 1000,
        unit = "Pcs",
        priority = OrderPriority.HIGH,
        status = ProductionJobStatus.READY_FOR_PRODUCTION,
        items = listOf(sampleItem),
        stages = ProductionJobStage.createInitialStages("job-001"),
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    // ─────────────────────────────────────────────────────────────────────────
    // 1. Identity Validations
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun test01_validJob_passesValidation() {
        val result = ProductionJobValidator.validateJob(validJob)
        assertTrue(result is DomainResult.Success<*>)
    }

    @Test
    fun test02_blankJobId_rejected() {
        val invalid = validJob.copy(jobId = "   ")
        val result = ProductionJobValidator.validateJob(invalid)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Job ID"))
    }

    @Test
    fun test03_blankJobNumber_rejected() {
        val invalid = validJob.copy(jobNumber = "")
        val result = ProductionJobValidator.validateJob(invalid)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Job Number"))
    }

    @Test
    fun test04_blankOrderId_rejected() {
        val invalid = validJob.copy(orderId = "   ")
        val result = ProductionJobValidator.validateJob(invalid)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Order ID"))
    }

    @Test
    fun test05_blankCustomerId_rejected() {
        val invalid = validJob.copy(customerId = "")
        val result = ProductionJobValidator.validateJob(invalid)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Customer ID"))
    }

    @Test
    fun test06_blankHandoffId_rejected() {
        val invalid = validJob.copy(handoffId = "  ")
        val result = ProductionJobValidator.validateJob(invalid)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Handoff ID"))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Job Attributes Validations
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun test07_blankTitle_rejected() {
        val invalid = validJob.copy(title = "   ")
        val result = ProductionJobValidator.validateJob(invalid)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("title"))
    }

    @Test
    fun test08_zeroOrNegativeQuantity_rejected() {
        val zeroQty = validJob.copy(quantity = 0)
        val negQty = validJob.copy(quantity = -5)

        assertTrue(ProductionJobValidator.validateJob(zeroQty) is DomainResult.Error)
        assertTrue(ProductionJobValidator.validateJob(negQty) is DomainResult.Error)
    }

    @Test
    fun test09_blankUnit_rejected() {
        val invalid = validJob.copy(unit = "")
        val result = ProductionJobValidator.validateJob(invalid)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("unit"))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Stage Integrity Validations
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun test10_canonicalStages_containsExactly13Stages() {
        val stages = ProductionJobStage.createInitialStages("job-001")
        assertEquals(13, stages.size)
        assertEquals(13, ProductionStageType.TOTAL_STAGES)
        assertEquals(ProductionStageType.DESIGN, stages[0].stageType)
        assertEquals(ProductionStageType.APPROVAL, stages[1].stageType)
        assertEquals(ProductionStageType.QC, stages[2].stageType)
        assertEquals(ProductionStageType.ITEM_APPROVAL, stages[3].stageType)
        assertEquals(ProductionStageType.CTP, stages[4].stageType)
        assertEquals(ProductionStageType.PRINTING, stages[5].stageType)
        assertEquals(ProductionStageType.LAMINATION, stages[6].stageType)
        assertEquals(ProductionStageType.FOLDING, stages[7].stageType)
        assertEquals(ProductionStageType.BINDING, stages[8].stageType)
        assertEquals(ProductionStageType.FINAL_QC, stages[9].stageType)
        assertEquals(ProductionStageType.PACKAGING, stages[10].stageType)
        assertEquals(ProductionStageType.READY, stages[11].stageType)
        assertEquals(ProductionStageType.DELIVERED, stages[12].stageType)
    }

    @Test
    fun test11_initialStages_areAllPending() {
        val stages = ProductionJobStage.createInitialStages("job-001")
        assertTrue(stages.all { it.status == ProductionStageStatus.PENDING })
        assertEquals(0, validJob.completedStagesCount)
        assertEquals(0.0f, validJob.progressFraction, 0.001f)
        assertEquals(ProductionStageType.DESIGN, validJob.currentStage?.stageType)
    }

    @Test
    fun test12_duplicateStageTypes_rejected() {
        val duplicateStages = validJob.stages.toMutableList()
        duplicateStages[1] = duplicateStages[0] // Duplicate DESIGN stage
        val invalid = validJob.copy(stages = duplicateStages)

        val result = ProductionJobValidator.validateJob(invalid)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("duplicate"))
    }

    @Test
    fun test13_wrongStageCount_rejected() {
        val incompleteStages = validJob.stages.take(10)
        val invalid = validJob.copy(stages = incompleteStages)

        val result = ProductionJobValidator.validateJob(invalid)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("13 canonical stages"))
    }

    @Test
    fun test14_wrongSequence_rejected() {
        val swappedStages = validJob.stages.toMutableList()
        val temp = swappedStages[0]
        swappedStages[0] = swappedStages[1]
        swappedStages[1] = temp
        val invalid = validJob.copy(stages = swappedStages)

        val result = ProductionJobValidator.validateJob(invalid)
        assertTrue(result is DomainResult.Error)
    }
}

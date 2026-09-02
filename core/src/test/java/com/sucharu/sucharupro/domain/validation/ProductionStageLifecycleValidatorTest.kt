package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStage
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.production.ProductionStageStatus
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test suite for [ProductionStageLifecycleValidator].
 */
class ProductionStageLifecycleValidatorTest {

    private val baseJob = ProductionJob(
        jobId = "job-stg-01",
        jobNumber = "JOB-2026-STG01",
        orderId = "ord-stg-01",
        orderNumber = "ORD-2026-STG01",
        customerId = "cus-stg-01",
        handoffId = "hnd-stg-01",
        title = "ক্যালেন্ডার প্রিন্টিং ও স্পাইরাল বাইন্ডিং",
        quantity = 1000,
        unit = "Pcs",
        priority = OrderPriority.HIGH,
        status = ProductionJobStatus.READY_FOR_PRODUCTION,
        stages = ProductionJobStage.createInitialStages("job-stg-01"),
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    @Test
    fun test01_firstStage_canStart_whenReadyForProduction() {
        val designStageId = baseJob.stages[0].stageId
        val result = ProductionStageLifecycleValidator.validateStartStage(baseJob, designStageId)
        assertTrue(result is DomainResult.Success<*>)
        assertEquals(ProductionStageType.DESIGN, (result as DomainResult.Success).data.stageType)
    }

    @Test
    fun test02_secondStage_cannotStart_whileFirstIsPending() {
        val approvalStageId = baseJob.stages[1].stageId
        val result = ProductionStageLifecycleValidator.validateStartStage(baseJob, approvalStageId)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Predecessor stage 'Design' is Pending"))
    }

    @Test
    fun test03_secondStage_canStart_whenFirstIsCompleted() {
        val stagesWithDesignComplete = baseJob.stages.map {
            if (it.stageType == ProductionStageType.DESIGN) it.copy(status = ProductionStageStatus.COMPLETED) else it
        }
        val job = baseJob.copy(
            status = ProductionJobStatus.IN_PROGRESS,
            stages = stagesWithDesignComplete
        )

        val approvalStageId = job.stages[1].stageId
        val result = ProductionStageLifecycleValidator.validateStartStage(job, approvalStageId)
        assertTrue(result is DomainResult.Success<*>)
        assertEquals(ProductionStageType.APPROVAL, (result as DomainResult.Success).data.stageType)
    }

    @Test
    fun test04_stageCannotStartTwice() {
        val stagesWithDesignInProgress = baseJob.stages.map {
            if (it.stageType == ProductionStageType.DESIGN) it.copy(status = ProductionStageStatus.IN_PROGRESS) else it
        }
        val job = baseJob.copy(
            status = ProductionJobStatus.IN_PROGRESS,
            stages = stagesWithDesignInProgress
        )

        val designStageId = job.stages[0].stageId
        val result = ProductionStageLifecycleValidator.validateStartStage(job, designStageId)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("already in progress"))
    }

    @Test
    fun test05_completedStage_cannotStartAgain() {
        val stagesWithDesignComplete = baseJob.stages.map {
            if (it.stageType == ProductionStageType.DESIGN) it.copy(status = ProductionStageStatus.COMPLETED) else it
        }
        val job = baseJob.copy(
            status = ProductionJobStatus.IN_PROGRESS,
            stages = stagesWithDesignComplete
        )

        val designStageId = job.stages[0].stageId
        val result = ProductionStageLifecycleValidator.validateStartStage(job, designStageId)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("already completed"))
    }

    @Test
    fun test06_stageCannotComplete_beforeStart() {
        val designStageId = baseJob.stages[0].stageId
        val result = ProductionStageLifecycleValidator.validateCompleteStage(baseJob, designStageId)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Must be In Progress"))
    }

    @Test
    fun test07_inProgressStage_canBeCompleted() {
        val stagesWithDesignInProgress = baseJob.stages.map {
            if (it.stageType == ProductionStageType.DESIGN) it.copy(status = ProductionStageStatus.IN_PROGRESS) else it
        }
        val job = baseJob.copy(
            status = ProductionJobStatus.IN_PROGRESS,
            stages = stagesWithDesignInProgress
        )

        val designStageId = job.stages[0].stageId
        val result = ProductionStageLifecycleValidator.validateCompleteStage(job, designStageId)
        assertTrue(result is DomainResult.Success<*>)
        assertEquals(ProductionStageType.DESIGN, (result as DomainResult.Success).data.stageType)
    }

    @Test
    fun test08_stageOperations_rejectedOnTerminalJobs() {
        val deliveredJob = baseJob.copy(status = ProductionJobStatus.DELIVERED)
        val cancelledJob = baseJob.copy(status = ProductionJobStatus.CANCELLED)

        val stageId = baseJob.stages[0].stageId
        assertTrue(ProductionStageLifecycleValidator.validateStartStage(deliveredJob, stageId) is DomainResult.Error)
        assertTrue(ProductionStageLifecycleValidator.validateStartStage(cancelledJob, stageId) is DomainResult.Error)
        assertTrue(ProductionStageLifecycleValidator.validateCompleteStage(deliveredJob, stageId) is DomainResult.Error)
        assertTrue(ProductionStageLifecycleValidator.validateCompleteStage(cancelledJob, stageId) is DomainResult.Error)
    }

    @Test
    fun test09_stageStart_rejectedWhileJobOnHold() {
        val onHoldJob = baseJob.copy(status = ProductionJobStatus.ON_HOLD)
        val stageId = onHoldJob.stages[0].stageId

        val result = ProductionStageLifecycleValidator.validateStartStage(onHoldJob, stageId)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("On Hold"))
    }

    @Test
    fun test10_stageSkipping_validation() {
        // CTP stage can be skipped
        val ctpStage = baseJob.stages.find { it.stageType == ProductionStageType.CTP }!!
        val designStage = baseJob.stages.find { it.stageType == ProductionStageType.DESIGN }!!

        // Mandatory stage DESIGN cannot be skipped
        val designSkipResult = ProductionStageLifecycleValidator.validateSkipStage(baseJob, designStage.stageId)
        assertTrue(designSkipResult is DomainResult.Error)
        assertTrue((designSkipResult as DomainResult.Error).message.contains("mandatory"))

        // CTP cannot be skipped until predecessors are complete
        val ctpPrematureSkip = ProductionStageLifecycleValidator.validateSkipStage(baseJob, ctpStage.stageId)
        assertTrue(ctpPrematureSkip is DomainResult.Error)

        // CTP can be skipped when predecessors 1..4 are complete
        val stagesThrough4Complete = baseJob.stages.map {
            if (it.sequence <= 4) it.copy(status = ProductionStageStatus.COMPLETED) else it
        }
        val jobWith4Complete = baseJob.copy(stages = stagesThrough4Complete)
        val ctpValidSkip = ProductionStageLifecycleValidator.validateSkipStage(jobWith4Complete, ctpStage.stageId)
        assertTrue(ctpValidSkip is DomainResult.Success<*>)
    }
}

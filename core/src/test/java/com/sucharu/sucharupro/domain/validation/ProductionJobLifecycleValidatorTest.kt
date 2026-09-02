package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStage
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.production.ProductionStageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprehensive unit test suite for [ProductionJobLifecycleValidator].
 */
class ProductionJobLifecycleValidatorTest {

    private val baseJob = ProductionJob(
        jobId = "job-lc-01",
        jobNumber = "JOB-2026-LC01",
        orderId = "ord-lc-01",
        orderNumber = "ORD-2026-LC01",
        customerId = "cus-lc-01",
        handoffId = "hnd-lc-01",
        title = "প্যাকেজিং বক্স প্রিন্টিং",
        quantity = 500,
        unit = "Pcs",
        priority = OrderPriority.NORMAL,
        status = ProductionJobStatus.DRAFT,
        stages = ProductionJobStage.createInitialStages("job-lc-01"),
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    // ─────────────────────────────────────────────────────────────────────────
    // 1. DRAFT State Transitions
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun test01_draft_validTransitions() {
        val job = baseJob.copy(status = ProductionJobStatus.DRAFT)
        assertTrue(ProductionJobLifecycleValidator.validateStatusTransition(job, ProductionJobStatus.READY_FOR_PRODUCTION) is DomainResult.Success<*>)
        assertTrue(ProductionJobLifecycleValidator.validateStatusTransition(job, ProductionJobStatus.CANCELLED) is DomainResult.Success<*>)
    }

    @Test
    fun test02_draft_invalidTransitions() {
        val job = baseJob.copy(status = ProductionJobStatus.DRAFT)
        assertTrue(ProductionJobLifecycleValidator.validateStatusTransition(job, ProductionJobStatus.IN_PROGRESS) is DomainResult.Error)
        assertTrue(ProductionJobLifecycleValidator.validateStatusTransition(job, ProductionJobStatus.ON_HOLD) is DomainResult.Error)
        assertTrue(ProductionJobLifecycleValidator.validateStatusTransition(job, ProductionJobStatus.READY) is DomainResult.Error)
        assertTrue(ProductionJobLifecycleValidator.validateStatusTransition(job, ProductionJobStatus.DELIVERED) is DomainResult.Error)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. READY_FOR_PRODUCTION State Transitions
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun test03_readyForProduction_validTransitions() {
        val job = baseJob.copy(status = ProductionJobStatus.READY_FOR_PRODUCTION)
        assertTrue(ProductionJobLifecycleValidator.validateStatusTransition(job, ProductionJobStatus.IN_PROGRESS) is DomainResult.Success<*>)
        assertTrue(ProductionJobLifecycleValidator.validateStatusTransition(job, ProductionJobStatus.ON_HOLD) is DomainResult.Success<*>)
        assertTrue(ProductionJobLifecycleValidator.validateStatusTransition(job, ProductionJobStatus.CANCELLED) is DomainResult.Success<*>)
    }

    @Test
    fun test04_readyForProduction_invalidTransitions() {
        val job = baseJob.copy(status = ProductionJobStatus.READY_FOR_PRODUCTION)
        assertTrue(ProductionJobLifecycleValidator.validateStatusTransition(job, ProductionJobStatus.DRAFT) is DomainResult.Error)
        assertTrue(ProductionJobLifecycleValidator.validateStatusTransition(job, ProductionJobStatus.READY) is DomainResult.Error)
        assertTrue(ProductionJobLifecycleValidator.validateStatusTransition(job, ProductionJobStatus.DELIVERED) is DomainResult.Error)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. IN_PROGRESS State Transitions
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun test05_inProgress_validTransitions() {
        val job = baseJob.copy(status = ProductionJobStatus.IN_PROGRESS)
        assertTrue(ProductionJobLifecycleValidator.validateStatusTransition(job, ProductionJobStatus.ON_HOLD) is DomainResult.Success<*>)
        assertTrue(ProductionJobLifecycleValidator.validateStatusTransition(job, ProductionJobStatus.CANCELLED) is DomainResult.Success<*>)

        // READY requires stages 1..12 to be completed
        val stagesThrough12Completed = job.stages.map {
            if (it.sequence < 13) it.copy(status = ProductionStageStatus.COMPLETED) else it
        }
        val jobWith12Completed = job.copy(stages = stagesThrough12Completed)
        assertTrue(ProductionJobLifecycleValidator.validateStatusTransition(jobWith12Completed, ProductionJobStatus.READY) is DomainResult.Success<*>)
    }

    @Test
    fun test06_inProgress_invalidTransitions() {
        val job = baseJob.copy(status = ProductionJobStatus.IN_PROGRESS)
        assertTrue(ProductionJobLifecycleValidator.validateStatusTransition(job, ProductionJobStatus.DRAFT) is DomainResult.Error)
        assertTrue(ProductionJobLifecycleValidator.validateStatusTransition(job, ProductionJobStatus.READY_FOR_PRODUCTION) is DomainResult.Error)
        assertTrue(ProductionJobLifecycleValidator.validateStatusTransition(job, ProductionJobStatus.DELIVERED) is DomainResult.Error)

        // READY fails if stages are incomplete
        val readyResult = ProductionJobLifecycleValidator.validateStatusTransition(job, ProductionJobStatus.READY)
        assertTrue(readyResult is DomainResult.Error)
        assertTrue((readyResult as DomainResult.Error).message.contains("Incomplete stages"))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. ON_HOLD State Transitions
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun test07_onHold_validTransitions() {
        val job = baseJob.copy(status = ProductionJobStatus.ON_HOLD)
        assertTrue(ProductionJobLifecycleValidator.validateStatusTransition(job, ProductionJobStatus.IN_PROGRESS) is DomainResult.Success<*>)
        assertTrue(ProductionJobLifecycleValidator.validateStatusTransition(job, ProductionJobStatus.CANCELLED) is DomainResult.Success<*>)
    }

    @Test
    fun test08_onHold_invalidTransitions() {
        val job = baseJob.copy(status = ProductionJobStatus.ON_HOLD)
        assertTrue(ProductionJobLifecycleValidator.validateStatusTransition(job, ProductionJobStatus.DRAFT) is DomainResult.Error)
        assertTrue(ProductionJobLifecycleValidator.validateStatusTransition(job, ProductionJobStatus.READY_FOR_PRODUCTION) is DomainResult.Error)
        assertTrue(ProductionJobLifecycleValidator.validateStatusTransition(job, ProductionJobStatus.READY) is DomainResult.Error)
        assertTrue(ProductionJobLifecycleValidator.validateStatusTransition(job, ProductionJobStatus.DELIVERED) is DomainResult.Error)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. READY & Terminal State Transitions
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun test09_ready_validAndInvalidTransitions() {
        val stagesWithReadyCompleted = baseJob.stages.map {
            if (it.sequence <= 12) it.copy(status = ProductionStageStatus.COMPLETED) else it
        }
        val readyJob = baseJob.copy(
            status = ProductionJobStatus.READY,
            stages = stagesWithReadyCompleted
        )

        // Hold is allowed from READY
        assertTrue(ProductionJobLifecycleValidator.validateStatusTransition(readyJob, ProductionJobStatus.ON_HOLD) is DomainResult.Success<*>)

        // Delivered is allowed when ready stage is complete
        assertTrue(ProductionJobLifecycleValidator.validateStatusTransition(readyJob, ProductionJobStatus.DELIVERED) is DomainResult.Success<*>)

        // Direct transitions to DRAFT, READY_FOR_PRODUCTION, IN_PROGRESS rejected
        assertTrue(ProductionJobLifecycleValidator.validateStatusTransition(readyJob, ProductionJobStatus.DRAFT) is DomainResult.Error)
        assertTrue(ProductionJobLifecycleValidator.validateStatusTransition(readyJob, ProductionJobStatus.READY_FOR_PRODUCTION) is DomainResult.Error)
    }

    @Test
    fun test10_terminalStates_rejectAllTransitions() {
        val deliveredJob = baseJob.copy(status = ProductionJobStatus.DELIVERED)
        val cancelledJob = baseJob.copy(status = ProductionJobStatus.CANCELLED)

        assertTrue(ProductionJobLifecycleValidator.isTerminal(deliveredJob))
        assertTrue(ProductionJobLifecycleValidator.isTerminal(cancelledJob))
        assertFalse(ProductionJobLifecycleValidator.isMutable(deliveredJob))
        assertFalse(ProductionJobLifecycleValidator.isMutable(cancelledJob))

        for (target in ProductionJobStatus.entries) {
            val resDelivered = ProductionJobLifecycleValidator.validateStatusTransition(deliveredJob, target)
            val resCancelled = ProductionJobLifecycleValidator.validateStatusTransition(cancelledJob, target)
            assertTrue(resDelivered is DomainResult.Error)
            assertTrue(resCancelled is DomainResult.Error)
        }
    }

    @Test
    fun test11_selfTransitions_rejected() {
        for (status in ProductionJobStatus.entries) {
            val job = baseJob.copy(status = status)
            val result = ProductionJobLifecycleValidator.validateStatusTransition(job, status)
            assertTrue(result is DomainResult.Error)
            assertTrue((result as DomainResult.Error).message.contains("already in"))
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. Hold, Resume, and Cancellation Rules
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun test12_cancellation_requiresNonBlankReason() {
        val job = baseJob.copy(status = ProductionJobStatus.IN_PROGRESS)

        val emptyReason = ProductionJobLifecycleValidator.validateCancellation(job, "")
        val blankReason = ProductionJobLifecycleValidator.validateCancellation(job, "   ")
        val nullReason = ProductionJobLifecycleValidator.validateCancellation(job, null)
        val validReason = ProductionJobLifecycleValidator.validateCancellation(job, "গ্রাহক নকশা পরিবর্তন করতে চান")

        assertTrue(emptyReason is DomainResult.Error)
        assertTrue(blankReason is DomainResult.Error)
        assertTrue(nullReason is DomainResult.Error)
        assertTrue(validReason is DomainResult.Success<*>)
    }

    @Test
    fun test13_cancellation_rejectedOnTerminalJobs() {
        val deliveredJob = baseJob.copy(status = ProductionJobStatus.DELIVERED)
        val cancelledJob = baseJob.copy(status = ProductionJobStatus.CANCELLED)

        assertTrue(ProductionJobLifecycleValidator.validateCancellation(deliveredJob, "Any reason") is DomainResult.Error)
        assertTrue(ProductionJobLifecycleValidator.validateCancellation(cancelledJob, "Any reason") is DomainResult.Error)
    }

    @Test
    fun test14_holdAndResume_validation() {
        val inProgressJob = baseJob.copy(status = ProductionJobStatus.IN_PROGRESS)
        val onHoldJob = baseJob.copy(status = ProductionJobStatus.ON_HOLD)

        assertTrue(ProductionJobLifecycleValidator.validateHold(inProgressJob) is DomainResult.Success<*>)
        assertTrue(ProductionJobLifecycleValidator.validateResume(onHoldJob) is DomainResult.Success<*>)

        // Cannot resume a job that is not on hold
        assertTrue(ProductionJobLifecycleValidator.validateResume(inProgressJob) is DomainResult.Error)
    }
}

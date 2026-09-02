package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ProductionRework
import com.sucharu.sucharupro.domain.model.qc.ReworkReason
import com.sucharu.sucharupro.domain.model.qc.ReworkStatus
import com.sucharu.sucharupro.domain.model.qc.ReworkType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [ProductionReworkLifecycleValidator] state transitions and terminal rules (Module 06 Step 05).
 */
class ProductionReworkLifecycleTest {

    private fun createRework(status: ReworkStatus): ProductionRework {
        return ProductionRework(
            reworkId = "rew-101",
            projectId = "proj-01",
            productionJobId = "job-01",
            reworkType = ReworkType.CUTTING_CORRECTION,
            reason = ReworkReason.FAILED_QC,
            status = status,
            affectedQuantity = 100,
            quantityUnit = "pcs",
            description = "Trim margin correction",
            requestedBy = "insp-01",
            requestedAt = "2026-08-17T10:00:00Z",
            createdAt = "2026-08-17T10:00:00Z",
            updatedAt = "2026-08-17T10:00:00Z"
        )
    }

    @Test
    fun standardForwardLifecycle_succeeds() {
        val draft = createRework(ReworkStatus.DRAFT)
        assertTrue(ProductionReworkLifecycleValidator.validateStatusTransition(draft, ReworkStatus.REQUESTED) is DomainResult.Success)

        val requested = createRework(ReworkStatus.REQUESTED)
        assertTrue(ProductionReworkLifecycleValidator.validateStatusTransition(requested, ReworkStatus.UNDER_REVIEW) is DomainResult.Success)
        assertTrue(ProductionReworkLifecycleValidator.validateStatusTransition(requested, ReworkStatus.APPROVED) is DomainResult.Success)
        assertTrue(ProductionReworkLifecycleValidator.validateStatusTransition(requested, ReworkStatus.REJECTED) is DomainResult.Success)

        val underReview = createRework(ReworkStatus.UNDER_REVIEW)
        assertTrue(ProductionReworkLifecycleValidator.validateStatusTransition(underReview, ReworkStatus.APPROVED) is DomainResult.Success)
        assertTrue(ProductionReworkLifecycleValidator.validateStatusTransition(underReview, ReworkStatus.REJECTED) is DomainResult.Success)

        val approved = createRework(ReworkStatus.APPROVED)
        assertTrue(ProductionReworkLifecycleValidator.validateStatusTransition(approved, ReworkStatus.ASSIGNED) is DomainResult.Success)

        val assigned = createRework(ReworkStatus.ASSIGNED)
        assertTrue(ProductionReworkLifecycleValidator.validateStatusTransition(assigned, ReworkStatus.IN_PROGRESS) is DomainResult.Success)

        val inProgress = createRework(ReworkStatus.IN_PROGRESS)
        assertTrue(ProductionReworkLifecycleValidator.validateStatusTransition(inProgress, ReworkStatus.COMPLETED) is DomainResult.Success)

        val completed = createRework(ReworkStatus.COMPLETED)
        assertTrue(ProductionReworkLifecycleValidator.validateStatusTransition(completed, ReworkStatus.RETURNED_TO_QC) is DomainResult.Success)
    }

    @Test
    fun invalidBackwardTransitions_fail() {
        val inProgress = createRework(ReworkStatus.IN_PROGRESS)
        val toRequested = ProductionReworkLifecycleValidator.validateStatusTransition(inProgress, ReworkStatus.REQUESTED)
        assertTrue(toRequested is DomainResult.Error)

        val completed = createRework(ReworkStatus.COMPLETED)
        val toInProgress = ProductionReworkLifecycleValidator.validateStatusTransition(completed, ReworkStatus.IN_PROGRESS)
        assertTrue(toInProgress is DomainResult.Error)

        val returned = createRework(ReworkStatus.RETURNED_TO_QC)
        val toCompleted = ProductionReworkLifecycleValidator.validateStatusTransition(returned, ReworkStatus.COMPLETED)
        assertTrue(toCompleted is DomainResult.Error)
    }

    @Test
    fun terminalStates_cannotTransitionOut() {
        val cancelled = createRework(ReworkStatus.CANCELLED)
        val fromCancelled = ProductionReworkLifecycleValidator.validateStatusTransition(cancelled, ReworkStatus.REQUESTED)
        assertTrue(fromCancelled is DomainResult.Error)
        assertTrue((fromCancelled as DomainResult.Error).message.contains("Cannot transition terminal rework"))

        val rejected = createRework(ReworkStatus.REJECTED)
        val fromRejected = ProductionReworkLifecycleValidator.validateStatusTransition(rejected, ReworkStatus.APPROVED)
        assertTrue(fromRejected is DomainResult.Error)
        assertTrue((fromRejected as DomainResult.Error).message.contains("Cannot transition terminal rework"))
    }

    @Test
    fun returnedToQc_isProtectedBoundary_cannotTransitionOutInStep05() {
        val returned = createRework(ReworkStatus.RETURNED_TO_QC)
        val result = ProductionReworkLifecycleValidator.validateStatusTransition(returned, ReworkStatus.IN_PROGRESS)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Protected boundary state"))
    }

    @Test
    fun validateCancellation_requiresReason() {
        val requested = createRework(ReworkStatus.REQUESTED)
        val blankReason = ProductionReworkLifecycleValidator.validateCancellation(requested, "")
        assertTrue(blankReason is DomainResult.Error)
        assertEquals("Cancellation reason cannot be blank.", (blankReason as DomainResult.Error).message)

        val validReason = ProductionReworkLifecycleValidator.validateCancellation(requested, "Cancelled by supervisor")
        assertTrue(validReason is DomainResult.Success)
    }

    @Test
    fun validateRejection_requiresReasonAndValidStatus() {
        val inProgress = createRework(ReworkStatus.IN_PROGRESS)
        val rejectInProgress = ProductionReworkLifecycleValidator.validateRejection(inProgress, "Not needed")
        assertTrue(rejectInProgress is DomainResult.Error)
        assertTrue((rejectInProgress as DomainResult.Error).message.contains("Must be REQUESTED or UNDER_REVIEW"))

        val underReview = createRework(ReworkStatus.UNDER_REVIEW)
        val blankReason = ProductionReworkLifecycleValidator.validateRejection(underReview, "   ")
        assertTrue(blankReason is DomainResult.Error)
        assertEquals("Rejection reason cannot be blank.", (blankReason as DomainResult.Error).message)

        val validRejection = ProductionReworkLifecycleValidator.validateRejection(underReview, "Cost outweighs reprint value")
        assertTrue(validRejection is DomainResult.Success)
    }
}

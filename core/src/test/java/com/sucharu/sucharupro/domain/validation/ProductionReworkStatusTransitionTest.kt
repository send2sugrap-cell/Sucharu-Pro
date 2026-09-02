package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.qc.ReworkStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests exhaustively verifying the state transition matrix of [ReworkStatus] (Module 06 Step 05).
 */
class ProductionReworkStatusTransitionTest {

    @Test
    fun draft_validAndInvalidTransitions() {
        assertTrue(ReworkStatus.DRAFT.canTransitionTo(ReworkStatus.REQUESTED))
        assertTrue(ReworkStatus.DRAFT.canTransitionTo(ReworkStatus.CANCELLED))

        assertFalse(ReworkStatus.DRAFT.canTransitionTo(ReworkStatus.APPROVED))
        assertFalse(ReworkStatus.DRAFT.canTransitionTo(ReworkStatus.IN_PROGRESS))
        assertFalse(ReworkStatus.DRAFT.canTransitionTo(ReworkStatus.COMPLETED))
        assertFalse(ReworkStatus.DRAFT.canTransitionTo(ReworkStatus.RETURNED_TO_QC))
    }

    @Test
    fun requested_validAndInvalidTransitions() {
        assertTrue(ReworkStatus.REQUESTED.canTransitionTo(ReworkStatus.UNDER_REVIEW))
        assertTrue(ReworkStatus.REQUESTED.canTransitionTo(ReworkStatus.APPROVED))
        assertTrue(ReworkStatus.REQUESTED.canTransitionTo(ReworkStatus.REJECTED))
        assertTrue(ReworkStatus.REQUESTED.canTransitionTo(ReworkStatus.CANCELLED))

        assertFalse(ReworkStatus.REQUESTED.canTransitionTo(ReworkStatus.IN_PROGRESS))
        assertFalse(ReworkStatus.REQUESTED.canTransitionTo(ReworkStatus.COMPLETED))
    }

    @Test
    fun underReview_validAndInvalidTransitions() {
        assertTrue(ReworkStatus.UNDER_REVIEW.canTransitionTo(ReworkStatus.APPROVED))
        assertTrue(ReworkStatus.UNDER_REVIEW.canTransitionTo(ReworkStatus.REJECTED))
        assertTrue(ReworkStatus.UNDER_REVIEW.canTransitionTo(ReworkStatus.CANCELLED))

        assertFalse(ReworkStatus.UNDER_REVIEW.canTransitionTo(ReworkStatus.ASSIGNED))
        assertFalse(ReworkStatus.UNDER_REVIEW.canTransitionTo(ReworkStatus.IN_PROGRESS))
    }

    @Test
    fun approved_validAndInvalidTransitions() {
        assertTrue(ReworkStatus.APPROVED.canTransitionTo(ReworkStatus.ASSIGNED))
        assertTrue(ReworkStatus.APPROVED.canTransitionTo(ReworkStatus.CANCELLED))

        assertFalse(ReworkStatus.APPROVED.canTransitionTo(ReworkStatus.IN_PROGRESS))
        assertFalse(ReworkStatus.APPROVED.canTransitionTo(ReworkStatus.COMPLETED))
    }

    @Test
    fun assigned_validAndInvalidTransitions() {
        assertTrue(ReworkStatus.ASSIGNED.canTransitionTo(ReworkStatus.IN_PROGRESS))
        assertTrue(ReworkStatus.ASSIGNED.canTransitionTo(ReworkStatus.APPROVED))
        assertTrue(ReworkStatus.ASSIGNED.canTransitionTo(ReworkStatus.CANCELLED))

        assertFalse(ReworkStatus.ASSIGNED.canTransitionTo(ReworkStatus.COMPLETED))
        assertFalse(ReworkStatus.ASSIGNED.canTransitionTo(ReworkStatus.RETURNED_TO_QC))
    }

    @Test
    fun inProgress_validAndInvalidTransitions() {
        assertTrue(ReworkStatus.IN_PROGRESS.canTransitionTo(ReworkStatus.COMPLETED))
        assertTrue(ReworkStatus.IN_PROGRESS.canTransitionTo(ReworkStatus.CANCELLED))

        assertFalse(ReworkStatus.IN_PROGRESS.canTransitionTo(ReworkStatus.REQUESTED))
        assertFalse(ReworkStatus.IN_PROGRESS.canTransitionTo(ReworkStatus.RETURNED_TO_QC))
    }

    @Test
    fun completed_validAndInvalidTransitions() {
        assertTrue(ReworkStatus.COMPLETED.canTransitionTo(ReworkStatus.RETURNED_TO_QC))

        assertFalse(ReworkStatus.COMPLETED.canTransitionTo(ReworkStatus.IN_PROGRESS))
        assertFalse(ReworkStatus.COMPLETED.canTransitionTo(ReworkStatus.ASSIGNED))
        assertFalse(ReworkStatus.COMPLETED.canTransitionTo(ReworkStatus.REQUESTED))
    }

    @Test
    fun terminalAndBoundary_cannotTransitionToAny() {
        for (target in ReworkStatus.entries) {
            assertFalse(ReworkStatus.CANCELLED.canTransitionTo(target))
            assertFalse(ReworkStatus.REJECTED.canTransitionTo(target))
            assertFalse(ReworkStatus.RETURNED_TO_QC.canTransitionTo(target))
        }
    }
}

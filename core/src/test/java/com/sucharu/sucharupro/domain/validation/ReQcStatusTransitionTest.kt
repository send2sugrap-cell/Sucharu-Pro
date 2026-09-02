package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.qc.ReQcStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Matrix tests verifying all permutations of [ReQcStatus.canTransitionTo] (Module 06 Step 06).
 */
class ReQcStatusTransitionTest {

    @Test
    fun testDraftTransitions() {
        assertTrue(ReQcStatus.DRAFT.canTransitionTo(ReQcStatus.PENDING))
        assertTrue(ReQcStatus.DRAFT.canTransitionTo(ReQcStatus.CANCELLED))
        assertFalse(ReQcStatus.DRAFT.canTransitionTo(ReQcStatus.PASSED))
        assertFalse(ReQcStatus.DRAFT.canTransitionTo(ReQcStatus.FAILED))
    }

    @Test
    fun testPendingTransitions() {
        assertTrue(ReQcStatus.PENDING.canTransitionTo(ReQcStatus.ASSIGNED))
        assertTrue(ReQcStatus.PENDING.canTransitionTo(ReQcStatus.IN_INSPECTION))
        assertTrue(ReQcStatus.PENDING.canTransitionTo(ReQcStatus.CANCELLED))
        assertFalse(ReQcStatus.PENDING.canTransitionTo(ReQcStatus.PASSED))
        assertFalse(ReQcStatus.PENDING.canTransitionTo(ReQcStatus.FAILED))
    }

    @Test
    fun testAssignedTransitions() {
        assertTrue(ReQcStatus.ASSIGNED.canTransitionTo(ReQcStatus.IN_INSPECTION))
        assertTrue(ReQcStatus.ASSIGNED.canTransitionTo(ReQcStatus.PENDING))
        assertTrue(ReQcStatus.ASSIGNED.canTransitionTo(ReQcStatus.CANCELLED))
        assertFalse(ReQcStatus.ASSIGNED.canTransitionTo(ReQcStatus.PASSED))
    }

    @Test
    fun testInInspectionTransitions() {
        assertTrue(ReQcStatus.IN_INSPECTION.canTransitionTo(ReQcStatus.PASSED))
        assertTrue(ReQcStatus.IN_INSPECTION.canTransitionTo(ReQcStatus.FAILED))
        assertTrue(ReQcStatus.IN_INSPECTION.canTransitionTo(ReQcStatus.CANCELLED))
        assertFalse(ReQcStatus.IN_INSPECTION.canTransitionTo(ReQcStatus.PENDING))
    }

    @Test
    fun testFailedTransitions() {
        assertTrue(ReQcStatus.FAILED.canTransitionTo(ReQcStatus.RETURNED_TO_REWORK))
        assertFalse(ReQcStatus.FAILED.canTransitionTo(ReQcStatus.PASSED))
        assertFalse(ReQcStatus.FAILED.canTransitionTo(ReQcStatus.IN_INSPECTION))
    }

    @Test
    fun testPassedIsTerminal() {
        for (status in ReQcStatus.entries) {
            assertFalse(ReQcStatus.PASSED.canTransitionTo(status))
        }
    }

    @Test
    fun testCancelledIsTerminal() {
        for (status in ReQcStatus.entries) {
            assertFalse(ReQcStatus.CANCELLED.canTransitionTo(status))
        }
    }

    @Test
    fun testReturnedToReworkIsProtectedBoundary() {
        for (status in ReQcStatus.entries) {
            assertFalse(ReQcStatus.RETURNED_TO_REWORK.canTransitionTo(status))
        }
    }
}

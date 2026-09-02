package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.qc.FinalQcStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Direct unit tests verifying [FinalQcStatus.canTransitionTo] state machine rules (Module 06 Step 07).
 */
class FinalQcStatusTransitionTest {

    @Test
    fun selfTransition_alwaysAllowed() {
        FinalQcStatus.entries.forEach { status ->
            assertTrue("Self transition for $status should be true", status.canTransitionTo(status))
        }
    }

    @Test
    fun draft_allowedTransitions() {
        assertTrue(FinalQcStatus.DRAFT.canTransitionTo(FinalQcStatus.PENDING))
        assertTrue(FinalQcStatus.DRAFT.canTransitionTo(FinalQcStatus.ASSIGNED))
        assertTrue(FinalQcStatus.DRAFT.canTransitionTo(FinalQcStatus.IN_INSPECTION))
        assertTrue(FinalQcStatus.DRAFT.canTransitionTo(FinalQcStatus.CANCELLED))
        assertFalse(FinalQcStatus.DRAFT.canTransitionTo(FinalQcStatus.RELEASED))
    }

    @Test
    fun inInspection_allowedTransitions() {
        assertTrue(FinalQcStatus.IN_INSPECTION.canTransitionTo(FinalQcStatus.PASSED))
        assertTrue(FinalQcStatus.IN_INSPECTION.canTransitionTo(FinalQcStatus.FAILED))
        assertTrue(FinalQcStatus.IN_INSPECTION.canTransitionTo(FinalQcStatus.BLOCKED))
        assertTrue(FinalQcStatus.IN_INSPECTION.canTransitionTo(FinalQcStatus.CANCELLED))
        assertFalse(FinalQcStatus.IN_INSPECTION.canTransitionTo(FinalQcStatus.RELEASED))
    }

    @Test
    fun passed_allowedTransitions() {
        assertTrue(FinalQcStatus.PASSED.canTransitionTo(FinalQcStatus.RELEASED))
        assertTrue(FinalQcStatus.PASSED.canTransitionTo(FinalQcStatus.BLOCKED))
        assertTrue(FinalQcStatus.PASSED.canTransitionTo(FinalQcStatus.FAILED))
        assertFalse(FinalQcStatus.PASSED.canTransitionTo(FinalQcStatus.DRAFT))
    }

    @Test
    fun terminalStatuses_noTransitionsAllowed() {
        assertFalse(FinalQcStatus.RELEASED.canTransitionTo(FinalQcStatus.PENDING))
        assertFalse(FinalQcStatus.RELEASED.canTransitionTo(FinalQcStatus.IN_INSPECTION))
        assertFalse(FinalQcStatus.CANCELLED.canTransitionTo(FinalQcStatus.PENDING))
        assertFalse(FinalQcStatus.CANCELLED.canTransitionTo(FinalQcStatus.IN_INSPECTION))
    }
}

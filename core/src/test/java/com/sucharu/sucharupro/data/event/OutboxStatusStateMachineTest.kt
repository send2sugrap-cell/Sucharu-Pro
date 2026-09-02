package com.sucharu.sucharupro.data.event

import com.sucharu.sucharupro.data.event.model.OutboxStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OutboxStatusStateMachineTest {

    @Test
    fun test01_pending_legalTransitions() {
        assertTrue(OutboxStatus.PENDING.canTransitionTo(OutboxStatus.PROCESSING))
        assertTrue(OutboxStatus.PENDING.canTransitionTo(OutboxStatus.CANCELLED))
        assertFalse(OutboxStatus.PENDING.canTransitionTo(OutboxStatus.PUBLISHED))
        assertFalse(OutboxStatus.PENDING.canTransitionTo(OutboxStatus.DEAD_LETTER))
    }

    @Test
    fun test02_processing_legalTransitions() {
        assertTrue(OutboxStatus.PROCESSING.canTransitionTo(OutboxStatus.PUBLISHED))
        assertTrue(OutboxStatus.PROCESSING.canTransitionTo(OutboxStatus.RETRY_SCHEDULED))
        assertTrue(OutboxStatus.PROCESSING.canTransitionTo(OutboxStatus.DEAD_LETTER))
        assertTrue(OutboxStatus.PROCESSING.canTransitionTo(OutboxStatus.CANCELLED))
        assertFalse(OutboxStatus.PROCESSING.canTransitionTo(OutboxStatus.PENDING))
    }

    @Test
    fun test03_retryScheduled_legalTransitions() {
        assertTrue(OutboxStatus.RETRY_SCHEDULED.canTransitionTo(OutboxStatus.PROCESSING))
        assertTrue(OutboxStatus.RETRY_SCHEDULED.canTransitionTo(OutboxStatus.CANCELLED))
        assertFalse(OutboxStatus.RETRY_SCHEDULED.canTransitionTo(OutboxStatus.PUBLISHED))
        assertFalse(OutboxStatus.RETRY_SCHEDULED.canTransitionTo(OutboxStatus.DEAD_LETTER))
    }

    @Test
    fun test04_published_isTerminal() {
        assertFalse(OutboxStatus.PUBLISHED.canTransitionTo(OutboxStatus.PROCESSING))
        assertFalse(OutboxStatus.PUBLISHED.canTransitionTo(OutboxStatus.RETRY_SCHEDULED))
        assertFalse(OutboxStatus.PUBLISHED.canTransitionTo(OutboxStatus.DEAD_LETTER))
        assertFalse(OutboxStatus.PUBLISHED.canTransitionTo(OutboxStatus.CANCELLED))
    }

    @Test
    fun test05_deadLetter_canBeReplayedOrCancelled() {
        assertTrue(OutboxStatus.DEAD_LETTER.canTransitionTo(OutboxStatus.RETRY_SCHEDULED))
        assertTrue(OutboxStatus.DEAD_LETTER.canTransitionTo(OutboxStatus.CANCELLED))
        assertFalse(OutboxStatus.DEAD_LETTER.canTransitionTo(OutboxStatus.PROCESSING))
        assertFalse(OutboxStatus.DEAD_LETTER.canTransitionTo(OutboxStatus.PUBLISHED))
    }

    @Test
    fun test06_cancelled_isTerminal() {
        assertFalse(OutboxStatus.CANCELLED.canTransitionTo(OutboxStatus.PENDING))
        assertFalse(OutboxStatus.CANCELLED.canTransitionTo(OutboxStatus.PROCESSING))
        assertFalse(OutboxStatus.CANCELLED.canTransitionTo(OutboxStatus.PUBLISHED))
    }
}

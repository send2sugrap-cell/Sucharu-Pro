package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.returns.ReturnStatus
import com.sucharu.sucharupro.domain.validation.returns.ReturnLifecycleValidator
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [ReturnLifecycleValidator] (Module 11 Step 01).
 *
 * Covers: all valid transitions, all invalid transitions,
 * terminal state enforcement, and self-transition idempotency.
 */
class ReturnLifecycleTest {

    // =========================================================================
    // Valid lifecycle transitions
    // =========================================================================

    @Test
    fun `REQUESTED to UNDER_INSPECTION is valid`() {
        val res = ReturnLifecycleValidator.validateTransition(
            ReturnStatus.REQUESTED, ReturnStatus.UNDER_INSPECTION
        )
        assertTrue(res is DomainResult.Success)
    }

    @Test
    fun `UNDER_INSPECTION to APPROVED is valid`() {
        val res = ReturnLifecycleValidator.validateTransition(
            ReturnStatus.UNDER_INSPECTION, ReturnStatus.APPROVED
        )
        assertTrue(res is DomainResult.Success)
    }

    @Test
    fun `UNDER_INSPECTION to REJECTED is valid`() {
        val res = ReturnLifecycleValidator.validateTransition(
            ReturnStatus.UNDER_INSPECTION, ReturnStatus.REJECTED
        )
        assertTrue(res is DomainResult.Success)
    }

    @Test
    fun `APPROVED to RETURN_RECEIVED is valid`() {
        val res = ReturnLifecycleValidator.validateTransition(
            ReturnStatus.APPROVED, ReturnStatus.RETURN_RECEIVED
        )
        assertTrue(res is DomainResult.Success)
    }

    @Test
    fun `RETURN_RECEIVED to PROCESSED is valid`() {
        val res = ReturnLifecycleValidator.validateTransition(
            ReturnStatus.RETURN_RECEIVED, ReturnStatus.PROCESSED
        )
        assertTrue(res is DomainResult.Success)
    }

    @Test
    fun `full happy path lifecycle succeeds in sequence`() {
        val steps = listOf(
            ReturnStatus.REQUESTED to ReturnStatus.UNDER_INSPECTION,
            ReturnStatus.UNDER_INSPECTION to ReturnStatus.APPROVED,
            ReturnStatus.APPROVED to ReturnStatus.RETURN_RECEIVED,
            ReturnStatus.RETURN_RECEIVED to ReturnStatus.PROCESSED
        )
        for ((from, to) in steps) {
            val res = ReturnLifecycleValidator.validateTransition(from, to)
            assertTrue("Transition $from → $to should be valid", res is DomainResult.Success)
        }
    }

    // =========================================================================
    // Valid cancellation paths
    // =========================================================================

    @Test
    fun `REQUESTED to CANCELLED is valid`() {
        val res = ReturnLifecycleValidator.validateTransition(
            ReturnStatus.REQUESTED, ReturnStatus.CANCELLED
        )
        assertTrue(res is DomainResult.Success)
    }

    @Test
    fun `UNDER_INSPECTION to CANCELLED is valid`() {
        val res = ReturnLifecycleValidator.validateTransition(
            ReturnStatus.UNDER_INSPECTION, ReturnStatus.CANCELLED
        )
        assertTrue(res is DomainResult.Success)
    }

    @Test
    fun `APPROVED to CANCELLED is valid`() {
        val res = ReturnLifecycleValidator.validateTransition(
            ReturnStatus.APPROVED, ReturnStatus.CANCELLED
        )
        assertTrue(res is DomainResult.Success)
    }

    // =========================================================================
    // Self-transition idempotency
    // =========================================================================

    @Test
    fun `self transition is always valid for any status`() {
        for (status in ReturnStatus.entries) {
            val res = ReturnLifecycleValidator.validateTransition(status, status)
            assertTrue("Self-transition of $status should be valid", res is DomainResult.Success)
        }
    }

    // =========================================================================
    // Invalid lifecycle transitions (must be rejected)
    // =========================================================================

    @Test
    fun `REQUESTED to PROCESSED is rejected`() {
        val res = ReturnLifecycleValidator.validateTransition(
            ReturnStatus.REQUESTED, ReturnStatus.PROCESSED
        )
        assertTrue("REQUESTED → PROCESSED must be rejected", res is DomainResult.Error)
    }

    @Test
    fun `REQUESTED to RETURN_RECEIVED is rejected`() {
        val res = ReturnLifecycleValidator.validateTransition(
            ReturnStatus.REQUESTED, ReturnStatus.RETURN_RECEIVED
        )
        assertTrue("REQUESTED → RETURN_RECEIVED must be rejected", res is DomainResult.Error)
    }

    @Test
    fun `REQUESTED to APPROVED is rejected`() {
        val res = ReturnLifecycleValidator.validateTransition(
            ReturnStatus.REQUESTED, ReturnStatus.APPROVED
        )
        assertTrue("REQUESTED → APPROVED must be rejected", res is DomainResult.Error)
    }

    @Test
    fun `REJECTED to APPROVED is rejected`() {
        val res = ReturnLifecycleValidator.validateTransition(
            ReturnStatus.REJECTED, ReturnStatus.APPROVED
        )
        assertTrue("REJECTED → APPROVED must be rejected (terminal)", res is DomainResult.Error)
    }

    @Test
    fun `PROCESSED to REQUESTED is rejected`() {
        val res = ReturnLifecycleValidator.validateTransition(
            ReturnStatus.PROCESSED, ReturnStatus.REQUESTED
        )
        assertTrue("PROCESSED → REQUESTED must be rejected (terminal)", res is DomainResult.Error)
    }

    @Test
    fun `CANCELLED to any non-self state is rejected`() {
        val targets = ReturnStatus.entries.filter { it != ReturnStatus.CANCELLED }
        for (target in targets) {
            val res = ReturnLifecycleValidator.validateTransition(ReturnStatus.CANCELLED, target)
            assertTrue("CANCELLED → $target must be rejected (terminal)", res is DomainResult.Error)
        }
    }

    @Test
    fun `PROCESSED to any non-self state is rejected`() {
        val targets = ReturnStatus.entries.filter { it != ReturnStatus.PROCESSED }
        for (target in targets) {
            val res = ReturnLifecycleValidator.validateTransition(ReturnStatus.PROCESSED, target)
            assertTrue("PROCESSED → $target must be rejected (terminal)", res is DomainResult.Error)
        }
    }

    @Test
    fun `REJECTED to any non-self state is rejected`() {
        val targets = ReturnStatus.entries.filter { it != ReturnStatus.REJECTED }
        for (target in targets) {
            val res = ReturnLifecycleValidator.validateTransition(ReturnStatus.REJECTED, target)
            assertTrue("REJECTED → $target must be rejected (terminal)", res is DomainResult.Error)
        }
    }

    // =========================================================================
    // Error message quality
    // =========================================================================

    @Test
    fun `error message contains from and to status`() {
        val res = ReturnLifecycleValidator.validateTransition(
            ReturnStatus.PROCESSED, ReturnStatus.REQUESTED
        )
        assertTrue(res is DomainResult.Error)
        val msg = (res as DomainResult.Error).message
        assertTrue(msg.contains("PROCESSED"))
        assertTrue(msg.contains("REQUESTED"))
    }
}

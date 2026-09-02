package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.FinalQcInspection
import com.sucharu.sucharupro.domain.model.qc.FinalQcStatus
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [FinalQcLifecycleValidator] (Module 06 Step 07).
 */
class FinalQcLifecycleTest {

    @Test
    fun legalTransitions_pass() {
        val validTransitions = listOf(
            Pair(FinalQcStatus.DRAFT, FinalQcStatus.PENDING),
            Pair(FinalQcStatus.PENDING, FinalQcStatus.ASSIGNED),
            Pair(FinalQcStatus.ASSIGNED, FinalQcStatus.IN_INSPECTION),
            Pair(FinalQcStatus.IN_INSPECTION, FinalQcStatus.PASSED),
            Pair(FinalQcStatus.IN_INSPECTION, FinalQcStatus.FAILED),
            Pair(FinalQcStatus.PASSED, FinalQcStatus.RELEASED),
            Pair(FinalQcStatus.FAILED, FinalQcStatus.BLOCKED)
        )

        for ((from, to) in validTransitions) {
            val result = FinalQcLifecycleValidator.validateTransition(from, to)
            assertTrue("Transition $from -> $to should be valid", result is DomainResult.Success)
        }
    }

    @Test
    fun illegalTransitions_fail() {
        val invalidTransitions = listOf(
            Pair(FinalQcStatus.RELEASED, FinalQcStatus.PENDING),
            Pair(FinalQcStatus.RELEASED, FinalQcStatus.IN_INSPECTION),
            Pair(FinalQcStatus.CANCELLED, FinalQcStatus.PENDING),
            Pair(FinalQcStatus.PENDING, FinalQcStatus.RELEASED),
            Pair(FinalQcStatus.IN_INSPECTION, FinalQcStatus.RELEASED)
        )

        for ((from, to) in invalidTransitions) {
            val result = FinalQcLifecycleValidator.validateTransition(from, to)
            assertTrue("Transition $from -> $to should be invalid", result is DomainResult.Error)
        }
    }

    @Test
    fun terminalImmutability_rejectsReleasedOrCancelled() {
        val released = FinalQcInspection(
            finalQcId = "fqc-01",
            projectId = "proj-01",
            productionJobId = "job-01",
            status = FinalQcStatus.RELEASED,
            totalQuantity = 100,
            createdAt = "2026-08-17T10:00:00Z",
            updatedAt = "2026-08-17T10:00:00Z"
        )
        val result = FinalQcLifecycleValidator.validateTerminalImmutability(released)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("terminal state"))
    }

    @Test
    fun cancellation_failsForPassedOrReleased() {
        val passed = FinalQcInspection(
            finalQcId = "fqc-01",
            projectId = "proj-01",
            productionJobId = "job-01",
            status = FinalQcStatus.PASSED,
            totalQuantity = 100,
            createdAt = "2026-08-17T10:00:00Z",
            updatedAt = "2026-08-17T10:00:00Z"
        )
        val result = FinalQcLifecycleValidator.validateCancellation(passed, "Cancel reason")
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("already PASSED"))
    }
}

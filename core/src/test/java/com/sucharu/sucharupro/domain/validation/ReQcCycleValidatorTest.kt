package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ReQcInspection
import com.sucharu.sucharupro.domain.model.qc.ReQcStatus
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [ReQcCycleValidator] (Module 06 Step 06).
 */
class ReQcCycleValidatorTest {

    private fun createCycle(cycleNumber: Int, status: ReQcStatus, reworkId: String = "rew-001"): ReQcInspection {
        return ReQcInspection(
            reQcId = "reqc-00$cycleNumber",
            productionJobId = "job-001",
            projectId = "proj-001",
            productionReworkId = reworkId,
            cycleNumber = cycleNumber,
            status = status,
            createdBy = "user-001",
            createdAt = "2026-08-17T10:00:00Z",
            updatedAt = "2026-08-17T10:00:00Z"
        )
    }

    @Test
    fun validateCycleNumber_firstCycleMustBe1() {
        val result = ReQcCycleValidator.validateCycleNumber(emptyList(), 1)
        assertTrue(result is DomainResult.Success)

        val badResult = ReQcCycleValidator.validateCycleNumber(emptyList(), 2)
        assertTrue(badResult is DomainResult.Error)
    }

    @Test
    fun validateCycleNumber_sequentialProgression() {
        val existing = listOf(createCycle(1, ReQcStatus.FAILED))
        val result = ReQcCycleValidator.validateCycleNumber(existing, 2)
        assertTrue(result is DomainResult.Success)

        val nonSequential = ReQcCycleValidator.validateCycleNumber(existing, 4)
        assertTrue(nonSequential is DomainResult.Error)
    }

    @Test
    fun validateDuplicateActiveCycle_preventMultipleActive() {
        val existing = listOf(createCycle(1, ReQcStatus.IN_INSPECTION, reworkId = "rew-001"))
        val result = ReQcCycleValidator.validateDuplicateActiveCycle(existing, "rew-001")
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("active Re-QC cycle already exists"))
    }

    @Test
    fun validateDuplicateActiveCycle_allowsWhenPreviousIsReturnedToRework() {
        val existing = listOf(createCycle(1, ReQcStatus.RETURNED_TO_REWORK, reworkId = "rew-001"))
        val result = ReQcCycleValidator.validateDuplicateActiveCycle(existing, "rew-002")
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun validatePreviousCycle_cycleGreaterThan1RequiresValidPrevId() {
        val existing = listOf(createCycle(1, ReQcStatus.FAILED))
        val successRes = ReQcCycleValidator.validatePreviousCycle(2, "reqc-001", existing)
        assertTrue(successRes is DomainResult.Success)

        val missingRes = ReQcCycleValidator.validatePreviousCycle(2, null, existing)
        assertTrue(missingRes is DomainResult.Error)
    }

    @Test
    fun validatePreviousCycle_cannotLinkToPassedCycle() {
        val existing = listOf(createCycle(1, ReQcStatus.PASSED))
        val result = ReQcCycleValidator.validatePreviousCycle(2, "reqc-001", existing)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Must be FAILED or RETURNED_TO_REWORK"))
    }
}

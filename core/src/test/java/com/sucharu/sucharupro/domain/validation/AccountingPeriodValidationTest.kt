package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.finance.AccountingPeriod
import com.sucharu.sucharupro.domain.model.finance.AccountingPeriodStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountingPeriodValidationTest {

    @Test
    fun `validateCreatePayload succeeds for valid parameters`() {
        val result = AccountingPeriodValidator.validateCreatePayload(
            projectId = "PRJ-001",
            periodName = "January 2026",
            startDate = 1000L,
            endDate = 2000L,
            actorId = "USER-01"
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `validateCreatePayload fails when blank projectId or periodName or actorId`() {
        val blankProject = AccountingPeriodValidator.validateCreatePayload("", "Jan 2026", 1000L, 2000L, "U1")
        assertTrue(blankProject is DomainResult.Error)

        val blankName = AccountingPeriodValidator.validateCreatePayload("PRJ-001", "", 1000L, 2000L, "U1")
        assertTrue(blankName is DomainResult.Error)

        val blankActor = AccountingPeriodValidator.validateCreatePayload("PRJ-001", "Jan 2026", 1000L, 2000L, "")
        assertTrue(blankActor is DomainResult.Error)
    }

    @Test
    fun `validateCreatePayload fails when startDate exceeds endDate`() {
        val result = AccountingPeriodValidator.validateCreatePayload(
            projectId = "PRJ-001",
            periodName = "January 2026",
            startDate = 3000L,
            endDate = 2000L,
            actorId = "USER-01"
        )
        assertTrue(result is DomainResult.Error)
        assertEquals("Period start date must be before or equal to end date.", (result as DomainResult.Error).message)
    }

    @Test
    fun `validateNoOverlap detects overlapping dates with existing periods`() {
        val existing = listOf(
            AccountingPeriod(
                periodId = "PER-01",
                periodNo = "PER-2026-0001",
                projectId = "PRJ-001",
                periodName = "Jan 2026",
                startDate = 1000L,
                endDate = 2000L,
                createdBy = "U1"
            )
        )

        val overlapResult = AccountingPeriodValidator.validateNoOverlap(
            newStartDate = 1500L,
            newEndDate = 2500L,
            existingPeriods = existing
        )
        assertTrue(overlapResult is DomainResult.Error)

        val nonOverlapResult = AccountingPeriodValidator.validateNoOverlap(
            newStartDate = 2001L,
            newEndDate = 3000L,
            existingPeriods = existing
        )
        assertTrue(nonOverlapResult is DomainResult.Success)
    }
}

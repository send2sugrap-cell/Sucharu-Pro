package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.finance.AccountingPeriod
import com.sucharu.sucharupro.domain.model.finance.AccountingPeriodStatus
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialPeriodLockBoundaryTest {

    @Test
    fun `validateMutationAllowed blocks transactions in CLOSED accounting periods`() {
        val closedPeriod = AccountingPeriod(
            periodId = "PER-01",
            periodNo = "PER-2026-0001",
            projectId = "PRJ-01",
            periodName = "Jan 2026",
            startDate = 1000L,
            endDate = 2000L,
            status = AccountingPeriodStatus.CLOSED,
            createdBy = "ADMIN"
        )

        val result = FinancialPeriodLockValidator.validateMutationAllowed(
            period = closedPeriod,
            transactionDate = 1500L
        )

        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("CLOSED and LOCKED"))
    }

    @Test
    fun `validateMutationAllowed permits transactions in OPEN period within date bounds`() {
        val openPeriod = AccountingPeriod(
            periodId = "PER-01",
            periodNo = "PER-2026-0001",
            projectId = "PRJ-01",
            periodName = "Jan 2026",
            startDate = 1000L,
            endDate = 2000L,
            status = AccountingPeriodStatus.OPEN,
            createdBy = "ADMIN"
        )

        val withinBounds = FinancialPeriodLockValidator.validateMutationAllowed(
            period = openPeriod,
            transactionDate = 1500L
        )
        assertTrue(withinBounds is DomainResult.Success)

        val outsideBounds = FinancialPeriodLockValidator.validateMutationAllowed(
            period = openPeriod,
            transactionDate = 2500L
        )
        assertTrue(outsideBounds is DomainResult.Error)
    }
}

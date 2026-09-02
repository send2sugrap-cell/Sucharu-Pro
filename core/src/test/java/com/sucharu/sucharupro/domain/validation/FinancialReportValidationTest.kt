package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.*
import com.sucharu.sucharupro.domain.model.user.UserRole
import org.junit.Assert.*
import org.junit.Test

class FinancialReportValidationTest {

    @Test
    fun `validateFilter passes for valid filter with standard period`() {
        val filter = FinancialReportFilter(
            projectId = "PRJ-001",
            reportPeriod = FinancialReportPeriod.CurrentMonth
        )
        val result = FinancialReportControlValidator.validateFilter(filter)
        assertTrue(result is DomainResult.Success)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `custom period constructor throws for invalid date order`() {
        FinancialReportPeriod.Custom(10000L, 5000L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `report filter constructor throws for blank project ID`() {
        FinancialReportFilter(
            projectId = "",
            reportPeriod = FinancialReportPeriod.CurrentMonth
        )
    }

    @Test
    fun `validateFilter fails for wildcard project ID`() {
        val filter = FinancialReportFilter(
            projectId = "*",
            reportPeriod = FinancialReportPeriod.CurrentMonth
        )
        val result = FinancialReportControlValidator.validateFilter(filter)
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun `validateTrialBalance passes when debits equal credits`() {
        val (status, exceptions) = FinancialReportControlValidator.validateTrialBalance(
            totalDebit = Money(50000),
            totalCredit = Money(50000)
        )
        assertEquals(FinancialReportStatus.READY, status)
        assertTrue(exceptions.isEmpty())
    }

    @Test
    fun `validateTrialBalance flags CONTROL_EXCEPTION when debits do not equal credits`() {
        val (status, exceptions) = FinancialReportControlValidator.validateTrialBalance(
            totalDebit = Money(50000),
            totalCredit = Money(48000)
        )
        assertEquals(FinancialReportStatus.CONTROL_EXCEPTION, status)
        assertFalse(exceptions.isEmpty())
    }

    @Test
    fun `validateBalanceSheet passes when assets equal liabilities plus equity`() {
        val (status, exceptions) = FinancialReportControlValidator.validateBalanceSheet(
            totalAssets = Money(100000),
            totalLiabilities = Money(40000),
            totalEquity = Money(60000)
        )
        assertEquals(FinancialReportStatus.READY, status)
        assertTrue(exceptions.isEmpty())
    }

    @Test
    fun `validateBalanceSheet flags CONTROL_EXCEPTION when equation does not hold`() {
        val (status, exceptions) = FinancialReportControlValidator.validateBalanceSheet(
            totalAssets = Money(100000),
            totalLiabilities = Money(40000),
            totalEquity = Money(55000)
        )
        assertEquals(FinancialReportStatus.CONTROL_EXCEPTION, status)
        assertFalse(exceptions.isEmpty())
    }

    @Test
    fun `validateAccess forbids customer role from viewing Trial Balance`() {
        val filter = FinancialReportFilter(projectId = "PRJ-001")
        val result = FinancialReportAuthorizationValidator.validateAccess(
            FinancialReportType.TRIAL_BALANCE,
            filter,
            UserRole.CUSTOMER,
            "CUST-001"
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun `validateAccess allows admin role full access`() {
        val filter = FinancialReportFilter(projectId = "PRJ-001")
        val result = FinancialReportAuthorizationValidator.validateAccess(
            FinancialReportType.TRIAL_BALANCE,
            filter,
            UserRole.ADMIN,
            "ADMIN-001"
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `validateSnapshotGeneration restricts non-privileged roles`() {
        val staffResult = FinancialReportAuthorizationValidator.validateSnapshotGeneration(UserRole.STAFF)
        assertTrue(staffResult is DomainResult.Error)

        val adminResult = FinancialReportAuthorizationValidator.validateSnapshotGeneration(UserRole.ADMIN)
        assertTrue(adminResult is DomainResult.Success)
    }
}

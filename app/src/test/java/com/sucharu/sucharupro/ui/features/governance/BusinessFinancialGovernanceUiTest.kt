package com.sucharu.sucharupro.ui.features.governance

import com.sucharu.sucharupro.data.api.model.businessfinancialgovernance.BusinessFinancialBudgetDto
import com.sucharu.sucharupro.data.api.model.businessfinancialgovernance.ExecutiveGovernanceOverviewDto
import com.sucharu.sucharupro.domain.model.businessfinancialgovernance.BusinessFinancialBudgetDimensionType
import com.sucharu.sucharupro.domain.model.businessfinancialgovernance.BusinessFinancialBudgetStatus
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class BusinessFinancialGovernanceUiTest {

    @Test
    fun `test GovernanceTab enumeration contains expected values`() {
        val tabs = GovernanceTab.values()
        assertEquals(7, tabs.size)
        assertTrue(tabs.any { it.name == "OVERVIEW" })
        assertTrue(tabs.any { it.name == "BUDGETS" })
        assertTrue(tabs.any { it.name == "VARIANCE" })
        assertTrue(tabs.any { it.name == "FORECAST" })
        assertTrue(tabs.any { it.name == "ALERTS" })
        assertTrue(tabs.any { it.name == "AUDIT" })
        assertTrue(tabs.any { it.name == "INTEGRITY" })
    }

    @Test
    fun `test ExecutiveGovernanceOverviewDto data structure`() {
        val overview = ExecutiveGovernanceOverviewDto(
            tenantId = "T1",
            projectId = "P1",
            periodId = "2026-Q1",
            currency = "BDT",
            totalActiveBudgetsCount = 5,
            totalAllocatedBudgetAmount = BigDecimal("500000.0000"),
            totalActualSpendAmount = BigDecimal("320000.0000"),
            totalCommittedExposureAmount = BigDecimal("40000.0000"),
            totalAccruedExposureAmount = BigDecimal("15000.0000"),
            totalProjectedExposureAmount = BigDecimal("375000.0000"),
            totalRemainingBudgetAmount = BigDecimal("180000.0000"),
            overallUtilizationPercentage = BigDecimal("64.0000"),
            activeThresholdsCount = 2,
            openAlertsCount = 1,
            criticalAlertsCount = 0,
            warningAlertsCount = 1,
            comparisons = emptyList(),
            alerts = emptyList(),
            forecasts = emptyList()
        )

        assertEquals(5, overview.totalActiveBudgetsCount)
        assertEquals(BigDecimal("500000.0000"), overview.totalAllocatedBudgetAmount)
        assertEquals(BigDecimal("180000.0000"), overview.totalRemainingBudgetAmount)
    }
}

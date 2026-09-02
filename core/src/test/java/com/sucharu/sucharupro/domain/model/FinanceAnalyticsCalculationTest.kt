package com.sucharu.sucharupro.domain.model

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.*
import org.junit.Assert.*
import org.junit.Test

class FinanceAnalyticsCalculationTest {

    @Test
    fun `FinancialHealthEngine calculates excellent score for high profitability and liquidity`() {
        val health = FinancialHealthEngine.calculateHealthScore(
            revenue = Money(200000),
            expenses = Money(100000),
            netProfit = Money(100000),
            cashPosition = Money(150000),
            totalReceivable = Money(20000),
            overdueReceivable = Money.ZERO,
            totalPayable = Money(10000),
            overduePayable = Money.ZERO,
            collectionRate = 95.0,
            settlementRate = 100.0,
            discrepancyCount = 0,
            isTrialBalanced = true,
            isBalanceSheetBalanced = true
        )

        assertTrue(health.score >= 85)
        assertEquals(FinancialHealthStatus.EXCELLENT, health.status)
        assertTrue(health.criticalIndicators.isEmpty())
    }

    @Test
    fun `FinancialHealthEngine flags critical status on negative cash and high overdues`() {
        val health = FinancialHealthEngine.calculateHealthScore(
            revenue = Money(100000),
            expenses = Money(120000),
            netProfit = Money(-20000),
            cashPosition = Money(-5000),
            totalReceivable = Money(80000),
            overdueReceivable = Money(50000), // > 50% overdue
            totalPayable = Money(60000),
            overduePayable = Money(40000),
            collectionRate = 30.0,
            settlementRate = 40.0,
            discrepancyCount = 8,
            isTrialBalanced = false,
            isBalanceSheetBalanced = false
        )

        assertTrue(health.score < 40)
        assertEquals(FinancialHealthStatus.CRITICAL, health.status)
        assertFalse(health.criticalIndicators.isEmpty())
    }

    @Test
    fun `FinancialForecastEngine generates deterministic moving average forecast`() {
        val historicalRev = listOf(Money(10000), Money(20000), Money(30000))
        val historicalExp = listOf(Money(5000), Money(10000), Money(15000))

        val forecast = FinancialForecastEngine.generateForecast(
            projectId = "PRJ-01",
            historicalRevenue = historicalRev,
            historicalExpenses = historicalExp,
            baselinePeriodLabel = "Q1 Baseline",
            forecastPeriodLabel = "Q2 Projection",
            method = ForecastMethod.MOVING_AVERAGE
        )

        // Weighted: 1*10000 + 2*20000 + 3*30000 = 140000 / 6 = 23333.33
        assertEquals(Money(23333.33), forecast.projectedRevenue)
        assertEquals(Money(11666.67), forecast.projectedExpenses)
        assertEquals(Money(11666.66), forecast.projectedNetProfit)
    }

    @Test
    fun `FinancialForecastEngine historical average calculation`() {
        val historicalRev = listOf(Money(10000), Money(20000), Money(30000))
        val historicalExp = listOf(Money(6000), Money(6000), Money(6000))

        val forecast = FinancialForecastEngine.generateForecast(
            projectId = "PRJ-01",
            historicalRevenue = historicalRev,
            historicalExpenses = historicalExp,
            baselinePeriodLabel = "Base",
            forecastPeriodLabel = "Next",
            method = ForecastMethod.HISTORICAL_AVERAGE
        )

        assertEquals(Money(20000), forecast.projectedRevenue)
        assertEquals(Money(6000), forecast.projectedExpenses)
        assertEquals(Money(14000), forecast.projectedNetProfit)
    }
}

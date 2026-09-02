package com.sucharu.sucharupro.domain.model.finance

import com.sucharu.sucharupro.domain.model.common.Money
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Pure deterministic management forecast engine (Module 09 Step 10).
 *
 * MANAGEMENT ESTIMATES ONLY. Never written to the financial ledger.
 */
object FinancialForecastEngine {

    fun generateForecast(
        projectId: String,
        historicalRevenue: List<Money>,
        historicalExpenses: List<Money>,
        baselinePeriodLabel: String,
        forecastPeriodLabel: String,
        method: ForecastMethod = ForecastMethod.MOVING_AVERAGE
    ): FinancialForecastSummary {
        val projRev = when {
            historicalRevenue.isEmpty() -> Money.ZERO
            method == ForecastMethod.HISTORICAL_AVERAGE -> {
                val sum = historicalRevenue.fold(Money.ZERO) { acc, m -> acc.plus(m) }
                sum.div(historicalRevenue.size)
            }
            method == ForecastMethod.MOVING_AVERAGE -> {
                // Weighted: recent periods have higher weight
                var weightedSum = BigDecimal.ZERO
                var totalWeight = 0
                for (i in historicalRevenue.indices) {
                    val weight = i + 1
                    weightedSum = weightedSum.add(historicalRevenue[i].amount.multiply(BigDecimal.valueOf(weight.toLong())))
                    totalWeight += weight
                }
                val avg = weightedSum.divide(BigDecimal.valueOf(totalWeight.toLong()), 2, RoundingMode.HALF_UP)
                Money(avg)
            }
            else -> historicalRevenue.lastOrNull() ?: Money.ZERO
        }

        val projExp = when {
            historicalExpenses.isEmpty() -> Money.ZERO
            method == ForecastMethod.HISTORICAL_AVERAGE -> {
                val sum = historicalExpenses.fold(Money.ZERO) { acc, m -> acc.plus(m) }
                sum.div(historicalExpenses.size)
            }
            method == ForecastMethod.MOVING_AVERAGE -> {
                var weightedSum = BigDecimal.ZERO
                var totalWeight = 0
                for (i in historicalExpenses.indices) {
                    val weight = i + 1
                    weightedSum = weightedSum.add(historicalExpenses[i].amount.multiply(BigDecimal.valueOf(weight.toLong())))
                    totalWeight += weight
                }
                val avg = weightedSum.divide(BigDecimal.valueOf(totalWeight.toLong()), 2, RoundingMode.HALF_UP)
                Money(avg)
            }
            else -> historicalExpenses.lastOrNull() ?: Money.ZERO
        }

        val projProfit = projRev.minus(projExp)
        val projCash = projProfit

        return FinancialForecastSummary(
            projectId = projectId,
            baselinePeriodLabel = baselinePeriodLabel,
            forecastPeriodLabel = forecastPeriodLabel,
            projectedRevenue = projRev,
            projectedExpenses = projExp,
            projectedNetProfit = projProfit,
            projectedCashFlow = projCash,
            method = method,
            confidenceLevel = if (historicalRevenue.size >= 3) "HIGH_CONFIDENCE_ESTIMATE" else "LOW_SAMPLE_ESTIMATE"
        )
    }
}

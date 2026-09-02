package com.sucharu.sucharupro.domain.model.profitability

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Pure mathematical and precision utility for Profitability & Cost Analysis (Module 16 Step 01).
 * Enforces strict BigDecimal arithmetic (scale 4, RoundingMode.HALF_UP) with zero-division safeguards.
 */
object ProfitabilityMathUtils {

    const val SCALE = 4
    val ROUNDING_MODE = RoundingMode.HALF_UP
    val ONE_HUNDRED = BigDecimal("100.0000").setScale(SCALE, ROUNDING_MODE)
    val ZERO_MONEY = BigDecimal.ZERO.setScale(SCALE, ROUNDING_MODE)

    /**
     * Standardizes a BigDecimal to 4 decimal places with HALF_UP rounding.
     */
    fun scaleMoney(value: BigDecimal?): BigDecimal {
        if (value == null) return ZERO_MONEY
        return value.setScale(SCALE, ROUNDING_MODE)
    }

    /**
     * Calculates Total Cost = Direct Cost + Indirect Cost.
     */
    fun calculateTotalCost(directCost: BigDecimal, indirectCost: BigDecimal): BigDecimal {
        val direct = scaleMoney(directCost)
        val indirect = scaleMoney(indirectCost)
        return direct.add(indirect).setScale(SCALE, ROUNDING_MODE)
    }

    /**
     * Calculates Gross Profit = Revenue - Total Cost.
     */
    fun calculateGrossProfit(revenue: BigDecimal, totalCost: BigDecimal): BigDecimal {
        val rev = scaleMoney(revenue)
        val cost = scaleMoney(totalCost)
        return rev.subtract(cost).setScale(SCALE, ROUNDING_MODE)
    }

    /**
     * Calculates Gross Margin % = (Gross Profit / Revenue) * 100.
     * Safely returns 0.0000 when revenue is zero or negative, preventing division by zero or NaN/Infinity.
     */
    fun calculateGrossMarginPercentage(grossProfit: BigDecimal, revenue: BigDecimal): BigDecimal {
        val rev = scaleMoney(revenue)
        val profit = scaleMoney(grossProfit)

        if (rev.compareTo(BigDecimal.ZERO) <= 0) {
            return ZERO_MONEY
        }

        return profit
            .multiply(ONE_HUNDRED)
            .divide(rev, SCALE, ROUNDING_MODE)
    }

    /**
     * Calculates Cost Variance = Actual Cost - Baseline Cost (if baseline is provided).
     */
    fun calculateCostVariance(actualCost: BigDecimal, baselineCost: BigDecimal?): BigDecimal? {
        if (baselineCost == null) return null
        val actual = scaleMoney(actualCost)
        val baseline = scaleMoney(baselineCost)
        return actual.subtract(baseline).setScale(SCALE, ROUNDING_MODE)
    }

    /**
     * Computes full metric suite deterministically from components.
     */
    fun computeMetrics(
        revenue: BigDecimal,
        directCost: BigDecimal,
        indirectCost: BigDecimal = BigDecimal.ZERO,
        baselineCost: BigDecimal? = null,
        baselineRevenue: BigDecimal? = null
    ): ProfitabilityMetric {
        val rev = scaleMoney(revenue)
        val direct = scaleMoney(directCost)
        val indirect = scaleMoney(indirectCost)
        val total = calculateTotalCost(direct, indirect)
        val profit = calculateGrossProfit(rev, total)
        val marginPct = calculateGrossMarginPercentage(profit, rev)
        val costVar = calculateCostVariance(total, baselineCost)
        val revVar = baselineRevenue?.let { rev.subtract(scaleMoney(it)).setScale(SCALE, ROUNDING_MODE) }
        val marginVar = if (baselineRevenue != null && baselineCost != null && baselineRevenue.compareTo(BigDecimal.ZERO) > 0) {
            val baseProfit = scaleMoney(baselineRevenue).subtract(scaleMoney(baselineCost))
            val baseMargin = calculateGrossMarginPercentage(baseProfit, baselineRevenue)
            marginPct.subtract(baseMargin).setScale(SCALE, ROUNDING_MODE)
        } else null

        return ProfitabilityMetric(
            revenue = rev,
            directCost = direct,
            indirectCost = indirect,
            totalCost = total,
            grossProfit = profit,
            grossMarginPercentage = marginPct,
            baselineCost = baselineCost?.let { scaleMoney(it) },
            costVariance = costVar,
            revenueVariance = revVar,
            marginVariance = marginVar
        )
    }

    /**
     * Aggregates cost component breakdowns from a list of attribution references.
     */
    fun aggregateCostBreakdowns(
        attributions: List<CostAttributionReference>,
        totalCost: BigDecimal
    ): List<CostComponentBreakdown> {
        if (attributions.isEmpty()) return emptyList()

        val grouped = attributions.groupBy { it.componentType }
        val scaledTotal = scaleMoney(totalCost)

        return grouped.map { (compType, items) ->
            val sum = items.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.attributableAmount) }
                .setScale(SCALE, ROUNDING_MODE)
            val pct = if (scaledTotal.compareTo(BigDecimal.ZERO) > 0) {
                sum.multiply(ONE_HUNDRED).divide(scaledTotal, SCALE, ROUNDING_MODE)
            } else {
                ZERO_MONEY
            }
            CostComponentBreakdown(
                componentType = compType,
                totalAmount = sum,
                percentageOfTotalCost = pct,
                itemCount = items.size
            )
        }.sortedByDescending { it.totalAmount }
    }
}

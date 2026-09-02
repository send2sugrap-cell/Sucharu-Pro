package com.sucharu.sucharupro.domain.model.finance

import com.sucharu.sucharupro.domain.model.common.Money
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Pure, deterministic calculation engine for management KPIs (Module 09 Step 09).
 *
 * All formulas handle zero-denominator deterministically (returns null rather than NaN / Infinity).
 * Never mutates input data.
 */
object FinancialKpiCalculator {

    /**
     * Calculate Collection Rate % = (totalCollected / totalRevenue) * 100.
     * If totalRevenue <= 0, returns null.
     */
    fun calculateCollectionRate(totalCollected: Money, totalRevenue: Money): Double? {
        if (totalRevenue.amount <= BigDecimal.ZERO) return null
        val rate = totalCollected.amount
            .multiply(BigDecimal(100))
            .divide(totalRevenue.amount, 2, RoundingMode.HALF_EVEN)
            .toDouble()
        return if (rate.isFinite()) rate else null
    }

    /**
     * Calculate Net Profit Margin % = (netProfit / totalRevenue) * 100.
     * If totalRevenue <= 0, returns null.
     */
    fun calculateNetProfitMargin(netProfit: Money, totalRevenue: Money): Double? {
        if (totalRevenue.amount <= BigDecimal.ZERO) return null
        val margin = netProfit.amount
            .multiply(BigDecimal(100))
            .divide(totalRevenue.amount, 2, RoundingMode.HALF_EVEN)
            .toDouble()
        return if (margin.isFinite()) margin else null
    }

    /**
     * Calculate Expense Ratio % = (totalExpenses / totalRevenue) * 100.
     * If totalRevenue <= 0, returns null.
     */
    fun calculateExpenseRatio(totalExpenses: Money, totalRevenue: Money): Double? {
        if (totalRevenue.amount <= BigDecimal.ZERO) return null
        val ratio = totalExpenses.amount
            .multiply(BigDecimal(100))
            .divide(totalRevenue.amount, 2, RoundingMode.HALF_EVEN)
            .toDouble()
        return if (ratio.isFinite()) ratio else null
    }

    /**
     * Calculate Overdue Receivable Ratio % = (overdueReceivable / totalReceivable) * 100.
     * If totalReceivable <= 0, returns null.
     */
    fun calculateOverdueReceivableRatio(overdueReceivable: Money, totalReceivable: Money): Double? {
        if (totalReceivable.amount <= BigDecimal.ZERO) return null
        val ratio = overdueReceivable.amount
            .multiply(BigDecimal(100))
            .divide(totalReceivable.amount, 2, RoundingMode.HALF_EVEN)
            .toDouble()
        return if (ratio.isFinite()) ratio else null
    }

    /**
     * Calculate Overdue Payable Ratio % = (overduePayable / totalPayable) * 100.
     * If totalPayable <= 0, returns null.
     */
    fun calculateOverduePayableRatio(overduePayable: Money, totalPayable: Money): Double? {
        if (totalPayable.amount <= BigDecimal.ZERO) return null
        val ratio = overduePayable.amount
            .multiply(BigDecimal(100))
            .divide(totalPayable.amount, 2, RoundingMode.HALF_EVEN)
            .toDouble()
        return if (ratio.isFinite()) ratio else null
    }
}

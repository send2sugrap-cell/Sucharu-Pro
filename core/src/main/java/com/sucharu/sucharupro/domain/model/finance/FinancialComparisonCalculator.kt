package com.sucharu.sucharupro.domain.model.finance

import com.sucharu.sucharupro.domain.model.common.Money
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Pure, deterministic calculation engine for period-over-period financial comparisons (Module 09 Step 09).
 */
object FinancialComparisonCalculator {

    /**
     * Calculate absolute and percentage change between Period A and Period B.
     * absoluteChange = B - A
     * percentageChange = ((B - A) / A) * 100
     * If A == 0, percentageChange is null.
     */
    fun calculateChange(valA: Money, valB: Money): Pair<Money, Double?> {
        val diff = valB.minus(valA)
        if (valA.amount.compareTo(BigDecimal.ZERO) == 0) {
            return diff to null
        }
        val pct = diff.amount
            .multiply(BigDecimal(100))
            .divide(valA.amount.abs(), 2, RoundingMode.HALF_EVEN)
            .toDouble()
        return diff to (if (pct.isFinite()) pct else null)
    }
}

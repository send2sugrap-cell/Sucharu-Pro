package com.sucharu.sucharupro.domain.service.vendor

import com.sucharu.sucharupro.domain.model.common.Money
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Pure, deterministic mathematical calculations for Vendor Settlement & Analytics (Module 12 Step 10).
 */
object VendorSettlementCalculator {

    /**
     * Outstanding Amount = Approved Payable - Previously Settled Amount - Credits/Adjustments.
     * Guaranteed non-negative.
     */
    fun calculateOutstandingAmount(
        approvedPayable: Money,
        previouslySettled: Money,
        credits: Money = Money.ZERO
    ): Money {
        val totalDeductions = previouslySettled + credits
        val balance = approvedPayable - totalDeductions
        return if (balance.isNegative()) Money.ZERO else balance
    }

    /**
     * Calculate variance between expected amount and actual settled / ledger amounts.
     */
    fun calculateVariance(
        expectedAmount: Money,
        actualAmount: Money
    ): Money {
        return (expectedAmount - actualAmount).abs()
    }

    /**
     * Compute rate as percentage, zero-safe.
     */
    fun calculateRatePercentage(numerator: Double, denominator: Double): Double {
        if (denominator <= 0.0) return 100.0
        val result = (numerator / denominator) * 100.0
        return BigDecimal(result).setScale(2, RoundingMode.HALF_UP).toDouble().coerceIn(0.0, 100.0)
    }

    /**
     * Compute defect/rejection rate as percentage, zero-safe (defaults to 0.0 if denominator is 0).
     */
    fun calculateDefectRatePercentage(defectiveQty: Double, totalQty: Double): Double {
        if (totalQty <= 0.0) return 0.0
        val result = (defectiveQty / totalQty) * 100.0
        return BigDecimal(result).setScale(2, RoundingMode.HALF_UP).toDouble().coerceIn(0.0, 100.0)
    }

    /**
     * Compute average invoice processing / payment cycle days.
     */
    fun calculateAverageDays(totalDays: Double, count: Int): Double {
        if (count <= 0) return 0.0
        return BigDecimal(totalDays / count.toDouble()).setScale(2, RoundingMode.HALF_UP).toDouble()
    }
}

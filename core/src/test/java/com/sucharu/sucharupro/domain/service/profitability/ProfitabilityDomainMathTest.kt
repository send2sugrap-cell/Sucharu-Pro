package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.profitability.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class ProfitabilityDomainMathTest {

    @Test
    fun testMoneyScaling() {
        val scaled = ProfitabilityMathUtils.scaleMoney(BigDecimal("123.456789"))
        assertEquals(BigDecimal("123.4568"), scaled)
        assertEquals(4, scaled.scale())
    }

    @Test
    fun testTotalCostCalculation() {
        val direct = BigDecimal("1000.5000")
        val indirect = BigDecimal("250.2500")
        val total = ProfitabilityMathUtils.calculateTotalCost(direct, indirect)
        assertEquals(BigDecimal("1250.7500"), total)
    }

    @Test
    fun testGrossProfitCalculation() {
        val revenue = BigDecimal("5000.0000")
        val totalCost = BigDecimal("3200.0000")
        val profit = ProfitabilityMathUtils.calculateGrossProfit(revenue, totalCost)
        assertEquals(BigDecimal("1800.0000"), profit)
    }

    @Test
    fun testGrossMarginPercentageCalculation() {
        val revenue = BigDecimal("5000.0000")
        val profit = BigDecimal("1800.0000")
        val margin = ProfitabilityMathUtils.calculateGrossMarginPercentage(profit, revenue)
        // 1800 / 5000 * 100 = 36.0000%
        assertEquals(BigDecimal("36.0000"), margin)
    }

    @Test
    fun testZeroRevenueSafeHandling() {
        val profit = BigDecimal("0.0000")
        val zeroRevenue = BigDecimal.ZERO
        val margin = ProfitabilityMathUtils.calculateGrossMarginPercentage(profit, zeroRevenue)
        assertEquals(BigDecimal("0.0000"), margin)
        assertFalse(margin.toDouble().isNaN())
        assertFalse(margin.toDouble().isInfinite())
    }

    @Test
    fun testNegativeRevenueSafeHandling() {
        val profit = BigDecimal("-500.0000")
        val negRevenue = BigDecimal("-1000.0000")
        val margin = ProfitabilityMathUtils.calculateGrossMarginPercentage(profit, negRevenue)
        assertEquals(BigDecimal("0.0000"), margin)
    }

    @Test
    fun testNegativeProfitHandling() {
        val revenue = BigDecimal("2000.0000")
        val cost = BigDecimal("2500.0000")
        val profit = ProfitabilityMathUtils.calculateGrossProfit(revenue, cost)
        assertEquals(BigDecimal("-500.0000"), profit)

        val margin = ProfitabilityMathUtils.calculateGrossMarginPercentage(profit, revenue)
        // -500 / 2000 * 100 = -25.0000%
        assertEquals(BigDecimal("-25.0000"), margin)
    }

    @Test
    fun testCostVarianceCalculation() {
        val actual = BigDecimal("12000.0000")
        val baseline = BigDecimal("10000.0000")
        val variance = ProfitabilityMathUtils.calculateCostVariance(actual, baseline)
        assertNotNull(variance)
        assertEquals(BigDecimal("2000.0000"), variance)

        val nullBaselineVariance = ProfitabilityMathUtils.calculateCostVariance(actual, null)
        assertNull(nullBaselineVariance)
    }

    @Test
    fun testComputeMetricsSuite() {
        val metrics = ProfitabilityMathUtils.computeMetrics(
            revenue = BigDecimal("100000.0000"),
            directCost = BigDecimal("60000.0000"),
            indirectCost = BigDecimal("10000.0000"),
            baselineCost = BigDecimal("65000.0000"),
            baselineRevenue = BigDecimal("95000.0000")
        )

        assertEquals(BigDecimal("100000.0000"), metrics.revenue)
        assertEquals(BigDecimal("60000.0000"), metrics.directCost)
        assertEquals(BigDecimal("10000.0000"), metrics.indirectCost)
        assertEquals(BigDecimal("70000.0000"), metrics.totalCost)
        assertEquals(BigDecimal("30000.0000"), metrics.grossProfit)
        assertEquals(BigDecimal("30.0000"), metrics.grossMarginPercentage)
        assertEquals(BigDecimal("5000.0000"), metrics.costVariance) // 70000 - 65000
    }

    @Test
    fun testCostBreakdownAggregation() {
        val attributions = listOf(
            CostAttributionReference("A1", "T1", "P1", CostAttributionSourceType.EXPENSE, "E1", CostComponentType.MATERIAL, attributableAmount = BigDecimal("30000.0000")),
            CostAttributionReference("A2", "T1", "P1", CostAttributionSourceType.EXPENSE, "E2", CostComponentType.MATERIAL, attributableAmount = BigDecimal("20000.0000")),
            CostAttributionReference("A3", "T1", "P1", CostAttributionSourceType.PAYABLE, "P1", CostComponentType.LABOUR, attributableAmount = BigDecimal("25000.0000")),
            CostAttributionReference("A4", "T1", "P1", CostAttributionSourceType.JOB_COST, "J1", CostComponentType.MACHINE, attributableAmount = BigDecimal("25000.0000"))
        )

        val totalCost = BigDecimal("100000.0000")
        val breakdowns = ProfitabilityMathUtils.aggregateCostBreakdowns(attributions, totalCost)

        assertEquals(3, breakdowns.size)
        val material = breakdowns.first { it.componentType == CostComponentType.MATERIAL }
        assertEquals(BigDecimal("50000.0000"), material.totalAmount)
        assertEquals(BigDecimal("50.0000"), material.percentageOfTotalCost)
        assertEquals(2, material.itemCount)

        val labour = breakdowns.first { it.componentType == CostComponentType.LABOUR }
        assertEquals(BigDecimal("25000.0000"), labour.totalAmount)
        assertEquals(BigDecimal("25.0000"), labour.percentageOfTotalCost)
        assertEquals(1, labour.itemCount)
    }
}

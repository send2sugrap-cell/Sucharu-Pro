package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.profitability.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class ProductProfitabilityDomainMathTest {

    @Test
    fun testMoneyScaling() {
        val scaled = ProductProfitabilityMathUtils.scaleMoney(BigDecimal("1234.56789"))
        assertEquals(BigDecimal("1234.5679"), scaled)
        assertEquals(4, scaled.scale())
    }

    @Test
    fun testGrossProfitCalculation() {
        val rev = BigDecimal("50000.0000")
        val cost = BigDecimal("35000.0000")
        val gp = ProductProfitabilityMathUtils.calculateGrossProfit(rev, cost)
        assertEquals(BigDecimal("15000.0000"), gp)
    }

    @Test
    fun testGrossMarginPercentageCalculation() {
        val rev = BigDecimal("100000.0000")
        val cost = BigDecimal("70000.0000")
        val margin = ProductProfitabilityMathUtils.calculateGrossMarginPercentage(rev, cost)
        assertNotNull(margin)
        assertEquals(BigDecimal("30.0000"), margin)
    }

    @Test
    fun testGrossMarginPercentageWithZeroRevenueReturnsNull() {
        val rev = BigDecimal("0.0000")
        val cost = BigDecimal("5000.0000")
        val margin = ProductProfitabilityMathUtils.calculateGrossMarginPercentage(rev, cost)
        assertNull(margin)
    }

    @Test
    fun testUnitEconomicsZeroQuantitySafety() {
        val ue = ProductProfitabilityMathUtils.calculateUnitEconomics(
            quantity = 0,
            recognizedRevenue = BigDecimal("10000.0000"),
            totalActualCost = BigDecimal("7000.0000"),
            components = emptyList()
        )
        assertEquals("UNIT_METRIC_UNAVAILABLE", ue.unitMetricStatus)
        assertNull(ue.unitRevenue)
        assertNull(ue.unitActualCost)
        assertNull(ue.unitGrossProfit)
    }

    @Test
    fun testUnitEconomicsCalculationWithPositiveQuantity() {
        val comps = listOf(
            ProductCostBreakdownItem(componentType = JobCostComponentType.MATERIAL_COST, amount = BigDecimal("2000.0000")),
            ProductCostBreakdownItem(componentType = JobCostComponentType.LABOUR_COST, amount = BigDecimal("1000.0000"))
        )
        val ue = ProductProfitabilityMathUtils.calculateUnitEconomics(
            quantity = 100,
            recognizedRevenue = BigDecimal("5000.0000"),
            totalActualCost = BigDecimal("3000.0000"),
            components = comps
        )
        assertEquals("AVAILABLE", ue.unitMetricStatus)
        assertEquals(BigDecimal("50.0000"), ue.unitRevenue)
        assertEquals(BigDecimal("30.0000"), ue.unitActualCost)
        assertEquals(BigDecimal("20.0000"), ue.unitGrossProfit)
        assertEquals(BigDecimal("20.0000"), ue.unitMaterialCost)
        assertEquals(BigDecimal("10.0000"), ue.unitLabourCost)
    }

    @Test
    fun testProfitabilityClassificationThresholds() {
        // Highly profitable: >= 30%
        val c1 = ProductProfitabilityMathUtils.classifyProfitability(
            recognizedRevenue = BigDecimal("100.0000"),
            totalActualCost = BigDecimal("65.0000"),
            grossMarginPercentage = BigDecimal("35.0000")
        )
        assertEquals(ProductProfitabilityClassification.HIGHLY_PROFITABLE, c1)

        // Profitable: 15% - 30%
        val c2 = ProductProfitabilityMathUtils.classifyProfitability(
            recognizedRevenue = BigDecimal("100.0000"),
            totalActualCost = BigDecimal("80.0000"),
            grossMarginPercentage = BigDecimal("20.0000")
        )
        assertEquals(ProductProfitabilityClassification.PROFITABLE, c2)

        // Low margin: 0% - 15%
        val c3 = ProductProfitabilityMathUtils.classifyProfitability(
            recognizedRevenue = BigDecimal("100.0000"),
            totalActualCost = BigDecimal("95.0000"),
            grossMarginPercentage = BigDecimal("5.0000")
        )
        assertEquals(ProductProfitabilityClassification.LOW_MARGIN, c3)

        // Break-even: 0%
        val c4 = ProductProfitabilityMathUtils.classifyProfitability(
            recognizedRevenue = BigDecimal("100.0000"),
            totalActualCost = BigDecimal("100.0000"),
            grossMarginPercentage = BigDecimal("0.0000")
        )
        assertEquals(ProductProfitabilityClassification.BREAK_EVEN, c4)

        // Loss: < 0%
        val c5 = ProductProfitabilityMathUtils.classifyProfitability(
            recognizedRevenue = BigDecimal("100.0000"),
            totalActualCost = BigDecimal("120.0000"),
            grossMarginPercentage = BigDecimal("-20.0000")
        )
        assertEquals(ProductProfitabilityClassification.LOSS, c5)
    }

    @Test
    fun testCostVarianceAnalysis() {
        val (varianceClass, values) = ProductProfitabilityMathUtils.calculateVariance(
            actualCost = BigDecimal("9500.0000"),
            baselineCost = BigDecimal("10000.0000")
        )
        assertEquals(ProductVarianceClassification.UNDER_BUDGET, varianceClass)
        assertEquals(BigDecimal("-500.0000"), values.first)
        assertEquals(BigDecimal("-5.0000"), values.second)
    }
}

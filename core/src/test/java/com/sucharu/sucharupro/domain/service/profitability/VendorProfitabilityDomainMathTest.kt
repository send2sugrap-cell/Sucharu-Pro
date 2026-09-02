package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.profitability.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class VendorProfitabilityDomainMathTest {

    @Test
    fun testCostPerJobAndCostPerUnitZeroSafety() {
        val cost = BigDecimal("50000.0000")

        // Non-zero
        val cpj = VendorProfitabilityMathUtils.calculateCostPerJob(cost, 10)
        assertEquals(BigDecimal("5000.0000"), cpj)

        val cpu = VendorProfitabilityMathUtils.calculateCostPerUnit(cost, 500L)
        assertEquals(BigDecimal("100.0000"), cpu)

        // Zero-safe returns null
        assertNull(VendorProfitabilityMathUtils.calculateCostPerJob(cost, 0))
        assertNull(VendorProfitabilityMathUtils.calculateCostPerJob(cost, -1))
        assertNull(VendorProfitabilityMathUtils.calculateCostPerUnit(cost, 0L))
        assertNull(VendorProfitabilityMathUtils.calculateCostPerUnit(cost, -5L))
    }

    @Test
    fun testCostShareAndCostToRevenueContext() {
        val vendorCost = BigDecimal("20000.0000")
        val totalJobCost = BigDecimal("80000.0000")
        val revenueContext = BigDecimal("100000.0000")

        val costShare = VendorProfitabilityMathUtils.calculateCostSharePercentage(vendorCost, totalJobCost)
        assertEquals(BigDecimal("25.0000"), costShare)

        val costToRev = VendorProfitabilityMathUtils.calculateCostToRevenueContextPercentage(vendorCost, revenueContext)
        assertEquals(BigDecimal("20.0000"), costToRev)

        // Zero revenue context returns null
        assertNull(VendorProfitabilityMathUtils.calculateCostSharePercentage(vendorCost, BigDecimal.ZERO))
        assertNull(VendorProfitabilityMathUtils.calculateCostToRevenueContextPercentage(vendorCost, BigDecimal.ZERO))
    }

    @Test
    fun testCostVarianceCalculations() {
        val actual = BigDecimal("55000.0000")
        val baseline = BigDecimal("50000.0000")

        val (varAmt, varPct) = VendorProfitabilityMathUtils.calculateCostVariance(actual, baseline)
        assertEquals(BigDecimal("5000.0000"), varAmt)
        assertEquals(BigDecimal("10.0000"), varPct)

        // Null baseline
        val (nullAmt, nullPct) = VendorProfitabilityMathUtils.calculateCostVariance(actual, null)
        assertNull(nullAmt)
        assertNull(nullPct)
    }

    @Test
    fun testExplainableEfficiencyScore() {
        val breakdown = VendorProfitabilityMathUtils.calculateEfficiencyScoreBreakdown(
            costVariancePercentage = BigDecimal("0.0000"),
            reworkRate = BigDecimal.ZERO,
            qualityFailureRate = BigDecimal.ZERO,
            disputeCount = 0,
            outstandingExposure = BigDecimal("10000.0000"),
            totalVendorCost = BigDecimal("50000.0000")
        )

        assertEquals(BigDecimal("100.0000"), breakdown.totalScore)
        assertTrue(breakdown.explanations.isNotEmpty())

        // Severe penalties
        val penalized = VendorProfitabilityMathUtils.calculateEfficiencyScoreBreakdown(
            costVariancePercentage = BigDecimal("20.0000"), // -10 pts
            reworkRate = BigDecimal("5.0000"), // -12.5 pts
            qualityFailureRate = BigDecimal("2.0000"), // -4 pts
            disputeCount = 2, // -10 pts
            outstandingExposure = BigDecimal("45000.0000"), // high exposure -5 pts
            totalVendorCost = BigDecimal("50000.0000")
        )

        assertTrue(penalized.totalScore < BigDecimal("70.0000"))
    }

    @Test
    fun testRiskClassification() {
        val (lowRisk, _) = VendorProfitabilityMathUtils.classifyRisk(
            efficiencyScore = BigDecimal("95.0000"),
            costVariancePercentage = BigDecimal.ZERO,
            reworkCount = 0,
            disputeCount = 0,
            qualityFailureCount = 0
        )
        assertEquals(VendorRiskClassification.LOW_RISK, lowRisk)

        val (criticalRisk, reasons) = VendorProfitabilityMathUtils.classifyRisk(
            efficiencyScore = BigDecimal("35.0000"),
            costVariancePercentage = BigDecimal("40.0000"),
            reworkCount = 5,
            disputeCount = 3,
            qualityFailureCount = 4
        )
        assertEquals(VendorRiskClassification.CRITICAL_RISK, criticalRisk)
        assertTrue(reasons.isNotEmpty())
    }

    @Test
    fun testDependencyClassification() {
        val (criticalDep, share) = VendorProfitabilityMathUtils.classifyDependency(
            vendorSpend = BigDecimal("30000.0000"),
            totalSpend = BigDecimal("100000.0000")
        )
        assertEquals(VendorDependencyClassification.CRITICAL_DEPENDENCY, criticalDep)
        assertEquals(BigDecimal("30.0000"), share)

        val (lowDep, _) = VendorProfitabilityMathUtils.classifyDependency(
            vendorSpend = BigDecimal("3000.0000"),
            totalSpend = BigDecimal("100000.0000")
        )
        assertEquals(VendorDependencyClassification.LOW_DEPENDENCY, lowDep)
    }

    @Test
    fun testTrendDirection() {
        val current = BigDecimal("45000.0000")
        val previous = BigDecimal("50000.0000")

        // 10% cost reduction is improving
        val trend = VendorProfitabilityMathUtils.determineTrend(current, previous)
        assertEquals(VendorTrendDirection.STRONGLY_IMPROVING, trend)

        // Previous null
        assertEquals(VendorTrendDirection.INSUFFICIENT_DATA, VendorProfitabilityMathUtils.determineTrend(current, null))
    }
}

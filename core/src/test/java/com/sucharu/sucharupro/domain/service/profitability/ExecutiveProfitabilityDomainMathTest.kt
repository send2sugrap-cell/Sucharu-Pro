package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.profitability.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

/**
 * Unit Test Suite for Executive Profitability Mathematics, Scoring & Hashing.
 * Module 16 Step 10.
 */
class ExecutiveProfitabilityDomainMathTest {

    @Test
    fun testMarginCalculation_withSafeZeroHandling() {
        val profit = BigDecimal("2500.0000")
        val revenue = BigDecimal("10000.0000")
        val margin = ExecutiveProfitabilityMathUtils.calculateMargin(profit, revenue)
        assertEquals(BigDecimal("25.0000"), margin)

        // Zero revenue handles safely without division by zero
        val zeroMargin = ExecutiveProfitabilityMathUtils.calculateMargin(profit, BigDecimal.ZERO)
        assertEquals(BigDecimal("0.0000"), zeroMargin)
    }

    @Test
    fun testVarianceCalculation_andDirection() {
        val current = BigDecimal("12000.0000")
        val previous = BigDecimal("10000.0000")

        val varAmt = ExecutiveProfitabilityMathUtils.calculateVariance(current, previous)
        assertEquals(BigDecimal("2000.0000"), varAmt)

        val varPct = ExecutiveProfitabilityMathUtils.calculateVariancePercentage(current, previous)
        assertNotNull(varPct)
        assertEquals(BigDecimal("20.0000"), varPct)

        val direction = ExecutiveProfitabilityMathUtils.determineVarianceDirection(varPct, isHigherBetter = true)
        assertEquals(KpiDirection.IMPROVING, direction)

        // Cost variance: higher is deteriorating
        val costDirection = ExecutiveProfitabilityMathUtils.determineVarianceDirection(varPct, isHigherBetter = false)
        assertEquals(KpiDirection.DETERIORATING, costDirection)
    }

    @Test
    fun testHealthClassification_marginAndScorecard() {
        assertEquals(KpiHealthClassification.EXCELLENT, ExecutiveProfitabilityMathUtils.classifyMarginHealth(BigDecimal("35.0000")))
        assertEquals(KpiHealthClassification.HEALTHY, ExecutiveProfitabilityMathUtils.classifyMarginHealth(BigDecimal("22.5000")))
        assertEquals(KpiHealthClassification.STABLE, ExecutiveProfitabilityMathUtils.classifyMarginHealth(BigDecimal("14.0000")))
        assertEquals(KpiHealthClassification.WATCH, ExecutiveProfitabilityMathUtils.classifyMarginHealth(BigDecimal("6.0000")))
        assertEquals(KpiHealthClassification.WARNING, ExecutiveProfitabilityMathUtils.classifyMarginHealth(BigDecimal("2.0000")))
        assertEquals(KpiHealthClassification.CRITICAL, ExecutiveProfitabilityMathUtils.classifyMarginHealth(BigDecimal("-5.0000")))

        assertEquals(KpiHealthClassification.EXCELLENT, ExecutiveProfitabilityMathUtils.classifyScoreHealth(BigDecimal("88.0000")))
        assertEquals(KpiHealthClassification.HEALTHY, ExecutiveProfitabilityMathUtils.classifyScoreHealth(BigDecimal("72.0000")))
        assertEquals(KpiHealthClassification.STABLE, ExecutiveProfitabilityMathUtils.classifyScoreHealth(BigDecimal("58.0000")))
        assertEquals(KpiHealthClassification.WATCH, ExecutiveProfitabilityMathUtils.classifyScoreHealth(BigDecimal("45.0000")))
        assertEquals(KpiHealthClassification.WARNING, ExecutiveProfitabilityMathUtils.classifyScoreHealth(BigDecimal("30.0000")))
        assertEquals(KpiHealthClassification.CRITICAL, ExecutiveProfitabilityMathUtils.classifyScoreHealth(BigDecimal("15.0000")))
    }

    @Test
    fun testWeightedScore_clampedBetweenZeroAndHundred() {
        val raw = BigDecimal("90.0000")
        val weight = BigDecimal("0.2000")
        val weighted = ExecutiveProfitabilityMathUtils.calculateWeightedScore(raw, weight)
        assertEquals(BigDecimal("18.0000"), weighted)

        val clampedHigh = ExecutiveProfitabilityMathUtils.clampScore(BigDecimal("150.0000"))
        assertEquals(BigDecimal("100.0000"), clampedHigh)

        val clampedLow = ExecutiveProfitabilityMathUtils.clampScore(BigDecimal("-25.0000"))
        assertEquals(BigDecimal("0.0000"), clampedLow)
    }

    @Test
    fun testConcentrationRisk_classification() {
        assertEquals(ForecastRiskLevel.VERY_HIGH, ExecutiveProfitabilityMathUtils.calculateConcentrationRisk(BigDecimal("45.0000"), BigDecimal("85.0000")))
        assertEquals(ForecastRiskLevel.HIGH, ExecutiveProfitabilityMathUtils.calculateConcentrationRisk(BigDecimal("30.0000"), BigDecimal("70.0000")))
        assertEquals(ForecastRiskLevel.MODERATE, ExecutiveProfitabilityMathUtils.calculateConcentrationRisk(BigDecimal("18.0000"), BigDecimal("55.0000")))
        assertEquals(ForecastRiskLevel.LOW, ExecutiveProfitabilityMathUtils.calculateConcentrationRisk(BigDecimal("10.0000"), BigDecimal("35.0000")))
    }

    @Test
    fun testDeterministicSha256_hashingAndFingerprints() {
        val fp1 = ExecutiveProfitabilityMathUtils.generateExecutiveSnapshotFingerprint(
            "tenant-01", "tenant-01", "2026-M09", BigDecimal("10000.0000"), BigDecimal("7000.0000"), BigDecimal("3000.0000")
        )
        val fp2 = ExecutiveProfitabilityMathUtils.generateExecutiveSnapshotFingerprint(
            "tenant-01", "tenant-01", "2026-M09", BigDecimal("10000.0000"), BigDecimal("7000.0000"), BigDecimal("3000.0000")
        )
        assertEquals(fp1, fp2)
        assertEquals(64, fp1.length)

        val hash = ExecutiveProfitabilityMathUtils.generateExecutiveSnapshotIntegrityHash(
            "snp-01", "tenant-01", "tenant-01", "2026-M09", BigDecimal("10000.0000"), BigDecimal("7000.0000"), BigDecimal("3000.0000"),
            BigDecimal("85.0000"), KpiHealthClassification.EXCELLENT, fp1
        )
        assertEquals(64, hash.length)
    }
}

package com.sucharu.sucharupro.domain.model.profitability

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class ProfitabilityIntelligenceDomainMathTest {

    @Test
    fun testScaleMoneyAndPrecision() {
        val amount = BigDecimal("123.456789")
        val scaled = ProfitabilityIntelligenceMathUtils.scaleMoney(amount)
        assertEquals(BigDecimal("123.4568"), scaled)
        assertEquals(4, scaled.scale())
    }

    @Test
    fun testSafeDivideHandlingZeroDenominator() {
        val num = BigDecimal("100.0000")
        val den = BigDecimal("0.0000")
        val res = ProfitabilityIntelligenceMathUtils.safeDivide(num, den)
        assertNull(res)

        val fallback = ProfitabilityIntelligenceMathUtils.safeDivide(num, den, BigDecimal.ZERO)
        assertEquals(BigDecimal.ZERO, fallback)
    }

    @Test
    fun testCalculateGrossProfitAndMargin() {
        val revenue = BigDecimal("100000.0000")
        val cost = BigDecimal("65000.0000")

        val profit = ProfitabilityIntelligenceMathUtils.calculateGrossProfit(revenue, cost)
        assertEquals(BigDecimal("35000.0000"), profit)

        val margin = ProfitabilityIntelligenceMathUtils.calculateGrossMarginPercentage(revenue, cost)
        assertEquals(BigDecimal("35.0000"), margin)
    }

    @Test
    fun testCalculatePercentageChange() {
        val current = BigDecimal("120000.0000")
        val previous = BigDecimal("100000.0000")

        val change = ProfitabilityIntelligenceMathUtils.calculatePercentageChange(current, previous)
        assertEquals(BigDecimal("20.0000"), change)
    }

    @Test
    fun testCalculatePriorityScoreDeterministicBounds() {
        val score = ProfitabilityIntelligenceMathUtils.calculatePriorityScore(
            financialImpact = BigDecimal("50000.0000"),
            totalRevenue = BigDecimal("100000.0000"),
            severityLevel = ManagementPriorityLevel.HIGH,
            trend = PeriodTrendDirection.DECLINING,
            concentrationShare = BigDecimal("25.0000"),
            occurrenceCount = 2
        )

        assertTrue(score >= BigDecimal.ZERO)
        assertTrue(score <= BigDecimal("100.0000"))
        assertEquals(4, score.scale())
    }

    @Test
    fun testCalculateHealthScoreDeterministicBounds() {
        val result = ProfitabilityIntelligenceMathUtils.calculateHealthScore(
            marginPercentage = BigDecimal("35.0000"),
            trendDirection = PeriodTrendDirection.IMPROVING,
            costVariancePct = BigDecimal("3.0000"),
            revenueVariancePct = BigDecimal("4.0000"),
            top1CustomerConcentration = BigDecimal("15.0000"),
            top1VendorConcentration = BigDecimal("20.0000"),
            hasIntegrityIssue = false,
            hasDuplicates = false
        )

        assertTrue(result.overallScore >= BigDecimal.ZERO)
        assertTrue(result.overallScore <= BigDecimal("100.0000"))
        assertEquals(ProfitabilityHealthLevel.EXCELLENT, result.healthLevel)
    }

    @Test
    fun testGenerateProvenanceFingerprintAndIntegrityHash() {
        val fp1 = ProfitabilityIntelligenceMathUtils.generateProvenanceFingerprint(
            tenantId = "TEN-001",
            periodId = "2026-M09",
            sourceModule = "MODULE_16_STEP_04",
            sourceEntityType = "CUSTOMER",
            sourceEntityId = "CUST-001",
            sourceTransactionId = "TX-01",
            metricType = "REVENUE"
        )
        val fp2 = ProfitabilityIntelligenceMathUtils.generateProvenanceFingerprint(
            tenantId = "TEN-001",
            periodId = "2026-M09",
            sourceModule = "MODULE_16_STEP_04",
            sourceEntityType = "CUSTOMER",
            sourceEntityId = "CUST-001",
            sourceTransactionId = "TX-01",
            metricType = "REVENUE"
        )
        assertEquals(fp1, fp2)
        assertEquals(64, fp1.length) // SHA-256 hex length
    }
}
